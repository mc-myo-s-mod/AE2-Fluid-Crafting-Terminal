package me.myogoo.ae2fct.integration.jei;

import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.integration.BlacklistRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class BlacklistRecipeCategory implements IRecipeCategory<BlacklistRecipe> {
    public static final RecipeType<BlacklistRecipe> TYPE = RecipeType.create(
            Ae2fct.MODID, "virtual_fluid_blacklist", BlacklistRecipe.class);
    private final IDrawable icon;

    public BlacklistRecipeCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemLike(AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get());
    }

    @Override
    public RecipeType<BlacklistRecipe> getRecipeType() { return TYPE; }

    @Override
    public Component getTitle() { return Component.translatable("category.ae2fct.virtual_fluid_blacklist"); }

    @Override
    public IDrawable getIcon() { return icon; }

    @Override
    public int getWidth() { return 140; }

    @Override
    public int getHeight() { return 80; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BlacklistRecipe recipe, IFocusGroup focuses) {
        for (int slot = 0; slot < 9; slot++) {
            builder.addInputSlot(1 + slot % 3 * 18, 1 + slot / 3 * 18)
                    .setStandardSlotBackground().addItemStacks(recipe.inputAt(slot));
        }
        builder.addOutputSlot(96, 19).setOutputSlotBackground().addItemStacks(recipe.outputs());
        if (recipe.shapeless()) {
            builder.setShapeless();
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, BlacklistRecipe recipe, IFocusGroup focuses) {
        builder.addRecipeArrow().setPosition(62, 19);
        builder.addText(Component.translatable("category.ae2fct.virtual_fluid_blacklist.real_buckets"), 140, 16)
                .setPosition(0, 62);
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, BlacklistRecipe recipe, IRecipeSlotsView slots,
            double mouseX, double mouseY) {
        if (mouseY >= 60) {
            tooltip.add(Component.literal(recipe.id().toString()));
            tooltip.add(Component.translatable("category.ae2fct.virtual_fluid_blacklist.local_config"));
        }
    }

    @Override
    public Identifier getIdentifier(BlacklistRecipe recipe) { return recipe.displayId(); }
}
