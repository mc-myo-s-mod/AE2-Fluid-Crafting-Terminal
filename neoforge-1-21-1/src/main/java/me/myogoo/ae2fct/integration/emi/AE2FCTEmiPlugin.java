package me.myogoo.ae2fct.integration.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.integration.recipeviewer.VirtualFluidBlacklistRecipes;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@EmiEntrypoint
public final class AE2FCTEmiPlugin implements EmiPlugin {
    private static final EmiRecipeCategory VIRTUAL_FLUID_BLACKLIST = new EmiRecipeCategory(
            VirtualFluidBlacklistRecipes.CATEGORY_ID,
            EmiStack.of(Items.WATER_BUCKET));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(VIRTUAL_FLUID_BLACKLIST);
        registry.addWorkstation(VIRTUAL_FLUID_BLACKLIST, EmiStack.of(AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get()));
        for (RecipeHolder<CraftingRecipe> recipe : VirtualFluidBlacklistRecipes.fromLocalConfig(registry.getRecipeManager())) {
            registry.addRecipe(new VirtualFluidBlacklistEmiRecipe(recipe));
        }
    }

    private record VirtualFluidBlacklistEmiRecipe(RecipeHolder<CraftingRecipe> recipe) implements EmiRecipe {
        @Override
        public EmiRecipeCategory getCategory() {
            return VIRTUAL_FLUID_BLACKLIST;
        }

        @Override
        public ResourceLocation getId() {
            return VirtualFluidBlacklistRecipes.syntheticViewerId(recipe);
        }

        @Override
        public List<EmiIngredient> getInputs() {
            return recipe.value().getIngredients().stream().map(EmiIngredient::of).toList();
        }

        @Override
        public List<EmiStack> getOutputs() {
            return List.of(EmiStack.of(VirtualFluidBlacklistRecipes.result(recipe,
                    Minecraft.getInstance().level.registryAccess())));
        }

        @Override
        public int getDisplayWidth() {
            return 118;
        }

        @Override
        public int getDisplayHeight() {
            return 68;
        }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            widgets.addTexture(EmiTexture.EMPTY_ARROW, 60, 18);
            List<EmiIngredient> grid = new ArrayList<>(Collections.nCopies(9, EmiStack.EMPTY));
            List<EmiIngredient> inputs = getInputs();
            for (int i = 0; i < inputs.size(); i++) {
                int gridSlot = VirtualFluidBlacklistRecipes.gridSlot(recipe, i);
                grid.set(gridSlot, inputs.get(i));
            }
            for (int i = 0; i < grid.size(); i++) {
                widgets.addSlot(grid.get(i), i % 3 * 18, i / 3 * 18);
            }
            widgets.addSlot(getOutputs().getFirst(), 92, 14).large(true);
            widgets.addText(VirtualFluidBlacklistRecipes.FOOTER, 0, 58, 0x808080, false);
            widgets.addTooltipText(List.of(VirtualFluidBlacklistRecipes.FOOTER_TOOLTIP), 0, 56, 118, 12);
        }

        @Override
        public boolean supportsRecipeTree() {
            return false;
        }

        @Override
        public boolean hideCraftable() {
            return true;
        }

        @Override
        public RecipeHolder<?> getBackingRecipe() {
            return recipe;
        }
    }
}
