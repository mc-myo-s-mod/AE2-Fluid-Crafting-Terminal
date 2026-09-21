package me.myogoo.ae2fct.client;

import me.myogoo.ae2fct.Ae2fct;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterItemModelsEvent;

@Mod(value = Ae2fct.MODID, dist = Dist.CLIENT)
public final class AE2FCTClient {
    public AE2FCTClient(IEventBus modEventBus) {
        modEventBus.addListener(AE2FCTClient::clientSetup);
        modEventBus.addListener(AE2FCTClient::registerItemModels);
    }

    private static void registerItemModels(RegisterItemModelsEvent event) {
        event.register(Ae2fct.makeId("virtual_fluid"), VirtualFluidItemModel.Unbaked.CODEC);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(AE2FCTConfigTab::initialize);
    }
}
