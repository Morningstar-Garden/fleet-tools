package com.fleettools.mixin;

import com.fleettools.events.VanishManager;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

// Removes vanished players from the server-list ping: they don't count toward the online
// total and never appear in the hover name-sample. The ping is anonymous (we can't know
// who is looking), so vanished players are hidden from it unconditionally.
@Mixin(MinecraftServer.class)
public class VanishServerStatusMixin {

    @Inject(method = "buildPlayerStatus", at = @At("RETURN"), cancellable = true)
    private void fleettools_hideVanishedFromPing(CallbackInfoReturnable<ServerStatus.Players> cir) {
        ServerStatus.Players original = cir.getReturnValue();
        if (original == null) {
            return;
        }
        MinecraftServer server = (MinecraftServer) (Object) this;

        int vanishedOnline = 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (VanishManager.isVanished(p.getUUID())) {
                vanishedOnline++;
            }
        }
        if (vanishedOnline == 0) {
            return;
        }

        List<NameAndId> sample = new ArrayList<>();
        for (NameAndId entry : original.sample()) {
            if (!VanishManager.isVanished(entry.id())) {
                sample.add(entry);
            }
        }
        cir.setReturnValue(new ServerStatus.Players(
                original.max(), Math.max(0, original.online() - vanishedOnline), sample));
    }
}
