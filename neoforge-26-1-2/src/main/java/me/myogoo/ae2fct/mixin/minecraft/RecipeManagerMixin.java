package me.myogoo.ae2fct.mixin.minecraft;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.myogoo.ae2fct.util.VirtualFluidRecipePolicy;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    @WrapMethod(method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;")
    private <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> ae2fct$checkRecipeHint(
            RecipeType<T> type, I input, Level level, RecipeHolder<T> hint,
            Operation<Optional<RecipeHolder<T>>> original) {
        return original.call(type, input, level,
                hint != null && !VirtualFluidRecipePolicy.isAllowed(hint, input, level) ? null : hint);
    }
}
