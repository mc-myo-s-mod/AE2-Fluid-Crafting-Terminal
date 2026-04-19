package me.myogoo.ae2fct.mixin;

import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.util.VirtualFluidStorage;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
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

        VirtualFluid virtualFluid = VirtualFluidStorage.get(stack);
        if (virtualFluid == null || virtualFluid.fluid().isEmpty()) {
            return;
        }

        ItemStack virtualBucket = new ItemStack(virtualFluid.fluid().getFluid().getBucket());
        if (virtualBucket.isEmpty() || virtualBucket.getItem() == stack.getItem()) {
            return;
        }

        Ingredient self = (Ingredient) (Object) this;
        if (self.test(virtualBucket)) {
            cir.setReturnValue(true);
        }
    }
}
