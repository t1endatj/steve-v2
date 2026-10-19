package com.steve.v2.forge;

import com.steve.v2.core.agent.AgentRuntime;
import com.steve.v2.core.task.*;
import com.steve.v2.forge.entity.SteveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import java.util.*;

@GameTestHolder(value = "steve_v2", namespace = "steve_v2")
public final class MigrationGameTests {
    @GameTest(template = "steve_v2:empty", timeoutTicks = 100)
    public static void spawnAndSave(GameTestHelper helper) {
        SteveEntity steve = spawn(helper, 2, 2);
        UUID owner = UUID.randomUUID(); steve.setOwner(owner); steve.setSteveName("Bob");
        steve.getMemory().record("Built a platform");
        CompoundTag saved = new CompoundTag(); steve.saveWithoutId(saved);
        SteveEntity loaded = SteveMod.STEVE_ENTITY.get().create(helper.getLevel());
        helper.assertTrue(loaded != null, "Entity type creates Steve");
        loaded.load(saved);
        helper.assertTrue(owner.equals(loaded.getOwnerId()), "Owner survives NBT round trip");
        helper.assertTrue(loaded.getSteveName().equals("Bob"), "Name survives NBT round trip");
        helper.assertTrue(loaded.getMemory().history().contains("Built a platform"), "History survives NBT round trip");
        helper.runAtTickTime(5, () -> {
            helper.assertTrue(steve.isAlive(), "Steve ticks without errors");
            helper.assertTrue(steve.getActionExecutor().runtime().state() == AgentRuntime.State.IDLE, "Starts idle without API key");
            helper.succeed();
        });
    }
    @GameTest(template = "steve_v2:empty", timeoutTicks = 100)
    public static void placeBlockThroughRuntime(GameTestHelper helper) {
        SteveEntity steve = spawn(helper, 2, 2);
        BlockPos relative = new BlockPos(3, 1, 2), pos = helper.absolutePos(relative);
        steve.getActionExecutor().submit(new ActionPlan("Place", List.of(new Task("place", Map.of(
            "block", "minecraft:oak_planks", "x", pos.getX(), "y", pos.getY(), "z", pos.getZ())))));
        helper.runAtTickTime(10, () -> {
            helper.assertBlockPresent(Blocks.OAK_PLANKS, relative);
            helper.assertTrue(steve.getActionExecutor().runtime().state() == AgentRuntime.State.IDLE, "Placement completes");
            helper.succeed();
        });
    }
    @GameTest(template = "steve_v2:empty", timeoutTicks = 100)
    public static void mineBlockThroughRuntime(GameTestHelper helper) {
        SteveEntity steve = spawn(helper, 2, 2);
        helper.setBlock(3, 1, 2, Blocks.IRON_ORE);
        steve.getActionExecutor().submit(new ActionPlan("Mine", List.of(new Task("mine", Map.of("block", "iron", "quantity", 1)))));
        helper.runAtTickTime(20, () -> {
            helper.assertBlockNotPresent(Blocks.IRON_ORE, new BlockPos(3, 1, 2));
            helper.assertTrue(steve.getActionExecutor().runtime().state() == AgentRuntime.State.IDLE, "Mining completes");
            helper.succeed();
        });
    }
    @GameTest(template = "steve_v2:empty", timeoutTicks = 150)
    public static void collaborativeBuildCompletes(GameTestHelper helper) {
        SteveEntity a = spawn(helper, 2, 2), b = spawn(helper, 3, 2);
        BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 4));
        Task task = new Task("build", Map.of("structure", "platform", "blocks", List.of("stone"),
            "dimensions", List.of(8, 1, 8), "x", pos.getX(), "y", pos.getY(), "z", pos.getZ()));
        a.getActionExecutor().submit(new ActionPlan("Build platform", List.of(task)));
        b.getActionExecutor().submit(new ActionPlan("Build platform", List.of(task)));
        helper.runAtTickTime(100, () -> {
            for (int x = 4; x < 12; x++) for (int z = 4; z < 12; z++) helper.assertBlockPresent(Blocks.STONE, new BlockPos(x, 1, z));
            helper.assertTrue(a.getActionExecutor().runtime().state() == AgentRuntime.State.IDLE, "First worker completes");
            helper.assertTrue(b.getActionExecutor().runtime().state() == AgentRuntime.State.IDLE, "Second worker completes");
            helper.succeed();
        });
    }
    private static SteveEntity spawn(GameTestHelper helper, int x, int z) {
        for (int px = 1; px < 14; px++) for (int pz = 1; pz < 14; pz++) helper.setBlock(px, 0, pz, Blocks.STONE);
        return helper.spawn(SteveMod.STEVE_ENTITY.get(), new BlockPos(x, 1, z));
    }

    @GameTest(template = "steve_v2:empty", timeoutTicks = 240, batch = "follow_flat")
    public static void followActuallyWalks(GameTestHelper helper) {
        SteveEntity steve = spawn(helper, 2, 7);
        var player = helper.makeMockServerPlayerInLevel();
        var target = helper.absolutePos(new BlockPos(12, 1, 7));
        player.setPos(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        steve.setOwner(player.getUUID());
        var start = steve.position();
        steve.getActionExecutor().submit(new ActionPlan("Follow", List.of(new Task("follow", Map.of("player", player.getName().getString())))));
        helper.runAtTickTime(180, () -> {
            helper.assertTrue(steve.position().distanceTo(start) > 4, "Steve must actually walk, not just report EXECUTING");
            helper.assertTrue(steve.distanceTo(player) < 4, "Steve reaches the player on flat ground");
            steve.getActionExecutor().stopCurrentAction();
            player.discard();
            helper.succeed();
        });
    }

    @GameTest(template = "steve_v2:empty", timeoutTicks = 900, batch = "build_then_follow")
    public static void singleHouseBuildCompletes(GameTestHelper helper) {
        SteveEntity steve = spawn(helper, 2, 2);
        BlockPos pos = helper.absolutePos(new BlockPos(4, 1, 4));
        Task task = new Task("build", Map.of("structure", "house", "blocks", List.of("oak_planks", "cobblestone", "glass"),
            "dimensions", List.of(7, 4, 7), "x", pos.getX(), "y", pos.getY(), "z", pos.getZ()));
        steve.getActionExecutor().submit(new ActionPlan("Build house", List.of(task)));
        helper.runAtTickTime(550, () -> {
            helper.assertTrue(steve.getActionExecutor().runtime().state() == AgentRuntime.State.IDLE,
                "Single worker completes house without a template: " + steve.getActionExecutor().runtime().state() + " " + steve.getMemory().history());
            helper.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(4, 1, 4));
            helper.assertBlockProperty(new BlockPos(7, 2, 4), net.minecraft.world.level.block.DoorBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
            helper.assertBlockProperty(new BlockPos(7, 3, 4), net.minecraft.world.level.block.DoorBlock.HALF,
                net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
            var player = helper.makeMockServerPlayerInLevel();
            var target = helper.absolutePos(new BlockPos(2, 1, 2));
            player.setPos(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
            steve.setOwner(player.getUUID());
            follow(steve, player);
            helper.runAfterDelay(180, () -> {
                helper.assertTrue(steve.distanceTo(player) < 4 && !steve.isInWall(), "Steve follows again after finishing a house");
                steve.getActionExecutor().stopCurrentAction();
                player.discard();
                helper.succeed();
            });
        });
    }

    @GameTest(template = "steve_v2:empty", timeoutTicks = 430, batch = "follow_door")
    public static void followsThroughDoorAndAcceptsFollowAgain(GameTestHelper helper) {
        SteveEntity steve = spawn(helper, 2, 7);
        var player = helper.makeMockServerPlayerInLevel();
        steve.setOwner(player.getUUID());
        for (int x = 0; x < 16; x++) for (int y = 1; y <= 4; y++) {
            helper.setBlock(x, y, 6, Blocks.STONE); helper.setBlock(x, y, 8, Blocks.STONE);
        }
        for (int y = 1; y <= 4; y++) { helper.setBlock(0, y, 7, Blocks.STONE); helper.setBlock(15, y, 7, Blocks.STONE); }
        var door = Blocks.OAK_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, net.minecraft.core.Direction.EAST);
        helper.setBlock(new BlockPos(6, 1, 7), door);
        helper.setBlock(new BlockPos(6, 2, 7), door.setValue(net.minecraft.world.level.block.DoorBlock.HALF,
            net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        var target = helper.absolutePos(new BlockPos(12, 1, 7));
        player.setPos(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        follow(steve, player);
        helper.runAtTickTime(180, () -> {
            helper.assertTrue(steve.distanceTo(player) < 4, "Steve passes the closed wooden door");
            helper.assertTrue(!steve.getActionExecutor().runtime().currentAction().contains("recoveries="), "Door route must work by walking without a teleport");
            var returnTarget = helper.absolutePos(new BlockPos(2, 1, 7));
            player.setPos(returnTarget.getX() + 0.5, returnTarget.getY(), returnTarget.getZ() + 0.5);
            follow(steve, player);
        });
        helper.runAtTickTime(360, () -> {
            helper.assertTrue(steve.distanceTo(player) < 4, "Repeated follow command moves Steve back through the doorway");
            helper.assertTrue(!steve.getActionExecutor().runtime().currentAction().contains("recoveries="), "Repeated follow still walks");
            steve.getActionExecutor().stopCurrentAction(); player.discard(); helper.succeed();
        });
    }
    @GameTest(template = "steve_v2:empty", timeoutTicks = 430, batch = "follow_blocked")
    public static void recoversFromSealedRoomWithoutBreakingWalls(GameTestHelper helper) {
        SteveEntity steve = spawn(helper, 3, 3);
        for (int y = 1; y <= 4; y++) for (int n = 1; n <= 5; n++) {
            helper.setBlock(1, y, n, Blocks.STONE); helper.setBlock(5, y, n, Blocks.STONE);
            helper.setBlock(n, y, 1, Blocks.STONE); helper.setBlock(n, y, 5, Blocks.STONE);
        }
        for (int x = 1; x <= 5; x++) for (int z = 1; z <= 5; z++) helper.setBlock(x, 4, z, Blocks.STONE);
        var player = helper.makeMockServerPlayerInLevel();
        var target = helper.absolutePos(new BlockPos(12, 1, 10));
        player.setPos(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        steve.setOwner(player.getUUID()); follow(steve, player);
        helper.runAtTickTime(180, () -> {
            helper.assertTrue(steve.distanceTo(player) < 4 && !steve.isInWall(), "Blocked assistant reaches safe ground near player");
            helper.assertTrue(steve.getActionExecutor().runtime().currentAction().contains("recoveries="), "Recovery was actually exercised");
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(5, 1, 3));
            steve.getActionExecutor().stopCurrentAction();
            var inside = helper.absolutePos(new BlockPos(3, 1, 3));
            steve.teleportTo(inside.getX() + 0.5, inside.getY(), inside.getZ() + 0.5);
            helper.runAfterDelay(180, () -> {
                helper.assertTrue(steve.getActionExecutor().runtime().state() == AgentRuntime.State.IDLE,
                    "Idle follow uses the same recovery without an explicit follow task");
                helper.assertTrue(steve.distanceTo(player) < 4 && !steve.isInWall(), "Idle assistant also recovers from the sealed room");
                player.discard(); helper.succeed();
            });
        });
    }

    @GameTest(template = "steve_v2:empty", timeoutTicks = 280, batch = "follow_flying")
    public static void followsFlyingPlayerOnSafeGround(GameTestHelper helper) {
        SteveEntity steve = spawn(helper, 2, 2);
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        player.getAbilities().flying = true;
        var target = helper.absolutePos(new BlockPos(12, 10, 12));
        player.setPos(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
        steve.setOwner(player.getUUID()); follow(steve, player);
        helper.runAtTickTime(200, () -> {
            double horizontal = Math.hypot(steve.getX() - player.getX(), steve.getZ() - player.getZ());
            helper.assertTrue(horizontal <= 4, "Steve follows the flying player horizontally");
            helper.assertTrue(steve.getY() < player.getY() - 3 && !steve.isInWall(), "Recovery never places Steve unsupported in the air");
            steve.getActionExecutor().stopCurrentAction(); player.discard(); helper.succeed();
        });
    }
    @GameTest(template = "steve_v2:empty", timeoutTicks = 100, batch = "status_commands")
    public static void statusSelectsActualOwnerAndAlwaysReplies(GameTestHelper helper) {
        SteveEntity steve = spawn(helper, 2, 2);
        steve.setSteveName("status_datmini");
        var player = helper.makeMockServerPlayerInLevel();
        steve.setOwner(player.getUUID());
        var dispatcher = helper.getLevel().getServer().getCommands().getDispatcher();
        var messages = new java.util.ArrayList<String>();
        var silentFeedback = new net.minecraft.commands.CommandSource() {
            public void sendSystemMessage(net.minecraft.network.chat.Component text) { messages.add(text.getString()); }
            public boolean acceptsSuccess() { return false; }
            public boolean acceptsFailure() { return true; }
            public boolean shouldInformAdmins() { return false; }
        };
        try {
            var owned = player.createCommandSourceStack().withPermission(0);
            helper.assertTrue(dispatcher.execute("steve status", owned) == 1, "Unnamed status resolves the player's Steve");
            helper.assertTrue(dispatcher.execute("steve follow", owned) == 1, "Unnamed follow resolves the player's Steve");
            var console = helper.getLevel().getServer().createCommandSourceStack().withLevel(helper.getLevel()).withSource(silentFeedback);
            helper.assertTrue(dispatcher.execute("steve status status_datmini", console) == 1, "Named status succeeds");
            helper.assertTrue(dispatcher.execute("steve status status_datmini", console) == 1, "Repeated status succeeds");
            helper.assertTrue(messages.size() == 2 && messages.stream().allMatch(s -> s.startsWith("status_datmini:")),
                "Every status replies even when ordinary command success feedback is disabled");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) { throw new RuntimeException(e); }
        steve.getActionExecutor().stopCurrentAction(); player.discard(); helper.succeed();
    }
    private static void follow(SteveEntity steve, net.minecraft.server.level.ServerPlayer player) {
        steve.getActionExecutor().submit(new ActionPlan("Follow", List.of(new Task("follow", Map.of(
            "player", player.getName().getString(), "playerId", player.getUUID().toString())))));
    }
}
