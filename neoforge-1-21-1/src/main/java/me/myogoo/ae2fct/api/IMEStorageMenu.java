package me.myogoo.ae2fct.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;

public interface IMEStorageMenu {
    void ae2fct$returnVirtualFluidItems(Player player);

    boolean ae2fct$isVirtualFluidRecipeBlacklisted(ResourceLocation recipeId);
}
