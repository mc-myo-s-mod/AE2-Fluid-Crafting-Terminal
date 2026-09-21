package me.myogoo.ae2fct.mixin;

import appeng.crafting.pattern.AECraftingPattern;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.myogoo.ae2fct.util.VirtualFluidRecipePolicy;
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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AECraftingPattern.class, remap = false)
public abstract class AECraftingPatternMixin {
    @Shadow @Final
    private RecipeHolder<?> recipeHolder;

    @Shadow @Final
    private CraftingInput.Positioned positionedPattern;

    @WrapOperation(method = {"<init>", "isItemValid"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;matches(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Z"))
    private boolean ae2fct$checkVirtualFluidPattern(CraftingRecipe recipe, RecipeInput input, Level level,
            Operation<Boolean> original) {
        return VirtualFluidRecipePolicy.isAllowed(recipeHolder, input, level) && original.call(recipe, input, level);
    }

    @Inject(method = "assemble", at = @At("HEAD"), cancellable = true)
    private void ae2fct$checkCachedPattern(CraftingInput input, Level level, CallbackInfoReturnable<ItemStack> cir) {
        if (!VirtualFluidRecipePolicy.isAllowed(recipeHolder, positionedPattern.input(), level)
                || !VirtualFluidRecipePolicy.isAllowed(recipeHolder, input, level)) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
