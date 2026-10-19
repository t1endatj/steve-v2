package com.steve.v2.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.steve.v2.core.task.*;
import com.steve.v2.forge.SteveMod;
import com.steve.v2.forge.config.SteveConfig;
import com.steve.v2.forge.entity.SteveEntity;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public final class SteveCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("steve");
        root.then(Commands.literal("spawn").then(Commands.argument("name", StringArgumentType.word()).executes(SteveCommands::spawn)));
        root.then(Commands.literal("list").executes(c -> {
            reply(c.getSource(), "Loaded Steves: " + names(agents(c.getSource())));
            return 1;
        }));
        root.then(Commands.literal("tell").then(named()
            .then(Commands.argument("command", StringArgumentType.greedyString()).executes(c -> withSteve(c,
                s -> s.getActionExecutor().processNaturalLanguageCommand(StringArgumentType.getString(c, "command")))))));
        for (String operation : List.of("stop", "pause", "resume", "remove", "status", "follow")) {
            root.then(Commands.literal(operation).executes(c -> operate(c, operation))
                .then(named().executes(c -> operate(c, operation))));
        }
        root.then(Commands.literal("build").then(named()
            .then(Commands.argument("structure", StringArgumentType.word()).executes(SteveCommands::build))));
        dispatcher.register(root);
    }
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> named() {
        return Commands.argument("name", StringArgumentType.word()).suggests((c, b) ->
            SharedSuggestionProvider.suggest(agents(c.getSource()).stream().filter(s -> canControl(c.getSource(), s))
                .map(SteveEntity::getSteveName), b));
    }
    private static int operate(CommandContext<CommandSourceStack> c, String operation) {
        return withSteve(c, s -> {
            var executor = s.getActionExecutor();
            switch (operation) {
                case "stop" -> { executor.stopCurrentAction(); reply(c.getSource(), "Stopped " + s.getSteveName()); }
                case "pause" -> { executor.pause(); reply(c.getSource(), "Paused " + s.getSteveName()); }
                case "resume" -> { executor.resume(); reply(c.getSource(), "Resumed " + s.getSteveName()); }
                case "remove" -> { s.discard(); reply(c.getSource(), "Removed " + s.getSteveName()); }
                case "status" -> {
                    var r = executor.runtime();
                    var player = c.getSource().getEntity();
                    String distance = player == null ? "" : ", distance=" + Math.round(s.distanceTo(player)) + "m";
                    reply(c.getSource(), s.getSteveName() + ": " + r.state() + ", goal=" + r.goal()
                        + ", action=" + r.currentAction() + ", queued=" + r.queuedTasks()
                        + ", pos=" + s.blockPosition().toShortString() + distance);
                }
                case "follow" -> {
                    if (!(c.getSource().getEntity() instanceof ServerPlayer player))
                        throw new IllegalArgumentException("Run follow as a player");
                    executor.submit(new ActionPlan("Follow player", List.of(new Task("follow", Map.of(
                        "player", player.getName().getString(), "playerId", player.getUUID().toString())))));
                    reply(c.getSource(), s.getSteveName() + " is following " + player.getName().getString());
                }
            }
        });
    }
    private static int build(CommandContext<CommandSourceStack> c) {
        String structure = StringArgumentType.getString(c, "structure");
        Task task = new Task("build", Map.of("structure", structure,
            "blocks", List.of("oak_planks", "cobblestone", "glass"), "dimensions", List.of(9, 6, 9)));
        return withSteve(c, s -> s.getActionExecutor().submit(new ActionPlan("Build " + structure, List.of(task))));
    }
    private static int spawn(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(c, "name");
        if (name.length() > 32 || agents(c.getSource()).stream().anyMatch(s -> s.getSteveName().equalsIgnoreCase(name))) {
            c.getSource().sendFailure(Component.literal("Name is too long or already exists in this dimension")); return 0;
        }
        if (agents(c.getSource()).stream().filter(s -> player.getUUID().equals(s.getOwnerId())).count() >= SteveConfig.MAX_STEVES.get()) {
            c.getSource().sendFailure(Component.literal("Steve limit reached")); return 0;
        }
        var steve = SteveMod.STEVE_ENTITY.get().create(c.getSource().getLevel());
        if (steve == null) return 0;
        steve.setSteveName(name); steve.setOwner(player.getUUID());
        steve.setPos(player.getX() + 2, player.getY(), player.getZ());
        if (!c.getSource().getLevel().addFreshEntity(steve)) return 0;
        reply(c.getSource(), "Spawned " + name);
        return 1;
    }
    private static int withSteve(CommandContext<CommandSourceStack> c, java.util.function.Consumer<SteveEntity> work) {
        String requested;
        try { requested = StringArgumentType.getString(c, "name"); }
        catch (IllegalArgumentException missing) { requested = ""; }
        var loaded = agents(c.getSource());
        var controlled = loaded.stream().filter(s -> canControl(c.getSource(), s)).toList();
        final String name = requested;
        var matches = name.isBlank() ? controlled : loaded.stream().filter(s -> s.getSteveName().equalsIgnoreCase(name)).toList();
        if (matches.size() != 1) {
            String problem = name.isBlank() ? "Specify a Steve name" : "Steve '" + name + "' not found or ambiguous";
            c.getSource().sendFailure(Component.literal(problem + ". Available in this dimension: " + names(controlled)));
            return 0;
        }
        var steve = matches.getFirst();
        if (!canControl(c.getSource(), steve)) {
            c.getSource().sendFailure(Component.literal("Only the owner or an operator can control this Steve")); return 0;
        }
        try { work.accept(steve); return 1; }
        catch (IllegalArgumentException e) { c.getSource().sendFailure(Component.literal(e.getMessage())); return 0; }
    }
    private static boolean canControl(CommandSourceStack source, SteveEntity steve) {
        return source.hasPermission(2) || (source.getEntity() instanceof ServerPlayer p && p.getUUID().equals(steve.getOwnerId()));
    }
    private static String names(List<SteveEntity> steves) {
        return steves.isEmpty() ? "(none loaded)" : String.join(", ", steves.stream().map(SteveEntity::getSteveName).toList());
    }
    private static void reply(CommandSourceStack source, String text) {
        // Unlike sendSuccess, requested status must not disappear when sendCommandFeedback is false.
        source.sendSystemMessage(Component.literal(text));
    }
    private static List<SteveEntity> agents(CommandSourceStack source) {
        List<SteveEntity> list = new ArrayList<>();
        for (var entity : source.getLevel().getAllEntities()) if (entity instanceof SteveEntity s && s.isAlive() && !s.isRemoved()) list.add(s);
        return list;
    }
    private SteveCommands() {}
}
