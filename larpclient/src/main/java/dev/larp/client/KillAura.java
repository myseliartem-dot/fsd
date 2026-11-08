package dev.larp.client;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class KillAura extends Module {
    public static LivingEntity target;

    private final Setting range    = add(Setting.num("Range", 3.2, 2.0, 6.0, 0.1));
    private final Setting fov      = add(Setting.num("FOV", 120, 30, 360, 5));
    private final Setting sort     = add(Setting.mode("Sort", 0, "Distance", "Health", "Angle"));
    private final Setting players  = add(Setting.bool("Players", true));
    private final Setting hostile  = add(Setting.bool("Hostile mobs", true));
    private final Setting animals  = add(Setting.bool("Animals", false));
    private final Setting walls    = add(Setting.bool("Through walls", false));
    private final Setting cooldown = add(Setting.bool("Wait cooldown", true));
    private final Setting noUse    = add(Setting.bool("Pause while using item", true));
    private final Setting rotate   = add(Setting.bool("Rotate", true));
    private final Setting speed    = add(Setting.num("Rotation speed", 0.35, 0.05, 1.0, 0.05)).showIf(rotate::b);

    public KillAura() {
        super("KillAura", "Automatically attacks nearby targets", Category.COMBAT);
    }

    @Override public void onDisable() { target = null; }

    @Override
    public void onTick() {
        ClientPlayerEntity p = mc.player;
        if (p == null || mc.world == null || mc.currentScreen != null || !p.isAlive()) { target = null; return; }

        target = pick(p);
        if (target == null) return;

        Vec3d eye = new Vec3d(p.getX(), p.getEyeY(), p.getZ());
        if (rotate.b()) {
            float[] r = rots(eye, aim(target, eye));
            float dy = MathHelper.wrapDegrees(r[0] - p.getYaw());
            float dp = r[1] - p.getPitch();
            float f = speed.f();
            p.setYaw(p.getYaw() + MathHelper.clamp(dy * f, -45f, 45f));
            p.setPitch(MathHelper.clamp(p.getPitch() + MathHelper.clamp(dp * f, -30f, 30f), -90f, 90f));
        }

        if (noUse.b() && p.isUsingItem()) return;
        if (cooldown.b() && p.getAttackCooldownProgress(0.5f) < 0.93f) return;
        if (dist(target, eye) > range.d()) return;

        mc.interactionManager.attackEntity(p, target);
        p.swingHand(Hand.MAIN_HAND);
    }

    private LivingEntity pick(ClientPlayerEntity p) {
        Vec3d eye = new Vec3d(p.getX(), p.getEyeY(), p.getZ());
        List<LivingEntity> list = new ArrayList<>();
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le)) continue;
            if (!Targets.valid(le, players.b(), hostile.b(), animals.b())) continue;
            if (dist(le, eye) > range.d()) continue;
            if (!walls.b() && !p.canSee(le)) continue;
            if (angle(p, le, eye) > fov.d() / 2.0) continue;
            list.add(le);
        }
        if (list.isEmpty()) return null;
        Comparator<LivingEntity> cmp = switch (sort.m()) {
            case "Health" -> Comparator.comparingDouble(LivingEntity::getHealth);
            case "Angle"  -> Comparator.comparingDouble(e -> angle(p, e, eye));
            default       -> Comparator.comparingDouble(e -> dist(e, eye));
        };
        list.sort(cmp);
        return list.get(0);
    }

    /** Distance from the eye to the closest point of the target's hitbox. */
    private static double dist(Entity e, Vec3d eye) {
        Box b = e.getBoundingBox();
        double x = MathHelper.clamp(eye.x, b.minX, b.maxX);
        double y = MathHelper.clamp(eye.y, b.minY, b.maxY);
        double z = MathHelper.clamp(eye.z, b.minZ, b.maxZ);
        return eye.distanceTo(new Vec3d(x, y, z));
    }

    /** Aim at the hitbox centre column, at eye height clamped inside the box. */
    private static Vec3d aim(Entity e, Vec3d eye) {
        Box b = e.getBoundingBox();
        double y = MathHelper.clamp(eye.y, b.minY + 0.15, b.maxY - 0.15);
        return new Vec3d(e.getX(), y, e.getZ());
    }

    private static double angle(ClientPlayerEntity p, Entity e, Vec3d eye) {
        float[] r = rots(eye, aim(e, eye));
        float dy = MathHelper.wrapDegrees(r[0] - p.getYaw());
        float dp = r[1] - p.getPitch();
        return Math.hypot(dy, dp);
    }

    private static float[] rots(Vec3d from, Vec3d to) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        return new float[]{
                (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0),
                (float) -Math.toDegrees(Math.atan2(dy, h))
        };
    }
}
