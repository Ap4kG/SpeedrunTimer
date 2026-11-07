package com.ap4k.speedruntimer.client;

final class TimerState {

    private static boolean inWorld;
    private static boolean running;
    private static long startedAt;
    private static long accumulated;
    private static double igtAccum;

    private TimerState() {
    }

    static void onWorldJoin() {
        inWorld = true;
        // всегда стартуем с нуля и на паузе: первый тик мира не должен съесть время
        reset();
    }

    static void onWorldLeave() {
        pause();
        inWorld = false;
    }

    static void reset() {
        running = false;
        startedAt = 0L;
        accumulated = 0L;
        igtAccum = 0.0;
    }

    static void restore(long rtaMs, long igtMs) {
        reset();
        accumulated = rtaMs * 1_000_000L;
        igtAccum = igtMs;
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

    /** Таймер на нуле и не идёт — можно стартовать по первому вводу. */
    static boolean isArmed() {
        return !running && accumulated == 0L && igtAccum <= 0.0;
    }

    static void tick(boolean worldRunning, float msPerTick) {
        if (worldRunning && running && inWorld) {
            igtAccum += msPerTick;
        }
    }

    static long rtaMillis() {
        return (accumulated + (running ? System.nanoTime() - startedAt : 0L)) / 1_000_000L;
    }

    static long igtMillis() {
        return (long) igtAccum;
    }

    static long igtMillis(float partial, float msPerTick) {
        double total = igtAccum;
        if (running) {
            // тик мира ещё не прошёл — добавляем его долю, иначе IGT дёргается
            total += msPerTick * Math.min(0.999F, Math.max(0F, partial));
        }
        return (long) total;
    }
}
