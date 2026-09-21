package me.myogoo.ae2fct.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.integration.recipeviewer.VirtualFluidBlacklistRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;

@EmiEntrypoint
public final class AE2FCTEmiPlugin implements EmiPlugin {
    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            Ae2fct.makeId(VirtualFluidBlacklistRecipes.CATEGORY_PATH),
            EmiStack.of(AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(CATEGORY);
        registry.addWorkstation(CATEGORY, EmiStack.of(AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get()));
        for (var recipe : VirtualFluidBlacklistRecipes.fromLocalConfig(registry.getRecipeManager(), registryAccess())) {
            registry.addRecipe(new VirtualFluidBlacklistEmiRecipe(recipe));
        }
    }

    private static RegistryAccess registryAccess() {
        var level = Minecraft.getInstance().level;
        return level == null ? RegistryAccess.EMPTY : level.registryAccess();
    }
}
