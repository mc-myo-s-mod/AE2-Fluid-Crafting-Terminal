package me.myogoo.ae2fct.integration;

import appeng.api.networking.crafting.ICraftingService;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.menu.me.common.IClientRepo;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.util.prioritylist.IPartitionList;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import me.myogoo.myotus.api.MyotusAPI.Terminal.TerminalUpgradeHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class FluidCraftingTerminalIntegration {
    private static final ThreadLocal<Boolean> FILL_GRID_CONTEXT = ThreadLocal.withInitial(() -> false);

    private FluidCraftingTerminalIntegration() {
    }

    public static boolean hasFluidInteractUpgrade(MEStorageMenu menu) {
        return FluidCraftingConfig.allowFluidInteractionWithoutUpgrade()
                || TerminalUpgradeHelper.hasUpgrade(menu, AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get());
    }

    public static void beginFillGridContext(ServerPlayer player, Predicate<MEStorageMenu> menuPredicate) {
        boolean enabled = false;
        if (player.containerMenu instanceof MEStorageMenu menu) {
            enabled = menuPredicate.test(menu);
        }
        beginFillGridContext(enabled);
    }

    public static void beginFillGridContext(boolean enabled) {
        FILL_GRID_CONTEXT.set(enabled);
    }

    public static boolean isFillGridContextEnabled() {
        return FILL_GRID_CONTEXT.get();
    }

    public static void endFillGridContext() {
        FILL_GRID_CONTEXT.remove();
    }

    public static CraftingTermMenu.MissingIngredientSlots resolveMissingIngredients(
            CraftingTermMenu.MissingIngredientSlots result,
            Map<Integer, Ingredient> ingredients,
            IClientRepo clientRepo) {
        if (result == null || !result.anyMissingOrCraftable() || clientRepo == null) {
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
        return FluidCraftingHelper.hasAvailableFluidInClientRepo(ingredient, clientRepo, AEFluidKey.AMOUNT_BUCKET);
    }

    public static boolean isFluidCraftable(ItemStack itemStack, IClientRepo clientRepo) {
        if (clientRepo == null) {
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
}
