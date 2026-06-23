package com.fleettools.mixin;

import com.fleettools.data.PlayerDataManager;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerPlayNetworkHandlerMixin {
    
    @Shadow
    public ServerPlayer player;
    
    @Inject(method = "handleChat", at = @At("HEAD"), cancellable = true)
    private void onChatMessage(ServerboundChatPacket packet, CallbackInfo ci) {
        if (PlayerDataManager.isMuted(this.player)) {
            this.player.sendSystemMessage(Component.literal("§cYou are muted and cannot send messages."), false);
            ci.cancel();
        }
    }
}
