package me.myogoo.ae2fct.integration;

import appeng.menu.me.common.MEStorageMenu;
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
}
