package me.myogoo.ae2fct.gametest;

import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod("ae2fct_gametest")
public final class Ae2fctGameTestMod {
    public Ae2fctGameTestMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(Ae2fctGameTestMod::register);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> Ae2fctClientProbe::register);
    }

    private static void register(RegisterGameTestsEvent event) {
        if (System.getProperty("ae2fct.persistenceSmoke") != null) {
            event.register(PersistentWorldGameTest.class);
            return;
        }
        // Fixture mutations must not autosave and race Forge's asynchronous file reloads.
        var configData = ConfigTracker.INSTANCE.fileMap().get("ae2fct-common.toml").getConfigData();
        FluidCraftingConfig.COMMON_SPEC.setConfig(new TomlParser().parse(new TomlWriter().writeToString(configData)));
        event.register(Ae2fctGameTests.class);
        if (net.minecraftforge.fml.ModList.get().isLoaded("extendedterminal")) {
            event.register(EtJeiPreviewGameTests.class);
        }
    }
}
