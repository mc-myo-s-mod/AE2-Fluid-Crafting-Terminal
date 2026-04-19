package me.myogoo.ae2fct.mixin.et;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import me.myogoo.extendedterminal.menu.ETTerminalBaseMenu;
import me.myogoo.myotus.menu.TerminalUpgradeHelper;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "me.myogoo.extendedterminal.network.serverbound.FillTableCraftingGridFromRecipePacket", remap = false)
public abstract class ETFillTableCraftingGridFromRecipePacketMixin {

    @WrapMethod(method = "handleOnServer")
    private void ae2fct$withFluidCraftingState(ServerPlayer player, Operation<Void> original) {
        boolean enabled = false;
        if (player.containerMenu instanceof ETTerminalBaseMenu<?> menu) {
            enabled = TerminalUpgradeHelper.hasUpgrade(menu, AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get());
        }

        FluidCraftingHelper.setFluidCraftingEnabled(enabled);
        try {
            original.call(player);
        } finally {
            FluidCraftingHelper.clearFluidCraftingEnabled();
        }
    }
}
