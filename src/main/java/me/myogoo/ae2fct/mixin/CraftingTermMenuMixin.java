package me.myogoo.ae2fct.mixin;

import appeng.menu.me.common.IClientRepo;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Mixin(value = CraftingTermMenu.class, remap = false)
public abstract class CraftingTermMenuMixin extends MEStorageMenu {

    // MEStorageMenu의 생성자를 만족시키기 위한 더미 - 실제로 호출되지 않음
    private CraftingTermMenuMixin() {
        super(null, 0, null, null, false);
    }

    /**
     * findMissingIngredients의 결과에서 fluid 네트워크를 추가로 검사하여
     * fluid로 충족 가능한 재료를 missing/craftable에서 제거합니다.
     */
    @Inject(method = "findMissingIngredients", at = @At("RETURN"), cancellable = true)
    private void checkFluidForMissingIngredients(Map<Integer, Ingredient> ingredients,
            CallbackInfoReturnable<CraftingTermMenu.MissingIngredientSlots> cir) {
        if (!FluidCraftingTerminalIntegration.hasFluidInteractUpgrade(this)) {
            return;
        }

        CraftingTermMenu.MissingIngredientSlots result = cir.getReturnValue();
        if (!result.anyMissingOrCraftable()) {
            return;
        }

        IClientRepo clientRepo = this.getClientRepo();
        if (clientRepo == null) {
            return;
        }

        Set<Integer> newMissing = new HashSet<>(result.missingSlots());
        Set<Integer> newCraftable = new HashSet<>(result.craftableSlots());
        boolean changed = false;

        // missing과 craftable 슬롯 모두 검사
        Set<Integer> slotsToCheck = new HashSet<>();
        slotsToCheck.addAll(result.missingSlots());
        slotsToCheck.addAll(result.craftableSlots());

        for (int slot : slotsToCheck) {
            Ingredient ingredient = ingredients.get(slot);
            if (ingredient == null)
                continue;

            FluidCraftingHelper.FluidAvailability availability = FluidCraftingHelper
                    .checkFluidAvailabilityInClientRepo(ingredient, clientRepo);

            if (availability.available()) {
                // fluid가 네트워크에 존재 → missing/craftable에서 제거
                newMissing.remove(slot);
                newCraftable.remove(slot);
                changed = true;
            } else if (availability.craftable() && newMissing.contains(slot)) {
                // fluid가 craftable → missing에서 craftable로 이동
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
        if (cir.getReturnValue()) {
            return;
        }

        if (!FluidCraftingTerminalIntegration.hasFluidInteractUpgrade(this)) {
            return;
        }

        IClientRepo clientRepo = this.getClientRepo();
        if (FluidCraftingHelper.hasAvailableFluidInClientRepo(ingredient, clientRepo, 1000)) {
            cir.setReturnValue(true);
        }
    }

    /**
     * isCraftable에서 아이템이 craftable하지 않은 경우,
     * 해당 아이템에서 fluid를 추출하여 fluid가 craftable인지도 확인합니다.
     */
    @Inject(method = "isCraftable", at = @At("RETURN"), cancellable = true)
    private void checkFluidCraftable(ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (!FluidCraftingTerminalIntegration.hasFluidInteractUpgrade(this)) {
            return;
        }

        if (cir.getReturnValue()) {
            return; // 이미 craftable이면 추가 검사 불필요
        }

        IClientRepo clientRepo = this.getClientRepo();
        if (clientRepo == null) {
            return;
        }

        // Ingredient.of(itemStack)을 사용하여 Helper 활용
        FluidCraftingHelper.FluidAvailability availability = FluidCraftingHelper
                .checkFluidAvailabilityInClientRepo(Ingredient.of(itemStack), clientRepo);

        if (availability.craftable()) {
            cir.setReturnValue(true);
        }
    }
}
