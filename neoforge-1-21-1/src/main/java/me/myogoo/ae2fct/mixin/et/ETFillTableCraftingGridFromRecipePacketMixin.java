package me.myogoo.ae2fct.mixin.et;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import me.myogoo.extendedterminal.menu.ETTerminalBaseMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(targets = "me.myogoo.extendedterminal.network.serverbound.FillTableCraftingGridFromRecipePacket", remap = false)
public abstract class ETFillTableCraftingGridFromRecipePacketMixin {
    @Shadow
    @Final
    @Nullable
    protected ResourceLocation recipeId;

    @WrapMethod(method = "handleOnServer")
    private void ae2fct$withFluidCraftingState(ServerPlayer player, Operation<Void> original) {
        boolean enabled = false;
        if (player.containerMenu instanceof ETTerminalBaseMenu<?> menu) {
            enabled = FluidCraftingTerminalIntegration.hasFluidInteractUpgrade(menu);
        }

        FluidCraftingHelper.setFluidCraftingEnabled(enabled, this.recipeId);
        try {
            original.call(player);
        } finally {
            FluidCraftingHelper.clearFluidCraftingEnabled();
        }
    }
}
