package me.myogoo.ae2fct;

import me.myogoo.ae2fct.init.AE2FCTConfig;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.init.AE2FCTItems;
import me.myogoo.ae2fct.item.VirtualFluidItem;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Ae2fct.MODID)
public class Ae2fct {
    public static final String MODID = "ae2fct";

    public Ae2fct(IEventBus modEventBus, ModContainer modContainer) {
        AE2FCTConfig.initialize(modContainer);
        AE2FCTDataComponent.REGISTER.register(modEventBus);
        AE2FCTItems.REGISTER.register(modEventBus);
        modEventBus.addListener(Ae2fct::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.Fluid.ITEM,
                (stack, access) -> VirtualFluidItem.createFluidHandler(stack),
                AE2FCTItems.VIRTUAL_FLUID_ITEM.get());
    }

    public static Identifier makeId(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
