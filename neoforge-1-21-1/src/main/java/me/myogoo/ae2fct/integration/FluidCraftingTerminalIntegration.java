package me.myogoo.ae2fct.integration;

import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.HashSet;
import java.util.Map;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.myotus.menu.TerminalUpgradeHelper;

public final class FluidCraftingTerminalIntegration {
    private FluidCraftingTerminalIntegration() {
    }

    public static boolean hasFluidInteractUpgrade(MEStorageMenu menu) {
        return FluidCraftingConfig.QoL()
                || TerminalUpgradeHelper.hasUpgrade(menu, AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get());
    }

    public static CraftingTermMenu.MissingIngredientSlots resolveMissingIngredients(MEStorageMenu menu,
            Map<Integer, Ingredient> ingredients, CraftingTermMenu.MissingIngredientSlots result) {
        var repo = menu.getClientRepo();
        if (!result.anyMissingOrCraftable() || repo == null) return result;
        var missing = new HashSet<>(result.missingSlots());
        var craftable = new HashSet<>(result.craftableSlots());
        for (int slot : result.missingSlots()) {
            var ingredient = ingredients.get(slot);
            if (ingredient != null && FluidCraftingHelper.checkFluidAvailabilityInClientRepo(ingredient, repo).craftable()) {
                missing.remove(slot);
                craftable.add(slot);
            }
        }
        return missing.equals(result.missingSlots()) ? result
                : new CraftingTermMenu.MissingIngredientSlots(missing, craftable);
    }
}
