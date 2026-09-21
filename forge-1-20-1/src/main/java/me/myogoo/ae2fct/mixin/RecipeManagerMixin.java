package me.myogoo.ae2fct.mixin;

import com.mojang.datafixers.util.Pair;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    @Shadow
    private <C extends Container, T extends Recipe<C>> Map<ResourceLocation, T> byType(RecipeType<T> recipeType) {
        throw new AssertionError();
    }

    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private <C extends Container, T extends Recipe<C>> void ae2fct$getRecipeFor(RecipeType<T> recipeType,
            C container, Level level, CallbackInfoReturnable<Optional<T>> cir) {
        if (!FluidCraftingTerminalIntegration.hasVirtualFluidInput(container)) {
            return;
        }
        if (!FluidCraftingTerminalIntegration.shouldApplyVirtualFluidRecipeBlacklist(level)) {
            return;
        }

        cir.setReturnValue(this.byType(recipeType).values().stream()
                .filter(recipe -> ae2fct$matchesWithRecipeContext(recipe, container, level))
                .findFirst());
    }

    @Inject(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;", at = @At("HEAD"), cancellable = true)
    private <C extends Container, T extends Recipe<C>> void ae2fct$getRecipeFor(RecipeType<T> recipeType,
            C container, Level level, @Nullable ResourceLocation recipeId,
            CallbackInfoReturnable<Optional<Pair<ResourceLocation, T>>> cir) {
        if (!FluidCraftingTerminalIntegration.hasVirtualFluidInput(container)) {
            return;
        }
        if (!FluidCraftingTerminalIntegration.shouldApplyVirtualFluidRecipeBlacklist(level)) {
            return;
        }

        Map<ResourceLocation, T> recipes = this.byType(recipeType);
        if (recipeId != null) {
            T hintedRecipe = recipes.get(recipeId);
            if (hintedRecipe != null && ae2fct$matchesWithRecipeContext(hintedRecipe, container, level)) {
                cir.setReturnValue(Optional.of(Pair.of(recipeId, hintedRecipe)));
                return;
            }
        }

        cir.setReturnValue(recipes.entrySet().stream()
                .filter(entry -> ae2fct$matchesWithRecipeContext(entry.getValue(), container, level))
                .findFirst()
                .map(entry -> Pair.of(entry.getKey(), entry.getValue())));
    }

    @Inject(method = "getRecipesFor", at = @At("HEAD"), cancellable = true)
    private <C extends Container, T extends Recipe<C>> void ae2fct$getRecipesFor(RecipeType<T> recipeType,
            C container, Level level, CallbackInfoReturnable<List<T>> cir) {
        if (!FluidCraftingTerminalIntegration.hasVirtualFluidInput(container)) {
            return;
        }
        if (!FluidCraftingTerminalIntegration.shouldApplyVirtualFluidRecipeBlacklist(level)) {
            return;
        }

        cir.setReturnValue(this.byType(recipeType).values().stream()
                .filter(recipe -> ae2fct$matchesWithRecipeContext(recipe, container, level))
                .sorted(Comparator.comparing(recipe -> recipe.getResultItem(level.registryAccess()).getDescriptionId()))
                .collect(Collectors.toList()));
    }

    private static <C extends Container> boolean ae2fct$matchesWithRecipeContext(Recipe<C> recipe, C container,
            Level level) {
        return ae2fct$withRecipeContext(recipe.getId(), () -> {
            if (FluidCraftingTerminalIntegration.isCurrentRecipeBlacklisted()
                    && FluidCraftingTerminalIntegration.hasVirtualFluidInput(container)) {
                return false;
            }
            return recipe.matches(container, level);
        });
    }

    private static boolean ae2fct$withRecipeContext(@Nullable ResourceLocation recipeId, BooleanSupplier action) {
        ResourceLocation previous = FluidCraftingTerminalIntegration.setRecipeContext(recipeId);
        try {
            return action.getAsBoolean();
        } finally {
            FluidCraftingTerminalIntegration.restoreRecipeContext(previous);
        }
    }
}
