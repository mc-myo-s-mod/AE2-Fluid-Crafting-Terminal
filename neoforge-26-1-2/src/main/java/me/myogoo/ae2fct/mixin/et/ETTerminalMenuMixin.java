package me.myogoo.ae2fct.mixin.et;

import appeng.menu.me.common.MEStorageMenu;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import me.myogoo.ae2fct.util.VirtualFluidRecipePolicy;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "me.myogoo.extendedterminal.menu.extendedterminal.ETTerminalMenu", remap = false)
public abstract class ETTerminalMenuMixin extends MEStorageMenu {
    private ETTerminalMenuMixin() {
        super(null, 0, null, null, false);
    }

    @ModifyExpressionValue(method = "updateCraftingOutput", at = @At(value = "INVOKE",
            target = "Lappeng/crafting/RecipeAccess;getRecipeFor(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;)Lnet/minecraft/world/item/crafting/RecipeHolder;"))
    private RecipeHolder<CraftingRecipe> ae2fct$filterVirtualFluidOutput(RecipeHolder<CraftingRecipe> recipe,
            @Local CraftingInput input) {
        return recipe != null && VirtualFluidRecipePolicy.withRecipe(this, recipe.id(),
                () -> VirtualFluidRecipePolicy.isAllowed(recipe, input, getPlayer().level())) ? recipe : null;
    }
}
