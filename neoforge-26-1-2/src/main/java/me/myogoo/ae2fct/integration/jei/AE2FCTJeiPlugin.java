package me.myogoo.ae2fct.integration.jei;

import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.integration.BlacklistRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

@JeiPlugin
public final class AE2FCTJeiPlugin implements IModPlugin {
    private static IPlatformFluidHelper<?> fluidHelper;

    @Override
    public Identifier getPluginUid() {
        return Ae2fct.makeId("virtual_fluid_lookup");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new BlacklistRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(BlacklistRecipeCategory.TYPE, BlacklistRecipe.load());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(BlacklistRecipeCategory.TYPE, AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get());
    }

    @Override
    public <T> void registerFluidSubtypes(mezz.jei.api.registration.ISubtypeRegistration registration,
            IPlatformFluidHelper<T> platformFluidHelper) {
        fluidHelper = platformFluidHelper;
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
                fluidStack.typeHolder(),
                fluidStack.getAmount(),
                fluidStack.getComponentsPatch());
        IIngredientType type = helper.getFluidIngredientType();
        return ingredientManager.createTypedIngredient(type, ingredient).map(it -> (ITypedIngredient<?>) it);
    }
}
