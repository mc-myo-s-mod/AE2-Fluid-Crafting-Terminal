package me.myogoo.ae2fct;

import me.myogoo.ae2fct.init.AE2FCTConfig;
import me.myogoo.ae2fct.init.AE2FCTItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Ae2fct.MODID)
public class Ae2fct {
    public static final String MODID = "ae2fct";

    public Ae2fct() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        AE2FCTConfig.initialize();
        AE2FCTItems.ITEMS.register(modEventBus);
    }
}
