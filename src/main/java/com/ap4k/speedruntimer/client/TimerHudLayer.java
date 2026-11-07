package com.ap4k.speedruntimer.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.client.event.ScreenEvent;

public class TimerHudLayer {

    private static final int LINE_HEIGHT = 11;
    private static final int OUTLINE = 0x80FFFFFF;

    // последняя отрисовка — по ней ловим клик, пока чат открыт
    private static float lastX, lastY, lastW, lastH;
    private static boolean lastValid;

    private static boolean dragging;
    private static double grabX, grabY, dragX, dragY;

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        lastValid = false;

        if (!TimerConfig.ENABLED.get()) {
            return;
        }

        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }

        float partial = TimerConfig.SHOW_MILLISECONDS.get()
                ? delta.getGameTimeDeltaPartialTick(false)
                : 0F;
        float msPerTick = mc.level.tickRateManager().millisecondsPerTick();
        String rta = "RTA: " + clock(TimerState.rtaMillis());
        String igt = "IGT: " + clock(TimerState.igtMillis(partial, msPerTick));

        var font = mc.font;
        int textW = Math.max(font.width(rta), font.width(igt));
        int textH = LINE_HEIGHT * 2 - 2;

        float scale = TimerConfig.SCALE.get().floatValue();
        float w = textW * scale;
        float h = textH * scale;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        float x = anchorX(TimerConfig.ANCHOR.get(), TimerConfig.OFFSET_X.get(), screenW, w);
        float y = anchorY(TimerConfig.ANCHOR.get(), TimerConfig.OFFSET_Y.get(), screenH, h);

        boolean chatOpen = mc.gui.screen() instanceof ChatScreen;
        double mouseX = mc.mouseHandler.getScaledXPos(mc.getWindow());
        double mouseY = mc.mouseHandler.getScaledYPos(mc.getWindow());

        if (dragging && !chatOpen) {
            endDrag(screenW, screenH, w, h);
        }

        if (dragging) {
            dragX = Math.max(0, Math.min(mouseX - grabX, screenW - w));
            dragY = Math.max(0, Math.min(mouseY - grabY, screenH - h));
            x = (float) dragX;
            y = (float) dragY;
        }

        lastX = x;
        lastY = y;
        lastW = w;
        lastH = h;
        lastValid = true;

        // текст тянем к ближнему краю, иначе цифры дёргаются при разной ширине
        boolean right = x + w / 2F >= screenW / 2F;

        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale, scale);

        if (dragging || (chatOpen && hit(mouseX, mouseY, x, y, w, h))) {
            outline(graphics, textW, textH);
        }

        int rtaColor = TimerConfig.rtaColor();
        int igtColor = TimerConfig.igtColor();
        if (!TimerState.isRunning()) {
            rtaColor = ARGB.multiplyAlpha(rtaColor, 0.55F);
            igtColor = ARGB.multiplyAlpha(igtColor, 0.55F);
        }

        boolean shadow = TimerConfig.TEXT_SHADOW.get();
        graphics.text(font, rta, right ? textW - font.width(rta) : 0, 0, rtaColor, shadow);
        graphics.text(font, igt, right ? textW - font.width(igt) : 0, LINE_HEIGHT, igtColor, shadow);

        graphics.pose().popMatrix();
    }

    /** Сброс состояния перетаскивания: HUD-прямоугольник живёт только внутри мира. */
    static void cancelDrag() {
        dragging = false;
        lastValid = false;
    }

    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (dragging || !lastValid || event.getButton() != InputConstants.MOUSE_BUTTON_LEFT) {
            return;
        }
        if (!(event.getScreen() instanceof ChatScreen)) {
            return;
        }
        if (!hit(event.getMouseX(), event.getMouseY(), lastX, lastY, lastW, lastH)) {
            return;
        }

        dragging = true;
        grabX = event.getMouseX() - lastX;
        grabY = event.getMouseY() - lastY;
        dragX = lastX;
        dragY = lastY;
    }

    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!dragging || event.getButton() != InputConstants.MOUSE_BUTTON_LEFT) {
            return;
        }
        var mc = Minecraft.getInstance();
        endDrag(
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight(),
                lastW,
                lastH);
    }

    private static void endDrag(int screenW, int screenH, float w, float h) {
        dragging = false;

        boolean right = dragX + w / 2.0 >= screenW / 2.0;
        boolean bottom = dragY + h / 2.0 >= screenH / 2.0;

        TimerConfig.Anchor anchor;
        if (bottom) {
            anchor = right ? TimerConfig.Anchor.BOTTOM_RIGHT : TimerConfig.Anchor.BOTTOM_LEFT;
        } else {
            anchor = right ? TimerConfig.Anchor.TOP_RIGHT : TimerConfig.Anchor.TOP_LEFT;
        }

        int offsetX = Math.max(0, (int) Math.round(right ? screenW - dragX - w : dragX));
        int offsetY = Math.max(0, (int) Math.round(bottom ? screenH - dragY - h : dragY));

        if (TimerConfig.ANCHOR.get() == anchor
                && TimerConfig.OFFSET_X.get() == offsetX
                && TimerConfig.OFFSET_Y.get() == offsetY) {
            return;
        }

        TimerConfig.ANCHOR.set(anchor);
        TimerConfig.OFFSET_X.set(offsetX);
        TimerConfig.OFFSET_Y.set(offsetY);
        TimerConfig.SPEC.save();
    }

    private static float anchorX(TimerConfig.Anchor anchor, int offset, int screenW, float w) {
        return switch (anchor) {
            case TOP_RIGHT, BOTTOM_RIGHT -> screenW - offset - w;
            case TOP_LEFT, BOTTOM_LEFT -> offset;
        };
    }

    private static float anchorY(TimerConfig.Anchor anchor, int offset, int screenH, float h) {
        return switch (anchor) {
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenH - offset - h;
            case TOP_LEFT, TOP_RIGHT -> offset;
        };
    }

    private static boolean hit(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static void outline(GuiGraphicsExtractor graphics, int w, int h) {
        graphics.fill(-3, -3, w + 3, -2, OUTLINE);
        graphics.fill(-3, h + 2, w + 3, h + 3, OUTLINE);
        graphics.fill(-3, -3, -2, h + 3, OUTLINE);
        graphics.fill(w + 2, -3, w + 3, h + 3, OUTLINE);
    }

    static String clock(long ms) {
        long h = ms / 3_600_000L;
        long m = ms / 60_000L % 60L;
        long s = ms / 1_000L % 60L;
        if (!TimerConfig.SHOW_MILLISECONDS.get()) {
            return String.format("%d:%02d:%02d", h, m, s);
        }
        return String.format("%d:%02d:%02d.%03d", h, m, s, ms % 1_000L);
    }
}
