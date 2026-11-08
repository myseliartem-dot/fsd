package dev.larp.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/** Manual world -> screen projection (first person), so ESP can be drawn in the HUD layer. */
public final class Projector {
    public static boolean ok;
    public static double depth;
    private static double cx, cy, cz, fx, fy, fz, rx, rz, ux, uy, uz, scale;
    private static int w, h;

    private Projector() {}

    public static boolean begin(float delta, int sw, int sh) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity p = mc.player;
        if (p == null) return false;
        Vec3d pos = p.getLerpedPos(delta);
        cx = pos.x; cy = pos.y + (p.getEyeY() - p.getY()); cz = pos.z;
        double yaw = Math.toRadians(p.getYaw(delta)), pitch = Math.toRadians(p.getPitch(delta));
        double sy = Math.sin(yaw), cyw = Math.cos(yaw), sp = Math.sin(pitch), cp = Math.cos(pitch);
        fx = -sy * cp; fy = -sp; fz = cyw * cp;      // forward
        rx = -cyw;     rz = -sy;                     // right
        ux = -sy * sp; uy = cp; uz = cyw * sp;       // up
        double fov = Math.toRadians(mc.options.getFov().getValue());
        w = sw; h = sh;
        scale = (sh / 2.0) / Math.tan(fov / 2.0);
        return true;
    }

    /** out[0], out[1] = scaled GUI coordinates. Returns false if behind the camera. */
    public static boolean project(double x, double y, double z, float[] out) {
        double dx = x - cx, dy = y - cy, dz = z - cz;
        double zc = dx * fx + dy * fy + dz * fz;
        if (zc < 0.05) return false;
        double xs = dx * rx + dz * rz;
        double ys = dx * ux + dy * uy + dz * uz;
        depth = zc;
        out[0] = (float) (w / 2.0 + xs / zc * scale);
        out[1] = (float) (h / 2.0 - ys / zc * scale);
        return true;
    }
}
