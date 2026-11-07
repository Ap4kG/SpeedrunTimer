package com.ap4k.speedruntimer.client;

import com.ap4k.speedruntimer.SpeedrunTimer;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class TimerKeys {

    private static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(SpeedrunTimer.id("timer"));

    // F8 / F9 / F10
    static final KeyMapping RESET = new KeyMapping("key.speedruntimer.reset",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_F8, CATEGORY);
    static final KeyMapping PAUSE = new KeyMapping("key.speedruntimer.pause",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_F9, CATEGORY);
    static final KeyMapping TOGGLE = new KeyMapping("key.speedruntimer.toggle",
            InputConstants.Type.KEYBOARD, InputConstants.KEY_F10, CATEGORY);

    private TimerKeys() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(RESET);
        event.register(PAUSE);
        event.register(TOGGLE);
    }
}
