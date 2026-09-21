package me.myogoo.ae2fct.mixin;

import appeng.helpers.ICraftingGridMenu;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.slot.CraftingTermSlot;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CraftingTermSlot.class, remap = false)
public abstract class CraftingTermSlotMixin {

    @Shadow
    @Final
    private ICraftingGridMenu menu;

    @WrapOperation(method = "getRemainingItems", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;matches(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Z"))
    private boolean ae2fct$checkRemainingItemsRecipe(CraftingRecipe recipe, RecipeInput input, Level level,
            Operation<Boolean> original, @Local RecipeHolder<CraftingRecipe> holder) {
        if (!(input instanceof CraftingInput craftingInput)) {
            return original.call(recipe, input, level);
        }
        return FluidCraftingHelper.matchesCraftingRecipeWithVirtualFluidPolicy(
                this.menu instanceof MEStorageMenu storageMenu ? storageMenu : null,
                holder.id(), recipe, craftingInput, level);
    }

    @WrapMethod(method = "findRecipe")
    private RecipeHolder<CraftingRecipe> ae2fct$findRecipeWithVirtualFluidPolicy(CraftingInput input, Level level,
            Operation<RecipeHolder<CraftingRecipe>> original) {
        if (!(this.menu instanceof CraftingTermMenu terminalMenu)) {
            return original.call(input, level);
        }

        if (!FluidCraftingHelper.containsVirtualFluidItem(input)) {
            return original.call(input, level);
        }

        RecipeHolder<CraftingRecipe> recipe = terminalMenu.getCurrentRecipe();
        MEStorageMenu storageMenu = (MEStorageMenu) terminalMenu;
        if (recipe != null && FluidCraftingHelper.matchesCraftingRecipeWithVirtualFluidPolicy(storageMenu, recipe,
                input, level)) {
            return recipe;
        }

        return FluidCraftingHelper.findCraftingRecipeWithVirtualFluidPolicy(storageMenu, input, level)
                .orElse(null);
    }
}
