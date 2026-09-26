package me.myogoo.ae2fct.mixin.jei.et;

import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;

import java.util.Map;

@Pseudo
@Mixin(targets = {
        "me.myogoo.extendedterminal.integration.jei.extendedterminal.handler.ETCraftingRecipeTransfer",
        "me.myogoo.extendedterminal.integration.jei.extendedterminal.handler.ETSmithingRecipeTransfer",
        "me.myogoo.extendedterminal.integration.jei.extendedterminal.handler.ETStonecutterRecipeTransfer",
        "me.myogoo.extendedterminal.integration.jei.extendedcrafting.handler.ECJeiRecipeTransferHandler",
        "me.myogoo.extendedterminal.integration.jei.avaritiaRe.handler.AVJeiRecipeTransferHandler",
        "me.myogoo.extendedterminal.integration.jei.avaritiaNeo.handler.AVNeoJeiRecipeTransferHandler"
}, remap = false)
public abstract class ETJeiRecipeTransferMixin {
    @Group(name = "ae2fct$findMissingIngredients", min = 1, max = 1)
    @WrapOperation(method = "transferRecipe", require = 0, at = @At(value = "INVOKE", target =
            "Lme/myogoo/extendedterminal/menu/ETTerminalBaseMenu;findMissingIngredients(Ljava/util/Map;)Lappeng/menu/me/items/CraftingTermMenu$MissingIngredientSlots;"))
    private CraftingTermMenu.MissingIngredientSlots ae2fct$withBaseMenuRecipeContext(
            @Coerce MEStorageMenu menu, Map<Integer, Ingredient> ingredients,
            Operation<CraftingTermMenu.MissingIngredientSlots> original,
            @Coerce MEStorageMenu originalMenu, @Coerce Recipe<?> recipe,
            IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        return ae2fct$withRecipeContext(menu, ingredients, original, recipe);
    }

    @Group(name = "ae2fct$findMissingIngredients", min = 1, max = 1)
    @WrapOperation(method = "transferRecipe", require = 0, at = @At(value = "INVOKE", target =
            "Lme/myogoo/extendedterminal/menu/extendedterminal/ETTerminalMenu;findMissingIngredients(Ljava/util/Map;)Lappeng/menu/me/items/CraftingTermMenu$MissingIngredientSlots;"))
    private CraftingTermMenu.MissingIngredientSlots ae2fct$withTerminalMenuRecipeContext(
            @Coerce MEStorageMenu menu, Map<Integer, Ingredient> ingredients,
            Operation<CraftingTermMenu.MissingIngredientSlots> original,
            @Coerce MEStorageMenu originalMenu, @Coerce Recipe<?> recipe,
            IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        return ae2fct$withRecipeContext(menu, ingredients, original, recipe);
    }

    @Group(name = "ae2fct$findMissingIngredients", min = 1, max = 1)
    @WrapOperation(method = "transferRecipe", require = 0, at = @At(value = "INVOKE", target =
            "Lme/myogoo/extendedterminal/menu/extendedcrafting/ExtendedTerminalBaseMenu;findMissingIngredients(Ljava/util/Map;)Lappeng/menu/me/items/CraftingTermMenu$MissingIngredientSlots;"))
    private CraftingTermMenu.MissingIngredientSlots ae2fct$withExtendedCraftingMenuRecipeContext(
            @Coerce MEStorageMenu menu, Map<Integer, Ingredient> ingredients,
            Operation<CraftingTermMenu.MissingIngredientSlots> original,
            @Coerce MEStorageMenu originalMenu, @Coerce Recipe<?> recipe,
            IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        return ae2fct$withRecipeContext(menu, ingredients, original, recipe);
    }

    @Group(name = "ae2fct$findMissingIngredients", min = 1, max = 1)
    @WrapOperation(method = "transferRecipe", require = 0, at = @At(value = "INVOKE", target =
            "Lme/myogoo/extendedterminal/menu/avaritiaRe/AvaritiaTerminalBaseMenu;findMissingIngredients(Ljava/util/Map;)Lappeng/menu/me/items/CraftingTermMenu$MissingIngredientSlots;"))
    private CraftingTermMenu.MissingIngredientSlots ae2fct$withReAvaritiaMenuRecipeContext(
            @Coerce MEStorageMenu menu, Map<Integer, Ingredient> ingredients,
            Operation<CraftingTermMenu.MissingIngredientSlots> original,
            @Coerce MEStorageMenu originalMenu, @Coerce Recipe<?> recipe,
            IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        return ae2fct$withRecipeContext(menu, ingredients, original, recipe);
    }

    @Group(name = "ae2fct$findMissingIngredients", min = 1, max = 1)
    @WrapOperation(method = "transferRecipe", require = 0, at = @At(value = "INVOKE", target =
            "Lme/myogoo/extendedterminal/menu/avaritiaNeo/NeoExtremeTerminalMenu;findMissingIngredients(Ljava/util/Map;)Lappeng/menu/me/items/CraftingTermMenu$MissingIngredientSlots;"))
    private CraftingTermMenu.MissingIngredientSlots ae2fct$withAvaritiaNeoMenuRecipeContext(
            @Coerce MEStorageMenu menu, Map<Integer, Ingredient> ingredients,
            Operation<CraftingTermMenu.MissingIngredientSlots> original,
            @Coerce MEStorageMenu originalMenu, @Coerce Recipe<?> recipe,
            IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer) {
        return ae2fct$withRecipeContext(menu, ingredients, original, recipe);
    }

    private static CraftingTermMenu.MissingIngredientSlots ae2fct$withRecipeContext(
            MEStorageMenu menu, Map<Integer, Ingredient> ingredients,
            Operation<CraftingTermMenu.MissingIngredientSlots> original, Recipe<?> recipe) {
        return FluidCraftingTerminalIntegration.withRecipeViewerContext(menu, recipe == null ? null : recipe.getId(),
                () -> original.call(menu, ingredients));
    }
}
