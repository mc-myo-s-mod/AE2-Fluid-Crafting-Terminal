package me.myogoo.ae2fct.mixin;

import appeng.api.stacks.AEItemKey;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.util.ConfigInventory;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import me.myogoo.ae2fct.api.IMEStorageMenu;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import me.myogoo.ae2fct.init.AE2FCTItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PatternEncodingTermMenu.class, remap = false)
public abstract class PatternEncodingTermMenuMixin {
    @Shadow
    @Nullable
    private CraftingRecipe currentRecipe;

    @Shadow
    @Final
    private ConfigInventory encodedInputsInv;

    @WrapMethod(method = "getAndUpdateOutput", remap = false)
    private ItemStack ae2fct$withRecipeViewerMenuContext(Operation<ItemStack> original) {
        return FluidCraftingTerminalIntegration.withRecipeViewerMenuContext((MEStorageMenu) (Object) this,
                original::call);
    }

    @WrapOperation(method = "getAndUpdateOutput", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;matches(Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;)Z", remap = true))
    private boolean ae2fct$withRecipeContext(CraftingRecipe recipe, Container container, Level level,
            Operation<Boolean> original) {
        ResourceLocation previous = FluidCraftingTerminalIntegration.setRecipeContext(recipe.getId());
        try {
            if (FluidCraftingTerminalIntegration.isCurrentRecipeBlacklisted()
                    && FluidCraftingTerminalIntegration.hasVirtualFluidInput(container)) {
                return false;
            }
            return original.call(recipe, container, level);
        } finally {
            FluidCraftingTerminalIntegration.restoreRecipeContext(previous);
        }
    }

    @WrapOperation(method = "encodeCraftingPattern", at = @At(value = "INVOKE", target = "Lappeng/api/crafting/PatternDetailsHelper;encodeCraftingPattern(Lnet/minecraft/world/item/crafting/CraftingRecipe;[Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack ae2fct$rejectBlacklistedVirtualFluidPattern(CraftingRecipe recipe, ItemStack[] ingredients,
            ItemStack output, boolean allowSubstitutes, boolean allowFluidSubstitutes,
            Operation<ItemStack> original) {
        if (ae2fct$hasVirtualFluidInput()
                && ((IMEStorageMenu) (Object) this).ae2fct$isVirtualFluidRecipeBlacklisted(recipe.getId())) {
            return null;
        }
        return original.call(recipe, ingredients, output, allowSubstitutes, allowFluidSubstitutes);
    }

    private boolean ae2fct$hasVirtualFluidInput() {
        for (int slot = 0; slot < 9; slot++) {
            if (encodedInputsInv.getKey(slot) instanceof AEItemKey itemKey
                    && itemKey.getItem() == AE2FCTItems.VIRTUAL_FLUID_ITEM.get()) {
                return true;
            }
        }
        return false;
    }
}
