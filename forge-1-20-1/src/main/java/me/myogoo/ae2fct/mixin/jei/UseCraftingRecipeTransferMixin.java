package me.myogoo.ae2fct.mixin.jei;

import appeng.integration.modules.jei.transfer.UseCraftingRecipeTransfer;
import appeng.menu.me.items.CraftingTermMenu;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(value = UseCraftingRecipeTransfer.class, remap = false)
public abstract class UseCraftingRecipeTransferMixin {
    @WrapOperation(method = "transferRecipe", at = @At(value = "INVOKE", target = "Lappeng/menu/me/items/CraftingTermMenu;findMissingIngredients(Ljava/util/Map;)Lappeng/menu/me/items/CraftingTermMenu$MissingIngredientSlots;"))
    private CraftingTermMenu.MissingIngredientSlots ae2fct$withRecipeContext(CraftingTermMenu menu,
            Map<Integer, Ingredient> ingredients, Operation<CraftingTermMenu.MissingIngredientSlots> original,
            CraftingTermMenu originalMenu, CraftingRecipe recipe, IRecipeSlotsView display, Player player,
            boolean maxTransfer, boolean doTransfer) {
        return FluidCraftingTerminalIntegration.withRecipeViewerContext(menu, recipe.getId(),
                () -> original.call(menu, ingredients));
    }
}
