package com.fleettools.mixin;

import com.fleettools.events.VanishManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Hides vanished players from command name-suggestions (used by vanilla EntityArgument
// completions like /tp and /msg) for a viewer who isn't allowed to see them. Console and
// command blocks (no player) get the full list.
@Mixin(CommandSourceStack.class)
public class VanishSuggestionMixin {

    @Inject(method = "getOnlinePlayerNames", at = @At("RETURN"), cancellable = true)
    private void fleettools_hideVanishedNames(CallbackInfoReturnable<Collection<String>> cir) {
        CommandSourceStack self = (CommandSourceStack) (Object) this;
        ServerPlayer viewer = self.getPlayer();
        if (viewer == null || VanishManager.canSee(viewer)) {
            return;
        }
        MinecraftServer server = self.getServer();
        if (server == null) {
            return;
        }

        Set<String> hidden = new HashSet<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (VanishManager.isVanished(p.getUUID())) {
                hidden.add(p.getName().getString());
            }
        }
        if (hidden.isEmpty()) {
            return;
        }

        List<String> filtered = new ArrayList<>(cir.getReturnValue());
        filtered.removeAll(hidden);
        cir.setReturnValue(filtered);
    }
}
