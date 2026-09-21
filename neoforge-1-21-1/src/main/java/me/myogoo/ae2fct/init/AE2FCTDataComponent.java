package me.myogoo.ae2fct.init;

import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.codec.VirtualFluid;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

public class AE2FCTDataComponent {
        public static DeferredRegister.DataComponents REGISTER = DeferredRegister
                        .createDataComponents(Registries.DATA_COMPONENT_TYPE, Ae2fct.MODID);

        public static final DataComponentType<VirtualFluid> VIRTUAL_FLUID = register("virtual_fluid",
                        builder -> builder
                                        .persistent(VirtualFluid.CODEC)
                                        .networkSynchronized(VirtualFluid.STREAM_CODEC));

        private static <T> DataComponentType<T> register(String name,
                        Consumer<DataComponentType.Builder<T>> customizer) {
                var builder = DataComponentType.<T>builder();
                customizer.accept(builder);
                var componentType = builder.build();
                REGISTER.register(name, () -> componentType);
                return componentType;
        }

}
