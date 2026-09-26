package me.myogoo.ae2fct.mixin.jei.et;

import appeng.menu.me.items.CraftingTermMenu;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import me.myogoo.extendedterminal.menu.ETTerminalBaseMenu;
import me.myogoo.extendedterminal.menu.extendedterminal.ETTerminalMenu;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;

import java.util.Map;

@Pseudo
@Mixin(targets = {
        "me.myogoo.extendedterminal.integration.itemList.jei.extendedterminal.handler.panel.ETCraftingRecipeTransfer",
        "me.myogoo.extendedterminal.integration.itemList.jei.extendedterminal.handler.panel.ETSmithingRecipeTransfer",
        "me.myogoo.extendedterminal.integration.itemList.jei.extendedterminal.handler.panel.ETStonecutterRecipeTransfer",
        "me.myogoo.extendedterminal.integration.itemList.jei.extendedcrafting.handler.ECJeiRecipeTransferHandler",
        "me.myogoo.extendedterminal.integration.itemList.jei.avaritiaRe.handler.AVJeiRecipeTransferHandler",
        "me.myogoo.extendedterminal.integration.itemList.jei.avaritiaNeo.handler.AVNeoJeiRecipeTransferHandler"
}, remap = false)
public abstract class ETJeiRecipeTransferMixin {
    @Group(name = "ae2fct$findMissingIngredients", min = 1, max = 1)
    @WrapOperation(method = "transferRecipe", require = 0, at = @At(value = "INVOKE", target =
            "Lme/myogoo/extendedterminal/menu/ETTerminalBaseMenu;findMissingIngredients(Ljava/util/Map;)Lappeng/menu/me/items/CraftingTermMenu$MissingIngredientSlots;"))
    private CraftingTermMenu.MissingIngredientSlots ae2fct$withBaseMenuRecipeContext(ETTerminalBaseMenu<?> menu,
            Map<Integer, Ingredient> ingredients, Operation<CraftingTermMenu.MissingIngredientSlots> original,
            @Local(argsOnly = true) RecipeHolder<?> recipe) {
        return FluidCraftingHelper.withVirtualFluidRecipePolicy(menu, recipe == null ? null : recipe.id(),
                () -> original.call(menu, ingredients));
    }

    @Group(name = "ae2fct$findMissingIngredients", min = 1, max = 1)
    @WrapOperation(method = "transferRecipe", require = 0, at = @At(value = "INVOKE", target =
            "Lme/myogoo/extendedterminal/menu/extendedterminal/ETTerminalMenu;findMissingIngredients(Ljava/util/Map;)Lappeng/menu/me/items/CraftingTermMenu$MissingIngredientSlots;"))
    private CraftingTermMenu.MissingIngredientSlots ae2fct$withTerminalMenuRecipeContext(ETTerminalMenu menu,
            Map<Integer, Ingredient> ingredients, Operation<CraftingTermMenu.MissingIngredientSlots> original,
            @Local(argsOnly = true) RecipeHolder<?> recipe) {
        return FluidCraftingHelper.withVirtualFluidRecipePolicy(menu, recipe == null ? null : recipe.id(),
                () -> original.call(menu, ingredients));
    }
}
