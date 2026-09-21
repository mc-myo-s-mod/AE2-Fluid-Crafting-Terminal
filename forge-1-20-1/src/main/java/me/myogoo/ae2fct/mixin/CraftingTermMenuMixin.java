package me.myogoo.ae2fct.mixin;

import appeng.menu.me.common.IClientRepo;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = CraftingTermMenu.class, remap = false)
public abstract class CraftingTermMenuMixin extends MEStorageMenu {
    private CraftingTermMenuMixin() {
        super(null, 0, null, null, false);
    }

    @Inject(method = "findMissingIngredients", at = @At("RETURN"), cancellable = true)
    private void checkFluidForMissingIngredients(Map<Integer, Ingredient> ingredients,
            CallbackInfoReturnable<CraftingTermMenu.MissingIngredientSlots> cir) {
        if (!FluidCraftingTerminalIntegration.hasFluidInteractUpgrade(this)) {
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
        if (cir.getReturnValue()) {
            return;
        }

        if (!FluidCraftingTerminalIntegration.hasFluidInteractUpgrade(this)) {
            return;
        }

        IClientRepo clientRepo = this.getClientRepo();
        if (FluidCraftingTerminalIntegration.hasFluidIngredient(ingredient, clientRepo)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isCraftable", at = @At("RETURN"), cancellable = true)
    private void checkFluidCraftable(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            return;
        }

        if (FluidCraftingTerminalIntegration.hasFluidInteractUpgrade(this)
                && FluidCraftingTerminalIntegration.isFluidCraftable(itemStack, this.getClientRepo())) {
            cir.setReturnValue(true);
        }
    }
}
