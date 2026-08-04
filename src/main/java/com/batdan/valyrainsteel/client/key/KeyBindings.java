package com.batdan.valyrainsteel.client.key;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {

    public static final String CATEGORY =
            "key.category.valyrainsteel";

    public static final KeyMapping OPEN_REPUTATION =
            new KeyMapping(
                    "key.valyrainsteel.reputation",
                    GLFW.GLFW_KEY_R,
                    CATEGORY
            );

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_REPUTATION);
    }
}