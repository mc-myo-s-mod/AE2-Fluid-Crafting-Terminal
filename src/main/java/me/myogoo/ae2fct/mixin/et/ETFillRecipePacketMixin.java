package me.myogoo.ae2fct.mixin.et;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import me.myogoo.extendedterminal.menu.ETTerminalBaseMenu;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = {
        "me.myogoo.extendedterminal.network.serverbound.ETFillCraftingGridFromRecipePacket",
        "me.myogoo.extendedterminal.network.serverbound.ETFillSmithingGridFromRecipePacket",
        "me.myogoo.extendedterminal.network.serverbound.ETFillStonecutterGridFromRecipePacket"
}, remap = false)
public abstract class ETFillRecipePacketMixin {
    @WrapMethod(method = "handleOnServer")
    private void ae2fct$withFluidCraftingContext(ServerPlayer player, Operation<Void> original) {
        FluidCraftingTerminalIntegration.beginFillGridContext(player,
                menu -> menu instanceof ETTerminalBaseMenu<?>
                        && FluidCraftingTerminalIntegration.hasFluidInteractUpgrade(menu));
        try {
            original.call(player);
        } finally {
            FluidCraftingTerminalIntegration.endFillGridContext();
        }
    }
}
