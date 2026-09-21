package me.myogoo.ae2fct.gametest;

import appeng.integration.modules.emi.EmiUseCraftingRecipeHandler;
import appeng.integration.modules.itemlists.EncodingHelper;
import com.mojang.logging.LogUtils;
import me.myogoo.ae2fct.integration.emi.AE2FCTEmiPlugin;
import me.myogoo.ae2fct.integration.jei.AE2FCTJeiPlugin;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandles;

@Mod(value = "ae2fct_gametest", dist = Dist.CLIENT)
public final class BlacklistClientSmoke {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean done;

    public BlacklistClientSmoke() {
        if (Boolean.getBoolean("ae2fct.blacklistClientSmoke")) {
            NeoForge.EVENT_BUS.addListener(BlacklistClientSmoke::onClientTick);
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (done || minecraft.getOverlay() != null || minecraft.screen == null) {
            return;
        }

        done = true;
        try {
            ensureInitialized(EmiUseCraftingRecipeHandler.class);
            ensureInitialized(EncodingHelper.class);
            ensureInitialized(AE2FCTJeiPlugin.class);
            ensureInitialized(AE2FCTEmiPlugin.class);
            LOGGER.info("AE2FCT blacklist client mixin smoke PASS");
            minecraft.execute(minecraft::stop);
        } catch (Throwable e) {
            throw new AssertionError("AE2FCT blacklist client mixin smoke failed", e);
        }
    }

    private static void ensureInitialized(Class<?> type) throws IllegalAccessException {
        MethodHandles.lookup().ensureInitialized(type);
    }
}
