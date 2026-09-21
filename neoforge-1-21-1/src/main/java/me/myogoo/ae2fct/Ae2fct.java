package me.myogoo.ae2fct;

import com.mojang.logging.LogUtils;
import me.myogoo.ae2fct.init.AE2FCTConfig;
import me.myogoo.ae2fct.init.AE2FCTItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Ae2fct.MODID)
public class Ae2fct {
    public static final String MODID = "ae2fct";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Ae2fct(IEventBus modEventBus, ModContainer modContainer) {
        AE2FCTConfig.initialize(modContainer);
        AE2FCTDataComponent.REGISTER.register(modEventBus);
        AE2FCTItems.REGISTER.register(modEventBus);
    }
}
