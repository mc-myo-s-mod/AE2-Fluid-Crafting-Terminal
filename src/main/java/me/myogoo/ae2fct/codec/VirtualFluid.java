package me.myogoo.ae2fct.codec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public record VirtualFluid(FluidStack fluid, int capacity) {
    private static final String FLUID_TAG = "Fluid";
    private static final String CAPACITY_TAG = "Capacity";

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put(FLUID_TAG, fluid.writeToNBT(new CompoundTag()));
        tag.putInt(CAPACITY_TAG, capacity);
        return tag;
    }

    @Nullable
    public static VirtualFluid load(@Nullable CompoundTag tag) {
        if (tag == null || !tag.contains(FLUID_TAG, Tag.TAG_COMPOUND)) {
            return null;
        }

        FluidStack fluid = FluidStack.loadFluidStackFromNBT(tag.getCompound(FLUID_TAG));
        if (fluid.isEmpty()) {
            return null;
        }

        int capacity = tag.contains(CAPACITY_TAG, Tag.TAG_INT) ? tag.getInt(CAPACITY_TAG) : fluid.getAmount();
        return new VirtualFluid(fluid, capacity);
    }
}
