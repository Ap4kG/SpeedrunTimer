package com.ap4k.speedruntimer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class TimerEvents {

    private TimerEvents() {
    }

    public static void init() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(ClientTickEvent.Post.class, e -> tick());
        bus.addListener(ClientPlayerNetworkEvent.LoggingIn.class, e -> TimerState.onWorldJoin());
        bus.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e -> TimerState.onWorldLeave());
        bus.addListener(ScreenEvent.MouseButtonPressed.Pre.class, TimerHudLayer::onMousePressed);
        bus.addListener(ScreenEvent.MouseButtonReleased.Pre.class, TimerHudLayer::onMouseReleased);
    }

    private static void tick() {
        var mc = Minecraft.getInstance();

        if (mc.level != null) {
            // LoggingIn иногда не долетает (респавн, быстрый /reconnect), ловим по уровню
            if (!TimerState.isInWorld()) {
                TimerState.onWorldJoin();
            }
            TimerState.tick(!mc.isPaused() && mc.level.tickRateManager().runsNormally());
        }

        if (TimerKeys.RESET.consumeClick()) {
            TimerState.restart();
            msg("msg.speedruntimer.reset");
        }
        if (TimerKeys.PAUSE.consumeClick()) {
            msg(TimerState.togglePause() ? "msg.speedruntimer.resumed" : "msg.speedruntimer.paused");
        }
        if (TimerKeys.TOGGLE.consumeClick()) {
            boolean show = !TimerConfig.ENABLED.get();
            TimerConfig.ENABLED.set(show);
            msg(show ? "msg.speedruntimer.shown" : "msg.speedruntimer.hidden");
        }
    }

    private static void msg(String key) {
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.hud.setOverlayMessage(Component.translatable(key), false);
        }
    }
}
