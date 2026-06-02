package me.myogoo.ae2fct.mixin;

import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.init.AE2FCTItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.jetbrains.annotations.Nullable;

@Mixin(Ingredient.class)
public abstract class IngredientMixin {

    @Inject(method = "test(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void interceptVirtualFluidTesting(@Nullable ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack != null && stack.is(AE2FCTItems.VIRTUAL_FLUID_ITEM.get())) {
            VirtualFluid vf = stack.get(AE2FCTDataComponent.VIRTUAL_FLUID);
            if (vf != null && !vf.fluid().isEmpty()) {
                // Determine the corresponding bucket item for the contained fluid
                ItemStack virtualBucket = new ItemStack(vf.fluid().getFluid().getBucket());

                // Prevent infinite recursion if the virtual item is literally the bucket itself
                // (which shouldn't happen)
                if (!virtualBucket.isEmpty() && virtualBucket.getItem() != stack.getItem()) {
                    // Check if this Ingredient accepts the simulated bucket
                    Ingredient self = (Ingredient) (Object) this;
                    if (self.test(virtualBucket)) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }
}
