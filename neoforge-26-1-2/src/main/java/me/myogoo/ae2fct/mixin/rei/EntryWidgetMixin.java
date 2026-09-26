package me.myogoo.ae2fct.mixin.rei;

import me.myogoo.ae2fct.integration.rei.ReiFluidRecipeLookup;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.impl.client.gui.widget.EntryWidget;
import net.minecraft.client.input.KeyEvent;
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

    @Inject(
            method = "keyPressedIgnoreContains(Lnet/minecraft/client/input/KeyEvent;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/shedaniel/rei/api/client/config/ConfigObject;getRecipeKeybind()Lme/shedaniel/clothconfig2/api/ModifierKeyCode;",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void ae2fct$showVirtualFluidTargets(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        ConfigObject config = ConfigObject.getInstance();
        boolean showRecipes = config.getRecipeKeybind().matchesKey(event.key(), event.scancode());
        boolean showUses = config.getUsageKeybind().matchesKey(event.key(), event.scancode());
        if (!showRecipes && !showUses) {
            return;
        }

        ReiFluidRecipeLookup.openVirtualFluidTarget(getCurrentEntry(), showRecipes)
                .ifPresent(cir::setReturnValue);
    }
}
