package dev.larp.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

public class TargetEsp extends Module {
    private final Setting source = add(Setting.mode("Source", 0, "KillAura", "Crosshair"));
    private final Setting style  = add(Setting.mode("Style", 0, "Ring", "Orbs", "Marker"));
    private final Setting trail  = add(Setting.num("Trail length", 7, 2, 14, 1));
    private final Setting card   = add(Setting.bool("Target card", true));

    private LivingEntity shown;
    private float alpha, hpAnim = 1f;
    private final float[] o = new float[2];

    public TargetEsp() {
        super("Target ESP", "Animated ring / orbs / marker + info card", Category.VISUALS);
    }

    private LivingEntity current() {
        if (source.m().equals("Crosshair")) {
            return mc.targetedEntity instanceof LivingEntity le && le.isAlive() ? le : null;
        }
        return ModuleManager.killAura.enabled ? KillAura.target : null;
    }

    @Override
    public void onHud(DrawContext c, float delta) {
        float dt = Anim.dt;
        LivingEntity t = current();
        if (t != null) shown = t;
        alpha = Anim.approach(alpha, t != null ? 1f : 0f, 10f, dt);
        if (shown == null || alpha < 0.01f) { shown = null; return; }

        if (Projector.ok) {
            switch (style.m()) {
                case "Orbs"   -> orbs(c, delta);
                case "Marker" -> marker(c, delta);
                default       -> ring(c, delta);
            }
        }
        if (card.b()) drawCard(c, c.getScaledWindowWidth(), c.getScaledWindowHeight(), dt);
    }

    private float dotSize() {
        return (float) Math.max(1.6, Math.min(5.0, 5.5 / Projector.depth * 3.0));
    }

    private void ring(DrawContext c, float delta) {
        Vec3d p = shown.getLerpedPos(delta);
        double hgt = shown.getHeight(), rad = shown.getWidth() * 0.75 + 0.18;
        double t = System.nanoTime() / 1e9;
        int tr = trail.i(), n = 56;
        for (int k = tr - 1; k >= 0; k--) {
            double tt = t - k * 0.04;
            double yy = p.y + hgt * (0.5 + 0.5 * Math.sin(tt * 2.2));
            float fade = 1f - k / (float) tr;
            for (int i = 0; i < n; i++) {
                double ang = i / (double) n * Math.PI * 2 + tt * 1.6;
                if (!Projector.project(p.x + Math.cos(ang) * rad, yy, p.z + Math.sin(ang) * rad, o)) continue;
                float head = i / (float) n;                       // comet-like brightness around the ring
                int rgb = Theme.grad(head * 0.5) & 0xFFFFFF;
                float a = alpha * fade * (0.25f + 0.75f * head);
                float s = dotSize();
                if (k == 0) R.glow(c, o[0], o[1], s, rgb, a);
                else R.dot(c, o[0], o[1], s, R.rgba(rgb, a * 0.55f));
            }
        }
    }

    private void orbs(DrawContext c, float delta) {
        Vec3d p = shown.getLerpedPos(delta);
        double hgt = shown.getHeight(), rad = shown.getWidth() * 0.8 + 0.3;
        double t = System.nanoTime() / 1e9;
        int tr = trail.i();
        for (int orb = 0; orb < 3; orb++) {
            for (int k = tr - 1; k >= 0; k--) {
                double tt = t - k * 0.03;
                double ang = tt * 2.4 + orb * Math.PI * 2 / 3;
                double yy = p.y + hgt * (0.5 + 0.38 * Math.sin(tt * 1.7 + orb * 2.1));
                if (!Projector.project(p.x + Math.cos(ang) * rad, yy, p.z + Math.sin(ang) * rad, o)) continue;
                float fade = 1f - k / (float) tr;
                int rgb = Theme.grad(orb * 0.3 + k * 0.01) & 0xFFFFFF;
                float s = dotSize() * (0.6f + 0.9f * fade);
                if (k == 0) R.glow(c, o[0], o[1], s * 1.4f, rgb, alpha);
                else R.dot(c, o[0], o[1], s, R.rgba(rgb, alpha * fade * 0.6f));
            }
        }
    }

    private void marker(DrawContext c, float delta) {
        Vec3d p = shown.getLerpedPos(delta);
        double t = System.nanoTime() / 1e9;
        double cy = p.y + shown.getHeight() + 0.55 + Math.sin(t * 2.5) * 0.06;
        double r = 0.16, hh = 0.24;
        float[][] ring = new float[4][2];
        boolean[] ok = new boolean[4];
        for (int i = 0; i < 4; i++) {
            double a = t * 2.0 + i * Math.PI / 2;
            ok[i] = Projector.project(p.x + Math.cos(a) * r, cy, p.z + Math.sin(a) * r, o);
            ring[i][0] = o[0]; ring[i][1] = o[1];
        }
        float[] top = new float[2], bot = new float[2];
        if (!Projector.project(p.x, cy + hh, p.z, o)) return;
        top[0] = o[0]; top[1] = o[1];
        if (!Projector.project(p.x, cy - hh, p.z, o)) return;
        bot[0] = o[0]; bot[1] = o[1];
        float s = dotSize();
        for (int i = 0; i < 4; i++) {
            if (!ok[i]) continue;
            int j = (i + 1) % 4;
            int col = R.alpha(Theme.grad(i * 0.15), alpha);
            if (ok[j]) R.line(c, ring[i][0], ring[i][1], ring[j][0], ring[j][1], col, 1);
            R.line(c, top[0], top[1], ring[i][0], ring[i][1], col, 1);
            R.line(c, bot[0], bot[1], ring[i][0], ring[i][1], col, 1);
            R.glow(c, ring[i][0], ring[i][1], s * 0.9f, Theme.grad(i * 0.15) & 0xFFFFFF, alpha);
        }
    }

    private void drawCard(DrawContext c, int w, int h, float dt) {
        LivingEntity e = shown;
        float ratio = Math.max(0f, Math.min(1f, e.getHealth() / Math.max(1f, e.getMaxHealth())));
        hpAnim = Anim.approach(hpAnim, ratio, 8f, dt);
        int cw = 138, ch = 44;
        int x = w / 2 + 38, y = h / 2 + 12 + (int) ((1f - alpha) * 8);
        int a1 = Theme.c1();
        R.rrect(c, x - 1, y - 1, cw + 2, ch + 2, 9, R.alpha(a1, 0.35f * alpha));
        R.rrect(c, x, y, cw, ch, 8, R.alpha(0xF2101014, alpha));
        c.drawText(mc.textRenderer, e.getName().getString(), x + 10, y + 8, R.alpha(0xFFFFFFFF, alpha), false);
        double d = mc.player != null ? mc.player.distanceTo(e) : 0;
        String info = String.format(Locale.ROOT, "%.1f hp  %.1fm", e.getHealth(), d);
        c.drawText(mc.textRenderer, info, x + 10, y + 20, R.alpha(0xFF8A8A98, alpha), false);
        R.rrect(c, x + 10, y + 33, cw - 20, 5, 2, R.alpha(0x40FFFFFF, alpha));
        int fw = Math.max(0, (int) ((cw - 20) * hpAnim));
        if (fw > 0) {
            R.hgrad(c, x + 10, y + 33, fw, 5, R.alpha(Theme.c1(), alpha), R.alpha(Theme.c2(), alpha));
        }
    }
}
