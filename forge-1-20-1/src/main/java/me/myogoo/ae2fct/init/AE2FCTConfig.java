package me.myogoo.ae2fct.init;

import me.myogoo.ae2fct.config.FluidCraftingConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class AE2FCTConfig {
    private AE2FCTConfig() {
    }

    public static void initialize() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, FluidCraftingConfig.COMMON_SPEC,
                "ae2fct-common.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, FluidCraftingConfig.CLIENT_SPEC,
                "ae2fct-client.toml");
    }
}
