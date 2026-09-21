package me.myogoo.ae2fct.integration.recipeviewer;

import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.ArrayList;
import java.util.List;

public final class VirtualFluidBlacklistRecipes {
    public static final ResourceLocation CATEGORY_ID =
            ResourceLocation.fromNamespaceAndPath(Ae2fct.MODID, "virtual_fluid_blacklist");
    public static final Component TITLE = Component.translatable("gui.ae2fct.virtual_fluid_blacklist.title");
    public static final Component FOOTER = Component.translatable("gui.ae2fct.virtual_fluid_blacklist.footer");
    public static final Component FOOTER_TOOLTIP =
            Component.translatable("gui.ae2fct.virtual_fluid_blacklist.footer.tooltip");

    private VirtualFluidBlacklistRecipes() {
    }

    public static List<RecipeHolder<CraftingRecipe>> fromLocalConfig(RecipeManager recipeManager) {
        List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();
        for (String configuredId : FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get()) {
            ResourceLocation recipeId = ResourceLocation.tryParse(configuredId);
            if (recipeId == null) {
                continue;
            }

            recipeManager.byKey(recipeId)
                    .filter(holder -> holder.value().getType() == RecipeType.CRAFTING)
                    .map(VirtualFluidBlacklistRecipes::craftingRecipe)
                    .ifPresent(recipes::add);
        }
        return recipes;
    }

    public static ItemStack result(RecipeHolder<CraftingRecipe> recipe, RegistryAccess registryAccess) {
        return recipe.value().getResultItem(registryAccess).copy();
    }

    public static int recipeWidth(RecipeHolder<CraftingRecipe> recipe) {
        return recipe.value() instanceof ShapedRecipe shapedRecipe ? shapedRecipe.getWidth() : 3;
    }

    public static int recipeHeight(RecipeHolder<CraftingRecipe> recipe) {
        return recipe.value() instanceof ShapedRecipe shapedRecipe ? shapedRecipe.getHeight() : 3;
    }

    public static int gridSlot(RecipeHolder<CraftingRecipe> recipe, int ingredientIndex) {
        int width = recipeWidth(recipe);
        return (ingredientIndex / width) * 3 + ingredientIndex % width;
    }

    public static ResourceLocation syntheticViewerId(RecipeHolder<CraftingRecipe> recipe) {
        ResourceLocation recipeId = recipe.id();
        return ResourceLocation.fromNamespaceAndPath(Ae2fct.MODID,
                "virtual_fluid_blacklist/" + recipeId.getNamespace() + "/" + recipeId.getPath());
    }

    @SuppressWarnings("unchecked")
    private static RecipeHolder<CraftingRecipe> craftingRecipe(RecipeHolder<?> recipe) {
        return (RecipeHolder<CraftingRecipe>) recipe;
    }
}
