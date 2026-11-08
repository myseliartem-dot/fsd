package dev.larp.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    public static final List<Module> ALL = new ArrayList<>();
    public static KillAura killAura;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ModuleManager() {}

    public static void init() {
        killAura = new KillAura();
        ALL.add(killAura);
        ALL.add(new Esp());
        ALL.add(new TargetEsp());
        ALL.add(new HudModule());
        load();
    }

    public static List<Module> byCat(Module.Category cat) {
        List<Module> l = new ArrayList<>();
        for (Module m : ALL) if (m.cat == cat) l.add(m);
        return l;
    }

    public static void tick() {
        for (Module m : ALL) if (m.enabled) m.onTick();
    }

    public static void hud(DrawContext c, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        Anim.frame();
        Projector.ok = mc.options.getPerspective().isFirstPerson()
                && Projector.begin(delta, c.getScaledWindowWidth(), c.getScaledWindowHeight());
        for (Module m : ALL) {
            if (m.enabled || m instanceof HudModule) m.onHud(c, delta);
        }
    }

    // ---- config -----------------------------------------------------------------------------

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("larpclient.json");
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("theme", Theme.index);
            for (Module m : ALL) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.enabled);
                for (Setting s : m.settings) {
                    switch (s.type) {
                        case BOOL -> o.addProperty(s.name, s.bool);
                        case NUM -> o.addProperty(s.name, s.num);
                        case MODE -> o.addProperty(s.name, s.mode);
                    }
                }
                root.add(m.name, o);
            }
            try (Writer w = Files.newBufferedWriter(file())) {
                GSON.toJson(root, w);
            }
        } catch (Exception ignored) {
        }
    }

    private static void load() {
        Path f = file();
        if (!Files.exists(f)) return;
        try (Reader r = Files.newBufferedReader(f)) {
            JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
            if (root.has("theme")) Theme.index = Math.floorMod(root.get("theme").getAsInt(), Theme.NAMES.length);
            for (Module m : ALL) {
                if (!root.has(m.name)) continue;
                JsonObject o = root.getAsJsonObject(m.name);
                if (o.has("enabled")) m.enabled = o.get("enabled").getAsBoolean();
                m.toggleAnim = m.enabled ? 1f : 0f;
                for (Setting s : m.settings) {
                    if (!o.has(s.name)) continue;
                    switch (s.type) {
                        case BOOL -> s.bool = o.get(s.name).getAsBoolean();
                        case NUM -> s.setNum(o.get(s.name).getAsDouble());
                        case MODE -> s.mode = Math.floorMod(o.get(s.name).getAsInt(), s.modes.length);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
