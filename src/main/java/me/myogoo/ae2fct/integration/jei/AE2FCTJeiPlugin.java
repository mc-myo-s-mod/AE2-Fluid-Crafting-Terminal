package me.myogoo.ae2fct.integration.jei;

import me.myogoo.ae2fct.Ae2fct;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.runtime.IIngredientManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Optional;

@JeiPlugin
public final class AE2FCTJeiPlugin implements IModPlugin {
    private static IPlatformFluidHelper<?> fluidHelper;

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(Ae2fct.MODID, "virtual_fluid_lookup");
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
        Object ingredient = helper.create(fluid, amount);
        IIngredientType type = helper.getFluidIngredientType();
        return ingredientManager.createTypedIngredient(type, ingredient).map(it -> (ITypedIngredient<?>) it);
    }
}
