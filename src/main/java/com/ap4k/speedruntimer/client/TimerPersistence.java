package com.ap4k.speedruntimer.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

/**
 * Слепок забега на диск: если игра упадёт, таймер не обнулится при следующем заходе в тот же мир.
 */
final class TimerPersistence {

    record Snapshot(long rtaMs, long igtMs) {
    }

    private static final String FILE_NAME = "speedruntimer-run.properties";

    /** Последний известный мир: на выходе сервер уже может быть не виден, а сохранить надо. */
    private static String lastWorld;

    private TimerPersistence() {
    }

    private static Path file(Minecraft mc) {
        return mc.gameDirectory.toPath().resolve("config").resolve(FILE_NAME);
    }

    /** Устойчивый ключ мира: имя локального мира или адрес сервера. */
    static String worldKey(Minecraft mc) {
        var integrated = mc.getSingleplayerServer();
        if (integrated != null) {
            lastWorld = "sp:" + integrated.getWorldData().getLevelName();
            return lastWorld;
        }
        ServerData remote = mc.getCurrentServer();
        if (remote != null) {
            lastWorld = "mp:" + remote.ip;
            return lastWorld;
        }
        return null;
    }

    static void save(Minecraft mc) {
        String world = worldKey(mc);
        if (world == null) {
            world = lastWorld;
        }
        if (world == null) {
            return;
        }
        save(file(mc), world, TimerState.rtaMillis(), TimerState.igtMillis(), TimerState.isRunning());
    }

    /** Возвращает забег, который оборвался (игра упала) в этом же мире. */
    static Snapshot loadUnfinished(Minecraft mc, String world) {
        return loadUnfinished(file(mc), world);
    }

    static void save(Path path, String world, long rtaMs, long igtMs, boolean running) {
        Properties props = new Properties();
        props.setProperty("world", world);
        props.setProperty("rtaMs", Long.toString(rtaMs));
        props.setProperty("igtMs", Long.toString(igtMs));
        props.setProperty("running", Boolean.toString(running));
        props.setProperty("savedAt", Long.toString(System.currentTimeMillis()));

        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (OutputStream out = Files.newOutputStream(tmp)) {
                props.store(out, "Speedrun Timer run state");
            }
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            // конфиг-папка недоступна — забег просто не переживёт краш, игру не ломаем
        }
    }

    static Snapshot loadUnfinished(Path path, String world) {
        if (!Files.isRegularFile(path)) {
            return null;
        }

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            return null;
        }

        if (!world.equals(props.getProperty("world"))) {
            return null;
        }
        // running=true в файле остаётся только если игру закрыли аварийно
        if (!Boolean.parseBoolean(props.getProperty("running"))) {
            return null;
        }

        try {
            long rta = Long.parseLong(props.getProperty("rtaMs", "0"));
            long igt = Long.parseLong(props.getProperty("igtMs", "0"));
            if (rta <= 0L && igt <= 0L) {
                return null;
            }
            return new Snapshot(rta, igt);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
