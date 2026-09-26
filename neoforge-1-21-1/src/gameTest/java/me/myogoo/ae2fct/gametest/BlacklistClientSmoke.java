package me.myogoo.ae2fct.gametest;

import appeng.integration.modules.itemlists.EncodingHelper;
import com.mojang.logging.LogUtils;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.EmiRecipeManager;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.registry.EmiRecipes;
import dev.emi.emi.screen.EmiScreenManager;
import me.myogoo.ae2fct.integration.emi.AE2FCTEmiPlugin;
import me.myogoo.ae2fct.integration.jei.AE2FCTJeiPlugin;
import appeng.integration.modules.emi.EmiUseCraftingRecipeHandler;
import me.myogoo.ae2fct.integration.rei.AE2FCTReiPlugin;
import me.myogoo.extendedterminal.menu.extendedterminal.ETTerminalMenu;
import me.myogoo.extendedterminal.integration.itemList.emi.extendedterminal.handler.pnael.ETCraftingRecipeHandler;
import me.myogoo.extendedterminal.integration.itemList.jei.extendedterminal.handler.panel.ETCraftingRecipeTransfer;
import me.myogoo.extendedterminal.integration.itemList.jei.extendedterminal.handler.panel.ETSmithingRecipeTransfer;
import me.myogoo.extendedterminal.integration.itemList.jei.extendedterminal.handler.panel.ETStonecutterRecipeTransfer;
import me.myogoo.extendedterminal.integration.itemList.jei.extendedcrafting.handler.ECJeiRecipeTransferHandler;
import me.myogoo.extendedterminal.integration.itemList.jei.avaritiaRe.handler.AVJeiRecipeTransferHandler;
import me.myogoo.extendedterminal.integration.itemList.jei.avaritiaNeo.handler.AVNeoJeiRecipeTransferHandler;
import me.shedaniel.rei.impl.client.gui.ScreenOverlayImpl;
import me.shedaniel.rei.impl.client.gui.widget.EntryWidget;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;
import org.slf4j.Logger;

import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

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
            ensureInitialized(EncodingHelper.class);
            if (ModList.get().isLoaded("extendedterminal")) {
                ensureInitialized(ETTerminalMenu.class);
            }
            if (ModList.get().isLoaded("emi")) {
                ensureInitialized(EmiUseCraftingRecipeHandler.class);
                ensureInitialized(AE2FCTEmiPlugin.class);
                EmiLookupProbe.run();
            }
            if (ModList.get().isLoaded("jei")) {
                ensureInitialized(AE2FCTJeiPlugin.class);
                if (ModList.get().isLoaded("extendedterminal") && !ModList.get().isLoaded("emi")) {
                    ensureInitialized(ETCraftingRecipeTransfer.class);
                    ensureInitialized(ETSmithingRecipeTransfer.class);
                    ensureInitialized(ETStonecutterRecipeTransfer.class);
                    if (ModList.get().isLoaded("extendedcrafting")) {
                        ensureInitialized(ECJeiRecipeTransferHandler.class);
                    }
                    if (ModList.get().isLoaded("avaritia")) {
                        var avaritia = net.neoforged.fml.loading.LoadingModList.get().getModFileById("avaritia");
                        if (Files.exists(avaritia.getFile().findResource(
                                "committee/nova/mods/avaritia/api/common/crafting/ITierCraftingRecipe.class"))) {
                            ensureInitialized(AVJeiRecipeTransferHandler.class);
                        }
                        if (Files.exists(avaritia.getFile().findResource(
                                "net/byAqua3/avaritia/recipe/RecipeExtremeCrafting.class"))) {
                            ensureInitialized(AVNeoJeiRecipeTransferHandler.class);
                        }
                    }
                }
            }
            if (ModList.get().isLoaded("roughlyenoughitems")) {
                ensureInitialized(EntryWidget.class);
                ensureInitialized(ScreenOverlayImpl.class);
                ensureInitialized(AE2FCTReiPlugin.class);
                LOGGER.info("AE2FCT REI fluid lookup mixin smoke PASS");
            }
            if (ModList.get().isLoaded("emi") && ModList.get().isLoaded("extendedterminal")) {
                ensureInitialized(ETCraftingRecipeHandler.class);
            }
            LOGGER.info("AE2FCT blacklist client mixin smoke PASS");
            minecraft.execute(minecraft::stop);
        } catch (Throwable e) {
            throw new AssertionError("AE2FCT blacklist client mixin smoke failed", e);
        }
    }

    private static void ensureInitialized(Class<?> type) throws IllegalAccessException {
        MethodHandles.lookup().ensureInitialized(type);
    }

    private static final class EmiLookupProbe {
        private static void run() {
            EmiRecipeManager previousManager = EmiRecipes.manager;
            boolean previousBucketLookup = FluidCraftingConfig.CLIENT.showBucketRecipesForAe2FluidKeys.get();
            var queries = new ArrayList<EmiStack>();
            var fluid = NeoForgeEmiStack.of(new FluidStack(Fluids.WATER, 1000));
            var bucket = EmiStack.of(Items.WATER_BUCKET);
            var lookupComplete = new RuntimeException("Stop before opening recipe UI at the title screen");
            try {
                EmiRecipes.manager = new EmiRecipeManager() {
                    public List<EmiRecipeCategory> getCategories() { return List.of(); }
                    public List<EmiIngredient> getWorkstations(EmiRecipeCategory category) { return List.of(); }
                    public List<EmiRecipe> getRecipes() { return List.of(); }
                    public List<EmiRecipe> getRecipes(EmiRecipeCategory category) { return List.of(); }
                    public EmiRecipe getRecipe(ResourceLocation id) { return null; }
                    public List<EmiRecipe> getRecipesByInput(EmiStack stack) {
                        queries.add(stack);
                        if (stack.isEqual(fluid)) { throw lookupComplete; }
                        return List.of();
                    }
                    public List<EmiRecipe> getRecipesByOutput(EmiStack stack) {
                        queries.add(stack);
                        if (stack.isEqual(fluid)) { throw lookupComplete; }
                        return List.of();
                    }
                };
                for (boolean bucketsEnabled : new boolean[]{false, true}) {
                    FluidCraftingConfig.CLIENT.showBucketRecipesForAe2FluidKeys.set(bucketsEnabled);
                    for (var bind : List.of(EmiConfig.viewRecipes, EmiConfig.viewUses)) {
                        queries.clear();
                        try {
                            EmiScreenManager.stackInteraction(new EmiStackInteraction(fluid), candidate -> candidate == bind);
                        } catch (RuntimeException e) {
                            if (e != lookupComplete) { throw e; }
                        }
                        if (queries.stream().noneMatch(fluid::isEqual)
                                || queries.stream().anyMatch(bucket::isEqual) != bucketsEnabled) {
                            throw new AssertionError("EMI native fluid lookup must respect fluid/bucket targets: " + queries);
                        }
                    }
                }
            } finally {
                EmiRecipes.manager = previousManager;
                FluidCraftingConfig.CLIENT.showBucketRecipesForAe2FluidKeys.set(previousBucketLookup);
            }
            LOGGER.info("AE2FCT EMI native fluid recipe/usage bucket lookup PASS");
        }
    }
}
