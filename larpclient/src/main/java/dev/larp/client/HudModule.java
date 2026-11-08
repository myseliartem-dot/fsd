package dev.larp.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HudModule extends Module {
    private final Setting watermark = add(Setting.bool("Watermark", true));
    private final Setting arraylist = add(Setting.bool("Module list", true));

    public HudModule() {
        super("HUD", "Watermark and active module list", Category.VISUALS);
        enabled = true;
    }

    @Override
    public void onHud(DrawContext c, float delta) {
        float dt = Anim.dt;
        for (Module m : ModuleManager.ALL) {
            m.hudAnim = Anim.approach(m.hudAnim, m.enabled && m != this ? 1f : 0f, 12f, dt);
        }
        if (!enabled) return;
        int sw = c.getScaledWindowWidth();

        if (watermark.b()) {
            String fps = mc.getCurrentFps() + " fps";
            String name = "LARP";
            int nw = mc.textRenderer.getWidth(name) + name.length() - 1, fw = mc.textRenderer.getWidth(fps);
            int w = nw + fw + 24;
            R.rrect(c, 6, 6, w, 18, 6, 0xC00E0E12);
            R.rrect(c, 6, 22, w, 2, 1, R.alpha(Theme.grad(0), 0.9f));
            int x = 13;
            for (int i = 0; i < name.length(); i++) {
                String ch = String.valueOf(name.charAt(i));
                c.drawText(mc.textRenderer, Text.literal(ch).formatted(Formatting.BOLD), x, 11, Theme.grad(i * 0.12), false);
                x += mc.textRenderer.getWidth(ch) + 1;
            }
            c.drawText(mc.textRenderer, fps, x + 8, 11, 0xFF8A8A98, false);
        }

        if (arraylist.b()) {
            List<Module> list = new ArrayList<>();
            for (Module m : ModuleManager.ALL) if (m.hudAnim > 0.02f) list.add(m);
            list.sort(Comparator.comparingInt((Module m) -> -mc.textRenderer.getWidth(m.name)));
            float y = 6;
            int i = 0;
            for (Module m : list) {
                int tw = mc.textRenderer.getWidth(m.name);
                int rowH = 13;
                int x = sw - 6 - tw - 10 + (int) ((1f - m.hudAnim) * (tw + 16));
                int yy = (int) y;
                R.rrect(c, x, yy, tw + 10, rowH, 3, R.alpha(0xB00E0E12, m.hudAnim));
                R.rrect(c, x + tw + 8, yy + 2, 2, rowH - 4, 1, R.alpha(Theme.grad(i * 0.1), m.hudAnim));
                c.drawText(mc.textRenderer, m.name, x + 4, yy + 3, R.alpha(Theme.grad(i * 0.1), m.hudAnim), false);
                y += (rowH + 2) * m.hudAnim;
                i++;
            }
        }
    }
}
