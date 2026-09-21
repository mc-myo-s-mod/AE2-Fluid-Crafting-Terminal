package me.myogoo.ae2fct.gametest;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

@Mod("ae2fct_gametest")
public final class Ae2fctGameTestMod {

    public Ae2fctGameTestMod(IEventBus modEventBus) {
        modEventBus.addListener(Ae2fctGameTestMod::registerGameTests);
    }

    private static void registerGameTests(RegisterGameTestsEvent event) {
        event.register(VirtualFluidRecipePolicyGameTest.class);
    }
}
