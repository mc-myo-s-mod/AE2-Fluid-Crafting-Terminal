package me.myogoo.ae2fct.integration.jei;

import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.integration.recipeviewer.VirtualFluidBlacklistRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import java.util.Optional;

@JeiPlugin
public final class AE2FCTJeiPlugin implements IModPlugin {
    private static IPlatformFluidHelper<?> fluidHelper;

    @Override
    public ResourceLocation getPluginUid() {
        return Ae2fct.makeId("virtual_fluid_lookup");
    }

    @Override
    public <T> void registerFluidSubtypes(ISubtypeRegistration registration,
            IPlatformFluidHelper<T> platformFluidHelper) {
        fluidHelper = platformFluidHelper;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new VirtualFluidBlacklistJeiCategory(
                registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get(),
                VirtualFluidBlacklistJeiCategory.TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        registration.addRecipes(VirtualFluidBlacklistJeiCategory.TYPE,
                VirtualFluidBlacklistRecipes.fromLocalConfig(level.getRecipeManager(), registryAccess()));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Optional<ITypedIngredient<?>> createTypedFluidIngredient(IIngredientManager ingredientManager, Fluid fluid,
            long amount) {
        if (fluidHelper == null || fluid == Fluids.EMPTY) {
            return Optional.empty();
        }
        IPlatformFluidHelper helper = fluidHelper;
        Object ingredient = helper.create(fluid, amount);
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
        Object ingredient = helper.create(fluidStack.getFluid(), fluidStack.getAmount(), fluidStack.getTag());
        IIngredientType type = helper.getFluidIngredientType();
        return ingredientManager.createTypedIngredient(type, ingredient).map(it -> (ITypedIngredient<?>) it);
    }

    private static RegistryAccess registryAccess() {
        var level = Minecraft.getInstance().level;
        return level == null ? RegistryAccess.EMPTY : level.registryAccess();
    }
}
