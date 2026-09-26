package me.myogoo.ae2fct.mixin.rei;

import me.myogoo.ae2fct.integration.rei.ReiFluidRecipeLookup;
import me.shedaniel.rei.impl.client.gui.ScreenOverlayImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = ScreenOverlayImpl.class, remap = false)
public abstract class ScreenOverlayImplMixin {
    @Inject(method = "keyPressed(III)Z", at = @At(value = "INVOKE",
            target = "Lme/shedaniel/rei/api/client/registry/screen/ScreenRegistry;getInstance()Lme/shedaniel/rei/api/client/registry/screen/ScreenRegistry;",
            shift = At.Shift.BEFORE), cancellable = true)
    private void ae2fct$showFluidTargets(int keyCode, int scanCode, int modifiers,
            CallbackInfoReturnable<Boolean> cir) {
        ReiFluidRecipeLookup.handleKeyPressed(keyCode, scanCode).ifPresent(cir::setReturnValue);
    }
}
