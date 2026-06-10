package me.myogoo.ae2fct.mixin.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.input.EmiBind;
import dev.emi.emi.screen.EmiScreenManager;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.util.VirtualFluidStorage;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Mixin(EmiScreenManager.class)
public abstract class EmiScreenManagerMixin {
    @Inject(method = "stackInteraction", at = @At("HEAD"), cancellable = true, remap = false)
    private static void ae2fct$showVirtualFluidTargets(EmiStackInteraction interaction, Function<EmiBind, Boolean> function,
            CallbackInfoReturnable<Boolean> cir) {
        EmiIngredient hovered = interaction.getStack();
        if (hovered.getEmiStacks().isEmpty()) {
            return;
        }
        ItemStack stack = hovered.getEmiStacks().get(0).getItemStack();
        if (!FluidCraftingHelper.isVirtualFluidItem(stack)) {
            return;
        }
        var virtualFluid = VirtualFluidStorage.get(stack);
        if (virtualFluid == null || virtualFluid.fluid().isEmpty()) {
            return;
        }

        EmiIngredient targets = ae2fct$createTargetIngredient(virtualFluid.fluid().getFluid(), virtualFluid.fluid().getAmount());
        if (targets.isEmpty()) {
            return;
        }
        if (function.apply(EmiConfig.viewRecipes)) {
            EmiApi.displayRecipes(targets);
            cir.setReturnValue(true);
            return;
        }
        if (function.apply(EmiConfig.viewUses)) {
            EmiApi.displayUses(targets);
            cir.setReturnValue(true);
        }
    }

    private static EmiIngredient ae2fct$createTargetIngredient(Fluid fluid, long amount) {
        List<EmiIngredient> targets = new ArrayList<>();
        if (FluidCraftingConfig.showBucketRecipesForVirtualFluids()) {
            targets.add(EmiStack.of(new ItemStack(fluid.getBucket())));
        }
        if (FluidCraftingConfig.showFluidRecipesForVirtualFluids()) {
            targets.add(EmiStack.of(fluid, amount));
        }
        if (targets.isEmpty()) {
            return EmiStack.EMPTY;
        }
        if (targets.size() == 1) {
            return targets.get(0);
        }
        return EmiIngredient.of(targets);
    }
}
