package me.myogoo.ae2fct.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {

    @WrapMethod(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;")
    private <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> ae2fct$findVirtualFluidRecipeWithPolicy(
            RecipeType<T> type,
            I input,
            Level level,
            RecipeHolder<T> hint,
            Operation<Optional<RecipeHolder<T>>> original) {
        if (type != RecipeType.CRAFTING || !(input instanceof CraftingInput craftingInput)
                || !FluidCraftingHelper.containsVirtualFluidItem(craftingInput)) {
            return original.call(type, input, level, hint);
        }

        if (hint != null && hint.value() instanceof CraftingRecipe recipe
                && FluidCraftingHelper.matchesCraftingRecipeWithVirtualFluidPolicy(null, hint.id(), recipe,
                        craftingInput, level)) {
            return Optional.of(hint);
        }

        @SuppressWarnings("unchecked")
        Optional<RecipeHolder<T>> result = (Optional<RecipeHolder<T>>) (Optional<?>) FluidCraftingHelper
                .findCraftingRecipeWithVirtualFluidPolicy(null, craftingInput, level);
        return result;
    }

    @WrapMethod(method = "getRecipesFor")
    private <I extends RecipeInput, T extends Recipe<I>> List<RecipeHolder<T>> ae2fct$findVirtualFluidRecipesWithPolicy(
            RecipeType<T> type, I input, Level level, Operation<List<RecipeHolder<T>>> original) {
        if (type != RecipeType.CRAFTING || !(input instanceof CraftingInput craftingInput)
                || !FluidCraftingHelper.containsVirtualFluidItem(craftingInput)) {
            return original.call(type, input, level);
        }

        @SuppressWarnings("unchecked")
        List<RecipeHolder<T>> result = (List<RecipeHolder<T>>) (List<?>) level.getRecipeManager()
                .getAllRecipesFor(RecipeType.CRAFTING).stream()
                .filter(recipe -> FluidCraftingHelper.matchesCraftingRecipeWithVirtualFluidPolicy(null, recipe,
                        craftingInput, level))
                .sorted(Comparator.comparing(recipe -> recipe.value().getResultItem(level.registryAccess())
                        .getDescriptionId()))
                .toList();
        return result;
    }
}
