package me.myogoo.ae2fct.mixin.rei;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.client.gui.AEBaseScreen;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "me.shedaniel.rei.impl.client.gui.ScreenOverlayImpl", remap = false)
public abstract class ScreenOverlayImplMixin {
    @Inject(
            method = "keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/shedaniel/rei/api/client/registry/screen/ScreenRegistry;getInstance()Lme/shedaniel/rei/api/client/registry/screen/ScreenRegistry;",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true
    )
    private void ae2fct$showFluidTargets(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        boolean showRecipes = ConfigObject.getInstance().getRecipeKeybind().matchesKey(event.key(), event.scancode());
        boolean showUses = ConfigObject.getInstance().getUsageKeybind().matchesKey(event.key(), event.scancode());
        if (!showRecipes && !showUses) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        if (!(minecraft.screen instanceof AEBaseScreen<?> aeScreen)) {
            return;
        }

        var window = minecraft.getWindow();
        double mouseX = minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
        double mouseY = minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
        var stackWithBounds = aeScreen.getStackUnderMouse(mouseX, mouseY);
        if (stackWithBounds == null) {
            return;
        }

        var target = ae2fct$getFluidTarget(stackWithBounds.stack().what());
        if (target == null) {
            return;
        }

        List<EntryStack<?>> entries = ae2fct$createTargetEntries(target);
        if (entries.isEmpty()) {
            cir.setReturnValue(false);
            return;
        }

        ViewSearchBuilder builder = ViewSearchBuilder.builder();
        for (EntryStack<?> entry : entries) {
            if (showRecipes) {
                builder.addRecipesFor(entry);
            } else {
                builder.addUsagesFor(entry);
            }
        }
        cir.setReturnValue(builder.open());
    }

    @Unique
    private static FluidTarget ae2fct$getFluidTarget(Object what) {
        if (what instanceof AEFluidKey fluidKey) {
            return new FluidTarget(fluidKey.toStack((int) AEFluidKey.AMOUNT_BUCKET), false);
        }
        if (what instanceof AEItemKey itemKey) {
            ItemStack itemStack = itemKey.toStack();
            if (FluidCraftingHelper.isVirtualFluidItem(itemStack)) {
                var virtualFluid = itemStack.get(AE2FCTDataComponent.VIRTUAL_FLUID);
                if (virtualFluid != null && !virtualFluid.fluid().isEmpty()) {
                    return new FluidTarget(virtualFluid.fluid(), true);
                }
            }
        }
        return null;
    }

    @Unique
    private static List<EntryStack<?>> ae2fct$createTargetEntries(FluidTarget target) {
        List<EntryStack<?>> entries = new ArrayList<>();
        boolean showBucketRecipes = target.virtualFluid()
                ? FluidCraftingConfig.showBucketRecipesForVirtualFluids()
                : FluidCraftingConfig.showBucketRecipesForAe2FluidKeys();
        boolean showFluidRecipes = !target.virtualFluid()
                || FluidCraftingConfig.showFluidRecipesForVirtualFluids();

        if (showBucketRecipes) {
            ItemStack bucket = target.fluidStack().getFluidType().getBucket(target.fluidStack());
            if (!bucket.isEmpty()) {
                entries.add(EntryStacks.of(bucket));
            }
        }
        if (showFluidRecipes) {
            entries.add(EntryStacks.of(target.fluidStack().getFluid(), target.fluidStack().getAmount()));
        }
        return entries;
    }

    @Unique
    private record FluidTarget(FluidStack fluidStack, boolean virtualFluid) {
    }
}
