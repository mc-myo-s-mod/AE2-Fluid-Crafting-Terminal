package me.myogoo.ae2fct.gametest;

import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod("ae2fct_gametest")
public final class Ae2fctGameTestMod {
    public Ae2fctGameTestMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(Ae2fctGameTestMod::register);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> Ae2fctClientProbe::register);
    }

    private static void register(RegisterGameTestsEvent event) {
        event.register(Ae2fctGameTests.class);
    }
}
