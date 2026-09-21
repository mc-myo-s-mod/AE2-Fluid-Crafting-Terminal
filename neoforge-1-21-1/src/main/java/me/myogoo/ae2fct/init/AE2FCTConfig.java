package me.myogoo.ae2fct.init;

import me.myogoo.ae2fct.config.FluidCraftingConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

public class AE2FCTConfig {
    public static void initialize(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, FluidCraftingConfig.COMMON_SPEC, "ae2fct-common.toml");
        container.registerConfig(ModConfig.Type.CLIENT, FluidCraftingConfig.CLIENT_SPEC, "ae2fct-client.toml");
    }
}
