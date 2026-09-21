package me.myogoo.ae2fct.mixin;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.function.Predicate;

@Mixin(value = EncodingHelper.class, remap = false)
public abstract class EncodingHelperMixin {

    @WrapMethod(method = "encodeCraftingRecipe")
    private static void ae2fct$encodeWithVirtualFluidPolicy(PatternEncodingTermMenu menu,
            RecipeHolder<?> recipe,
            List<List<GenericStack>> genericIngredients,
            Predicate<ItemStack> visiblePredicate,
            Operation<Void> original) {
        FluidCraftingHelper.withVirtualFluidRecipePolicy(menu, recipe == null ? null : recipe.id(),
                () -> original.call(menu, recipe, genericIngredients, visiblePredicate));
    }
}
