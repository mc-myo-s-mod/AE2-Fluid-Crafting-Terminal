package me.myogoo.ae2fct.gametest;

import appeng.integration.modules.emi.EmiUseCraftingRecipeHandler;
import appeng.integration.modules.jei.transfer.UseCraftingRecipeTransfer;
import appeng.integration.modules.jeirei.EncodingHelper;
import appeng.menu.me.crafting.CraftConfirmMenu;
import dev.emi.emi.api.widget.AnimatedTextureWidget;
import dev.emi.emi.screen.WidgetGroup;
import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.item.VirtualFluidItem;
import me.myogoo.ae2fct.integration.rei.ReiFluidRecipeLookup;
import me.shedaniel.rei.api.common.entry.type.EntryTypeRegistry;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.plugin.common.runtime.DefaultRuntimePlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.impl.client.registry.display.DisplayRegistryImpl;
import me.myogoo.ae2fct.integration.emi.VirtualFluidBlacklistEmiRecipe;
import me.myogoo.ae2fct.integration.recipeviewer.VirtualFluidBlacklistRecipe;
import me.myogoo.extendedterminal.integration.jei.extendedterminal.handler.ETCraftingRecipeTransfer;
import me.myogoo.extendedterminal.integration.jei.extendedterminal.handler.ETSmithingRecipeTransfer;
import me.myogoo.extendedterminal.integration.jei.extendedterminal.handler.ETStonecutterRecipeTransfer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.lang.invoke.MethodHandles;
import java.util.List;

final class Ae2fctClientProbe {
    private static boolean done;

    private Ae2fctClientProbe() {
    }

    static void register() {
        if (Boolean.getBoolean("ae2fct.clientProbe")) {
            MinecraftForge.EVENT_BUS.addListener(Ae2fctClientProbe::clientTick);
        }
    }

    private static void clientTick(TickEvent.ClientTickEvent event) {
        var minecraft = Minecraft.getInstance();
        if (event.phase != TickEvent.Phase.END || done || minecraft.getOverlay() != null
                || minecraft.screen == null) {
            return;
        }

        done = true;
        boolean reiLoaded = ModList.get().isLoaded("roughlyenoughitems");
        if (ModList.get().isLoaded("jei") && !reiLoaded) {
            ensureInitialized(UseCraftingRecipeTransfer.class);
            if (ModList.get().isLoaded("extendedterminal")) {
                ensureInitialized(ETCraftingRecipeTransfer.class);
                ensureInitialized(ETSmithingRecipeTransfer.class);
                ensureInitialized(ETStonecutterRecipeTransfer.class);
            }
        }
        if (ModList.get().isLoaded("emi")) {
            EmiProbe.run(minecraft);
        }
        ensureInitialized(EncodingHelper.class);
        ensureInitialized(CraftConfirmMenu.class);
        if (reiLoaded) {
            ReiProbe.run();
        }
        MixinEnvironment.getCurrentEnvironment().audit();
        System.out.println("AE2FCT client probe and mixin audit complete");
        minecraft.stop();
    }

    private static final class ReiProbe {
        private static void run() {
            // REI normally registers these on joining a world; this probe runs at the title screen.
            var entryTypes = EntryTypeRegistry.getInstance();
            if (!entryTypes.keySet().contains(VanillaEntryTypes.ITEM.getId())) {
                new DefaultRuntimePlugin().registerEntryTypes(entryTypes);
            }
            var config = FluidCraftingConfig.CLIENT;
            boolean oldBucket = config.showBucketRecipesForVirtualFluids.get();
            boolean oldFluid = config.showFluidRecipesForVirtualFluids.get();
            boolean oldAeBucket = config.showBucketRecipesForAe2FluidKeys.get();
            try {
                var virtual = ReiFluidRecipeLookup.getVirtualFluidTarget(
                        VirtualFluidItem.createItemStack(Fluids.WATER)).orElseThrow();
                var aeFluid = new ReiFluidRecipeLookup.FluidTarget(new FluidStack(Fluids.WATER, 1000), false);
                for (boolean bucket : new boolean[]{false, true}) {
                    for (boolean fluid : new boolean[]{false, true}) {
                        config.showBucketRecipesForVirtualFluids.set(bucket);
                        config.showFluidRecipesForVirtualFluids.set(fluid);
                        var entries = ReiFluidRecipeLookup.createTargetEntries(virtual);
                        boolean hasBucket = entries.stream().anyMatch(entry -> entry.getValue() instanceof ItemStack item
                                && item.is(Items.WATER_BUCKET));
                        boolean hasFluid = entries.stream().anyMatch(entry ->
                                entry.getValue() instanceof dev.architectury.fluid.FluidStack value
                                        && value.getFluid() == Fluids.WATER);
                        if (hasBucket != bucket || hasFluid != fluid
                                || entries.size() != (bucket ? 1 : 0) + (fluid ? 1 : 0)) {
                            throw new AssertionError("REI virtual-fluid config target mismatch");
                        }
                    }
                    config.showBucketRecipesForAe2FluidKeys.set(bucket);
                    if (ReiFluidRecipeLookup.createTargetEntries(aeFluid).size() != (bucket ? 2 : 1)) {
                        throw new AssertionError("REI AE2 fluid keys must retain fluid lookup and optionally add buckets");
                    }
                }
                var tag = new CompoundTag();
                tag.putString("probe", "preserved");
                var tagged = new FluidStack(Fluids.WATER, 1000, tag);
                var converted = ReiFluidRecipeLookup.toReiFluidStack(tagged);
                if (!tag.equals(converted.getTag()) || tag == converted.getTag()) {
                    throw new AssertionError("REI fluid conversion must copy NBT");
                }
                if (ReiFluidRecipeLookup.getVirtualFluidTarget(new ItemStack(Items.STICK)).isPresent()) {
                    throw new AssertionError("REI ordinary item lookups must not be intercepted");
                }
            } finally {
                config.showBucketRecipesForVirtualFluids.set(oldBucket);
                config.showFluidRecipesForVirtualFluids.set(oldFluid);
                config.showBucketRecipesForAe2FluidKeys.set(oldAeBucket);
            }
            System.out.println("AE2FCT REI fluid/bucket lookup config and NBT checks PASS");
            verifyCachedUnion();
        }

        private static void verifyCachedUnion() {
            CategoryIdentifier<Display> category = CategoryIdentifier.of(Ae2fct.makeId("lookup_probe"));
            var bucket = EntryStacks.of(Items.WATER_BUCKET);
            var fluid = EntryStacks.of(ReiFluidRecipeLookup.toReiFluidStack(new FluidStack(Fluids.WATER, 1000)));
            var targets = List.<me.shedaniel.rei.api.common.entry.EntryStack<?>>of(bucket, fluid);
            CategoryRegistry.getInstance().add(new DisplayCategory<Display>() {
                @Override
                public CategoryIdentifier<Display> getCategoryIdentifier() {
                    return category;
                }

                @Override
                public Component getTitle() {
                    return Component.literal("Fluid lookup probe");
                }

                @Override
                public me.shedaniel.rei.api.client.gui.Renderer getIcon() {
                    return bucket;
                }
            });
            var displays = targets.stream().map(entry -> new BasicDisplay(
                    List.of(EntryIngredient.of(entry)), List.of(EntryIngredient.of(entry))) {
                @Override
                public CategoryIdentifier<?> getCategoryIdentifier() {
                    return category;
                }
            }).toList();
            var registry = (DisplayRegistryImpl) DisplayRegistry.getInstance();
            registry.startReload();
            displays.forEach(registry::add);
            registry.displaysHolder().endReload();
            if (!displays.stream().allMatch(registry.displaysHolder().cache()::isCached)) {
                throw new AssertionError("REI union probe must exercise cached displays");
            }
            for (boolean recipes : new boolean[]{false, true}) {
                var found = ReiFluidRecipeLookup.createViewSearchBuilder(targets, recipes).mergingDisplays(false)
                        .buildMapInternal().values().stream().flatMap(List::stream).toList();
                if (found.size() != 2 || !found.containsAll(displays)) {
                    throw new AssertionError("REI must combine both cached fluid and bucket displays: " + found.size());
                }
            }
            System.out.println("AE2FCT REI cached fluid/bucket recipe AND usage union PASS");
        }
    }

    private static final class EmiProbe {
        private static void run(Minecraft minecraft) {
            ensureInitialized(EmiUseCraftingRecipeHandler.class);
            var id = Ae2fct.makeId("blacklist_render_probe");
            var inputs = NonNullList.of(Ingredient.EMPTY, Ingredient.of(Items.WATER_BUCKET));
            var output = new ItemStack(Items.CLAY_BALL);
            var recipe = new ShapelessRecipe(id, "", CraftingBookCategory.MISC, output, inputs);
            var display = new VirtualFluidBlacklistEmiRecipe(new VirtualFluidBlacklistRecipe(
                    id, id, recipe, inputs, output, 3, 3, true));
            var widgets = new WidgetGroup(display, 0, 0, display.getDisplayWidth(), display.getDisplayHeight());
            display.addWidgets(widgets);
            var graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
            for (var widget : widgets.widgets) {
                if (widget instanceof AnimatedTextureWidget) {
                    throw new AssertionError("The read-only blacklist must use a static arrow, not a timed animation");
                }
                widget.render(graphics, 0, 0, 0);
            }
            graphics.flush();
            System.out.println("AE2FCT blacklist EMI widget render PASS");
        }
    }

    private static void ensureInitialized(Class<?> targetClass) {
        try {
            MethodHandles.lookup().ensureInitialized(targetClass);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("AE2FCT client probe could not initialize " + targetClass.getName(), e);
        }
    }
}
