package me.myogoo.ae2fct.util;

import me.myogoo.ae2fct.codec.VirtualFluid;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class VirtualFluidStorage {
    private static final String ROOT_TAG = "AE2FCTVirtualFluid";

    private VirtualFluidStorage() {
    }

    public static boolean has(ItemStack stack) {
        return get(stack) != null;
    }

    @Nullable
    public static VirtualFluid get(ItemStack stack) {
        return VirtualFluid.load(stack.getTagElement(ROOT_TAG));
    }

    public static void set(ItemStack stack, VirtualFluid virtualFluid) {
        stack.addTagElement(ROOT_TAG, virtualFluid.save());
    }
}
