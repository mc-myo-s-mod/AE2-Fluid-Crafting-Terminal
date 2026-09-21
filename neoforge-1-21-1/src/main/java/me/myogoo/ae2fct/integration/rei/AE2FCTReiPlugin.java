package me.myogoo.ae2fct.integration.rei;

import com.google.common.collect.Lists;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.integration.recipeviewer.VirtualFluidBlacklistRecipes;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.Optional;

@REIPluginClient
public final class AE2FCTReiPlugin implements REIClientPlugin {
    private static final CategoryIdentifier<VirtualFluidBlacklistDisplay> VIRTUAL_FLUID_BLACKLIST =
            CategoryIdentifier.of(VirtualFluidBlacklistRecipes.CATEGORY_ID);

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new VirtualFluidBlacklistCategory());
        registry.addWorkstations(VIRTUAL_FLUID_BLACKLIST, EntryStacks.of(AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get()));
        registry.configure(VIRTUAL_FLUID_BLACKLIST, config -> {
            config.setQuickCraftingEnabledByDefault(false);
            config.removePlusButton();
        });
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (RecipeHolder<CraftingRecipe> recipe : VirtualFluidBlacklistRecipes.fromLocalConfig(registry.getRecipeManager())) {
            registry.add(new VirtualFluidBlacklistDisplay(recipe));
        }
    }

    public static final class VirtualFluidBlacklistDisplay extends BasicDisplay {
        private final RecipeHolder<CraftingRecipe> recipe;

        private VirtualFluidBlacklistDisplay(RecipeHolder<CraftingRecipe> recipe) {
            super(EntryIngredients.ofIngredients(recipe.value().getIngredients()),
                    List.of(EntryIngredients.of(VirtualFluidBlacklistRecipes.result(recipe, BasicDisplay.registryAccess()))),
                    Optional.of(recipe.id()));
            this.recipe = recipe;
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() {
            return VIRTUAL_FLUID_BLACKLIST;
        }

        @Override
        public List<EntryIngredient> getRequiredEntries() {
            return List.of();
        }

        private int gridSlot(int ingredientIndex) {
            return VirtualFluidBlacklistRecipes.gridSlot(recipe, ingredientIndex);
        }
    }

    private static final class VirtualFluidBlacklistCategory implements DisplayCategory<VirtualFluidBlacklistDisplay> {
        @Override
        public CategoryIdentifier<? extends VirtualFluidBlacklistDisplay> getCategoryIdentifier() {
            return VIRTUAL_FLUID_BLACKLIST;
        }

        @Override
        public Component getTitle() {
            return VirtualFluidBlacklistRecipes.TITLE;
        }

        @Override
        public Renderer getIcon() {
            return EntryStacks.of(Items.WATER_BUCKET);
        }

        @Override
        public int getDisplayHeight() {
            return 68;
        }

        @Override
        public int getDisplayWidth(VirtualFluidBlacklistDisplay display) {
            return 118;
        }

        @Override
        public List<Widget> setupDisplay(VirtualFluidBlacklistDisplay display, Rectangle bounds) {
            Point start = new Point(bounds.getCenterX() - 58, bounds.getCenterY() - 33);
            List<Widget> widgets = Lists.newArrayList();
            widgets.add(Widgets.createRecipeBase(bounds));
            widgets.add(Widgets.createArrow(new Point(start.x + 60, start.y + 18)));
            widgets.add(Widgets.createResultSlotBackground(new Point(start.x + 95, start.y + 19)));

            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    Slot widget = Widgets.createSlot(new Point(start.x + 1 + x * 18, start.y + 1 + y * 18)).markInput();
                    widgets.add(widget);
                }
            }
            for (int i = 0; i < display.getInputEntries().size(); i++) {
                int gridSlot = display.gridSlot(i);
                widgets.add(Widgets.createSlot(new Point(start.x + 1 + gridSlot % 3 * 18,
                                start.y + 1 + gridSlot / 3 * 18))
                        .entries(display.getInputEntries().get(i))
                        .disableBackground()
                        .markInput());
            }

            widgets.add(Widgets.createSlot(new Point(start.x + 95, start.y + 19))
                    .entries(display.getOutputEntries().getFirst())
                    .disableBackground()
                    .markOutput());
            widgets.add(Widgets.createLabel(new Point(start.x, start.y + 58), VirtualFluidBlacklistRecipes.FOOTER)
                    .leftAligned()
                    .color(0x808080));
            widgets.add(Widgets.createTooltip(
                    new Rectangle(start.x, start.y + 56, 118, 12),
                    VirtualFluidBlacklistRecipes.FOOTER_TOOLTIP));
            return widgets;
        }
    }
}
