package com.batdan.valyrainsteel.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ReputationScreen extends Screen {

    public ReputationScreen() {
        super(Component.literal("Reputation"));
    }

    @Override
    protected void init() {

    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        this.renderBackground(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(
                font,
                "Reputation",
                width / 2,
                20,
                0xFFFFFF
        );

        graphics.drawString(
                font,
                "Current Reputation: 7000",
                width / 2 - 80,
                60,
                0xFFFFFF
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}