package me.myogoo.ae2fct.mixin;

import appeng.menu.slot.CraftingTermSlot;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CraftingTermSlot.class, remap = false)
public abstract class CraftingTermSlotMixin {
    @WrapOperation(method = { "findRecipe", "getRemainingItems" }, remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/Recipe;matches(Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;)Z", remap = true))
    private boolean ae2fct$withRecipeContext(Recipe<?> recipe, Container container, Level level,
            Operation<Boolean> original) {
        if (!FluidCraftingTerminalIntegration.hasVirtualFluidInput(container)
                || !FluidCraftingTerminalIntegration.shouldApplyVirtualFluidRecipeBlacklist(level)) {
            return original.call(recipe, container, level);
        }

        ResourceLocation previous = FluidCraftingTerminalIntegration.setRecipeContext(recipe.getId());
        try {
            return !FluidCraftingTerminalIntegration.isCurrentRecipeBlacklisted()
                    && original.call(recipe, container, level);
        } finally {
            FluidCraftingTerminalIntegration.restoreRecipeContext(previous);
        }
    }
}
