package com.fleettools.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = LivingEntity.class)
public class LivingEntityDeathMixin {
    // No keep inventory functionality - this mixin is now unused but kept for potential future use
}