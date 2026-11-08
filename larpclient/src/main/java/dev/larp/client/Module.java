package dev.larp.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public enum Category { COMBAT, VISUALS }

    protected static final MinecraftClient mc = MinecraftClient.getInstance();

    public final String name, desc;
    public final Category cat;
    public boolean enabled, expanded;
    public float toggleAnim, expandAnim, hudAnim;
    public final List<Setting> settings = new ArrayList<>();

    protected Module(String name, String desc, Category cat) {
        this.name = name; this.desc = desc; this.cat = cat;
    }

    protected Setting add(Setting s) { settings.add(s); return s; }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onHud(DrawContext c, float delta) {}
}
