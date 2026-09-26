package me.myogoo.ae2fct.mixin.rei;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.client.gui.AEBaseScreen;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.integration.rei.ReiFluidRecipeLookup;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.math.impl.PointHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        if (minecraft.screen instanceof AEBaseScreen<?> aeScreen) {
            var window = minecraft.getWindow();
            double mouseX = minecraft.mouseHandler.xpos() * window.getGuiScaledWidth() / window.getScreenWidth();
            double mouseY = minecraft.mouseHandler.ypos() * window.getGuiScaledHeight() / window.getScreenHeight();
            var stackWithBounds = aeScreen.getStackUnderMouse(mouseX, mouseY);
            if (stackWithBounds != null) {
                var target = ae2fct$getFluidTarget(stackWithBounds.stack().what());
                if (target != null) {
                    cir.setReturnValue(ReiFluidRecipeLookup
                            .openFluidTarget(target.fluidStack(), target.virtualFluid(), showRecipes).orElse(false));
                    return;
                }
            }
        }

        if (minecraft.screen == null) {
            return;
        }
        var focused = ScreenRegistry.getInstance().getFocusedStack(minecraft.screen, PointHelper.ofMouse());
        ReiFluidRecipeLookup.openVirtualFluidTarget(focused, showRecipes).ifPresent(cir::setReturnValue);
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
    private record FluidTarget(FluidStack fluidStack, boolean virtualFluid) {
    }
}
