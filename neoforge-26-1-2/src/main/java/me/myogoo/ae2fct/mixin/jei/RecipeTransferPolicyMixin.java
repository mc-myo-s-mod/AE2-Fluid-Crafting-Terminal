package me.myogoo.ae2fct.mixin.jei;

import appeng.menu.me.items.CraftingTermMenu;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.myogoo.ae2fct.util.VirtualFluidRecipePolicy;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets = "appeng.client.integrations.jei.transfer.UseCraftingRecipeTransfer", remap = false)
public abstract class RecipeTransferPolicyMixin {
    @WrapMethod(method = "transferRecipe(Lappeng/menu/me/items/CraftingTermMenu;Lnet/minecraft/world/item/crafting/RecipeHolder;Lmezz/jei/api/gui/ingredient/IRecipeSlotsView;Lnet/minecraft/world/entity/player/Player;ZZ)Lmezz/jei/api/recipe/transfer/IRecipeTransferError;")
    private IRecipeTransferError ae2fct$withRecipePolicy(CraftingTermMenu menu, RecipeHolder<CraftingRecipe> recipe,
            IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer,
            Operation<IRecipeTransferError> original) {
        return VirtualFluidRecipePolicy.withRecipe(menu, recipe.id(),
                () -> original.call(menu, recipe, slots, player, maxTransfer, doTransfer));
    }
}
