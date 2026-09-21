package me.myogoo.ae2fct.mixin;

import appeng.crafting.RecipeAccess;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.myogoo.ae2fct.util.VirtualFluidRecipePolicy;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = RecipeAccess.class, remap = false)
public abstract class RecipeAccessMixin {
    @WrapMethod(method = "getRecipeFor(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/item/crafting/RecipeHolder;)Lnet/minecraft/world/item/crafting/RecipeHolder;")
    private static <I extends RecipeInput, T extends Recipe<I>> RecipeHolder<T> ae2fct$checkRecipeHint(
            Level level, RecipeType<T> type, I input, RecipeHolder<T> hint, Operation<RecipeHolder<T>> original) {
        return original.call(level, type, input,
                hint != null && !VirtualFluidRecipePolicy.isAllowed(hint, input, level) ? null : hint);
    }
}
