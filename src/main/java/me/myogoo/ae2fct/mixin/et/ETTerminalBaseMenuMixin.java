package me.myogoo.ae2fct.mixin.et;

import appeng.menu.me.common.IClientRepo;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.myotus.menu.TerminalUpgradeHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Pseudo
@Mixin(targets = "me.myogoo.extendedterminal.menu.ETTerminalBaseMenu", remap = false)
public abstract class ETTerminalBaseMenuMixin extends MEStorageMenu {
    private ETTerminalBaseMenuMixin() {
        super(null, 0, null, null, false);
    }

    @Unique
    private boolean ae2fct$hasFluidInteractUpgrade() {
        return TerminalUpgradeHelper.hasUpgrade(this, AE2FCTItems.TERMINAL_FLUID_INTERACT_CARD.get());
    }

    @Inject(method = "findMissingIngredients", at = @At("RETURN"), cancellable = true)
    private void ae2fct$checkFluidForMissingIngredients(Map<Integer, Ingredient> ingredients,
            CallbackInfoReturnable<CraftingTermMenu.MissingIngredientSlots> cir) {
        if (!ae2fct$hasFluidInteractUpgrade()) {
            return;
        }

        var result = cir.getReturnValue();
        var updated = FluidCraftingTerminalIntegration.resolveMissingIngredients(result, ingredients, this.getClientRepo());
        if (updated != result) {
            cir.setReturnValue(updated);
        }
    }

    @Inject(method = "hasIngredient", at = @At("RETURN"), cancellable = true)
    private void ae2fct$hasFluidIngredient(Ingredient ingredient, Object2IntOpenHashMap<Object> usedIngredients,
            CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() || !ae2fct$hasFluidInteractUpgrade()) {
            return;
        }

        IClientRepo clientRepo = this.getClientRepo();
        if (FluidCraftingTerminalIntegration.hasFluidIngredient(ingredient, clientRepo)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isCraftable", at = @At("RETURN"), cancellable = true)
    private void ae2fct$checkFluidCraftable(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() || !ae2fct$hasFluidInteractUpgrade()) {
            return;
        }

        if (FluidCraftingTerminalIntegration.isFluidCraftable(itemStack, this.getClientRepo())) {
            cir.setReturnValue(true);
        }
    }
}
