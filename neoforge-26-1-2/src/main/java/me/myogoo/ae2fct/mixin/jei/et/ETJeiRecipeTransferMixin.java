package me.myogoo.ae2fct.mixin.jei.et;

import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import me.myogoo.ae2fct.util.VirtualFluidRecipePolicy;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Group;

import java.util.Map;

@Pseudo
@Mixin(targets = {
        "me.myogoo.extendedterminal.integration.itemList.jei.extendedterminal.handler.panel.ETCraftingRecipeTransfer",
        "me.myogoo.extendedterminal.integration.itemList.jei.extendedterminal.handler.panel.ETSmithingRecipeTransfer",
        "me.myogoo.extendedterminal.integration.itemList.jei.extendedterminal.handler.panel.ETStonecutterRecipeTransfer",
        "me.myogoo.extendedterminal.integration.itemList.jei.extendedcrafting.handler.ECJeiRecipeTransferHandler",
        "me.myogoo.extendedterminal.integration.itemList.jei.avaritiaRe.handler.AVJeiRecipeTransferHandler"
}, remap = false)
public abstract class ETJeiRecipeTransferMixin {
    @Group(name = "ae2fct$findMissingIngredients", min = 1, max = 1)
    @WrapOperation(method = "transferRecipe", require = 0, at = @At(value = "INVOKE", target =
            "Lme/myogoo/extendedterminal/menu/ETTerminalBaseMenu;findMissingIngredients(Ljava/util/Map;)Lappeng/menu/me/items/CraftingTermMenu$MissingIngredientSlots;"))
    private CraftingTermMenu.MissingIngredientSlots ae2fct$withBaseMenuRecipeContext(
            @Coerce MEStorageMenu menu, Map<Integer, Ingredient> ingredients,
            Operation<CraftingTermMenu.MissingIngredientSlots> original,
            @Coerce MEStorageMenu originalMenu, @Coerce RecipeHolder<?> recipeHolder,
            IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        return ae2fct$withRecipeContext(menu, ingredients, original, recipeHolder);
    }

    @Group(name = "ae2fct$findMissingIngredients", min = 1, max = 1)
    @WrapOperation(method = "transferRecipe", require = 0, at = @At(value = "INVOKE", target =
            "Lme/myogoo/extendedterminal/menu/extendedterminal/ETTerminalMenu;findMissingIngredients(Ljava/util/Map;)Lappeng/menu/me/items/CraftingTermMenu$MissingIngredientSlots;"))
    private CraftingTermMenu.MissingIngredientSlots ae2fct$withTerminalMenuRecipeContext(
            @Coerce MEStorageMenu menu, Map<Integer, Ingredient> ingredients,
            Operation<CraftingTermMenu.MissingIngredientSlots> original,
            @Coerce MEStorageMenu originalMenu, @Coerce RecipeHolder<?> recipeHolder,
            IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        return ae2fct$withRecipeContext(menu, ingredients, original, recipeHolder);
    }

    private static CraftingTermMenu.MissingIngredientSlots ae2fct$withRecipeContext(
            MEStorageMenu menu, Map<Integer, Ingredient> ingredients,
            Operation<CraftingTermMenu.MissingIngredientSlots> original,
            RecipeHolder<?> recipeHolder) {
        return VirtualFluidRecipePolicy.withRecipe(menu, recipeHolder.id(), () -> {
            var result = original.call(menu, ingredients);
            return FluidCraftingTerminalIntegration.resolveMissingIngredients(menu, result, ingredients);
        });
    }
}
