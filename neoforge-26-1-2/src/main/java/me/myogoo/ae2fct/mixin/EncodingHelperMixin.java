package me.myogoo.ae2fct.mixin;

import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.api.stacks.GenericStack;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.myogoo.ae2fct.util.VirtualFluidRecipePolicy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.function.Predicate;

@Mixin(value = EncodingHelper.class, remap = false)
public abstract class EncodingHelperMixin {
    @WrapMethod(method = "encodeCraftingRecipe")
    private static void ae2fct$withRecipePolicy(PatternEncodingTermMenu menu, RecipeHolder<?> recipe,
            List<List<GenericStack>> ingredients, Predicate<ItemStack> visible, Operation<Void> original) {
        VirtualFluidRecipePolicy.withRecipe(menu, recipe == null ? null : recipe.id(),
                () -> original.call(menu, recipe, ingredients, visible));
    }
}
