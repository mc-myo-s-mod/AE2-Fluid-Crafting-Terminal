package me.myogoo.ae2fct.mixin.jei;

import appeng.api.stacks.AEFluidKey;
import appeng.client.gui.AEBaseScreen;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.integration.jei.AE2FCTJeiPlugin;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IRecipesGui;
import mezz.jei.common.input.IInternalKeyMappings;
import mezz.jei.gui.input.CombinedRecipeFocusSource;
import mezz.jei.gui.input.IClickableIngredientInternal;
import mezz.jei.gui.input.IMouseOverable;
import mezz.jei.gui.input.IUserInputHandler;
import mezz.jei.gui.input.UserInput;
import mezz.jei.gui.input.handlers.FocusInputHandler;
import mezz.jei.gui.input.handlers.SameElementInputHandler;
import mezz.jei.gui.util.FocusUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
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
    private void ae2fct$showFluidTargets(Screen rawScreen, UserInput input, IInternalKeyMappings keyBindings,
            CallbackInfoReturnable<Optional<IUserInputHandler>> cir) {
        boolean showRecipes = input.is(keyBindings.getShowRecipe());
        boolean showUses = input.is(keyBindings.getShowUses());
        if (!showRecipes && !showUses) {
            return;
        }

        List<RecipeIngredientRole> roles = showRecipes
                ? List.of(RecipeIngredientRole.OUTPUT)
                : List.of(RecipeIngredientRole.INPUT, RecipeIngredientRole.CATALYST);

        Optional<IClickableIngredientInternal<?>> virtualIngredient = focusSource
                .getIngredientUnderMouse(input, keyBindings)
                .filter(it -> it.getElement().isVisible())
                .filter(it -> ae2fct$getVirtualFluidStack(it).isPresent())
                .findFirst();
        if (virtualIngredient.isPresent()) {
            IClickableIngredientInternal<?> ingredient = virtualIngredient.get();
            FluidStack fluidStack = ae2fct$getVirtualFluidStack(ingredient).orElseThrow();
            ae2fct$openFluidTargets(input, fluidStack, true, ingredient::isMouseOver, roles, cir);
            return;
        }

        if (!FluidCraftingConfig.showBucketRecipesForAe2FluidKeys()) {
            return;
        }
        if (rawScreen instanceof AEBaseScreen<?> aeScreen) {
            var stackWithBounds = aeScreen.getStackUnderMouse(input.getMouseX(), input.getMouseY());
            if (stackWithBounds != null && stackWithBounds.stack().what() instanceof AEFluidKey fluidKey) {
                IMouseOverable mouseOverable = (mouseX, mouseY) -> stackWithBounds.bounds().contains((int) mouseX,
                        (int) mouseY);
                ae2fct$openFluidTargets(
                        input,
                        fluidKey.toStack((int) AEFluidKey.AMOUNT_BUCKET),
                        false,
                        mouseOverable,
                        roles,
                        cir);
            }
        }
    }

    @Unique
    private void ae2fct$openFluidTargets(UserInput input, FluidStack fluidStack, boolean virtualFluid,
            IMouseOverable mouseOverable, List<RecipeIngredientRole> roles,
            CallbackInfoReturnable<Optional<IUserInputHandler>> cir) {
        List<IFocus<?>> focuses = ae2fct$createTargetFocuses(fluidStack, virtualFluid, roles);
        if (focuses.isEmpty()) {
            return;
        }

        if (!input.isSimulate()) {
            recipesGui.show(focuses);
        }
        cir.setReturnValue(Optional.of(
                new SameElementInputHandler((IUserInputHandler) (Object) this, mouseOverable)));
    }

    @Unique
    private Optional<FluidStack> ae2fct$getVirtualFluidStack(IClickableIngredientInternal<?> ingredient) {
        Optional<ItemStack> virtualStack = ingredient.getTypedIngredient().getItemStack()
                .filter(FluidCraftingHelper::isVirtualFluidItem);
        if (virtualStack.isPresent()) {
            var virtualFluid = virtualStack.get().get(AE2FCTDataComponent.VIRTUAL_FLUID);
            if (virtualFluid != null && !virtualFluid.fluid().isEmpty()) {
                return Optional.of(virtualFluid.fluid());
            }
        }
        return Optional.empty();
    }

    @Unique
    private List<IFocus<?>> ae2fct$createTargetFocuses(FluidStack fluidStack, boolean virtualFluid,
            List<RecipeIngredientRole> roles) {
        List<IFocus<?>> focuses = new ArrayList<>();
        boolean showBucketRecipes = virtualFluid
                ? FluidCraftingConfig.showBucketRecipesForVirtualFluids()
                : FluidCraftingConfig.showBucketRecipesForAe2FluidKeys();
        boolean showFluidRecipes = !virtualFluid
                || FluidCraftingConfig.showFluidRecipesForVirtualFluids();

        if (showBucketRecipes) {
            ItemStack bucket = FluidUtil.getFilledBucket(fluidStack);
            if (!bucket.isEmpty()) {
                ingredientManager.createTypedIngredient(bucket)
                        .ifPresent(typed -> focuses.addAll(focusUtil.createFocuses(typed, roles)));
            }
        }
        if (showFluidRecipes) {
            AE2FCTJeiPlugin.createTypedFluidIngredient(ingredientManager, fluidStack)
                    .ifPresent(typed -> focuses.addAll(focusUtil.createFocuses(typed, roles)));
        }
        return focuses;
    }
}
