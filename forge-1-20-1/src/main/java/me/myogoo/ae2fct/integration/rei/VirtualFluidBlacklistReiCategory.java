package me.myogoo.ae2fct.integration.rei;

import me.myogoo.ae2fct.init.AE2FCTItems;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class VirtualFluidBlacklistReiCategory implements DisplayCategory<VirtualFluidBlacklistReiDisplay> {
    private static final Component TITLE = Component.translatable("gui.ae2fct.category.virtual_fluid_blacklist");
    private static final Component FOOTER = Component.translatable("gui.ae2fct.category.virtual_fluid_blacklist.footer");
    private static final Component TOOLTIP = Component.translatable("gui.ae2fct.category.virtual_fluid_blacklist.tooltip");

    @Override
    public CategoryIdentifier<? extends VirtualFluidBlacklistReiDisplay> getCategoryIdentifier() {
        return VirtualFluidBlacklistReiDisplay.CATEGORY;
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get());
    }

    @Override
    public List<Widget> setupDisplay(VirtualFluidBlacklistReiDisplay display, Rectangle bounds) {
        List<Widget> widgets = new ArrayList<>();
        widgets.add(Widgets.createRecipeBase(bounds));
        int startX = bounds.getX() + 6;
        int startY = bounds.getY() + 6;
        var recipe = display.recipe();
        for (int i = 0; i < display.getInputEntries().size(); i++) {
            int x = startX + i % recipe.width() * 18;
            int y = startY + i / recipe.width() * 18;
            widgets.add(Widgets.createSlot(new Point(x, y))
                    .entries(display.getInputEntries().get(i))
                    .markInput());
        }
        widgets.add(Widgets.createArrow(new Point(bounds.getX() + 67, bounds.getY() + 23)));
        widgets.add(Widgets.createSlot(new Point(bounds.getX() + 100, bounds.getY() + 24))
                .entries(display.getOutputEntries().get(0))
                .markOutput());
        if (recipe.shapeless()) {
            widgets.add(Widgets.createShapelessIcon(new Point(bounds.getX() + 77, bounds.getY() + 5)));
        }
        widgets.add(Widgets.createLabel(new Point(bounds.getX() + 6, bounds.getY() + 66), FOOTER)
                .leftAligned()
                .color(0xFF808080, 0xFF808080));
        widgets.add(Widgets.createTooltip(new Rectangle(bounds.getX() + 6, bounds.getY() + 64, 116, 12), TOOLTIP));
        return widgets;
    }

    @Override
    public int getDisplayHeight() {
        return 82;
    }
}
