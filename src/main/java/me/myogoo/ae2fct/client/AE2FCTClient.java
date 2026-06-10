package me.myogoo.ae2fct.client;

import me.myogoo.ae2fct.Ae2fct;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = Ae2fct.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class AE2FCTClient {
    private AE2FCTClient() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(AE2FCTConfigTab::initialize);
    }
}
