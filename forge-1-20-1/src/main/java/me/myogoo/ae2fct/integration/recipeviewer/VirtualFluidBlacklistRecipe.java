package me.myogoo.ae2fct.integration.recipeviewer;

import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;

public record VirtualFluidBlacklistRecipe(ResourceLocation id, ResourceLocation viewerId, CraftingRecipe recipe,
                                          NonNullList<Ingredient> inputs, ItemStack output, int width, int height,
                                          boolean shapeless) {
}
