package me.myogoo.ae2fct.mixin.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.Map;

@Mixin(value = EmiApi.class, remap = false)
public interface EmiApiAccessor {
    @Invoker(value = "pruneSources", remap = false)
    static List<EmiRecipe> ae2fct$pruneSources(List<EmiRecipe> recipes, EmiStack stack) {
        throw new AssertionError();
    }

    @Invoker(value = "pruneUses", remap = false)
    static List<EmiRecipe> ae2fct$pruneUses(List<EmiRecipe> recipes, EmiIngredient ingredient) {
        throw new AssertionError();
    }

    @Invoker(value = "mapRecipes", remap = false)
    static Map<EmiRecipeCategory, List<EmiRecipe>> ae2fct$mapRecipes(List<EmiRecipe> recipes) {
        throw new AssertionError();
    }

    @Invoker(value = "setPages", remap = false)
    static void ae2fct$setPages(Map<EmiRecipeCategory, List<EmiRecipe>> recipes, EmiIngredient focus) {
        throw new AssertionError();
    }
}
