package me.myogoo.ae2fct.mixin;

import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.PatternEncodingTermMenu;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.myogoo.ae2fct.util.VirtualFluidRecipePolicy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PatternEncodingTermMenu.class, remap = false)
public abstract class PatternEncodingTermMenuMixin {
    @Shadow
    private RecipeHolder<CraftingRecipe> currentRecipe;

    @WrapMethod(method = "getAndUpdateOutput")
    private ItemStack ae2fct$withRecipePolicy(Operation<ItemStack> original) {
        return VirtualFluidRecipePolicy.withRecipe((MEStorageMenu) (Object) this, null, original::call);
    }

    @WrapOperation(method = "getAndUpdateOutput", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;matches(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Z"))
    private boolean ae2fct$checkCachedRecipe(CraftingRecipe recipe, RecipeInput input, Level level,
            Operation<Boolean> original) {
        return VirtualFluidRecipePolicy.matchesAllowed(currentRecipe, input, level,
                () -> original.call(recipe, input, level));
    }
}
