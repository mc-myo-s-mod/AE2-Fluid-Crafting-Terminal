package me.myogoo.ae2fct.mixin.jei;

import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.integration.jei.AE2FCTJeiPlugin;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IRecipesGui;
import mezz.jei.common.input.IInternalKeyMappings;
import mezz.jei.gui.input.CombinedRecipeFocusSource;
import mezz.jei.gui.input.IClickableIngredientInternal;
import mezz.jei.gui.input.IUserInputHandler;
import mezz.jei.gui.input.UserInput;
import mezz.jei.gui.input.handlers.FocusInputHandler;
import mezz.jei.gui.input.handlers.SameElementInputHandler;
import mezz.jei.gui.util.FocusUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(FocusInputHandler.class)
public abstract class FocusInputHandlerMixin {
    @Final
    @Shadow
    private CombinedRecipeFocusSource focusSource;
    @Final
    @Shadow
    private IRecipesGui recipesGui;
    @Final
    @Shadow
    private FocusUtil focusUtil;
    @Final
    @Shadow
    private IIngredientManager ingredientManager;

    @Inject(method = "handleUserInput", at = @At("HEAD"), cancellable = true, remap = false)
    private void ae2fct$showVirtualFluidTargets(Screen rawScreen, UserInput input, IInternalKeyMappings keyBindings,
            CallbackInfoReturnable<Optional<IUserInputHandler>> cir) {
        boolean showRecipes = input.is(keyBindings.getShowRecipe());
        boolean showUses = input.is(keyBindings.getShowUses());
        if (!showRecipes && !showUses) {
            return;
        }

        Optional<IClickableIngredientInternal<?>> clicked = focusSource.getIngredientUnderMouse(input, keyBindings)
                .filter(it -> it.getElement().isVisible())
                .filter(it -> ae2fct$getVirtualFluidStack(it).isPresent())
                .findFirst();
        if (clicked.isEmpty()) {
            return;
        }

        ItemStack virtualStack = ae2fct$getVirtualFluidStack(clicked.get()).orElse(ItemStack.EMPTY);
        var virtualFluid = virtualStack.get(AE2FCTDataComponent.VIRTUAL_FLUID);
        if (virtualFluid == null || virtualFluid.fluid().isEmpty()) {
            return;
        }

        List<RecipeIngredientRole> roles = showRecipes
                ? List.of(RecipeIngredientRole.OUTPUT)
                : List.of(RecipeIngredientRole.INPUT, RecipeIngredientRole.CATALYST);
        List<IFocus<?>> focuses = ae2fct$createTargetFocuses(virtualFluid.fluid().getFluid(), virtualFluid.fluid().getAmount(), roles);
        if (focuses.isEmpty()) {
            return;
        }

        if (!input.isSimulate()) {
            recipesGui.show(focuses);
        }
        cir.setReturnValue(Optional.of(new SameElementInputHandler((IUserInputHandler) (Object) this, clicked.get()::isMouseOver)));
    }

    @Unique
    private Optional<ItemStack> ae2fct$getVirtualFluidStack(IClickableIngredientInternal<?> ingredient) {
        return ingredient.getTypedIngredient().getItemStack()
                .filter(FluidCraftingHelper::isVirtualFluidItem);
    }

    @Unique
    private List<IFocus<?>> ae2fct$createTargetFocuses(Fluid fluid, long amount, List<RecipeIngredientRole> roles) {
        List<IFocus<?>> focuses = new ArrayList<>();
        if (FluidCraftingConfig.showBucketRecipesForVirtualFluids()) {
            ItemStack bucket = new ItemStack(fluid.getBucket());
            ingredientManager.createTypedIngredient(bucket)
                    .ifPresent(typed -> focuses.addAll(focusUtil.createFocuses(typed, roles)));
        }
        if (FluidCraftingConfig.showFluidRecipesForVirtualFluids()) {
            AE2FCTJeiPlugin.createTypedFluidIngredient(ingredientManager, fluid, amount)
                    .ifPresent(typed -> focuses.addAll(focusUtil.createFocuses(typed, roles)));
        }
        return focuses;
    }
}
