package me.myogoo.ae2fct.mixin;

import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(value = PatternEncodingTermMenu.class, remap = false)
public abstract class PatternEncodingTermMenuMixin extends MEStorageMenu {

    @Shadow
    @Nullable
    private RecipeHolder<CraftingRecipe> currentRecipe;

    private PatternEncodingTermMenuMixin() {
        super(null, 0, null, null, false);
    }

    @WrapOperation(method = "getAndUpdateOutput", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;matches(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Z"), require = 1)
    private boolean ae2fct$matchCachedRecipeWithVirtualFluidPolicy(CraftingRecipe recipe, RecipeInput input,
            Level level, Operation<Boolean> original) {
        if (this.currentRecipe == null || !(input instanceof CraftingInput craftingInput)) {
            return original.call(recipe, input, level);
        }

        return FluidCraftingHelper.matchesCraftingRecipeWithVirtualFluidPolicy(this, this.currentRecipe.id(),
                recipe, craftingInput, level);
    }

    @WrapOperation(method = "getAndUpdateOutput", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/RecipeManager;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"), require = 1)
    private <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> ae2fct$findRecipeWithVirtualFluidPolicy(
            RecipeManager recipeManager,
            RecipeType<T> type,
            I input,
            Level level,
            Operation<Optional<RecipeHolder<T>>> original) {
        if (type == RecipeType.CRAFTING && input instanceof CraftingInput craftingInput) {
            if (!FluidCraftingHelper.containsVirtualFluidItem(craftingInput)) {
                return original.call(recipeManager, type, input, level);
            }

            @SuppressWarnings("unchecked")
            Optional<RecipeHolder<T>> result = (Optional<RecipeHolder<T>>) (Optional<?>) FluidCraftingHelper
                    .findCraftingRecipeWithVirtualFluidPolicy(this, craftingInput, level);
            return result;
        }

        return original.call(recipeManager, type, input, level);
    }
}
