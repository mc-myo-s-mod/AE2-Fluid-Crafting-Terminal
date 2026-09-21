package me.myogoo.ae2fct.integration.recipeviewer;

import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;

public final class VirtualFluidBlacklistRecipes {
    public static final String CATEGORY_PATH = "virtual_fluid_blacklist";

    private VirtualFluidBlacklistRecipes() {
    }

    public static List<VirtualFluidBlacklistRecipe> fromLocalConfig(RecipeManager recipeManager,
            RegistryAccess registryAccess) {
        List<VirtualFluidBlacklistRecipe> recipes = new ArrayList<>();
        for (String configuredId : FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get()) {
            ResourceLocation id = ResourceLocation.tryParse(configuredId);
            if (id == null) {
                continue;
            }
            recipeManager.byKey(id)
                    .filter(recipe -> recipe.getType() == RecipeType.CRAFTING)
                    .filter(CraftingRecipe.class::isInstance)
                    .map(CraftingRecipe.class::cast)
                    .map(recipe -> create(recipe, registryAccess))
                    .ifPresent(recipes::add);
        }
        return recipes;
    }

    private static VirtualFluidBlacklistRecipe create(CraftingRecipe recipe, RegistryAccess registryAccess) {
        NonNullList<Ingredient> inputs = recipe.getIngredients();
        int width = 3;
        int height = 3;
        boolean shapeless = !(recipe instanceof ShapedRecipe);
        if (recipe instanceof ShapedRecipe shaped) {
            width = shaped.getWidth();
            height = shaped.getHeight();
        }
        ItemStack output = recipe.getResultItem(registryAccess).copy();
        return new VirtualFluidBlacklistRecipe(recipe.getId(), viewerId(recipe.getId()), recipe, inputs, output, width,
                height, shapeless);
    }

    public static ResourceLocation viewerId(ResourceLocation recipeId) {
        return ResourceLocation.fromNamespaceAndPath(Ae2fct.MODID,
                CATEGORY_PATH + "/" + recipeId.getNamespace() + "/" + recipeId.getPath());
    }
}
