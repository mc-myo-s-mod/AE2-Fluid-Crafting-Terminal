package me.myogoo.ae2fct.mixin.rei;

import me.myogoo.ae2fct.integration.rei.ReiFluidRecipeLookup;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.impl.client.gui.widget.EntryWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = EntryWidget.class, remap = false)
public abstract class EntryWidgetMixin {
    @Shadow
    public abstract EntryStack<?> getCurrentEntry();

    @Inject(method = "keyPressedIgnoreContains(III)Z", at = @At(value = "INVOKE",
            target = "Lme/shedaniel/rei/api/client/config/ConfigObject;getRecipeKeybind()Lme/shedaniel/clothconfig2/api/ModifierKeyCode;",
            shift = At.Shift.BEFORE), cancellable = true)
    private void ae2fct$showVirtualFluidTargets(int keyCode, int scanCode, int modifiers,
            CallbackInfoReturnable<Boolean> cir) {
        ReiFluidRecipeLookup.handleEntryKeyPressed(getCurrentEntry(), keyCode, scanCode).ifPresent(cir::setReturnValue);
    }
}
