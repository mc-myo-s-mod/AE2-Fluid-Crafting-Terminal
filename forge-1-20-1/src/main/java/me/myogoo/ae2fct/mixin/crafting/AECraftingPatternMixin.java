package me.myogoo.ae2fct.mixin.crafting;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.pattern.AECraftingPattern;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AECraftingPattern.class, remap = false)
public abstract class AECraftingPatternMixin {
    @Shadow
    @Final
    private CraftingRecipe recipe;

    @Shadow
    @Final
    private GenericStack[] sparseInputs;

    @WrapOperation(method = { "<init>", "isItemValid", "assemble" }, remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;matches(Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;)Z", remap = true))
    private boolean ae2fct$withRecipeContext(CraftingRecipe recipe, Container container, Level level,
            Operation<Boolean> original) {
        if (!FluidCraftingTerminalIntegration.shouldApplyVirtualFluidRecipeBlacklist(level)) {
            return original.call(recipe, container, level);
        }

        ResourceLocation previous = FluidCraftingTerminalIntegration.setRecipeContext(recipe.getId());
        try {
            if (FluidCraftingTerminalIntegration.isCurrentRecipeBlacklisted()
                    && (FluidCraftingTerminalIntegration.hasVirtualFluidInput(container)
                    || ae2fct$hasEncodedVirtualFluidInput())) {
                return false;
            }
            return original.call(recipe, container, level);
        } finally {
            FluidCraftingTerminalIntegration.restoreRecipeContext(previous);
        }
    }

    @Inject(method = "assemble", at = @At("HEAD"), cancellable = true)
    private void ae2fct$rejectCachedBlacklistedVirtualFluidPattern(Container container, Level level,
            CallbackInfoReturnable<ItemStack> cir) {
        if (!FluidCraftingTerminalIntegration.shouldApplyVirtualFluidRecipeBlacklist(level)) {
            return;
        }

        ResourceLocation previous = FluidCraftingTerminalIntegration.setRecipeContext(recipe.getId());
        try {
            if (FluidCraftingTerminalIntegration.isCurrentRecipeBlacklisted()
                    && (FluidCraftingTerminalIntegration.hasVirtualFluidInput(container)
                    || ae2fct$hasEncodedVirtualFluidInput())) {
                cir.setReturnValue(ItemStack.EMPTY);
            }
        } finally {
            FluidCraftingTerminalIntegration.restoreRecipeContext(previous);
        }
    }

    private boolean ae2fct$hasEncodedVirtualFluidInput() {
        for (GenericStack input : sparseInputs) {
            if (input != null && input.what() instanceof AEItemKey itemKey
                    && itemKey.getItem() == AE2FCTItems.VIRTUAL_FLUID_ITEM.get()) {
                return true;
            }
        }
        return false;
    }
}
