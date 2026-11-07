package com.ap4k.speedruntimer.client;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class TimerConfig {

    enum Anchor {
        BOTTOM_RIGHT,
        BOTTOM_LEFT,
        TOP_RIGHT,
        TOP_LEFT
    }

    public static final ModConfigSpec SPEC;

    static final ModConfigSpec.BooleanValue ENABLED;
    static final ModConfigSpec.BooleanValue AUTO_START;
    static final ModConfigSpec.BooleanValue SHOW_MILLISECONDS;
    static final ModConfigSpec.BooleanValue HIDE_IN_DEBUG;

    static final ModConfigSpec.EnumValue<Anchor> ANCHOR;
    static final ModConfigSpec.IntValue OFFSET_X;
    static final ModConfigSpec.IntValue OFFSET_Y;

    static final ModConfigSpec.DoubleValue SCALE;
    static final ModConfigSpec.BooleanValue TEXT_SHADOW;
    static final ModConfigSpec.ConfigValue<String> RTA_COLOR;
    static final ModConfigSpec.ConfigValue<String> IGT_COLOR;

    static {
        var builder = new ModConfigSpec.Builder();

        builder.push("general");
        ENABLED = builder.define("enabled", true);
        AUTO_START = builder.define("autoStart", true);
        SHOW_MILLISECONDS = builder.define("showMilliseconds", true);
        HIDE_IN_DEBUG = builder.define("hideInDebugScreen", true);
        builder.pop();

        builder.push("position");
        ANCHOR = builder.defineEnum("anchor", Anchor.BOTTOM_RIGHT);
        OFFSET_X = builder.defineInRange("offsetX", 4, 0, 4096);
        OFFSET_Y = builder.defineInRange("offsetY", 26, 0, 4096);
        builder.pop();

        builder.push("appearance");
        SCALE = builder.defineInRange("scale", 1.0, 0.5, 4.0);
        TEXT_SHADOW = builder.define("textShadow", true);
        RTA_COLOR = builder.comment("RRGGBB или AARRGGBB").define("rtaColor", "5CE1F2");
        IGT_COLOR = builder.comment("RRGGBB или AARRGGBB").define("igtColor", "FFD84D");
        builder.pop();

        SPEC = builder.build();
    }

    private TimerConfig() {
    }

    static int rtaColor() {
        return parseColor(RTA_COLOR.get(), 0xFF5CE1F2);
    }

    static int igtColor() {
        return parseColor(IGT_COLOR.get(), 0xFFFFD84D);
    }

    private static int parseColor(String raw, int fallback) {
        if (raw == null || raw.isEmpty()) {
            return fallback;
        }
        String s = raw.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        try {
            int value = Integer.parseUnsignedInt(s, 16);
            return s.length() <= 6 ? value | 0xFF000000 : value;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
