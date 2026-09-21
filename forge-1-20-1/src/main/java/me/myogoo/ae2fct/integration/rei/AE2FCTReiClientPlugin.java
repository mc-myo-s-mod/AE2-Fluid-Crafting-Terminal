package me.myogoo.ae2fct.integration.rei;

import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.integration.recipeviewer.VirtualFluidBlacklistRecipes;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;

@REIPluginClient
public final class AE2FCTReiClientPlugin implements REIClientPlugin {
    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new VirtualFluidBlacklistReiCategory());
        registry.addWorkstations(VirtualFluidBlacklistReiDisplay.CATEGORY,
                EntryStacks.of(AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get()));
        registry.removePlusButton(VirtualFluidBlacklistReiDisplay.CATEGORY);
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (var recipe : VirtualFluidBlacklistRecipes.fromLocalConfig(registry.getRecipeManager(), registryAccess())) {
            registry.add(new VirtualFluidBlacklistReiDisplay(recipe));
        }
    }

    private static RegistryAccess registryAccess() {
        var level = Minecraft.getInstance().level;
        return level == null ? RegistryAccess.EMPTY : level.registryAccess();
    }
}
