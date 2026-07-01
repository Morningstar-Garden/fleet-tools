package com.fleettools.mixin;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

// Lets PlayerInfoSendMixin overwrite the entry list on a freshly-built copy of a
// player-info packet (there is no public constructor that takes entries directly).
@Mixin(ClientboundPlayerInfoUpdatePacket.class)
public interface PlayerInfoUpdatePacketAccessor {
    @Mutable
    @Accessor("entries")
    void fleettools$setEntries(List<ClientboundPlayerInfoUpdatePacket.Entry> entries);
}
