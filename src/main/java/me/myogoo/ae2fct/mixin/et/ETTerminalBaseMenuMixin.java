package me.myogoo.ae2fct.mixin.et;

import appeng.menu.me.common.IClientRepo;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Pseudo
@Mixin(targets = "me.myogoo.extendedterminal.menu.ETTerminalBaseMenu", remap = false)
public abstract class ETTerminalBaseMenuMixin extends MEStorageMenu {

    private ETTerminalBaseMenuMixin() {
        super(null, 0, null, null, false);
    }

    @Unique
    private boolean ae2fct$hasFluidInteractUpgrade() {
        return FluidCraftingTerminalIntegration.hasFluidInteractUpgrade(this);
    }

    @Inject(method = "findMissingIngredients", at = @At("RETURN"), cancellable = true)
    private void ae2fct$checkFluidForMissingIngredients(Map<Integer, Ingredient> ingredients,
            CallbackInfoReturnable<CraftingTermMenu.MissingIngredientSlots> cir) {
        if (!ae2fct$hasFluidInteractUpgrade()) {
            return;
        }

        var result = cir.getReturnValue();
        if (!result.anyMissingOrCraftable()) {
            return;
        }

        IClientRepo clientRepo = this.getClientRepo();
        if (clientRepo == null) {
            return;
        }

        Set<Integer> newMissing = new HashSet<>(result.missingSlots());
        Set<Integer> newCraftable = new HashSet<>(result.craftableSlots());
        Set<Integer> slotsToCheck = new HashSet<>();
        boolean changed = false;

        slotsToCheck.addAll(result.missingSlots());
        slotsToCheck.addAll(result.craftableSlots());

        for (int slot : slotsToCheck) {
            Ingredient ingredient = ingredients.get(slot);
            if (ingredient == null) {
                continue;
            }

            var availability = FluidCraftingHelper.checkFluidAvailabilityInClientRepo(ingredient, clientRepo);
            if (availability.available()) {
                newMissing.remove(slot);
                newCraftable.remove(slot);
                changed = true;
            } else if (availability.craftable() && newMissing.contains(slot)) {
                newMissing.remove(slot);
                newCraftable.add(slot);
                changed = true;
            }
        }

        if (changed) {
            cir.setReturnValue(new CraftingTermMenu.MissingIngredientSlots(newMissing, newCraftable));
        }
    }

    @Inject(method = "hasIngredient", at = @At("RETURN"), cancellable = true)
    private void ae2fct$hasFluidIngredient(Ingredient ingredient, Object2IntOpenHashMap<Object> usedIngredients,
            CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() || !ae2fct$hasFluidInteractUpgrade()) {
            return;
        }

        IClientRepo clientRepo = this.getClientRepo();
        if (FluidCraftingHelper.hasAvailableFluidInClientRepo(ingredient, clientRepo, 1000)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isCraftable", at = @At("RETURN"), cancellable = true)
    private void ae2fct$checkFluidCraftable(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() || !ae2fct$hasFluidInteractUpgrade()) {
            return;
        }

        IClientRepo clientRepo = this.getClientRepo();
        if (clientRepo == null) {
            return;
        }

        var availability = FluidCraftingHelper.checkFluidAvailabilityInClientRepo(Ingredient.of(itemStack), clientRepo);
        if (availability.craftable()) {
            cir.setReturnValue(true);
        }
    }
}
