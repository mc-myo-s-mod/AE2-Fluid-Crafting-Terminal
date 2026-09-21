package me.myogoo.ae2fct.mixin.minecraft;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.RecipeMatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

import static me.myogoo.ae2fct.util.FluidCraftingHelper.isVirtualFluidItem;

@Mixin(ShapelessRecipe.class)
public abstract class ShapelessRecipeMixin {
    @Shadow
    @Final
    private NonNullList<Ingredient> ingredients;

    @Inject(method = "matches", at = @At("RETURN"), cancellable = true)
    private void ae2fct$matchVirtualFluidWithSimpleIngredients(CraftingContainer container, Level level,
            CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            return;
        }

        List<ItemStack> inputs = new ArrayList<>();
        boolean hasVirtualFluidInput = false;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            inputs.add(stack);
            hasVirtualFluidInput |= isVirtualFluidItem(stack);
        }

        if (hasVirtualFluidInput && RecipeMatcher.findMatches(inputs, this.ingredients) != null) {
            cir.setReturnValue(true);
        }
    }
}
