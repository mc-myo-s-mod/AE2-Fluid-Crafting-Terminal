package me.myogoo.ae2fct.util;

import appeng.menu.me.common.MEStorageMenu;
import me.myogoo.ae2fct.api.IMEStorageMenu;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class VirtualFluidRecipePolicy {
    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<>();

    private VirtualFluidRecipePolicy() {
    }

    public static <T> T withRecipe(MEStorageMenu menu, @Nullable ResourceKey<Recipe<?>> recipeId,
            Supplier<T> action) {
        Context previous = CONTEXT.get();
        CONTEXT.set(new Context(menu, recipeId));
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CONTEXT.remove();
            } else {
                CONTEXT.set(previous);
            }
        }
    }

    public static boolean isAllowed(RecipeHolder<?> recipe, RecipeInput input, Level level) {
        return !containsVirtualFluid(input) || !isBlacklisted(recipe.id(), level);
    }

    public static boolean matchesAllowed(RecipeHolder<?> recipe, RecipeInput input, Level level,
            BooleanSupplier matches) {
        if (!containsVirtualFluid(input)) {
            return matches.getAsBoolean();
        }
        if (!isAllowed(recipe, input, level)) {
            return false;
        }
        return FluidCraftingHelper.withFluidCraftingEnabled(true, recipe.id(), level, matches);
    }

    public static boolean containsVirtualFluid(RecipeInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
            if (FluidCraftingHelper.isVirtualFluidItem(input.getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlacklisted(ResourceKey<Recipe<?>> recipeId, Level level) {
        if (!level.isClientSide()) {
            return FluidCraftingConfig.isVirtualFluidRecipeBlacklisted(recipeId);
        }
        Context context = CONTEXT.get();
        return context != null && (!(context.menu() instanceof IMEStorageMenu menu)
                || !menu.ae2fct$allowsVirtualFluidRecipe(recipeId));
    }

    public static boolean isPreviewAllowed() {
        Context context = CONTEXT.get();
        return context != null && context.recipeId() != null
                && !isBlacklisted(context.recipeId(), context.menu().getPlayer().level());
    }

    public static boolean isCurrentRecipeBlocked() {
        Context context = CONTEXT.get();
        return FluidCraftingHelper.isCurrentRecipeBlacklisted()
                || context != null && context.recipeId() != null
                        && isBlacklisted(context.recipeId(), context.menu().getPlayer().level());
    }

    private record Context(MEStorageMenu menu, @Nullable ResourceKey<Recipe<?>> recipeId) {
    }
}
