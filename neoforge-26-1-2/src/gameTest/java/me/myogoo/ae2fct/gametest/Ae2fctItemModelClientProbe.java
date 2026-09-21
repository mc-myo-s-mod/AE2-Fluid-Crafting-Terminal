package me.myogoo.ae2fct.gametest;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import me.myogoo.ae2fct.item.VirtualFluidItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(value = "ae2fct_gametest", dist = Dist.CLIENT)
public final class Ae2fctItemModelClientProbe {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean opened;
    private static boolean captured;

    public Ae2fctItemModelClientProbe() {
        NeoForge.EVENT_BUS.addListener(Ae2fctItemModelClientProbe::onClientTick);
        NeoForge.EVENT_BUS.addListener(Ae2fctItemModelClientProbe::onRenderFramePost);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (opened || minecraft.getOverlay() != null || minecraft.level == null || minecraft.player == null) {
            return;
        }

        opened = true;
        minecraft.setScreen(new ProbeScreen(
                VirtualFluidItem.createItemStack(Fluids.WATER),
                VirtualFluidItem.createItemStack(Fluids.LAVA)));
    }

    private static void onRenderFramePost(RenderFrameEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (captured || minecraft.getOverlay() != null || !(minecraft.screen instanceof ProbeScreen screen)
                || screen.extractedFrames < 3) {
            return;
        }

        captured = true;
        Screenshot.takeScreenshot(minecraft.getMainRenderTarget(), image -> inspectAndFinish(minecraft, screen, image));
    }

    private static void inspectAndFinish(Minecraft minecraft, ProbeScreen screen, NativeImage image) {
        Path reportPath = minecraft.gameDirectory.toPath().resolve(
                "../build/reports/port-verification/virtual-fluid-render.png").normalize();
        try {
            Files.createDirectories(reportPath.getParent());
            int waterSlot = changedCenterPixels(image, screen, ProbeScreen.WATER_X, ProbeScreen.SLOT_Y);
            int lavaSlot = changedCenterPixels(image, screen, ProbeScreen.LAVA_X, ProbeScreen.SLOT_Y);
            int waterCursor = changedCenterPixels(image, screen, ProbeScreen.WATER_X, ProbeScreen.CURSOR_Y);
            int lavaCursor = changedCenterPixels(image, screen, ProbeScreen.LAVA_X, ProbeScreen.CURSOR_Y);
            image.writeToFile(reportPath);

            String counts = "waterSlot=" + waterSlot + ", lavaSlot=" + lavaSlot
                    + ", waterCursor=" + waterCursor + ", lavaCursor=" + lavaCursor;
            if (waterSlot == 0 || lavaSlot == 0 || waterCursor == 0 || lavaCursor == 0) {
                fail(minecraft, "AE2FCT virtual fluid GUI model center stayed invisible: " + counts
                        + ", screenshot=" + reportPath, null);
                return;
            }

            LOGGER.info("AE2FCT virtual fluid GUI model probe PASS: {}, screenshot={}", counts, reportPath);
            minecraft.execute(minecraft::stop);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            image.close();
        }
    }

    private static int changedCenterPixels(NativeImage image, ProbeScreen screen, int itemX, int itemY) {
        int minX = scale(itemX + 7, image.getWidth(), screen.width);
        int maxX = scale(itemX + 9, image.getWidth(), screen.width);
        int minY = scale(itemY + 7, image.getHeight(), screen.height);
        int maxY = scale(itemY + 9, image.getHeight(), screen.height);

        int changed = 0;
        for (int y = minY; y < maxY; y++) {
            for (int x = minX; x < maxX; x++) {
                if ((image.getPixel(x, y) & 0x00FFFFFF) != 0x0000FF00) {
                    changed++;
                }
            }
        }
        return changed;
    }

    private static int scale(int guiCoordinate, int imageSize, int guiSize) {
        return Math.round(guiCoordinate * imageSize / (float) guiSize);
    }

    private static void fail(Minecraft minecraft, String message, Throwable cause) {
        LOGGER.error(message, cause);
        minecraft.execute(() -> {
            throw cause == null ? new AssertionError(message) : new AssertionError(message, cause);
        });
    }

    private static final class ProbeScreen extends Screen {
        private static final int WATER_X = 40;
        private static final int LAVA_X = 72;
        private static final int SLOT_Y = 40;
        private static final int CURSOR_Y = 80;

        private final ItemStack water;
        private final ItemStack lava;
        private int extractedFrames;

        private ProbeScreen(ItemStack water, ItemStack lava) {
            super(Component.literal("AE2FCT item model probe"));
            this.water = water;
            this.lava = lava;
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor gui, int mouseX, int mouseY, float partialTick) {
            extractedFrames++;
            gui.fill(0, 0, gui.guiWidth(), gui.guiHeight(), 0xFF00FF00);
            gui.item(water, WATER_X, SLOT_Y);
            gui.item(lava, LAVA_X, SLOT_Y);
            gui.item(water, WATER_X, CURSOR_Y);
            gui.item(lava, LAVA_X, CURSOR_Y);
        }
    }
}
