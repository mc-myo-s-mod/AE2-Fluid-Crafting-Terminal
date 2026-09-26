package me.myogoo.ae2fct.mixin.et;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import appeng.menu.me.common.MEStorageMenu;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;
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
    protected ResourceKey<Recipe<?>> recipeId;

    @WrapMethod(method = "handleOnServer")
    private void ae2fct$withFluidCraftingContext(ServerPlayer player, Operation<Void> original) {
        boolean enabled = player.containerMenu instanceof MEStorageMenu menu
                && FluidCraftingTerminalIntegration.hasFluidInteractUpgrade(menu);
        FluidCraftingHelper.setFluidCraftingEnabled(enabled, recipeId, player.level());
        try {
            original.call(player);
        } finally {
            FluidCraftingHelper.clearFluidCraftingEnabled();
        }
    }
}
