package me.myogoo.ae2fct.mixin.fastsuite;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import me.myogoo.ae2fct.util.VirtualFluidRecipePolicy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "dev.shadowsoffire.fastsuite.CachedRecipeList", remap = false)
public abstract class CachedRecipeListMixin<T extends Recipe<?>> {
    @Shadow @Final
    private Reference2ObjectMap<Item, List<RecipeHolder<T>>> byPivotItem;

    @Inject(method = "gatherCandidates", at = @At("RETURN"))
    private <C extends RecipeInput> void ae2fct$addVirtualFluidBucketCandidates(C input,
            CallbackInfoReturnable<ObjectOpenHashSet<RecipeHolder<T>>> cir) {
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!FluidCraftingHelper.isVirtualFluidItem(stack)) {
                continue;
            }
            VirtualFluid virtualFluid = stack.get(AE2FCTDataComponent.VIRTUAL_FLUID);
            if (virtualFluid == null || virtualFluid.fluid().isEmpty()) {
                continue;
            }
            Item bucket = virtualFluid.fluid().getFluid().getBucket();
            if (bucket == null || bucket == Fluids.EMPTY.getBucket()) {
                continue;
            }
            List<RecipeHolder<T>> recipes = byPivotItem.get(bucket);
            if (recipes != null) {
                cir.getReturnValue().addAll(recipes);
            }
        }
    }

    @WrapOperation(method = "lambda$getRecipesFor$0", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/Recipe;matches(Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Z"))
    private static boolean ae2fct$matchRecipe(Recipe<?> recipe, RecipeInput input, Level level,
            Operation<Boolean> original, @Local(argsOnly = true) RecipeHolder<?> holder) {
        return VirtualFluidRecipePolicy.matchesAllowed(holder, input, level,
                () -> original.call(recipe, input, level));
    }
}
