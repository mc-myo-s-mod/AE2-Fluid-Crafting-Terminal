package me.myogoo.ae2fct.mixin.et;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import me.myogoo.extendedterminal.menu.ETTerminalBaseMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = {
        "me.myogoo.extendedterminal.network.serverbound.ETFillCraftingGridFromRecipePacket",
        "me.myogoo.extendedterminal.network.serverbound.ETFillSmithingGridFromRecipePacket",
        "me.myogoo.extendedterminal.network.serverbound.ETFillStonecutterGridFromRecipePacket"
}, remap = false)
public abstract class ETFillRecipePacketMixin {
    @Shadow(remap = false)
    @Final
    @Nullable
    private ResourceLocation recipeId;

    @WrapMethod(method = "handleOnServer")
    private void ae2fct$withFluidCraftingContext(ServerPlayer player, Operation<Void> original) {
        FluidCraftingTerminalIntegration.beginFillGridContext(player,
                menu -> menu instanceof ETTerminalBaseMenu<?>
                        && FluidCraftingTerminalIntegration.hasFluidInteractUpgrade(menu), this.recipeId);
        try {
            original.call(player);
        } finally {
            FluidCraftingTerminalIntegration.endFillGridContext();
        }
    }
}
