package me.myogoo.ae2fct.mixin.emi;

import appeng.integration.modules.emi.EmiUseCraftingRecipeHandler;
import appeng.menu.me.items.CraftingTermMenu;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.emi.api.recipe.EmiRecipe;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(value = EmiUseCraftingRecipeHandler.class, remap = false)
public abstract class EmiUseCraftingRecipeHandlerMixin {
    @WrapOperation(method = "transferRecipe", at = @At(value = "INVOKE", target = "Lappeng/menu/me/items/CraftingTermMenu;findMissingIngredients(Ljava/util/Map;)Lappeng/menu/me/items/CraftingTermMenu$MissingIngredientSlots;"))
    private CraftingTermMenu.MissingIngredientSlots ae2fct$withRecipeContext(CraftingTermMenu menu,
            Map<Integer, Ingredient> ingredients, Operation<CraftingTermMenu.MissingIngredientSlots> original,
            CraftingTermMenu originalMenu, Recipe<?> recipeBase, EmiRecipe emiRecipe, boolean doTransfer) {
        return FluidCraftingTerminalIntegration.withRecipeViewerContext(menu,
                recipeBase == null ? null : recipeBase.getId(), () -> original.call(menu, ingredients));
    }
}
