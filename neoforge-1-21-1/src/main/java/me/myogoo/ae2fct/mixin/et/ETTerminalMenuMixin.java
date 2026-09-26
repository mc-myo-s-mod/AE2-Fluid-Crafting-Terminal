package me.myogoo.ae2fct.mixin.et;

import me.myogoo.ae2fct.util.FluidCraftingHelper;
import me.myogoo.extendedterminal.menu.ETTerminalBaseMenu;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "me.myogoo.extendedterminal.menu.extendedterminal.ETTerminalMenu", remap = false)
public abstract class ETTerminalMenuMixin extends ETTerminalBaseMenu<CraftingRecipe> {
    @Shadow
    private CraftingInput lastTestedCraftingInput;

    private ETTerminalMenuMixin() {
        super(null, 0, null, null, null, null);
    }

    @Inject(method = "updateCraftingOutput", at = @At(value = "FIELD",
            target = "Lme/myogoo/extendedterminal/menu/extendedterminal/ETTerminalMenu;lastTestedCraftingInput:Lnet/minecraft/world/item/crafting/CraftingInput;",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void ae2fct$filterVirtualFluidOutput(boolean forceUpdate, CallbackInfo ci) {
        if (currentRecipe != null && FluidCraftingHelper.containsVirtualFluidItem(lastTestedCraftingInput)
                && FluidCraftingHelper.isVirtualFluidRecipeBlacklisted(this, currentRecipe.id())) {
            currentRecipe = null;
        }
    }
}
