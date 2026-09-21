package me.myogoo.ae2fct.mixin.minecraft;

import me.myogoo.ae2fct.util.VirtualFluidRecipePolicy;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

@Mixin(RecipeMap.class)
public abstract class RecipeMapMixin {
    @Inject(method = "getRecipesFor", at = @At("RETURN"), cancellable = true)
    private <I extends RecipeInput, T extends Recipe<I>> void ae2fct$filterVirtualFluidRecipes(
            RecipeType<T> type, I input, Level level, CallbackInfoReturnable<Stream<RecipeHolder<T>>> cir) {
        // Filter before findFirst so another allowed matching recipe can still be selected.
        cir.setReturnValue(cir.getReturnValue().filter(recipe -> VirtualFluidRecipePolicy.isAllowed(recipe, input, level)));
    }
}
