package com.fleettools.mixin;

import com.fleettools.events.VanishManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Suppresses the "X has made the advancement [...]" broadcast when the earner is
// vanished, so completing advancements doesn't reveal a hidden staff member.
@Mixin(PlayerAdvancements.class)
public class VanishAdvancementMixin {

    @Shadow
    private ServerPlayer player;

    // The announcement is emitted from the display lambda, not award() itself.
    @Redirect(
            method = "lambda$award$0(Lnet/minecraft/advancements/AdvancementHolder;Lnet/minecraft/advancements/DisplayInfo;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void fleettools_silentAdvancement(PlayerList instance, Component message, boolean overlay) {
        if (this.player == null || !VanishManager.isVanished(this.player.getUUID())) {
            instance.broadcastSystemMessage(message, overlay);
        }
    }
}
