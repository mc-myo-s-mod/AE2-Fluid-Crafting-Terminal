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
}
