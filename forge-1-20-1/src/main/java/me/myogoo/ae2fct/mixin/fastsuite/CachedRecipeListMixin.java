package me.myogoo.ae2fct.mixin.fastsuite;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.fastsuite.CachedRecipeList", remap = false)
public abstract class CachedRecipeListMixin {
    @WrapOperation(method = "getRecipeFor", remap = false, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/Recipe;matches(Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;)Z", remap = true))
    private boolean ae2fct$matchSerialRecipe(Recipe<?> recipe, Container container, Level level,
            Operation<Boolean> original) {
        return ae2fct$matchRecipe(recipe, container, level, original);
    }

    // FastSuite evaluates these predicates on worker threads; establish context there, not on the caller.
    @WrapOperation(method = { "lambda$getRecipeFor$0", "lambda$getRecipesFor$4" }, remap = false, require = 2, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/Recipe;matches(Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;)Z", remap = true))
    private static boolean ae2fct$matchRecipe(Recipe<?> recipe, Container container, Level level,
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
