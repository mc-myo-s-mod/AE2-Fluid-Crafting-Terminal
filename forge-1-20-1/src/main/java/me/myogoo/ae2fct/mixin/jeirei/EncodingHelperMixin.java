package me.myogoo.ae2fct.mixin.jeirei;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.jeirei.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;
import java.util.function.Predicate;

@Mixin(value = EncodingHelper.class, remap = false)
public abstract class EncodingHelperMixin {
    @WrapMethod(method = "encodeCraftingRecipe", remap = false)
    private static void ae2fct$withRecipeContext(PatternEncodingTermMenu menu, @Nullable Recipe<?> recipe,
            List<List<GenericStack>> genericIngredients, Predicate<ItemStack> visiblePredicate,
            Operation<Void> original) {
        FluidCraftingTerminalIntegration.withRecipeViewerContext(menu, recipe == null ? null : recipe.getId(),
                () -> original.call(menu, recipe, genericIngredients, visiblePredicate));
    }
}
