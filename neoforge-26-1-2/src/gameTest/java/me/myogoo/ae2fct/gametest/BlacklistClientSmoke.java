package me.myogoo.ae2fct.gametest;

import appeng.client.integrations.jei.transfer.UseCraftingRecipeTransfer;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.menu.me.crafting.CraftConfirmMenu;
import com.mojang.logging.LogUtils;
import me.myogoo.ae2fct.integration.jei.BlacklistRecipeCategory;
import me.myogoo.ae2fct.integration.rei.AE2FCTReiPlugin;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
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
            var lookup = MethodHandles.lookup();
            if (ModList.get().isLoaded("jei")) {
                lookup.ensureInitialized(UseCraftingRecipeTransfer.class);
                lookup.ensureInitialized(BlacklistRecipeCategory.class);
            }
            if (ModList.get().isLoaded("roughlyenoughitems")) {
                lookup.ensureInitialized(AE2FCTReiPlugin.class);
            }
            lookup.ensureInitialized(EncodingHelper.class);
            lookup.ensureInitialized(CraftConfirmMenu.class);
            LOGGER.info("AE2FCT blacklist client mixin smoke PASS");
            minecraft.execute(minecraft::stop);
        } catch (Throwable e) {
            throw new AssertionError("AE2FCT blacklist client mixin smoke failed", e);
        }
    }
}
