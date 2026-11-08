package dev.larp.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Immediate-mode menu: animated glass panel, sidebar tabs, module cards with expandable settings. */
public class ClickGui extends Screen {
    private static final String[] TABS = {"Combat", "Visuals", "Theme"};
    private static final int SW = 124;
    private static int tab = 0;

    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final float[] tabHover = new float[3];
    private final float[] scrollTarget = new float[3];
    private final float[] scrollAnim = new float[3];
    private float open, eased;
    private boolean down, wasDown, pressed, first = true;
    private Setting dragging;
    private int mx, my;

    public ClickGui() { super(Text.literal("LarpClient")); }

    @Override public boolean shouldPause() { return false; }

    @Override public void removed() { ModuleManager.save(); }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scrollTarget[tab] -= (float) vertical * 28f;
        return true;
    }

    private int f(int col) { return R.alpha(col, eased); }

    private void txt(DrawContext c, String s, int x, int y, int col) {
        c.drawText(mc.textRenderer, s, x, y, f(col), false);
    }

    private void bold(DrawContext c, String s, int x, int y, int col) {
        c.drawText(mc.textRenderer, Text.literal(s).formatted(Formatting.BOLD), x, y, f(col), false);
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        LarpClient.pollMenuKey();
        if (mc.currentScreen != this) return;
        Anim.frame();
        float dt = Anim.dt;

        open = Anim.approach(open, 1f, 11f, dt);
        eased = open * open * (3f - 2f * open);

        long win = mc.getWindow().getHandle();
        down = GLFW.glfwGetMouseButton(win, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (first) { wasDown = down; first = false; }
        pressed = down && !wasDown;
        wasDown = down;
        if (!down) dragging = null;
        mx = mouseX; my = mouseY;

        // backdrop
        c.fillGradient(0, 0, width, height, f(0x90060608), f(0xD0000000));

        int W = Math.min(580, width - 24), H = Math.min(350, height - 24);
        int px = (width - W) / 2, py = (height - H) / 2 + (int) ((1f - eased) * 14);

        // glow + border + panel
        for (int i = 8; i >= 1; i--) {
            R.rrect(c, px - i * 2, py - i * 2, W + i * 4, H + i * 4, 12 + i * 2, R.alpha(Theme.c1(), 0.035f * eased));
        }
        R.rrect(c, px - 1, py - 1, W + 2, H + 2, 11, f(R.alpha(Theme.c1(), 0.45f)));
        R.rrect(c, px, py, W, H, 10, f(0xF2101014));

        drawSidebar(c, px, py, H, dt);

        int cx = px + SW + 18, cw = W - SW - 30, cy = py + 18;
        bold(c, TABS[tab], cx, cy, 0xFFFFFFFF);
        R.rrect(c, cx, cy + 14, 22, 2, 1, f(Theme.grad(0)));
        int vy = cy + 26, vh = py + H - 12 - vy;

        if (tab == 2) {
            c.enableScissor(cx - 6, vy, cx + cw + 6, vy + vh);
            drawTheme(c, cx, vy + 2, cw);
            c.disableScissor();
        } else {
            drawModules(c, ModuleManager.byCat(tab == 0 ? Module.Category.COMBAT : Module.Category.VISUALS),
                    cx, vy, cw, vh, dt);
        }
    }

    private void drawSidebar(DrawContext c, int px, int py, int H, float dt) {
        R.rrect(c, px + 6, py + 6, SW, H - 12, 8, f(0xFF15151B));

        String logo = "LARP";
        int x = px + 20;
        for (int i = 0; i < logo.length(); i++) {
            String ch = String.valueOf(logo.charAt(i));
            bold(c, ch, x, py + 18, Theme.grad(i * 0.12));
            x += mc.textRenderer.getWidth(ch) + 2;
        }
        txt(c, "client  1.21.11", px + 20, py + 31, 0xFF6B6B78);
        R.rrect(c, px + 18, py + 46, SW - 24, 1, 0, f(0x22FFFFFF));

        for (int i = 0; i < TABS.length; i++) {
            int tx = px + 12, ty = py + 56 + i * 30, tw = SW - 12;
            boolean hov = R.in(mx, my, tx, ty, tw, 26);
            tabHover[i] = Anim.approach(tabHover[i], tab == i ? 1f : hov ? 0.45f : 0f, 14f, dt);
            float h = tabHover[i];
            R.rrect(c, tx, ty, tw, 26, 6, f(R.alpha(Theme.c1(), 0.16f * h)));
            R.rrect(c, tx + 2, ty + 7, 2, (int) (12 * Math.min(1f, h * 1.2f)), 1, f(Theme.grad(i * 0.2)));
            txt(c, TABS[i], tx + 14, ty + 9, R.lerp(0xFF8A8A98, 0xFFFFFFFF, h));
            if (pressed && hov) tab = i;
        }
        txt(c, "RShift - close", px + 20, py + H - 24, 0xFF55555F);
    }

    // ---- module list -------------------------------------------------------------------------

    private int rowH(Setting s) { return s.type == Setting.Type.NUM ? 30 : 22; }

    private void drawModules(DrawContext c, List<Module> mods, int cx, int vy, int cw, int vh, float dt) {
        scrollAnim[tab] = Anim.approach(scrollAnim[tab], scrollTarget[tab], 16f, dt);
        int y = (int) (vy - scrollAnim[tab]);
        int total = 0;
        boolean inView = my >= vy && my < vy + vh;

        c.enableScissor(cx - 6, vy, cx + cw + 6, vy + vh);
        for (Module m : mods) {
            m.toggleAnim = Anim.approach(m.toggleAnim, m.enabled ? 1f : 0f, 14f, dt);
            m.expandAnim = Anim.approach(m.expandAnim, m.expanded ? 1f : 0f, 12f, dt);

            int full = 0;
            for (Setting s : m.settings) if (s.visible.getAsBoolean()) full += rowH(s);
            int body = m.expandAnim > 0.01f ? (int) ((full + 8) * m.expandAnim) : 0;
            int cardH = 36 + body;

            drawCard(c, m, cx, y, cw, cardH, body, inView);
            y += cardH + 8;
            total += cardH + 8;
        }
        c.disableScissor();

        float max = Math.max(0, total - vh);
        scrollTarget[tab] = Math.max(0f, Math.min(max, scrollTarget[tab]));
    }

    private void drawCard(DrawContext c, Module m, int cx, int y, int cw, int cardH, int body, boolean inView) {
        boolean hovHead = inView && R.in(mx, my, cx, y, cw, 36);
        int base = hovHead ? 0xFF1E1E26 : 0xFF1A1A21;
        R.rrect(c, cx, y, cw, cardH, 8, f(base));
        R.rrect(c, cx, y, cw, cardH, 8, f(R.alpha(Theme.c1(), 0.08f * m.toggleAnim)));
        R.rrect(c, cx, y + 8, 3, 20, 1, f(R.alpha(Theme.grad(0), m.toggleAnim)));

        txt(c, m.name, cx + 14, y + 8, R.lerp(0xFFA0A0AE, 0xFFFFFFFF, m.toggleAnim));
        txt(c, m.desc, cx + 14, y + 21, 0xFF6B6B78);

        int swx = cx + cw - 14 - 28;
        drawSwitch(c, swx, y + 12, 28, 12, m.toggleAnim);
        if (!m.settings.isEmpty()) {
            txt(c, m.expanded ? "v" : ">", cx + cw - 58, y + 14, 0xFF6B6B78);
        }

        if (pressed && hovHead) {
            if (R.in(mx, my, swx - 6, y, 40, 36)) m.toggle();
            else if (!m.settings.isEmpty()) m.expanded = !m.expanded;
        }

        if (body > 0) {
            c.enableScissor(cx, y + 36, cx + cw, y + 36 + body);
            int ry = y + 40;
            boolean interactive = inView && m.expandAnim > 0.9f;
            for (Setting s : m.settings) {
                if (!s.visible.getAsBoolean()) continue;
                drawSetting(c, s, cx, ry, cw, interactive);
                ry += rowH(s);
            }
            c.disableScissor();
        }
    }

    private void drawSwitch(DrawContext c, int x, int y, int w, int h, float t) {
        R.rrect(c, x, y, w, h, h / 2, f(R.lerp(0xFF30303A, Theme.c1(), t)));
        int kx = x + 2 + (int) ((w - h) * t);
        R.rrect(c, kx, y + 2, h - 4, h - 4, (h - 4) / 2, f(0xFFFFFFFF));
    }

    private void drawSetting(DrawContext c, Setting s, int cx, int ry, int cw, boolean interactive) {
        int lx = cx + 14, rx = cx + cw - 14;
        switch (s.type) {
            case BOOL -> {
                if (s.anim < 0) s.anim = s.bool ? 1f : 0f;
                s.anim = Anim.approach(s.anim, s.bool ? 1f : 0f, 16f, Anim.dt);
                txt(c, s.name, lx, ry + 7, 0xFFB4B4C0);
                drawSwitch(c, rx - 22, ry + 5, 22, 11, s.anim);
                if (interactive && pressed && R.in(mx, my, cx, ry, cw, 22)) s.bool = !s.bool;
            }
            case MODE -> {
                txt(c, s.name, lx, ry + 7, 0xFFB4B4C0);
                String v = "< " + s.m() + " >";
                txt(c, v, rx - mc.textRenderer.getWidth(v), ry + 7, Theme.c1());
                if (interactive && pressed && R.in(mx, my, cx, ry, cw, 22)) s.cycle();
            }
            case NUM -> {
                int tx = lx, tw = rx - lx, ty = ry + 20;
                float target = (float) ((s.num - s.min) / (s.max - s.min));
                if (s.anim < 0) s.anim = target;
                s.anim = Anim.approach(s.anim, target, 18f, Anim.dt);
                txt(c, s.name, lx, ry + 5, 0xFFB4B4C0);
                String v = s.display();
                txt(c, v, rx - mc.textRenderer.getWidth(v), ry + 5, 0xFFFFFFFF);

                if (interactive && pressed && R.in(mx, my, tx - 4, ry + 13, tw + 8, 15)) dragging = s;
                if (dragging == s) {
                    s.setNum(s.min + R.clamp((mx - tx) / (float) tw) * (s.max - s.min));
                }
                R.rrect(c, tx, ty, tw, 4, 2, f(0xFF30303A));
                int fw = Math.max(4, (int) (tw * R.clamp(s.anim)));
                R.hgrad(c, tx, ty, fw, 4, f(Theme.c1()), f(Theme.c2()));
                R.rrect(c, tx + fw - 4, ty - 2, 8, 8, 4, f(0xFFFFFFFF));
            }
        }
    }

    // ---- theme tab ---------------------------------------------------------------------------

    private void drawTheme(DrawContext c, int cx, int vy, int cw) {
        int cols = 3, gap = 10, cwid = (cw - gap * (cols - 1)) / cols, ch = 66;
        for (int i = 0; i < Theme.NAMES.length; i++) {
            int x = cx + (i % cols) * (cwid + gap), y = vy + (i / cols) * (ch + gap);
            boolean sel = Theme.index == i;
            boolean hov = R.in(mx, my, x, y, cwid, ch);
            if (sel) R.rrect(c, x - 1, y - 1, cwid + 2, ch + 2, 9, f(R.rgba(Theme.P[i][0], 0.95f)));
            R.rrect(c, x, y, cwid, ch, 8, f(hov ? 0xFF1E1E26 : 0xFF1A1A21));
            R.hgrad(c, x + 8, y + 8, cwid - 16, 30, f(0xFF000000 | Theme.P[i][0]), f(0xFF000000 | Theme.P[i][1]));
            txt(c, Theme.NAMES[i], x + 8, y + 46, sel ? 0xFFFFFFFF : 0xFF8A8A98);
            if (pressed && hov) Theme.index = i;
        }
    }
}
