package me.myogoo.ae2fct.mixin.ie;

import blusunrize.immersiveengineering.common.crafting.fluidaware.IngredientFluidStack;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import me.myogoo.ae2fct.util.VirtualFluidStorage;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = IngredientFluidStack.class, remap = false)
public abstract class IngredientFluidStackMixin {
    @Inject(method = "test(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void ae2fct$testVirtualBucket(@Nullable ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack == null || !FluidCraftingHelper.isVirtualFluidItem(stack)) {
            return;
        }
        var virtualFluid = VirtualFluidStorage.get(stack);
        if (FluidCraftingTerminalIntegration.isCurrentRecipeBlacklisted() || virtualFluid == null
                || virtualFluid.fluid().getAmount() != FluidType.BUCKET_VOLUME) {
            cir.setReturnValue(false);
            return;
        }

        // Preserve IE's bucket amount/tag/NBT rules without exposing a drainable item capability.
        ItemStack bucket = FluidUtil.getFilledBucket(virtualFluid.fluid());
        cir.setReturnValue(!bucket.isEmpty() && !FluidCraftingHelper.isVirtualFluidItem(bucket)
                && ((Ingredient) (Object) this).test(bucket));
    }
}
