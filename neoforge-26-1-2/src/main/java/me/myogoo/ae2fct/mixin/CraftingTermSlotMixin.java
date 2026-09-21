package me.myogoo.ae2fct.mixin;

import appeng.menu.slot.CraftingTermSlot;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.myogoo.ae2fct.util.VirtualFluidRecipePolicy;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CraftingTermSlot.class, remap = false)
public abstract class CraftingTermSlotMixin {
    @WrapOperation(method = {"findRecipe", "getRemainingItems"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;matches(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Z"))
    private boolean ae2fct$checkCachedRecipe(CraftingRecipe recipe, RecipeInput input, Level level,
            Operation<Boolean> original, @Local RecipeHolder<CraftingRecipe> holder) {
        return VirtualFluidRecipePolicy.matchesAllowed(holder, input, level,
                () -> original.call(recipe, input, level));
    }
}
