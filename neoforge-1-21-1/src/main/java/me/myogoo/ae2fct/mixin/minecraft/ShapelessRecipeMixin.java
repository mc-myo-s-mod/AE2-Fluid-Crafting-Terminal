package me.myogoo.ae2fct.mixin.minecraft;

import me.myogoo.ae2fct.util.FluidCraftingHelper;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.RecipeMatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShapelessRecipe.class)
public abstract class ShapelessRecipeMixin {
    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true)
    private void ae2fct$matchVirtualFluids(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (!FluidCraftingHelper.containsVirtualFluidItem(input)) {
            return;
        }
        var ingredients = ((ShapelessRecipe) (Object) this).getIngredients();
        cir.setReturnValue(input.ingredientCount() == ingredients.size()
                && RecipeMatcher.findMatches(input.items().stream().filter(stack -> !stack.isEmpty()).toList(),
                        ingredients) != null);
    }
}
