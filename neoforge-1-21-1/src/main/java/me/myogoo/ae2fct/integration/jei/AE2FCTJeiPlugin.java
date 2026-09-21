package me.myogoo.ae2fct.integration.jei;

import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.integration.recipeviewer.VirtualFluidBlacklistRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

@JeiPlugin
public final class AE2FCTJeiPlugin implements IModPlugin {
    private static final RecipeType<RecipeHolder<CraftingRecipe>> VIRTUAL_FLUID_BLACKLIST =
            new RecipeType<>(VirtualFluidBlacklistRecipes.CATEGORY_ID, recipeHolderClass());
    private static IPlatformFluidHelper<?> fluidHelper;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Ae2fct.MODID, "virtual_fluid_lookup");
    }

    @Override
    public <T> void registerFluidSubtypes(ISubtypeRegistration registration,
            IPlatformFluidHelper<T> platformFluidHelper) {
        fluidHelper = platformFluidHelper;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new VirtualFluidBlacklistCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        registration.addRecipes(VIRTUAL_FLUID_BLACKLIST,
                VirtualFluidBlacklistRecipes.fromLocalConfig(minecraft.level.getRecipeManager()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get(), VIRTUAL_FLUID_BLACKLIST);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Optional<ITypedIngredient<?>> createTypedFluidIngredient(IIngredientManager ingredientManager, Fluid fluid,
            long amount) {
        if (fluidHelper == null || fluid == Fluids.EMPTY) {
            return Optional.empty();
        }
        IPlatformFluidHelper helper = fluidHelper;
        Object ingredient = helper.create(fluid.builtInRegistryHolder(), amount);
        IIngredientType type = helper.getFluidIngredientType();
        return ingredientManager.createTypedIngredient(type, ingredient).map(it -> (ITypedIngredient<?>) it);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Optional<ITypedIngredient<?>> createTypedFluidIngredient(IIngredientManager ingredientManager,
            FluidStack fluidStack) {
        if (fluidHelper == null || fluidStack.isEmpty()) {
            return Optional.empty();
        }
        IPlatformFluidHelper helper = fluidHelper;
        Object ingredient = helper.create(
                fluidStack.getFluidHolder(),
                fluidStack.getAmount(),
                fluidStack.getComponentsPatch());
        IIngredientType type = helper.getFluidIngredientType();
        return ingredientManager.createTypedIngredient(type, ingredient).map(it -> (ITypedIngredient<?>) it);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Class<RecipeHolder<CraftingRecipe>> recipeHolderClass() {
        return (Class) RecipeHolder.class;
    }

    private static final class VirtualFluidBlacklistCategory implements IRecipeCategory<RecipeHolder<CraftingRecipe>> {
        private static final int WIDTH = 118;
        private static final int HEIGHT = 68;
        private final IDrawable icon;
        private final IDrawable arrow;

        private VirtualFluidBlacklistCategory(IGuiHelper guiHelper) {
            this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(Items.WATER_BUCKET));
            this.arrow = guiHelper.getRecipeArrow();
        }

        @Override
        public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
            return VIRTUAL_FLUID_BLACKLIST;
        }

        @Override
        public Component getTitle() {
            return VirtualFluidBlacklistRecipes.TITLE;
        }

        @Override
        public int getWidth() {
            return WIDTH;
        }

        @Override
        public int getHeight() {
            return HEIGHT;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<CraftingRecipe> recipe, IFocusGroup focuses) {
            IRecipeSlotBuilder[] grid = new IRecipeSlotBuilder[9];
            for (int i = 0; i < grid.length; i++) {
                grid[i] = builder.addInputSlot(i % 3 * 18, i / 3 * 18)
                        .setStandardSlotBackground();
            }

            var ingredients = recipe.value().getIngredients();
            for (int i = 0; i < ingredients.size(); i++) {
                int gridSlot = VirtualFluidBlacklistRecipes.gridSlot(recipe, i);
                grid[gridSlot].addIngredients(ingredients.get(i));
            }

            RegistryAccess registryAccess = Minecraft.getInstance().level.registryAccess();
            builder.addOutputSlot(92, 14)
                    .setOutputSlotBackground()
                    .addItemStack(VirtualFluidBlacklistRecipes.result(recipe, registryAccess));
        }

        @Override
        public void draw(RecipeHolder<CraftingRecipe> recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                double mouseX, double mouseY) {
            arrow.draw(guiGraphics, 60, 18);
            guiGraphics.drawString(Minecraft.getInstance().font, VirtualFluidBlacklistRecipes.FOOTER, 0, 58, 0xFF808080,
                    false);
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip, RecipeHolder<CraftingRecipe> recipe,
                IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
            if (mouseY >= 56) {
                tooltip.add(VirtualFluidBlacklistRecipes.FOOTER_TOOLTIP);
            }
        }
    }
}
