package me.myogoo.ae2fct.mixin.fastsuite;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.fastsuite.AuxRecipeManager", remap = false)
public abstract class AuxRecipeManagerMixin {
    @WrapOperation(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/Recipe;matches(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Z"),
            require = 1)
    private boolean ae2fct$matchHintedRecipe(Recipe<?> recipe, RecipeInput input, Level level,
            Operation<Boolean> original, @Local(argsOnly = true) RecipeHolder<?> holder) {
        return ae2fct$matchRecipe(holder, recipe, input, level, original);
    }

    // FastSuite unsafe mode evaluates these predicates on worker threads; establish context there.
    @WrapOperation(method = { "lambda$getRecipeFor$0", "lambda$getRecipesFor$3" },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/Recipe;matches(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Z"),
            require = 2)
    private static boolean ae2fct$matchUnsafeRecipe(Recipe<?> recipe, RecipeInput input, Level level,
            Operation<Boolean> original, @Local(argsOnly = true) RecipeHolder<?> holder) {
        return ae2fct$matchRecipe(holder, recipe, input, level, original);
    }

    private static boolean ae2fct$matchRecipe(RecipeHolder<?> holder, Recipe<?> recipe, RecipeInput input, Level level,
            Operation<Boolean> original) {
        if (!FluidCraftingHelper.containsVirtualFluidItem(input)) {
            return original.call(recipe, input, level);
        }

        ResourceLocation recipeId = level.isClientSide() ? null : holder.id();
        if (FluidCraftingHelper.isVirtualFluidRecipeBlacklisted(null, recipeId)) {
            return false;
        }

        return FluidCraftingHelper.withVirtualFluidRecipePolicy(null, recipeId,
                () -> original.call(recipe, input, level));
    }
}
