package me.myogoo.ae2fct.mixin.itemlists;

import appeng.menu.me.items.CraftingTermMenu;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Pseudo
@Mixin(targets = {
        "appeng.integration.modules.emi.EmiUseCraftingRecipeHandler",
        "appeng.integration.modules.rei.transfer.UseCraftingRecipeTransfer",
        "tamaized.ae2jeiintegration.integration.modules.jei.transfer.UseCraftingRecipeTransfer"
}, remap = false)
public abstract class RecipeTransferPreviewMixin {
    @WrapOperation(method = "transferRecipe", at = @At(value = "INVOKE",
            target = "Lappeng/menu/me/items/CraftingTermMenu;findMissingIngredients(Ljava/util/Map;)Lappeng/menu/me/items/CraftingTermMenu$MissingIngredientSlots;"))
    private CraftingTermMenu.MissingIngredientSlots ae2fct$withRecipePolicy(CraftingTermMenu menu,
            Map<Integer, Ingredient> ingredients, Operation<CraftingTermMenu.MissingIngredientSlots> original,
            @Local(argsOnly = true) RecipeHolder<?> recipe) {
        return FluidCraftingHelper.withVirtualFluidRecipePolicy(menu, recipe == null ? null : recipe.id(),
                () -> original.call(menu, ingredients));
    }
}
