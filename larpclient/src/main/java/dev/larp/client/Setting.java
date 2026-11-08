package dev.larp.client;

import java.util.Locale;
import java.util.function.BooleanSupplier;

public class Setting {
    public enum Type { BOOL, NUM, MODE }

    public final String name;
    public final Type type;
    public boolean bool;
    public double num, min, max, step = 1;
    public String[] modes = new String[0];
    public int mode;
    public BooleanSupplier visible = () -> true;
    public float anim = -1f; // GUI animation state

    private Setting(String name, Type type) { this.name = name; this.type = type; }

    public static Setting bool(String n, boolean v) {
        Setting s = new Setting(n, Type.BOOL); s.bool = v; return s;
    }

    public static Setting num(String n, double v, double min, double max, double step) {
        Setting s = new Setting(n, Type.NUM);
        s.min = min; s.max = max; s.step = step; s.num = v; return s;
    }

    public static Setting mode(String n, int idx, String... modes) {
        Setting s = new Setting(n, Type.MODE); s.modes = modes; s.mode = idx; return s;
    }

    public Setting showIf(BooleanSupplier b) { this.visible = b; return this; }

    public boolean b() { return bool; }
    public double d() { return num; }
    public float f() { return (float) num; }
    public int i() { return (int) Math.round(num); }
    public String m() { return modes[mode]; }
    public void cycle() { mode = (mode + 1) % modes.length; }

    public void setNum(double v) {
        v = Math.round(v / step) * step;
        v = Math.max(min, Math.min(max, v));
        num = Math.round(v * 1000.0) / 1000.0;
    }

    public String display() {
        if (step >= 1) return String.valueOf((int) num);
        return String.format(Locale.ROOT, step >= 0.1 ? "%.1f" : "%.2f", num);
    }
}
