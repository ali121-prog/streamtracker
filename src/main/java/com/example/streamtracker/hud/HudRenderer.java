package com.example.streamtracker.hud;

import com.example.streamtracker.chat.MoneyParser;
import com.example.streamtracker.config.TrackerConfig;
import com.example.streamtracker.tracker.SessionTracker;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import java.util.List;
import java.util.Locale;

public final class HudRenderer {
    private static String flashTitle;
    private static String flashSub;
    private static long flashStart = 0;
    private static int flashRgb = 0xFFD700;
    private static int flashTint = 90;

    private HudRenderer() {}

    public static void register() {
        HudRenderCallback.EVENT.register(HudRenderer::render);
    }

    public static void triggerFlash(String title, String subtitle, int rgb, int tintStrength) {
        flashTitle = title;
        flashSub = subtitle;
        flashRgb = rgb;
        flashTint = tintStrength;
        flashStart = System.currentTimeMillis();
    }

    private static void render(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        TrackerConfig cfg = TrackerConfig.get();

        renderFlash(ctx, mc, cfg);
        if (cfg.overlayEnabled) renderPanel(ctx, mc, cfg);
    }

    // ------------------------------------------------------------------
    // Side panel: total, goal bar, recent supporters
    // ------------------------------------------------------------------
    private static void renderPanel(DrawContext ctx, MinecraftClient mc, TrackerConfig cfg) {
        TextRenderer tr = mc.textRenderer;
        List<SessionTracker.Supporter> feed = SessionTracker.recent(cfg.feedSize);

        final int pad = 5, lh = 10, barH = 8;
        final int w = cfg.panelWidth;
        int rows = Math.max(1, feed.size());
        int h = pad + lh            // title
                + lh + 2            // total
                + barH + 2 + lh + 4 // bar + progress text
                + lh                // "Recent Supporters"
                + rows * lh + pad;

        float scale = cfg.scalePercent / 100f;
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();
        float scaledW = w * scale, scaledH = h * scale;

        int x, y;
        switch (cfg.anchor) {
            case TOP_RIGHT -> { x = (int) (sw - scaledW - cfg.offsetX); y = cfg.offsetY; }
            case BOTTOM_LEFT -> { x = cfg.offsetX; y = (int) (sh - scaledH - cfg.offsetY); }
            case BOTTOM_RIGHT -> { x = (int) (sw - scaledW - cfg.offsetX); y = (int) (sh - scaledH - cfg.offsetY); }
            default -> { x = cfg.offsetX; y = cfg.offsetY; }
        }

        double total = SessionTracker.total();
        double progress = Math.max(0, Math.min(1, total / Math.max(1, cfg.goalAmount)));
        int accent = 0xFF000000 | cfg.barColor;

        MatrixStack m = ctx.getMatrices();
        m.push();
        m.translate(x, y, 0);
        m.scale(scale, scale, 1f);

        // background + accent line
        int bgAlpha = Math.max(0, Math.min(255, cfg.backgroundOpacityPercent * 255 / 100));
        ctx.fill(0, 0, w, h, bgAlpha << 24);
        ctx.fill(0, 0, w, 1, accent);

        int cy = pad;
        ctx.drawText(tr, "Donation Tracker", pad, cy, 0xAAAAAA, true);
        cy += lh;

        ctx.drawText(tr, "Total: $" + MoneyParser.format(total), pad, cy, 0xFFFFFF, true);
        cy += lh + 2;

        // progress bar
        int barX1 = pad, barX2 = w - pad;
        ctx.fill(barX1, cy, barX2, cy + barH, 0xFF000000);
        ctx.fill(barX1 + 1, cy + 1, barX2 - 1, cy + barH - 1, 0xFF2A2A2A);
        int fillW = (int) ((barX2 - barX1 - 2) * progress);
        if (fillW > 0) ctx.fill(barX1 + 1, cy + 1, barX1 + 1 + fillW, cy + barH - 1, accent);
        cy += barH + 2;

        String pct = String.format(Locale.ROOT, "%.1f%%  ($%s / $%s)",
                progress * 100, MoneyParser.format(total), MoneyParser.format(cfg.goalAmount));
        ctx.drawText(tr, pct, pad, cy, 0xDDDDDD, true);
        cy += lh + 4;

        ctx.drawText(tr, "Recent Supporters", pad, cy, accent, true);
        cy += lh;

        if (feed.isEmpty()) {
            ctx.drawText(tr, "Waiting for payments...", pad, cy, 0x777777, true);
        } else {
            for (SessionTracker.Supporter s : feed) {
                String amt = "$" + MoneyParser.format(s.amount());
                int amtW = tr.getWidth(amt);
                String name = tr.trimToWidth(s.name(), w - pad * 2 - amtW - 6);
                ctx.drawText(tr, name, pad, cy, 0xFFFFFF, true);
                ctx.drawText(tr, amt, w - pad - amtW, cy, accent, true);
                cy += lh;
            }
        }
        m.pop();
    }

    // ------------------------------------------------------------------
    // Full-screen flash + big text
    // ------------------------------------------------------------------
    private static void renderFlash(DrawContext ctx, MinecraftClient mc, TrackerConfig cfg) {
        if (flashTitle == null) return;
        long elapsed = System.currentTimeMillis() - flashStart;
        long dur = Math.max(500, cfg.flashDurationMs);
        if (elapsed > dur) { flashTitle = null; return; }

        float t = elapsed / (float) dur;               // 0 -> 1
        float fade = 1f - t;                           // 1 -> 0
        int sw = mc.getWindow().getScaledWidth();
        int sh = mc.getWindow().getScaledHeight();

        // golden screen tint, strongest at the start
        int tint = Math.max(0, Math.min(255, (int) (flashTint * fade * fade)));
        if (tint > 0) ctx.fill(0, 0, sw, sh, (tint << 24) | flashRgb);

        int textAlpha = Math.max(8, Math.min(255, (int) (255 * Math.min(1f, fade * 2f))));
        int color = (textAlpha << 24) | flashRgb;
        int subColor = (textAlpha << 24) | 0xFFFFFF;

        // quick "pop" at the start
        float pop = t < 0.1f ? 0.6f + 4f * t : 1.0f;
        TextRenderer tr = mc.textRenderer;
        MatrixStack m = ctx.getMatrices();

        m.push();
        m.translate(sw / 2f, sh / 3f, 0);
        m.scale(4f * pop, 4f * pop, 1f);
        ctx.drawCenteredTextWithShadow(tr, flashTitle, 0, 0, color);
        m.pop();

        m.push();
        m.translate(sw / 2f, sh / 3f + 42f, 0);
        m.scale(2f * pop, 2f * pop, 1f);
        ctx.drawCenteredTextWithShadow(tr, flashSub == null ? "" : flashSub, 0, 0, subColor);
        m.pop();
    }
}
