package dev.larp.client;

import net.minecraft.client.gui.DrawContext;

/** 2D drawing helpers built only on DrawContext.fill (no render-pipeline internals). */
public final class R {
    private R() {}

    public static float clamp(float v) { return v < 0f ? 0f : Math.min(v, 1f); }

    /** Multiply the alpha of an ARGB colour. */
    public static int alpha(int argb, float a) {
        int al = (int) (((argb >>> 24) & 255) * clamp(a));
        return (al << 24) | (argb & 0xFFFFFF);
    }

    /** 0xRRGGBB + alpha 0..1 -> ARGB. */
    public static int rgba(int rgb, float a) {
        return ((int) (255 * clamp(a)) << 24) | (rgb & 0xFFFFFF);
    }

    public static int lerp(int a, int b, float t) {
        t = clamp(t);
        int aa = (a >>> 24) & 255, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = (b >>> 24) & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    public static int hsb(float h, float s, float b) {
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(h, s, b) & 0xFFFFFF);
    }

    public static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int col) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        c.fill(x, y + r, x + w, y + h - r, col);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, col);
            c.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, col);
        }
    }

    public static void hgrad(DrawContext c, int x, int y, int w, int h, int c1, int c2) {
        for (int i = 0; i < w; i += 2) {
            c.fill(x + i, y, Math.min(x + i + 2, x + w), y + h, lerp(c1, c2, i / (float) w));
        }
    }

    public static void outline(DrawContext c, int x1, int y1, int x2, int y2, int col) {
        c.fill(x1, y1, x2, y1 + 1, col);
        c.fill(x1, y2 - 1, x2, y2, col);
        c.fill(x1, y1 + 1, x1 + 1, y2 - 1, col);
        c.fill(x2 - 1, y1 + 1, x2, y2 - 1, col);
    }

    public static void line(DrawContext c, float x1, float y1, float x2, float y2, int col, int thick) {
        float dx = x2 - x1, dy = y2 - y1;
        int steps = (int) Math.max(Math.abs(dx), Math.abs(dy));
        if (steps > 4000) return;
        if (steps == 0) { c.fill((int) x1, (int) y1, (int) x1 + thick, (int) y1 + thick, col); return; }
        for (int i = 0; i <= steps; i++) {
            int px = (int) (x1 + dx * i / steps), py = (int) (y1 + dy * i / steps);
            c.fill(px, py, px + thick, py + thick, col);
        }
    }

    public static void dot(DrawContext c, float x, float y, float size, int col) {
        int s = Math.max(2, Math.round(size));
        rrect(c, Math.round(x - s / 2f), Math.round(y - s / 2f), s, s, s / 2, col);
    }

    /** Dot with a soft halo (rgb = 0xRRGGBB, a = 0..1). */
    public static void glow(DrawContext c, float x, float y, float size, int rgb, float a) {
        dot(c, x, y, size * 3.2f, rgba(rgb, a * 0.07f));
        dot(c, x, y, size * 2.0f, rgba(rgb, a * 0.18f));
        dot(c, x, y, size, rgba(rgb, a * 0.95f));
    }
}
