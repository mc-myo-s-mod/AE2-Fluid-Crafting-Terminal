package me.myogoo.ae2fct.integration.jei;

import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.integration.recipeviewer.VirtualFluidBlacklistRecipe;
import me.myogoo.ae2fct.integration.recipeviewer.VirtualFluidBlacklistRecipes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class VirtualFluidBlacklistJeiCategory implements IRecipeCategory<VirtualFluidBlacklistRecipe> {
    public static final RecipeType<VirtualFluidBlacklistRecipe> TYPE = RecipeType.create(
            Ae2fct.MODID,
            VirtualFluidBlacklistRecipes.CATEGORY_PATH,
            VirtualFluidBlacklistRecipe.class);

    private static final int WIDTH = 116;
    private static final int HEIGHT = 72;
    private static final Component TITLE = Component.translatable("gui.ae2fct.category.virtual_fluid_blacklist");
    private static final Component FOOTER = Component.translatable("gui.ae2fct.category.virtual_fluid_blacklist.footer");
    private static final Component TOOLTIP = Component.translatable("gui.ae2fct.category.virtual_fluid_blacklist.tooltip");
    private final ICraftingGridHelper gridHelper;
    private final IDrawable background;
    private final IDrawable icon;

    public VirtualFluidBlacklistJeiCategory(IGuiHelper guiHelper) {
        this.gridHelper = guiHelper.createCraftingGridHelper();
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemLike(AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get());
    }

    @Override
    public RecipeType<VirtualFluidBlacklistRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, VirtualFluidBlacklistRecipe recipe, IFocusGroup focuses) {
        List<List<ItemStack>> inputs = recipe.inputs().stream()
                .map(ingredient -> List.of(ingredient.getItems()))
                .toList();
        gridHelper.createAndSetInputs(builder, inputs, recipe.width(), recipe.height());
        gridHelper.createAndSetOutputs(builder, List.of(recipe.output()));
        if (recipe.shapeless()) {
            builder.setShapeless(72, 0);
        }
    }

    @Override
    public void draw(VirtualFluidBlacklistRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
            double mouseX, double mouseY) {
        guiGraphics.drawString(Minecraft.getInstance().font, FOOTER, 0, 60, 0xFF808080, false);
    }

    @Override
    public List<Component> getTooltipStrings(VirtualFluidBlacklistRecipe recipe, IRecipeSlotsView recipeSlotsView,
            double mouseX, double mouseY) {
        if (mouseY >= 58 && mouseY <= 70) {
            return List.of(TOOLTIP);
        }
        return List.of();
    }

    @Override
    public ResourceLocation getRegistryName(VirtualFluidBlacklistRecipe recipe) {
        return recipe.viewerId();
    }
}
