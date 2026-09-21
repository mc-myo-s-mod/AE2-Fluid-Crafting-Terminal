package me.myogoo.ae2fct.codec;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.fluids.FluidStack;

public record VirtualFluid(FluidStack fluid, int capacity) {
        public static final Codec<VirtualFluid> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                        FluidStack.CODEC.fieldOf("fluid").forGetter(VirtualFluid::fluid),
                        Codec.INT.fieldOf("capacity").forGetter(VirtualFluid::capacity))
                        .apply(instance, VirtualFluid::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, VirtualFluid> STREAM_CODEC = StreamCodec
                        .composite(
                                        FluidStack.STREAM_CODEC, VirtualFluid::fluid,
                                        ByteBufCodecs.INT, VirtualFluid::capacity,
                                        VirtualFluid::new);

        @Override
        public boolean equals(Object obj) {
                return this == obj || obj instanceof VirtualFluid other
                                && this.capacity == other.capacity
                                && FluidStack.matches(this.fluid, other.fluid);
        }

        @Override
        public int hashCode() {
                int result = FluidStack.hashFluidAndComponents(this.fluid);
                result = 31 * result + this.fluid.getAmount();
                return 31 * result + this.capacity;
        }
}
