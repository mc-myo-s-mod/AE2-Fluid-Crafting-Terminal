package me.myogoo.ae2fct.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public interface IMEStorageMenu {
    void ae2fct$returnVirtualFluidItems(Player player);

    boolean ae2fct$isVirtualFluidRecipeBlacklisted(ResourceLocation recipeId);
}
