package dev.larp.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public class Esp extends Module {
    private final Setting style    = add(Setting.mode("Box style", 1, "Full", "Corners"));
    private final Setting fill     = add(Setting.bool("Fill", true));
    private final Setting color    = add(Setting.mode("Color", 0, "Accent", "Health", "Rainbow"));
    private final Setting health   = add(Setting.bool("Health bar", true));
    private final Setting names    = add(Setting.bool("Name tag", true));
    private final Setting distance = add(Setting.bool("Distance", true));
    private final Setting maxDist  = add(Setting.num("Max distance", 96, 16, 256, 8));
    private final Setting players  = add(Setting.bool("Players", true));
    private final Setting hostile  = add(Setting.bool("Hostile mobs", true));
    private final Setting animals  = add(Setting.bool("Animals", false));

    public Esp() {
        super("ESP", "2D boxes, health and name tags", Category.VISUALS);
    }

    @Override
    public void onHud(DrawContext c, float delta) {
        if (!Projector.ok || mc.world == null) return;
        float[] o = new float[2];
        double max2 = maxDist.d() * maxDist.d();
        int idx = 0;

        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le)) continue;
            if (!Targets.valid(le, players.b(), hostile.b(), animals.b())) continue;
            double d2 = mc.player.squaredDistanceTo(le);
            if (d2 > max2) continue;

            Vec3d p = le.getLerpedPos(delta);
            double hw = le.getWidth() / 2.0 + 0.05, hh = le.getHeight() + 0.05;
            float x1 = 1e9f, y1 = 1e9f, x2 = -1e9f, y2 = -1e9f;
            boolean ok = true;
            for (int i = 0; i < 8; i++) {
                double px = p.x + ((i & 1) == 0 ? -hw : hw);
                double py = p.y + ((i & 2) == 0 ? 0 : hh);
                double pz = p.z + ((i & 4) == 0 ? -hw : hw);
                if (!Projector.project(px, py, pz, o)) { ok = false; break; }
                x1 = Math.min(x1, o[0]); y1 = Math.min(y1, o[1]);
                x2 = Math.max(x2, o[0]); y2 = Math.max(y2, o[1]);
            }
            if (!ok) continue;

            int ix1 = Math.round(x1), iy1 = Math.round(y1), ix2 = Math.round(x2), iy2 = Math.round(y2);
            float hp = Math.max(0f, Math.min(1f, le.getHealth() / Math.max(1f, le.getMaxHealth())));
            int col = switch (color.m()) {
                case "Health"  -> R.lerp(0xFFEF4444, 0xFF22C55E, hp);
                case "Rainbow" -> Theme.rainbow(idx * 0.07);
                default        -> Theme.grad(idx * 0.07);
            };
            idx++;

            if (fill.b()) {
                c.fillGradient(ix1, iy1, ix2, iy2, R.alpha(col, 0.05f), R.alpha(col, 0.22f));
            }
            drawBox(c, ix1, iy1, ix2, iy2, col);

            if (health.b()) {
                int bx = ix1 - 5, bh = iy2 - iy1;
                c.fill(bx - 1, iy1 - 1, bx + 2, iy2 + 1, 0xAA000000);
                int fh = (int) (bh * hp);
                c.fillGradient(bx, iy2 - fh, bx + 1, iy2, R.lerp(0xFFEF4444, 0xFF22C55E, hp), 0xFF166534);
            }

            if (names.b() || distance.b()) {
                StringBuilder sb = new StringBuilder();
                if (names.b()) sb.append(le.getName().getString());
                if (distance.b()) {
                    if (sb.length() > 0) sb.append("  ");
                    sb.append((int) Math.sqrt(d2)).append("m");
                }
                String s = sb.toString();
                int tw = mc.textRenderer.getWidth(s);
                int tx = (ix1 + ix2) / 2 - tw / 2, ty = iy1 - 14;
                R.rrect(c, tx - 4, ty - 2, tw + 8, 12, 4, 0xB0101014);
                R.rrect(c, tx - 4, ty + 9, tw + 8, 1, 0, R.alpha(col, 0.9f));
                c.drawText(mc.textRenderer, s, tx, ty, 0xFFFFFFFF, false);
            }
        }
    }

    private void drawBox(DrawContext c, int x1, int y1, int x2, int y2, int col) {
        int shadow = 0xB0000000;
        if (style.m().equals("Full")) {
            R.outline(c, x1 - 1, y1 - 1, x2 + 1, y2 + 1, shadow);
            R.outline(c, x1 + 1, y1 + 1, x2 - 1, y2 - 1, shadow);
            R.outline(c, x1, y1, x2, y2, col);
            return;
        }
        int lx = Math.max(3, (x2 - x1) / 4), ly = Math.max(3, (y2 - y1) / 4);
        corner(c, x1, y1, lx, ly, 1, 1, shadow, col);
        corner(c, x2, y1, lx, ly, -1, 1, shadow, col);
        corner(c, x1, y2, lx, ly, 1, -1, shadow, col);
        corner(c, x2, y2, lx, ly, -1, -1, shadow, col);
    }

    private void corner(DrawContext c, int x, int y, int lx, int ly, int sx, int sy, int shadow, int col) {
        // shadow
        c.fill(Math.min(x - sx, x + sx * (lx + 1)), y - 1, Math.max(x - sx, x + sx * (lx + 1)), y + 2, shadow);
        c.fill(x - 1, Math.min(y - sy, y + sy * (ly + 1)), x + 2, Math.max(y - sy, y + sy * (ly + 1)), shadow);
        // colour
        c.fill(Math.min(x, x + sx * lx), y, Math.max(x, x + sx * lx), y + 1, col);
        c.fill(x, Math.min(y, y + sy * ly), x + 1, Math.max(y, y + sy * ly), col);
    }
}
