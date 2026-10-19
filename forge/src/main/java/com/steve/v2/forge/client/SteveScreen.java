package com.steve.v2.forge.client;

import com.steve.v2.core.agent.AgentSelection;
import com.steve.v2.forge.entity.SteveEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Send ordinary server commands; never access the integrated server or LLM keys from the client. */
public final class SteveScreen extends Screen {
    private static String lastName = "";
    private EditBox name, command;
    private String hint = "Blank name: use your only Steve";
    public SteveScreen() { super(Component.literal("Steve V2")); }
    @Override protected void init() {
        int x = width / 2 - 150, y = height / 2 - 75;
        name = new EditBox(font, x, y + 20, 190, 20, Component.literal("Steve name"));
        name.setMaxLength(32);
        name.setFilter(value -> value.matches("[a-zA-Z0-9_-]*"));
        var visible = new java.util.ArrayList<String>();
        if (minecraft != null && minecraft.level != null) {
            for (var entity : minecraft.level.entitiesForRendering()) {
                if (entity instanceof SteveEntity steve && !visible.contains(steve.getSteveName())) visible.add(steve.getSteveName());
            }
        }
        name.setValue(AgentSelection.choose(lastName, visible));
        name.setHint(Component.literal("Steve name (optional for status/follow)"));
        name.setResponder(value -> lastName = value);
        addRenderableWidget(name);
        addRenderableWidget(Button.builder(Component.literal("Spawn"), b -> send("spawn", false)).bounds(x + 200, y + 20, 100, 20).build());
        command = new EditBox(font, x, y + 54, 300, 20, Component.literal("Task"));
        command.setMaxLength(1024);
        command.setHint(Component.literal("Tell Steve what to do..."));
        addRenderableWidget(command);
        addRenderableWidget(Button.builder(Component.literal("Send task"), b -> send("tell", true)).bounds(x, y + 84, 145, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Status"), b -> send("status", false)).bounds(x + 155, y + 84, 145, 20).build());
        String[] operations = {"stop", "pause", "resume", "follow"};
        for (int i = 0; i < operations.length; i++) {
            String operation = operations[i];
            addRenderableWidget(Button.builder(Component.literal(operation), b -> send(operation, false)).bounds(x + i * 77, y + 110, 69, 20).build());
        }
        setInitialFocus(command);
    }
    private void send(String operation, boolean task) {
        if (minecraft == null || minecraft.player == null) return;
        if (task && command.getValue().isBlank()) return;
        lastName = name.getValue().strip();
        if ((task || operation.equals("spawn")) && lastName.isEmpty()) {
            hint = "Enter or select a Steve name first";
            setFocused(name);
            return;
        }
        minecraft.player.connection.sendCommand("steve " + operation + (lastName.isEmpty() ? "" : " " + lastName) + (task ? " " + command.getValue().strip() : ""));
        // Feedback is delivered through ordinary private server chat.
        onClose();
    }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { send("tell", true); return true; }
        return super.keyPressed(key, scan, modifiers);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() {
        if (name != null) lastName = name.getValue().strip();
        super.onClose();
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);
        int x = width / 2 - 150, y = height / 2 - 75;
        graphics.drawString(font, "Steve V2", x, y, 0xFFFFFF);
        graphics.drawString(font, "Server feedback appears in chat", x, y + 140, 0xBBBBBB);
        graphics.drawString(font, hint, x, y + 153, 0xBBBBBB);
    }
}
