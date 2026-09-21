package me.myogoo.ae2fct.integration.rei;

import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.integration.recipeviewer.VirtualFluidBlacklistRecipe;
import me.myogoo.ae2fct.integration.recipeviewer.VirtualFluidBlacklistRecipes;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;

import java.util.List;
import java.util.Optional;

public final class VirtualFluidBlacklistReiDisplay extends BasicDisplay {
    public static final CategoryIdentifier<VirtualFluidBlacklistReiDisplay> CATEGORY =
            CategoryIdentifier.of(Ae2fct.makeId(VirtualFluidBlacklistRecipes.CATEGORY_PATH));
    private final VirtualFluidBlacklistRecipe recipe;

    public VirtualFluidBlacklistReiDisplay(VirtualFluidBlacklistRecipe recipe) {
        super(inputs(recipe), List.of(EntryIngredients.of(recipe.output())), Optional.of(recipe.viewerId()));
        this.recipe = recipe;
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return CATEGORY;
    }

    public VirtualFluidBlacklistRecipe recipe() {
        return recipe;
    }

    private static List<EntryIngredient> inputs(VirtualFluidBlacklistRecipe recipe) {
        return recipe.inputs().stream()
                .map(EntryIngredients::ofIngredient)
                .toList();
    }
}
