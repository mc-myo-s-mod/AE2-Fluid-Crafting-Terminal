package me.myogoo.ae2fct.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

public interface IMEStorageMenu {
    void ae2fct$returnVirtualFluidItems(Player player);

    boolean ae2fct$allowsVirtualFluidRecipe(ResourceKey<Recipe<?>> recipeId);
}
