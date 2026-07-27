package me.myogoo.ae2fct.mixin.emi;

import appeng.api.stacks.AEFluidKey;
import appeng.client.gui.AEBaseScreen;
import com.mojang.blaze3d.platform.InputConstants;
import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.input.EmiBind;
import dev.emi.emi.registry.EmiRecipes;
import dev.emi.emi.screen.EmiScreenManager;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Mixin(EmiScreenManager.class)
public abstract class EmiScreenManagerMixin {
    @Inject(method = "stackInteraction", at = @At("HEAD"), cancellable = true, remap = false)
    private static void ae2fct$showFluidTargets(EmiStackInteraction interaction, Function<EmiBind, Boolean> function,
            CallbackInfoReturnable<Boolean> cir) {
        EmiIngredient hovered = interaction.getStack();
        if (hovered.getEmiStacks().isEmpty()) {
            return;
        }

        FluidStack fluidStack;
        boolean virtualFluid;
        boolean showRecipes;
        boolean showUses;
        ItemStack stack = hovered.getEmiStacks().get(0).getItemStack();
        if (FluidCraftingHelper.isVirtualFluidItem(stack) && stack.has(AE2FCTDataComponent.VIRTUAL_FLUID)) {
            var virtualFluidData = stack.get(AE2FCTDataComponent.VIRTUAL_FLUID);
            if (virtualFluidData == null || virtualFluidData.fluid().isEmpty()) {
                return;
            }
            fluidStack = virtualFluidData.fluid();
            virtualFluid = true;
            showRecipes = function.apply(EmiConfig.viewRecipes);
            showUses = function.apply(EmiConfig.viewUses);
        } else {
            fluidStack = ae2fct$getAeFluidStackUnderMouse(hovered);
            if (fluidStack.isEmpty()) {
                return;
            }
            virtualFluid = false;
            showRecipes = ae2fct$matchesKeyboardBind(function, EmiConfig.viewRecipes);
            showUses = ae2fct$matchesKeyboardBind(function, EmiConfig.viewUses);
            if (!showRecipes && !showUses) {
                if (function.apply(EmiConfig.viewRecipes) || function.apply(EmiConfig.viewUses)) {
                    cir.setReturnValue(false);
                }
                return;
            }
            if (!FluidCraftingConfig.showBucketRecipesForAe2FluidKeys()) {
                cir.setReturnValue(false);
                return;
            }
        }

        List<EmiIngredient> targets = ae2fct$createTargetIngredients(fluidStack, virtualFluid);
        if (targets.isEmpty()) {
            return;
        }
        if (showRecipes) {
            ae2fct$displayRecipes(targets);
            cir.setReturnValue(true);
            return;
        }
        if (showUses) {
            ae2fct$displayUses(targets);
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static boolean ae2fct$matchesKeyboardBind(Function<EmiBind, Boolean> function, EmiBind source) {
        EmiBind.ModifiedKey[] keys = source.boundKeys.stream()
                .filter(key -> !key.isUnbound())
                .filter(key -> {
                    InputConstants.Type type = key.key().getType();
                    return type == InputConstants.Type.KEYSYM || type == InputConstants.Type.SCANCODE;
                })
                .toArray(EmiBind.ModifiedKey[]::new);
        return keys.length > 0 && function.apply(new EmiBind(source.translationKey, keys));
    }

    @Unique
    private static FluidStack ae2fct$getAeFluidStackUnderMouse(EmiIngredient hovered) {
        if (Minecraft.getInstance().screen instanceof AEBaseScreen<?> aeScreen) {
            var stackWithBounds = aeScreen.getStackUnderMouse(
                    EmiScreenManager.lastMouseX,
                    EmiScreenManager.lastMouseY);
            if (stackWithBounds != null && stackWithBounds.stack().what() instanceof AEFluidKey fluidKey) {
                FluidStack fluidStack = fluidKey.toStack((int) AEFluidKey.AMOUNT_BUCKET);
                EmiStack expectedFluid = NeoForgeEmiStack.of(fluidStack);
                boolean hoveredFluidMatches = hovered.getEmiStacks().stream()
                        .anyMatch(expectedFluid::isEqual);
                if (hoveredFluidMatches) {
                    return fluidStack;
                }
            }
        }
        return FluidStack.EMPTY;
    }

    @Unique
    private static List<EmiIngredient> ae2fct$createTargetIngredients(FluidStack fluidStack, boolean virtualFluid) {
        List<EmiIngredient> targets = new ArrayList<>();
        boolean showBucketRecipes = virtualFluid
                ? FluidCraftingConfig.showBucketRecipesForVirtualFluids()
                : FluidCraftingConfig.showBucketRecipesForAe2FluidKeys();
        boolean showFluidRecipes = !virtualFluid
                || FluidCraftingConfig.showFluidRecipesForVirtualFluids();

        if (showBucketRecipes) {
            ItemStack bucket = FluidUtil.getFilledBucket(fluidStack);
            if (!bucket.isEmpty()) {
                targets.add(EmiStack.of(bucket));
            }
        }
        if (showFluidRecipes) {
            targets.add(NeoForgeEmiStack.of(fluidStack));
        }
        return targets;
    }

    @Unique
    private static EmiIngredient ae2fct$combinedTarget(List<EmiIngredient> targets) {
        if (targets.size() == 1) {
            return targets.get(0);
        }
        return EmiIngredient.of(targets);
    }

    @Unique
    private static void ae2fct$displayRecipes(List<EmiIngredient> targets) {
        if (targets.size() == 1) {
            EmiApi.displayRecipes(targets.get(0));
            return;
        }

        Set<EmiRecipe> recipes = new LinkedHashSet<>();
        for (EmiIngredient target : targets) {
            for (EmiStack stack : target.getEmiStacks()) {
                recipes.addAll(EmiApiAccessor.ae2fct$pruneSources(
                        EmiApi.getRecipeManager().getRecipesByOutput(stack),
                        stack
                ));
            }
        }

        EmiIngredient focus = ae2fct$combinedTarget(targets);
        if (recipes.isEmpty()) {
            EmiApi.displayRecipes(focus);
            return;
        }
        EmiApiAccessor.ae2fct$setPages(
                EmiApiAccessor.ae2fct$mapRecipes(new ArrayList<>(recipes)),
                focus
        );
    }

    @Unique
    private static void ae2fct$displayUses(List<EmiIngredient> targets) {
        if (targets.size() == 1) {
            EmiApi.displayUses(targets.get(0));
            return;
        }

        Set<EmiRecipe> recipes = new LinkedHashSet<>();
        for (EmiIngredient target : targets) {
            for (EmiStack stack : target.getEmiStacks()) {
                List<EmiRecipe> inputRecipes = EmiApi.getRecipeManager().getRecipesByInput(stack);
                recipes.addAll(EmiApiAccessor.ae2fct$pruneUses(inputRecipes, target));
                recipes.addAll(EmiRecipes.byWorkstation.getOrDefault(stack, List.of()));
            }
        }

        EmiApiAccessor.ae2fct$setPages(
                EmiApiAccessor.ae2fct$mapRecipes(new ArrayList<>(recipes)),
                ae2fct$combinedTarget(targets)
        );
    }
}
