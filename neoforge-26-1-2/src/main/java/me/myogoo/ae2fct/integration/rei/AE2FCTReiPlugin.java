package me.myogoo.ae2fct.integration.rei;

import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.integration.BlacklistRecipe;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

@REIPluginClient
public final class AE2FCTReiPlugin implements REIClientPlugin {
    public static final CategoryIdentifier<BlacklistDisplay> CATEGORY =
            CategoryIdentifier.of(Ae2fct.MODID, "virtual_fluid_blacklist");

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(new BlacklistCategory());
        registry.addWorkstations(CATEGORY, EntryStacks.of(AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get()));
        registry.removePlusButton(CATEGORY);
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        for (BlacklistRecipe recipe : BlacklistRecipe.load()) {
            registry.add(new BlacklistDisplay(recipe));
        }
    }

    public static final class BlacklistDisplay extends BasicDisplay {
        private final BlacklistRecipe recipe;

        public BlacklistDisplay(BlacklistRecipe recipe) {
            super(IntStream.range(0, 9).mapToObj(slot -> EntryIngredients.ofItemStacks(recipe.inputAt(slot))).toList(),
                    List.of(EntryIngredients.ofItemStacks(recipe.outputs())), Optional.of(recipe.displayId()));
            this.recipe = recipe;
        }

        @Override
        public CategoryIdentifier<?> getCategoryIdentifier() { return CATEGORY; }

        @Override
        public @Nullable DisplaySerializer<?> getSerializer() {
            // Client-only, config-derived displays are rebuilt with the recipe viewer.
            return null;
        }
    }

    private static final class BlacklistCategory implements DisplayCategory<BlacklistDisplay> {
        @Override
        public CategoryIdentifier<BlacklistDisplay> getCategoryIdentifier() { return CATEGORY; }

        @Override
        public Component getTitle() { return Component.translatable("category.ae2fct.virtual_fluid_blacklist"); }

        @Override
        public Renderer getIcon() { return EntryStacks.of(AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get()); }

        @Override
        public int getDisplayHeight() { return 86; }

        @Override
        public int getDisplayWidth(BlacklistDisplay display) { return 150; }

        @Override
        public List<Widget> setupDisplay(BlacklistDisplay display, Rectangle bounds) {
            List<Widget> widgets = new ArrayList<>();
            widgets.add(Widgets.createRecipeBase(bounds));
            for (int slot = 0; slot < 9; slot++) {
                widgets.add(Widgets.createSlot(new Point(bounds.x + 8 + slot % 3 * 18,
                        bounds.y + 5 + slot / 3 * 18)).entries(display.getInputEntries().get(slot)).markInput());
            }
            widgets.add(Widgets.createArrow(new Point(bounds.x + 69, bounds.y + 23)));
            widgets.add(Widgets.createResultSlotBackground(new Point(bounds.x + 105, bounds.y + 23)));
            widgets.add(Widgets.createSlot(new Point(bounds.x + 105, bounds.y + 23))
                    .entries(display.getOutputEntries().getFirst()).disableBackground().markOutput());
            widgets.add(Widgets.createLabel(new Point(bounds.x + 7, bounds.y + 68),
                    Component.translatable("category.ae2fct.virtual_fluid_blacklist.real_buckets"))
                    .leftAligned().noShadow().tooltip(Component.literal(display.recipe.id().toString()),
                            Component.translatable("category.ae2fct.virtual_fluid_blacklist.local_config")));
            return widgets;
        }
    }
}
