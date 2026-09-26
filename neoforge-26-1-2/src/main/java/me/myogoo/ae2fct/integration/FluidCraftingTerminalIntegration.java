package me.myogoo.ae2fct.integration;

import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.common.IClientRepo;
import appeng.menu.me.items.CraftingTermMenu;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import me.myogoo.ae2fct.util.VirtualFluidRecipePolicy;
import me.myogoo.myotus.menu.TerminalUpgradeHelper;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class FluidCraftingTerminalIntegration {
    private FluidCraftingTerminalIntegration() {
    }

    public static boolean hasFluidInteractUpgrade(MEStorageMenu menu) {
        return FluidCraftingConfig.QoL()
                || TerminalUpgradeHelper.hasUpgrade(menu, AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get());
    }

    public static CraftingTermMenu.MissingIngredientSlots resolveMissingIngredients(MEStorageMenu menu,
            CraftingTermMenu.MissingIngredientSlots result, Map<Integer, Ingredient> ingredients) {
        if (result == null || !result.anyMissingOrCraftable()
                || !hasFluidInteractUpgrade(menu) || !VirtualFluidRecipePolicy.isPreviewAllowed()) {
            return result;
        }

        IClientRepo clientRepo = menu.getClientRepo();
        if (clientRepo == null) {
            return result;
        }

        Set<Integer> newMissing = new HashSet<>(result.missingSlots());
        Set<Integer> newCraftable = new HashSet<>(result.craftableSlots());
        Set<Integer> slotsToCheck = new HashSet<>();
        slotsToCheck.addAll(result.missingSlots());
        slotsToCheck.addAll(result.craftableSlots());

        boolean changed = false;
        for (int slot : slotsToCheck) {
            Ingredient ingredient = ingredients.get(slot);
            if (ingredient == null) {
                continue;
            }

            var availability = FluidCraftingHelper.checkFluidAvailabilityInClientRepo(
                    ingredient, clientRepo, menu.getPlayer().level());
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

        return changed
                ? new CraftingTermMenu.MissingIngredientSlots(newMissing, newCraftable)
                : result;
    }
}
