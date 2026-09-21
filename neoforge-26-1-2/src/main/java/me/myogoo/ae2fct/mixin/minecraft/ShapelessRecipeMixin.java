package me.myogoo.ae2fct.mixin.minecraft;

import me.myogoo.ae2fct.util.VirtualFluidRecipePolicy;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ShapelessRecipe.class)
public abstract class ShapelessRecipeMixin {
    @Shadow @Final
    private List<Ingredient> ingredients;

    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true)
    private void ae2fct$matchVirtualFluidStacks(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (VirtualFluidRecipePolicy.containsVirtualFluid(input)) {
            // The simple-item fast path discards the fluid component and bypasses Ingredient.test.
            cir.setReturnValue(input.ingredientCount() == ingredients.size()
                    && RecipeMatcher.findMatches(input.items().stream().filter(stack -> !stack.isEmpty()).toList(),
                            ingredients) != null);
        }
    }
}
