package me.myogoo.ae2fct.integration.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import me.myogoo.ae2fct.integration.recipeviewer.VirtualFluidBlacklistRecipe;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class VirtualFluidBlacklistEmiRecipe implements EmiRecipe {
    private static final Component FOOTER = Component.translatable("gui.ae2fct.category.virtual_fluid_blacklist.footer");
    private static final Component TOOLTIP = Component.translatable("gui.ae2fct.category.virtual_fluid_blacklist.tooltip");
    private final VirtualFluidBlacklistRecipe recipe;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    public VirtualFluidBlacklistEmiRecipe(VirtualFluidBlacklistRecipe recipe) {
        this.recipe = recipe;
        this.inputs = recipe.inputs().stream()
                .map(EmiIngredient::of)
                .toList();
        this.outputs = List.of(EmiStack.of(recipe.output()));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return AE2FCTEmiPlugin.CATEGORY;
    }

    @Override
    public ResourceLocation getId() {
        return recipe.viewerId();
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public int getDisplayWidth() {
        return 116;
    }

    @Override
    public int getDisplayHeight() {
        return 72;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        for (int i = 0; i < inputs.size(); i++) {
            int x = 1 + i % recipe.width() * 18;
            int y = 1 + i / recipe.width() * 18;
            widgets.addSlot(inputs.get(i), x, y);
        }
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 62, 18);
        widgets.addSlot(outputs.get(0), 95, 19);
        if (recipe.shapeless()) {
            widgets.addText(Component.literal("*"), 72, 0, 0xFF808080, false);
        }
        widgets.addText(FOOTER, 0, 60, 0xFF808080, false);
        widgets.addTooltipText(List.of(TOOLTIP), 0, 58, 116, 14);
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public boolean hideCraftable() {
        return true;
    }
}
