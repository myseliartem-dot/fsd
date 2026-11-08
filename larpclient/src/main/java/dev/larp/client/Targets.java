package dev.larp.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;

public final class Targets {
    private Targets() {}

    public static boolean valid(LivingEntity e, boolean players, boolean hostile, boolean animals) {
        if (e == MinecraftClient.getInstance().player || !e.isAlive() || e.isRemoved()) return false;
        if (e instanceof ArmorStandEntity) return false;
        if (e instanceof PlayerEntity p) return players && !p.isSpectator();
        if (e instanceof Monster) return hostile;
        if (e instanceof AnimalEntity) return animals;
        return false;
    }
}
