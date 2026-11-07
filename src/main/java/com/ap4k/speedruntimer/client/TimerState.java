package com.ap4k.speedruntimer.client;

import net.minecraft.util.Mth;

final class TimerState {

    private static boolean inWorld;
    private static boolean running;
    private static long startedAt;
    private static long accumulated;
    private static long ticks;

    private TimerState() {
    }

    static void onWorldJoin() {
        inWorld = true;
        reset();
        if (TimerConfig.AUTO_START.get()) {
            resume();
        }
    }

    static void onWorldLeave() {
        pause();
        inWorld = false;
    }

    static void reset() {
        running = false;
        startedAt = 0L;
        accumulated = 0L;
        ticks = 0L;
    }

    static void restart() {
        reset();
        if (inWorld) {
            resume();
        }
    }

    static void resume() {
        if (running) {
            return;
        }
        startedAt = System.nanoTime();
        running = true;
    }

    static void pause() {
        if (!running) {
            return;
        }
        accumulated += System.nanoTime() - startedAt;
        startedAt = 0L;
        running = false;
    }

    static boolean togglePause() {
        if (running) {
            pause();
        } else if (inWorld) {
            resume();
        }
        return running;
    }

    static boolean isRunning() {
        return running;
    }

    static boolean isInWorld() {
        return inWorld;
    }

    static void tick(boolean worldRunning) {
        if (worldRunning && running && inWorld) {
            ticks++;
        }
    }

    static long rtaMillis() {
        return (accumulated + (running ? System.nanoTime() - startedAt : 0L)) / 1_000_000L;
    }

    static long igtMillis(float partial) {
        double total = ticks;
        if (running) {
            total += Mth.clamp(partial, 0F, 0.999F);
        }
        // TODO: 50мс на тик зашиты, tickrate из датапаков не учитываем
        return (long) (total * 50.0);
    }
}
