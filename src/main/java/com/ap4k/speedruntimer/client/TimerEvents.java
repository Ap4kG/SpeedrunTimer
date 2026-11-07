package com.ap4k.speedruntimer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class TimerEvents {

    private static final long SAVE_INTERVAL_MS = 5_000L;

    private static boolean restorePending;
    private static long lastSaveAt;

    private TimerEvents() {
    }

    public static void init() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(ClientTickEvent.Post.class, e -> tick());
        bus.addListener(ClientPlayerNetworkEvent.LoggingIn.class, e -> join());
        bus.addListener(ClientPlayerNetworkEvent.LoggingOut.class, e -> leave());
        bus.addListener(ScreenEvent.MouseButtonPressed.Pre.class, TimerHudLayer::onMousePressed);
        bus.addListener(ScreenEvent.MouseButtonReleased.Pre.class, TimerHudLayer::onMouseReleased);
    }

    private static void join() {
        TimerState.onWorldJoin();
        restorePending = true;
    }

    private static void leave() {
        TimerHudLayer.cancelDrag();
        TimerState.onWorldLeave();
        restorePending = false;
        TimerPersistence.save(Minecraft.getInstance());
    }

    private static void tick() {
        var mc = Minecraft.getInstance();

        if (mc.level != null) {
            // LoggingIn иногда не долетает (респавн, быстрый /reconnect), ловим по уровню
            if (!TimerState.isInWorld()) {
                join();
            }
            if (restorePending) {
                restoreInterruptedRun(mc);
            }

            var tickRate = mc.level.tickRateManager();
            handleAutoStart(mc);
            TimerState.tick(!mc.isPaused() && tickRate.runsNormally(), tickRate.millisecondsPerTick());
            persistPeriodically(mc);
        } else if (TimerState.isInWorld()) {
            leave();
        }

        if (TimerKeys.RESET.consumeClick()) {
            TimerState.restart();
            TimerPersistence.save(mc);
            msg("msg.speedruntimer.reset");
        }
        if (TimerKeys.PAUSE.consumeClick()) {
            boolean resumed = TimerState.togglePause();
            TimerPersistence.save(mc);
            msg(resumed ? "msg.speedruntimer.resumed" : "msg.speedruntimer.paused");
        }
        if (TimerKeys.TOGGLE.consumeClick()) {
            boolean show = !TimerConfig.ENABLED.get();
            TimerConfig.ENABLED.set(show);
            msg(show ? "msg.speedruntimer.shown" : "msg.speedruntimer.hidden");
        }
    }

    private static void restoreInterruptedRun(Minecraft mc) {
        String world = TimerPersistence.worldKey(mc);
        if (world == null) {
            // сервер ещё поднимается — попробуем на следующем тике
            return;
        }
        restorePending = false;

        var snapshot = TimerPersistence.loadUnfinished(mc, world);
        if (snapshot == null) {
            return;
        }

        TimerState.restore(snapshot.rtaMs(), snapshot.igtMs());
        TimerPersistence.save(mc);
        msg(Component.translatable("msg.speedruntimer.restored",
                TimerHudLayer.clock(snapshot.rtaMs()),
                TimerHudLayer.clock(snapshot.igtMs())));
    }

    /** Стоим на нуле — стартуем от первого действия, как на спидран-оверлеях. */
    private static void handleAutoStart(Minecraft mc) {
        if (!TimerConfig.START_ON_FIRST_INPUT.get() || !TimerState.isArmed()) {
            return;
        }
        if (mc.player == null || mc.gui.screen() != null) {
            return;
        }

        var options = mc.options;
        boolean pressed = options.keyUp.isDown()
                || options.keyDown.isDown()
                || options.keyLeft.isDown()
                || options.keyRight.isDown()
                || options.keyJump.isDown()
                || options.keyShift.isDown()
                || options.keySprint.isDown()
                || options.keyAttack.isDown()
                || options.keyUse.isDown();
        if (pressed) {
            TimerState.resume();
            TimerPersistence.save(mc);
        }
    }

    private static void persistPeriodically(Minecraft mc) {
        if (!TimerState.isRunning()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastSaveAt < SAVE_INTERVAL_MS) {
            return;
        }
        lastSaveAt = now;
        TimerPersistence.save(mc);
    }

    private static void msg(String key) {
        msg(Component.translatable(key));
    }

    private static void msg(Component component) {
        var mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.hud.setOverlayMessage(component, false);
        }
    }
}
