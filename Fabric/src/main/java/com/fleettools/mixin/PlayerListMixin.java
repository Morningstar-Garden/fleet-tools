package com.fleettools.mixin;

import com.fleettools.events.ChatFormatter;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// Decorates the displayed sender name on broadcast player chat with the LuckPerms
// prefix/suffix. The signed PlayerChatMessage is untouched, so messages stay
// cryptographically signed and reportable.
@Mixin(PlayerList.class)
public class PlayerListMixin {

    @ModifyVariable(
            method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private ChatType.Bound fleettools_decorateChat(ChatType.Bound bound, PlayerChatMessage message, ServerPlayer sender, ChatType.Bound boundArg) {
        Component decorated = ChatFormatter.decorateName(sender, bound.name());
        if (decorated == null) {
            return bound;
        }
        return new ChatType.Bound(bound.chatType(), decorated, bound.targetName());
    }
}
