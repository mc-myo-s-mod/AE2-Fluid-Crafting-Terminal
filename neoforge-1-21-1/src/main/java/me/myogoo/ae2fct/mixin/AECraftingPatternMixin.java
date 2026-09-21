package me.myogoo.ae2fct.mixin;

import appeng.crafting.pattern.AECraftingPattern;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AECraftingPattern.class, remap = false)
public abstract class AECraftingPatternMixin {

    @Shadow
    @Final
    private RecipeHolder<?> recipeHolder;

    @Shadow
    @Final
    private CraftingInput.Positioned positionedPattern;

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;matches(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Z"), require = 1)
    private boolean ae2fct$matchEncodedPatternWithVirtualFluidPolicy(CraftingRecipe recipe, RecipeInput input,
            Level level, Operation<Boolean> original) {
        if (!(input instanceof CraftingInput craftingInput)) {
            return original.call(recipe, input, level);
        }

        return FluidCraftingHelper.matchesCraftingRecipeWithVirtualFluidPolicy(null, this.recipeHolder.id(),
                recipe, craftingInput, level);
    }

    @WrapOperation(method = "isItemValid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;matches(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Z"), require = 1)
    private boolean ae2fct$matchSubstitutionWithVirtualFluidPolicy(CraftingRecipe recipe, RecipeInput input,
            Level level, Operation<Boolean> original) {
        if (!(input instanceof CraftingInput craftingInput)) {
            return original.call(recipe, input, level);
        }

        return FluidCraftingHelper.matchesCraftingRecipeWithVirtualFluidPolicy(null, this.recipeHolder.id(),
                recipe, craftingInput, level);
    }

    @WrapMethod(method = "assemble")
    private ItemStack ae2fct$assembleWithVirtualFluidPolicy(CraftingInput input, Level level,
            Operation<ItemStack> original) {
        if (!level.isClientSide() && (FluidCraftingHelper.containsVirtualFluidItem(input)
                || FluidCraftingHelper.containsVirtualFluidItem(this.positionedPattern.input()))
                && FluidCraftingHelper.isVirtualFluidRecipeBlacklisted(null, this.recipeHolder.id())) {
            return ItemStack.EMPTY;
        }

        return FluidCraftingHelper.withVirtualFluidRecipePolicy(null, level.isClientSide() ? null : this.recipeHolder.id(),
                () -> original.call(input, level));
    }
}
