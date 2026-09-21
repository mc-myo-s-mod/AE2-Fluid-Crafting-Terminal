package me.myogoo.ae2fct.integration;

import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.me.common.IClientRepo;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.util.prioritylist.IPartitionList;
import me.myogoo.ae2fct.api.IMEStorageMenu;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import me.myogoo.myotus.api.MyotusAPI;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class FluidCraftingTerminalIntegration {
    private static final ThreadLocal<Boolean> FILL_GRID_CONTEXT = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<ResourceLocation> FILL_GRID_RECIPE_ID = new ThreadLocal<>();
    private static final ThreadLocal<ResourceLocation> RECIPE_CONTEXT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> RECIPE_VIEWER_CONTEXT = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<MEStorageMenu> RECIPE_VIEWER_MENU = new ThreadLocal<>();

    private FluidCraftingTerminalIntegration() {
    }

    public static boolean hasFluidInteractUpgrade(MEStorageMenu menu) {
        return FluidCraftingConfig.QoL()
                || MyotusAPI.terminalUpgrades().hasUpgrade(menu, AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get());
    }

    public static void beginFillGridContext(ServerPlayer player, Predicate<MEStorageMenu> menuPredicate) {
        beginFillGridContext(player, menuPredicate, null);
    }

    public static void beginFillGridContext(ServerPlayer player, Predicate<MEStorageMenu> menuPredicate,
            @Nullable ResourceLocation recipeId) {
        boolean enabled = false;
        if (player.containerMenu instanceof MEStorageMenu menu) {
            enabled = menuPredicate.test(menu);
        }
        beginFillGridContext(enabled, recipeId);
    }

    public static void beginFillGridContext(boolean enabled) {
        beginFillGridContext(enabled, null);
    }

    public static void beginFillGridContext(boolean enabled, @Nullable ResourceLocation recipeId) {
        FILL_GRID_CONTEXT.set(enabled);
        setRecipeContext(recipeId);
        if (recipeId == null) {
            FILL_GRID_RECIPE_ID.remove();
        } else {
            FILL_GRID_RECIPE_ID.set(recipeId);
        }
    }

    public static boolean isFillGridContextEnabled() {
        return FILL_GRID_CONTEXT.get() && !isCurrentFillGridRecipeBlacklisted();
    }

    public static boolean isCurrentFillGridRecipeBlacklisted() {
        return FluidCraftingConfig.isVirtualFluidRecipeBlacklisted(FILL_GRID_RECIPE_ID.get());
    }

    public static void endFillGridContext() {
        FILL_GRID_CONTEXT.remove();
        FILL_GRID_RECIPE_ID.remove();
        RECIPE_CONTEXT_ID.remove();
    }

    public static @Nullable ResourceLocation setRecipeContext(@Nullable ResourceLocation recipeId) {
        ResourceLocation previous = RECIPE_CONTEXT_ID.get();
        if (recipeId == null) {
            RECIPE_CONTEXT_ID.remove();
        } else {
            RECIPE_CONTEXT_ID.set(recipeId);
        }
        return previous;
    }

    public static void restoreRecipeContext(@Nullable ResourceLocation recipeId) {
        setRecipeContext(recipeId);
    }

    public static boolean isCurrentRecipeBlacklisted() {
        ResourceLocation recipeId = RECIPE_CONTEXT_ID.get();
        if (RECIPE_VIEWER_CONTEXT.get()) {
            return recipeId == null || ae2fct$isRecipeViewerRecipeBlacklisted(RECIPE_VIEWER_MENU.get(), recipeId);
        }
        return FluidCraftingConfig.isVirtualFluidRecipeBlacklisted(recipeId);
    }

    public static boolean hasVirtualFluidInput(Container container) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (FluidCraftingHelper.isVirtualFluidItem(container.getItem(slot))) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldApplyVirtualFluidRecipeBlacklist(Level level) {
        return !level.isClientSide() || RECIPE_VIEWER_CONTEXT.get();
    }

    public static <T> T withRecipeViewerMenuContext(MEStorageMenu menu, Supplier<T> action) {
        return withRecipeViewerContext(menu, null, true, action);
    }

    public static <T> T withRecipeViewerContext(MEStorageMenu menu, @Nullable ResourceLocation recipeId,
            Supplier<T> action) {
        return withRecipeViewerContext(menu, recipeId, recipeId != null, action);
    }

    public static void withRecipeViewerContext(MEStorageMenu menu, @Nullable ResourceLocation recipeId,
            Runnable action) {
        withRecipeViewerContext(menu, recipeId, () -> {
            action.run();
            return null;
        });
    }

    private static <T> T withRecipeViewerContext(MEStorageMenu menu, @Nullable ResourceLocation recipeId,
            boolean viewerContext, Supplier<T> action) {
        ResourceLocation previousRecipeId = RECIPE_CONTEXT_ID.get();
        boolean previousViewerContext = RECIPE_VIEWER_CONTEXT.get();
        MEStorageMenu previousMenu = RECIPE_VIEWER_MENU.get();

        setRecipeContext(recipeId);
        RECIPE_VIEWER_CONTEXT.set(viewerContext);
        RECIPE_VIEWER_MENU.set(menu);
        try {
            return action.get();
        } finally {
            restoreRecipeContext(previousRecipeId);
            if (previousViewerContext) {
                RECIPE_VIEWER_CONTEXT.set(true);
            } else {
                RECIPE_VIEWER_CONTEXT.remove();
            }
            if (previousMenu == null) {
                RECIPE_VIEWER_MENU.remove();
            } else {
                RECIPE_VIEWER_MENU.set(previousMenu);
            }
        }
    }

    public static boolean canResolveRecipeViewerFluidIngredients() {
        return RECIPE_VIEWER_CONTEXT.get() && RECIPE_CONTEXT_ID.get() != null && !isCurrentRecipeBlacklisted();
    }

    public static CraftingTermMenu.MissingIngredientSlots resolveMissingIngredients(
            CraftingTermMenu.MissingIngredientSlots result,
            Map<Integer, Ingredient> ingredients,
            IClientRepo clientRepo) {
        if (result == null || !result.anyMissingOrCraftable() || clientRepo == null
                || !canResolveRecipeViewerFluidIngredients()) {
            return result;
        }

        Set<Integer> newMissing = new HashSet<>(result.missingSlots());
        Set<Integer> newCraftable = new HashSet<>(result.craftableSlots());
        Set<Integer> slotsToCheck = new LinkedHashSet<>();
        boolean changed = false;

        slotsToCheck.addAll(result.missingSlots());
        slotsToCheck.addAll(result.craftableSlots());

        for (int slot : slotsToCheck) {
            Ingredient ingredient = ingredients.get(slot);
            if (ingredient == null) {
                continue;
            }

            var availability = FluidCraftingHelper.checkFluidAvailabilityInClientRepo(ingredient, clientRepo);
            if (availability.available()) {
                newMissing.remove(slot);
                newCraftable.remove(slot);
                changed = true;
            } else if (availability.craftable() && newMissing.contains(slot)) {
                newMissing.remove(slot);
                newCraftable.add(slot);
                changed = true;
            }
        }

        if (!changed) {
            return result;
        }

        return new CraftingTermMenu.MissingIngredientSlots(newMissing, newCraftable);
    }

    public static boolean hasFluidIngredient(Ingredient ingredient, IClientRepo clientRepo) {
        if (!canResolveRecipeViewerFluidIngredients()) {
            return false;
        }
        return FluidCraftingHelper.hasAvailableFluidInClientRepo(ingredient, clientRepo, AEFluidKey.AMOUNT_BUCKET);
    }

    public static boolean isFluidCraftable(ItemStack itemStack, IClientRepo clientRepo) {
        if (clientRepo == null || !canResolveRecipeViewerFluidIngredients()) {
            return false;
        }

        var availability = FluidCraftingHelper.checkFluidAvailabilityInClientRepo(Ingredient.of(itemStack), clientRepo);
        return availability.craftable();
    }

    public static List<AEItemKey> mergeBestMatchingFluidItemKeys(Ingredient ingredient, IPartitionList filter,
            KeyCounter storage, List<AEItemKey> originalResults) {
        List<AEItemKey> fluidResults = FluidCraftingHelper.findBestMatchingFluidItemKeys(ingredient, filter, storage);
        if (fluidResults.isEmpty()) {
            return originalResults;
        }

        if (originalResults == null || originalResults.isEmpty()) {
            return fluidResults;
        }

        Set<AEItemKey> enhancedResults = new LinkedHashSet<>(fluidResults);
        enhancedResults.addAll(originalResults);
        return new ArrayList<>(enhancedResults);
    }

    public static Optional<AEItemKey> findCraftableFluidItemKey(Ingredient ingredient,
            ICraftingService craftingService, Optional<AEItemKey> originalResult) {
        if (originalResult.isPresent()) {
            return originalResult;
        }

        return FluidCraftingHelper.findCraftableFluidItemKey(ingredient, craftingService);
    }

    public static boolean clientRepoHasFluid(IClientRepo clientRepo, AEFluidKey fluidKey) {
        if (clientRepo == null || !canResolveRecipeViewerFluidIngredients()) {
            return false;
        }

        for (GridInventoryEntry entry : clientRepo.getAllEntries()) {
            if (fluidKey.equals(entry.getWhat()) && entry.getStoredAmount() >= AEFluidKey.AMOUNT_BUCKET) {
                return true;
            }
        }
        return false;
    }

    private static boolean ae2fct$isRecipeViewerRecipeBlacklisted(MEStorageMenu menu, ResourceLocation recipeId) {
        if (menu instanceof IMEStorageMenu ae2fctMenu) {
            return ae2fctMenu.ae2fct$isVirtualFluidRecipeBlacklisted(recipeId);
        }
        return FluidCraftingConfig.isVirtualFluidRecipeBlacklisted(recipeId);
    }
}
