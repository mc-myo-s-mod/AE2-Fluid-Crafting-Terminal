package me.myogoo.ae2fct.mixin.jei;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.AEBaseScreen;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.integration.jei.AE2FCTJeiPlugin;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.common.input.IMouseOverable;
import mezz.jei.common.input.UserInput;
import mezz.jei.common.input.interaction.IInputInteraction;
import mezz.jei.common.input.interaction.InputAction;
import mezz.jei.gui.input.IClickableIngredientInternal;
import mezz.jei.gui.input.handlers.IngredientCommands;
import mezz.jei.gui.recipes.RecipesGui;
import mezz.jei.gui.util.FocusUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Mixin(value = IngredientCommands.class, remap = false)
public abstract class IngredientCommandsMixin {
    @Final
    @Shadow
    private RecipesGui recipes;
    @Final
    @Shadow
    private FocusUtil focusUtil;
    @Final
    @Shadow
    private IIngredientManager ingredients;

    @WrapOperation(method = "prepareRecipeLookupAction", at = @At(value = "INVOKE",
            target = "Ljava/util/Optional;map(Ljava/util/function/Function;)Ljava/util/Optional;"), require = 1)
    private Optional<IInputInteraction> ae2fct$prepareFluidTargets(
            Optional<IClickableIngredientInternal<?>> selected,
            Function<IClickableIngredientInternal<?>, IInputInteraction> mapper,
            Operation<Optional<IInputInteraction>> original, UserInput input, List<RecipeIngredientRole> roles) {
        // Keep JEI's selection, including keyboard focus and conflicting mouse-button filtering.
        if (selected.isPresent()) {
            IClickableIngredientInternal<?> ingredient = selected.get();
            Optional<FluidStack> virtualFluid = ae2fct$getVirtualFluidStack(ingredient);
            if (virtualFluid.isPresent()
                    && (FluidCraftingConfig.showBucketRecipesForVirtualFluids()
                    || FluidCraftingConfig.showFluidRecipesForVirtualFluids())) {
                return Optional.of(ae2fct$fluidTargetsAction(virtualFluid.get(), true, ingredient::isMouseOver, roles,
                        original.call(selected, mapper))
                        .when(ingredient.getElement()::isVisible));
            }
        }

        if (Minecraft.getInstance().screen instanceof AEBaseScreen<?> aeScreen) {
            var stackWithBounds = aeScreen.getStackUnderMouse(input.getMouseX(), input.getMouseY());
            if (stackWithBounds != null && stackWithBounds.stack().what() instanceof AEFluidKey fluidKey
                    && selected.map(ingredient -> {
                        if (!ingredient.isMouseOver(input.getMouseX(), input.getMouseY())) {
                            return false;
                        }
                        Object value = ingredient.getTypedIngredient().getIngredient();
                        if (value instanceof FluidStack fluid) {
                            return fluidKey.equals(AEFluidKey.of(fluid));
                        }
                        if (value instanceof ItemStack item) {
                            // AE2 repository slots expose fluids as WrappedGenericStack items to JEI.
                            var wrapped = GenericStack.unwrapItemStack(item);
                            return wrapped != null && fluidKey.equals(wrapped.what());
                        }
                        return false;
                    }).orElse(true)) {
                if (!input.isKeyboardInput()
                        || !FluidCraftingConfig.showBucketRecipesForAe2FluidKeys()) {
                    return Optional.empty();
                }
                IMouseOverable mouseOverable = (mouseX, mouseY) -> stackWithBounds.bounds().contains((int) mouseX,
                        (int) mouseY);
                return Optional.of(ae2fct$fluidTargetsAction(
                        fluidKey.toStack((int) AEFluidKey.AMOUNT_BUCKET),
                        false, mouseOverable, roles, original.call(selected, mapper))
                        .when(() -> selected.map(ingredient -> ingredient.getElement().isVisible()).orElse(true)));
            }
        }
        return original.call(selected, mapper);
    }

    @Unique
    private InputAction ae2fct$fluidTargetsAction(FluidStack fluidStack, boolean virtualFluid,
            IMouseOverable mouseOverable, List<RecipeIngredientRole> roles, Optional<IInputInteraction> fallback) {
        return new InputAction(completedInput -> {
            List<IFocus<?>> focuses = ae2fct$createTargetFocuses(fluidStack, virtualFluid, roles);
            if (!focuses.isEmpty()) {
                recipes.show(focuses);
            } else {
                fallback.ifPresent(action -> action.complete(completedInput));
            }
        }).within(mouseOverable);
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
                ingredients.createTypedIngredient(bucket)
                        .ifPresent(typed -> focuses.addAll(focusUtil.createFocuses(typed, roles)));
            }
        }
        if (showFluidRecipes) {
            AE2FCTJeiPlugin.createTypedFluidIngredient(ingredients, fluidStack)
                    .ifPresent(typed -> focuses.addAll(focusUtil.createFocuses(typed, roles)));
        }
        return focuses;
    }
}
