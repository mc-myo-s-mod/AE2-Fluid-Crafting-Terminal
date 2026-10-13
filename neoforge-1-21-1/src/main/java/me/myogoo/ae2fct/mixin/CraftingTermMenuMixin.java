package me.myogoo.ae2fct.mixin;

import appeng.menu.me.common.IClientRepo;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Optional;

@Mixin(value = CraftingTermMenu.class, remap = false)
public abstract class CraftingTermMenuMixin extends MEStorageMenu {

    // MEStorageMenu의 생성자를 만족시키기 위한 더미 - 실제로 호출되지 않음
    private CraftingTermMenuMixin() {
        super(null, 0, null, null, false);
    }

    @WrapOperation(method = "updateCurrentRecipeAndOutput", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/RecipeManager;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"), require = 1)
    private <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> ae2fct$findRecipeWithVirtualFluidPolicy(
            RecipeManager recipeManager,
            RecipeType<T> type,
            I input,
            Level level,
            Operation<Optional<RecipeHolder<T>>> original) {
        if (type == RecipeType.CRAFTING && input instanceof CraftingInput craftingInput) {
            if (!FluidCraftingHelper.containsVirtualFluidItem(craftingInput)) {
                return original.call(recipeManager, type, input, level);
            }

            @SuppressWarnings("unchecked")
            Optional<RecipeHolder<T>> result = (Optional<RecipeHolder<T>>) (Optional<?>) FluidCraftingHelper
                    .findCraftingRecipeWithVirtualFluidPolicy((MEStorageMenu) (Object) this, craftingInput, level);
            return result;
        }

        return original.call(recipeManager, type, input, level);
    }

    @Unique
    private boolean ae2fct$canUseVirtualFluidForCurrentRecipe() {
        return FluidCraftingHelper.isRecipePreviewAllowed();
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

        if (!ae2fct$canUseVirtualFluidForCurrentRecipe()) {
            return;
        }

        var result = cir.getReturnValue();
        var updated = FluidCraftingTerminalIntegration.resolveMissingIngredients(this, ingredients, result);
        if (updated != result) cir.setReturnValue(updated);
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

        if (!ae2fct$canUseVirtualFluidForCurrentRecipe()) {
            return;
        }

        IClientRepo clientRepo = this.getClientRepo();
        if (FluidCraftingHelper.reserveAvailableFluidInClientRepo(ingredient, clientRepo, 1000, usedIngredients)) {
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

        if (!ae2fct$canUseVirtualFluidForCurrentRecipe()) {
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
