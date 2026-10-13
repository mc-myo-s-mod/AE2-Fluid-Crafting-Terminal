package me.myogoo.ae2fct.mixin;

import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.util.VirtualFluidStorage;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Ingredient.class)
public abstract class IngredientMixin {
    @Inject(method = "test(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void interceptVirtualFluidTesting(@Nullable ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack == null || !stack.is(AE2FCTItems.VIRTUAL_FLUID_ITEM.get())) {
            return;
        }
        if (FluidCraftingTerminalIntegration.isCurrentRecipeBlacklisted()) {
            cir.setReturnValue(false);
            return;
        }

        VirtualFluid virtualFluid = VirtualFluidStorage.get(stack);
        if (virtualFluid == null || virtualFluid.fluid().isEmpty()
                || virtualFluid.fluid().getAmount() != FluidType.BUCKET_VOLUME) {
            return;
        }

        ItemStack virtualBucket = FluidUtil.getFilledBucket(virtualFluid.fluid());
        if (virtualBucket.isEmpty() || virtualBucket.getItem() == stack.getItem()) {
            return;
        }

        Ingredient self = (Ingredient) (Object) this;
        if (self.test(virtualBucket)) {
            cir.setReturnValue(true);
        }
    }
}
