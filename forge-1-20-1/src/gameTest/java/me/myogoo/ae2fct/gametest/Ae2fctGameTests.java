package me.myogoo.ae2fct.gametest;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.implementations.menuobjects.IPortableTerminal;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.me.storage.NetworkStorage;
import appeng.api.storage.MEStorage;
import appeng.api.util.IConfigManager;
import appeng.core.definitions.AEItems;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.helpers.InventoryAction;
import appeng.menu.AutoCraftingMenu;
import appeng.menu.ISubMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.slot.CraftingTermSlot;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.parts.reporting.CraftingTerminalPart;
import appeng.util.ConfigManager;
import appeng.util.inv.AppEngInternalInventory;
import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import me.myogoo.ae2fct.integration.recipeviewer.VirtualFluidBlacklistRecipes;
import me.myogoo.ae2fct.item.VirtualFluidItem;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import me.myogoo.ae2fct.util.VirtualFluidStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.common.crafting.StrictNBTIngredient;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Ae2fctGameTests {
    private static final ResourceLocation BLACKLISTED_RECIPE_ID =
            Ae2fct.makeId("gametest/blacklisted_virtual_bucket");
    private static final ResourceLocation ALLOWED_TAGGED_RECIPE_ID =
            Ae2fct.makeId("gametest/allowed_tagged_bucket");
    private static final ResourceLocation BLACKLISTED_OVERLAP_RECIPE_ID =
            Ae2fct.makeId("gametest/aaa_blacklisted_overlap");
    private static final ResourceLocation ALLOWED_OVERLAP_RECIPE_ID =
            Ae2fct.makeId("gametest/zzz_allowed_overlap");

    private Ae2fctGameTests() {
    }

    public static void register(RegisterGameTestsEvent event) {
        event.register(Ae2fctGameTests.class);
    }

    @PrefixGameTestTemplate(false)
    @GameTest(templateNamespace = Ae2fct.MODID, template = "empty3x3x3")
    public static void networkVirtualBucketsUseWholeFluidAmounts(GameTestHelper helper) {

        var waterKey = AEFluidKey.of(Fluids.WATER);
        var virtualKey = AEItemKey.of(VirtualFluidItem.createItemStack(Fluids.WATER));
        FluidTransferRegression.run(helper);
        previewReservations(helper);
        var source = IActionSource.empty();
        for (long amount : new long[] { 999, 1000, 3999, 4000 }) {
            long[] stored = { amount };
            MEStorage tank = new MEStorage() {
                @Override
                public Component getDescription() {
                    return Component.literal("Whole-bucket test tank");
                }

                @Override
                public long extract(AEKey key, long requested, Actionable mode, IActionSource src) {
                    long moved = key.equals(waterKey) ? Math.min(stored[0], requested) : 0;
                    if (mode == Actionable.MODULATE) stored[0] -= moved;
                    return moved;
                }

                @Override
                public long insert(AEKey key, long requested, Actionable mode, IActionSource src) {
                    long moved = key.equals(waterKey) ? Math.min(4000 - stored[0], requested) : 0;
                    if (mode == Actionable.MODULATE) stored[0] += moved;
                    return moved;
                }
            };
            var network = PersistentNetworkFixture.network(helper);
            network.mount(0, tank);
            long buckets = amount / 1000;
            assertEquals(helper, buckets, network.extract(virtualKey, 4, Actionable.SIMULATE, source),
                    "ME simulated bucket extraction at " + amount);
            assertEquals(helper, amount, stored[0], "ME simulation must not consume fluid");
            assertEquals(helper, buckets, network.extract(virtualKey, 4, Actionable.MODULATE, source),
                    "ME actual bucket extraction at " + amount);
            assertEquals(helper, amount - buckets * 1000, stored[0], "ME extracts only whole bucket amounts");

            stored[0] = 4000 - amount;
            assertEquals(helper, buckets, network.insert(virtualKey, 4, Actionable.SIMULATE, source),
                    "ME simulated bucket insertion with room " + amount);
            assertEquals(helper, 4000 - amount, stored[0], "ME simulation must not insert fluid");
            assertEquals(helper, buckets, network.insert(virtualKey, 4, Actionable.MODULATE, source),
                    "ME actual bucket insertion with room " + amount);
            assertEquals(helper, 4000 - amount + buckets * 1000, stored[0], "ME inserts only whole bucket amounts");
        }
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(templateNamespace = Ae2fct.MODID, template = "empty3x3x3")
    public static void vanillaRecipesRequireExactlyOneVirtualBucket(GameTestHelper helper) {
        var level = helper.getLevel();
        var previousRecipes = List.copyOf(level.getRecipeManager().getRecipes());
        var ingredient = Ingredient.of(Items.WATER_BUCKET);
        var inputs = NonNullList.of(Ingredient.EMPTY, ingredient, Ingredient.of(Items.STICK));
        var id = Ae2fct.makeId("gametest/full_bucket_contract");
        List<CraftingRecipe> recipes = List.of(
                new ShapedRecipe(id, "", CraftingBookCategory.MISC, 2, 1, inputs, new ItemStack(Items.DIAMOND)),
                new ShapelessRecipe(id, "", CraftingBookCategory.MISC, new ItemStack(Items.DIAMOND), inputs));
        try {
            for (var recipe : recipes) {
                level.getRecipeManager().replaceRecipes(List.of(recipe));
                for (int amount : new int[] { 999, 1000, 1001 }) {
                    var virtual = VirtualFluidItem.createItemStack(Fluids.WATER);
                    VirtualFluidStorage.set(virtual, new VirtualFluid(new FluidStack(Fluids.WATER, amount), 1000));
                    boolean expected = amount == 1000;
                    assertEquals(helper, expected, ingredient.test(virtual), "bucket ingredient amount=" + amount);
                    var grid = craftingInput(virtual, new ItemStack(Items.STICK));
                    assertEquals(helper, expected, recipe.matches(grid, level), "direct recipe amount=" + amount);
                    assertEquals(helper, expected, findRecipe(level, virtual, new ItemStack(Items.STICK)) != null,
                            "recipe lookup amount=" + amount);
                    var player = FakePlayerFactory.getMinecraft(level);
                    var menu = new CraftingTermMenu(13, player.getInventory(), new TestPatternHost(player, level));
                    var slots = menu.getSlots(SlotSemantics.CRAFTING_GRID);
                    slots.get(0).set(virtual.copy());
                    slots.get(1).set(new ItemStack(Items.STICK));
                    var output = (CraftingTermSlot) menu.getSlots(SlotSemantics.CRAFTING_RESULT).get(0);
                    assertEquals(helper, expected, !output.getItem().isEmpty(), "terminal preview amount=" + amount);
                    output.doClick(InventoryAction.PICKUP_OR_SET_DOWN, player);
                    assertEquals(helper, expected, menu.getCarried().is(Items.DIAMOND), "terminal pickup amount=" + amount);
                    assertEquals(helper, expected, slots.get(0).getItem().isEmpty(), "fluid consumed once amount=" + amount);
                    assertEquals(helper, expected, slots.get(1).getItem().isEmpty(), "solid input consumption amount=" + amount);
                }
                assertTrue(helper, recipe.matches(craftingInput(new ItemStack(Items.WATER_BUCKET),
                        new ItemStack(Items.STICK)), level), "real bucket control");
            }
        } finally {
            level.getRecipeManager().replaceRecipes(previousRecipes);
        }
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(templateNamespace = Ae2fct.MODID, template = "empty3x3x3")
    public static void fluidCandidatesMustSatisfyTheIngredient(GameTestHelper helper) {
        var namedBucket = new ItemStack(Items.WATER_BUCKET);
        namedBucket.setHoverName(Component.literal("Required bucket"));
        var ingredient = StrictNBTIngredient.of(namedBucket);
        assertTrue(helper, ingredient.test(namedBucket), "strict NBT real bucket control");
        assertFalse(helper, ingredient.test(VirtualFluidItem.createItemStack(Fluids.WATER)),
                "virtual water cannot satisfy required bucket NBT");
        assertTrue(helper, FluidCraftingHelper.getFluidsFromIngredient(ingredient).isEmpty(),
                "fluid transfer and preview must not offer a substitute rejected by the ingredient");
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(templateNamespace = Ae2fct.MODID, template = "empty3x3x3")
    public static void offhandVirtualFluidReturnedOnlyOnce(GameTestHelper helper) {
        Player player = FakePlayerFactory.getMinecraft(helper.getLevel());
        var previousMenu = player.containerMenu;
        ItemStack previousHotbar = player.getInventory().getItem(0).copy();
        ItemStack previousOffhand = player.getOffhandItem().copy();
        try {
            var host = new TestPatternHost(player, helper.getLevel());
            var menu = new CraftingTermMenu(10, player.getInventory(), host);
            player.containerMenu = menu;
            player.getInventory().setItem(0, VirtualFluidItem.createItemStack(Fluids.WATER));
            player.getInventory().setItem(Inventory.SLOT_OFFHAND, ItemStack.EMPTY);
            var client = new OffhandSynchronizer();
            player.inventoryMenu.setSynchronizer(client);
            menu.clicked(menu.getSlots(SlotSemantics.PLAYER_HOTBAR).get(0).index,
                    Inventory.SLOT_OFFHAND, ClickType.SWAP, player);
            assertTrue(helper, FluidCraftingHelper.isVirtualFluidItem(player.getOffhandItem()),
                    "F swap should move virtual fluid into the offhand");
            client.offhand = player.getOffhandItem().copy(); // Client-side F-key prediction.

            menu.removed(player);
            assertTrue(helper, player.getOffhandItem().isEmpty(), "returned offhand fluid must be removed");
            assertEquals(helper, 1000L, host.storage.inserted, "menu close must return exactly one virtual fluid");
            player.inventoryMenu.transferState(menu);
            player.inventoryMenu.broadcastChanges();
            assertTrue(helper, client.offhand.isEmpty(), "menu close must clear the client-predicted offhand too");
            menu.removed(player);
            assertEquals(helper, 1000L, host.storage.inserted, "closing again must not return fluid twice");

            player.getInventory().setItem(Inventory.SLOT_OFFHAND, new ItemStack(Items.STICK, 3));
            menu.removed(player);
            assertEquals(helper, 3, player.getOffhandItem().getCount(), "ordinary offhand items must stay untouched");
            assertEquals(helper, 1000L, host.storage.inserted, "ordinary offhand items must not enter ME");

            host.storage.acceptInserts = false;
            player.getInventory().setItem(Inventory.SLOT_OFFHAND, VirtualFluidItem.createItemStack(Fluids.WATER));
            menu.removed(player);
            assertTrue(helper, FluidCraftingHelper.isVirtualFluidItem(player.getOffhandItem()),
                    "rejected fluid must remain in the offhand");
            assertEquals(helper, 1000L, host.storage.inserted, "rejected fluid must not enter ME");
        } finally {
            player.inventoryMenu.setSynchronizer(null);
            player.containerMenu = previousMenu;
            player.getInventory().setItem(0, previousHotbar);
            player.getInventory().setItem(Inventory.SLOT_OFFHAND, previousOffhand);
        }
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(templateNamespace = Ae2fct.MODID, template = "empty3x3x3")
    public static void blacklistRecipeViewerUsesLocalConfigRealRecipe(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<Recipe<?>> previousRecipes = List.copyOf(level.getRecipeManager().getRecipes());
        List<? extends String> previousBlacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();

        try {
            level.getRecipeManager().replaceRecipes(testRecipes());
            setVirtualFluidRecipeBlacklist(BLACKLISTED_RECIPE_ID);

            var recipes = VirtualFluidBlacklistRecipes.fromLocalConfig(level.getRecipeManager(), level.registryAccess());
            assertEquals(helper, 1, recipes.size(), "viewer category should include configured crafting recipe only");
            var recipe = recipes.get(0);
            assertEquals(helper, BLACKLISTED_RECIPE_ID, recipe.id(), "viewer recipe id");
            assertEquals(helper, Ae2fct.makeId("virtual_fluid_blacklist/ae2fct/gametest/blacklisted_virtual_bucket"),
                    recipe.viewerId(), "viewer synthetic recipe id");
            assertEquals(helper, new ItemStack(Items.DIAMOND).getItem(), recipe.output().getItem(),
                    "viewer should expose the original recipe output");
            assertTrue(helper, recipe.inputs().get(0).test(new ItemStack(Items.WATER_BUCKET)),
                    "viewer should expose the original real bucket ingredient");
            assertFalse(helper, FluidCraftingHelper.isVirtualFluidItem(recipe.inputs().get(0).getItems()[0]),
                    "viewer must not replace inputs with virtual fluid substitutes");
        } finally {
            level.getRecipeManager().replaceRecipes(previousRecipes);
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(previousBlacklist);
        }

        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(templateNamespace = Ae2fct.MODID, template = "empty3x3x3")
    public static void blacklistPolicyAppliesOnlyToVirtualFluid(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<Recipe<?>> previousRecipes = List.copyOf(level.getRecipeManager().getRecipes());
        List<? extends String> previousBlacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();

        try {
            level.getRecipeManager().replaceRecipes(testRecipes());
            setVirtualFluidRecipeBlacklist(BLACKLISTED_RECIPE_ID, BLACKLISTED_OVERLAP_RECIPE_ID);

            ItemStack virtualWater = VirtualFluidItem.createItemStack(Fluids.WATER);
            Ingredient waterBucket = Ingredient.of(Items.WATER_BUCKET);
            Ingredient waterBucketTag = Ingredient.of(ae2fct$waterBucketTag());

            assertTrue(helper, waterBucket.test(virtualWater),
                    "virtual water should keep default bucket ingredient behavior without recipe context");

            var previous = FluidCraftingTerminalIntegration.setRecipeContext(BLACKLISTED_RECIPE_ID);
            try {
                assertFalse(helper, waterBucket.test(virtualWater),
                        "blacklisted recipe should reject virtual water bucket ingredient");
                assertTrue(helper, waterBucket.test(new ItemStack(Items.WATER_BUCKET)),
                        "blacklisted recipe should still allow real filled bucket");
            } finally {
                FluidCraftingTerminalIntegration.restoreRecipeContext(previous);
            }

            previous = FluidCraftingTerminalIntegration.setRecipeContext(ALLOWED_TAGGED_RECIPE_ID);
            try {
                assertTrue(helper, waterBucketTag.test(virtualWater),
                        "unlisted tag ingredient should allow matching virtual fluid");
            } finally {
                FluidCraftingTerminalIntegration.restoreRecipeContext(previous);
            }

            assertRecipe(helper, null, findRecipe(level, virtualWater, new ItemStack(Items.STICK)),
                    "ordinary getRecipeFor should reject blacklisted virtual bucket");
            assertRecipe(helper, BLACKLISTED_RECIPE_ID,
                    findRecipe(level, new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.STICK)),
                    "ordinary getRecipeFor should allow real bucket for blacklisted recipe");
            assertRecipe(helper, ALLOWED_TAGGED_RECIPE_ID,
                    findRecipe(level, virtualWater, new ItemStack(Items.PAPER)),
                    "ordinary getRecipeFor should allow unlisted tag recipe");
            assertRecipe(helper, ALLOWED_OVERLAP_RECIPE_ID,
                    findRecipe(level, virtualWater, new ItemStack(Items.SUGAR)),
                    "ordinary getRecipeFor should skip blacklisted match and keep allowed alternative");
            assertEquals(helper, List.of(ALLOWED_OVERLAP_RECIPE_ID),
                    level.getRecipeManager().getRecipesFor(RecipeType.CRAFTING,
                            craftingInput(virtualWater, new ItemStack(Items.SUGAR)), level)
                            .stream().map(Recipe::getId).toList(),
                    "recipe selection list must retain only the allowed alternative");
        } finally {
            level.getRecipeManager().replaceRecipes(previousRecipes);
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(previousBlacklist);
            FluidCraftingTerminalIntegration.restoreRecipeContext(null);
        }

        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(templateNamespace = Ae2fct.MODID, template = "empty3x3x3")
    public static void patternEncodingRejectsBlacklistedVirtualFluid(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<Recipe<?>> previousRecipes = List.copyOf(level.getRecipeManager().getRecipes());
        List<? extends String> previousBlacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();

        try {
            level.getRecipeManager().replaceRecipes(testRecipes());
            setVirtualFluidRecipeBlacklist(BLACKLISTED_RECIPE_ID);

            assertPatternEncode(helper, level, VirtualFluidItem.createItemStack(Fluids.WATER),
                    new ItemStack(Items.STICK), false,
                    "manual pattern encoding should reject blacklisted virtual bucket");
            assertPatternEncode(helper, level, new ItemStack(Items.WATER_BUCKET),
                    new ItemStack(Items.STICK), true,
                    "manual pattern encoding should allow real bucket for blacklisted recipe");

            setVirtualFluidRecipeBlacklist();
            ItemStack encoded = encodePattern(helper, level, VirtualFluidItem.createItemStack(Fluids.WATER),
                    new ItemStack(Items.STICK));
            assertTrue(helper, PatternDetailsHelper.isEncodedPattern(encoded),
                    "virtual bucket pattern should encode before recipe is blacklisted");

            setVirtualFluidRecipeBlacklist(BLACKLISTED_RECIPE_ID);
            assertTrue(helper, PatternDetailsHelper.decodePattern(encoded, level) == null,
                    "cached virtual bucket pattern should stop decoding after recipe is blacklisted");
        } finally {
            level.getRecipeManager().replaceRecipes(previousRecipes);
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(previousBlacklist);
            FluidCraftingTerminalIntegration.restoreRecipeContext(null);
        }

        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(templateNamespace = Ae2fct.MODID, template = "empty3x3x3")
    public static void manualCraftingRejectsBlacklistedVirtualFluid(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<Recipe<?>> previousRecipes = List.copyOf(level.getRecipeManager().getRecipes());
        List<? extends String> previousBlacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();
        try {
            if (ModList.get().isLoaded("immersiveengineering")) {
                ResourceLocation acidId = new ResourceLocation("immersiveengineering", "crafting/redstone_acid");
                setVirtualFluidRecipeBlacklist(acidId);
                assertManualCrafting(helper, acidId, new ItemStack(Items.REDSTONE), new ItemStack(Items.REDSTONE),
                        new ItemStack(Items.REDSTONE), new ItemStack(Items.REDSTONE));
                System.out.println("AE2FCT verified actual IE redstone acid manual crafting and pickup");
            }
            level.getRecipeManager().replaceRecipes(testRecipes());
            setVirtualFluidRecipeBlacklist(BLACKLISTED_RECIPE_ID);
            assertManualCrafting(helper, BLACKLISTED_RECIPE_ID, new ItemStack(Items.STICK));
        } finally {
            level.getRecipeManager().replaceRecipes(previousRecipes);
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(previousBlacklist);
            FluidCraftingTerminalIntegration.restoreRecipeContext(null);
        }
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(templateNamespace = Ae2fct.MODID, template = "empty3x3x3")
    public static void cachedRecipeCannotBypassBlacklist(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<Recipe<?>> previousRecipes = List.copyOf(level.getRecipeManager().getRecipes());
        List<? extends String> previousBlacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();
        try {
            // A custom recipe can accept virtual fluid without ever calling Ingredient.test.
            CraftingRecipe recipe = new ShapelessRecipe(BLACKLISTED_RECIPE_ID, "", CraftingBookCategory.MISC,
                    new ItemStack(Items.DIAMOND), NonNullList.of(Ingredient.EMPTY,
                            Ingredient.of(Items.WATER_BUCKET), Ingredient.of(Items.STICK))) {
                @Override
                public boolean matches(CraftingContainer input, Level recipeLevel) {
                    return FluidCraftingHelper.isVirtualFluidItem(input.getItem(0))
                            && input.getItem(1).is(Items.STICK);
                }
            };
            level.getRecipeManager().replaceRecipes(List.of(recipe));
            setVirtualFluidRecipeBlacklist();
            Player player = FakePlayerFactory.getMinecraft(helper.getLevel());
            CraftingTermMenu menu = new CraftingTermMenu(2, player.getInventory(), new TestPatternHost(player, level));
            menu.getSlots(SlotSemantics.CRAFTING_GRID).get(0).set(VirtualFluidItem.createItemStack(Fluids.WATER));
            menu.getSlots(SlotSemantics.CRAFTING_GRID).get(1).set(new ItemStack(Items.STICK));
            CraftingTermSlot output = (CraftingTermSlot) menu.getSlots(SlotSemantics.CRAFTING_RESULT).get(0);
            assertFalse(helper, output.getItem().isEmpty(), "unlisted custom recipe must be craftable");

            setVirtualFluidRecipeBlacklist(BLACKLISTED_RECIPE_ID);
            output.doClick(InventoryAction.PICKUP_OR_SET_DOWN, player);
            assertTrue(helper, menu.getCarried().isEmpty(), "cached custom recipe must not bypass blacklist on pickup");
            assertTrue(helper, FluidCraftingHelper.isVirtualFluidItem(menu.getCraftingMatrix().getStackInSlot(0)),
                    "blocked cached crafting must not consume input");
        } finally {
            level.getRecipeManager().replaceRecipes(previousRecipes);
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(previousBlacklist);
            FluidCraftingTerminalIntegration.restoreRecipeContext(null);
        }
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(templateNamespace = Ae2fct.MODID, template = "empty3x3x3")
    public static void immersiveCreosoteCraftingConsumesVirtualBucket(GameTestHelper helper) {
        if (!ModList.get().isLoaded("immersiveengineering")) {
            helper.succeed();
            return;
        }

        var level = helper.getLevel();
        var id = new ResourceLocation("immersiveengineering", "crafting/treated_wood_horizontal");
        var recipe = (CraftingRecipe) level.getRecipeManager().byKey(id).orElseThrow();
        var creosote = BuiltInRegistries.FLUID.get(new ResourceLocation("immersiveengineering", "creosote"));
        var virtual = VirtualFluidItem.createItemStack(creosote);
        var realBucket = new ItemStack(creosote.getBucket());
        var grid = new TransientCraftingContainer(new AutoCraftingMenu(), 3, 3);
        for (int slot = 0; slot < 9; slot++) {
            grid.setItem(slot, new ItemStack(Items.OAK_PLANKS));
        }

        var previousBlacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();
        Player player = FakePlayerFactory.getMinecraft(level);
        var previousMenu = player.containerMenu;
        try {
            setVirtualFluidRecipeBlacklist();
            grid.setItem(4, realBucket);
            assertTrue(helper, recipe.matches(grid, level), "real creosote bucket control");
            grid.setItem(4, virtual);
            assertTrue(helper, recipe.matches(grid, level), "IE fluid ingredient must accept virtual creosote");
            assertEquals(helper, id, level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level)
                    .orElseThrow().getId(), "virtual creosote recipe lookup");
            assertTrue(helper, recipe.getRemainingItems(grid).stream().allMatch(ItemStack::isEmpty),
                    "virtual creosote must not create an empty bucket or retain fluid");
            assertFalse(helper, FluidUtil.getFluidHandler(virtual).isPresent(),
                    "virtual fluid must not become a drainable tank outside crafting");

            grid.setItem(4, VirtualFluidItem.createItemStack(Fluids.WATER));
            assertFalse(helper, recipe.matches(grid, level), "wrong fluid tag must not match");
            var insufficient = virtual.copy();
            VirtualFluidStorage.set(insufficient, new VirtualFluid(new FluidStack(creosote, 999), 1000));
            grid.setItem(4, insufficient);
            assertFalse(helper, recipe.matches(grid, level), "less than one bucket must not match");

            var menu = new CraftingTermMenu(12, player.getInventory(), new TestPatternHost(player, level));
            player.containerMenu = menu;
            var slots = menu.getSlots(SlotSemantics.CRAFTING_GRID);
            for (int slot = 0; slot < 9; slot++) {
                slots.get(slot).set(slot == 4 ? virtual.copy() : new ItemStack(Items.OAK_PLANKS));
            }
            var output = (CraftingTermSlot) menu.getSlots(SlotSemantics.CRAFTING_RESULT).get(0);
            assertEquals(helper, 8, output.getItem().getCount(), "ME terminal must preview eight treated planks");
            output.doClick(InventoryAction.PICKUP_OR_SET_DOWN, player);
            assertEquals(helper, 8, menu.getCarried().getCount(), "treated planks must be obtainable");
            assertTrue(helper, slots.stream().allMatch(slot -> slot.getItem().isEmpty()),
                    "crafting must consume all eight planks and exactly one virtual bucket");

            menu.setCarried(ItemStack.EMPTY);
            setVirtualFluidRecipeBlacklist(id);
            for (int slot = 0; slot < 9; slot++) {
                slots.get(slot).set(slot == 4 ? virtual.copy() : new ItemStack(Items.OAK_PLANKS));
            }
            assertTrue(helper, output.getItem().isEmpty(), "blacklisted IE recipe must have no virtual output");
            output.doClick(InventoryAction.PICKUP_OR_SET_DOWN, player);
            assertTrue(helper, menu.getCarried().isEmpty(), "blacklisted IE output must not be obtainable");
            assertTrue(helper, FluidCraftingHelper.isVirtualFluidItem(slots.get(4).getItem()),
                    "blocked crafting must preserve its virtual fluid");
            slots.get(4).set(realBucket.copy());
            output.doClick(InventoryAction.PICKUP_OR_SET_DOWN, player);
            assertEquals(helper, 8, menu.getCarried().getCount(), "blacklist must preserve original real-bucket crafting");
            assertTrue(helper, slots.get(4).getItem().is(Items.BUCKET), "only a real bucket should leave an empty bucket");
        } finally {
            player.containerMenu = previousMenu;
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(previousBlacklist);
            FluidCraftingTerminalIntegration.restoreRecipeContext(null);
        }
        helper.succeed();
    }

    private static void assertManualCrafting(GameTestHelper helper, ResourceLocation recipeId, ItemStack... inputs) {
        Player player = FakePlayerFactory.getMinecraft(helper.getLevel());
        TestPatternHost host = new TestPatternHost(player, helper.getLevel());
        CraftingTermMenu menu = new CraftingTermMenu(2, player.getInventory(), host) {
            @Override
            public void broadcastChanges() {
            }
        };
        var grid = menu.getSlots(SlotSemantics.CRAFTING_GRID);
        for (int slot = 0; slot < inputs.length; slot++) {
            grid.get(slot + 1).set(inputs[slot].copy());
        }
        grid.get(0).set(new ItemStack(Items.WATER_BUCKET));
        CraftingTermSlot output = (CraftingTermSlot) menu.getSlots(SlotSemantics.CRAFTING_RESULT).get(0);
        assertTrue(helper, menu.getCurrentRecipe() != null, "real bucket must have a recipe: " + recipeId);
        assertEquals(helper, recipeId, menu.getCurrentRecipe().getId(), "real bucket recipe id");
        assertFalse(helper, output.getItem().isEmpty(), "real bucket must produce output");

        grid.get(0).set(VirtualFluidItem.createItemStack(Fluids.WATER));
        assertTrue(helper, output.getItem().isEmpty(), "manual virtual bucket must not produce output: " + recipeId
                + ", selected=" + (menu.getCurrentRecipe() == null ? null : menu.getCurrentRecipe().getId()));
        output.doClick(InventoryAction.PICKUP_OR_SET_DOWN, player);
        assertTrue(helper, menu.getCarried().isEmpty(), "blacklisted virtual-fluid output must not be picked up");
        assertTrue(helper, FluidCraftingHelper.isVirtualFluidItem(grid.get(0).getItem()),
                "rejected crafting must not consume virtual fluid");

        grid.get(0).set(new ItemStack(Items.WATER_BUCKET));
        output.doClick(InventoryAction.PICKUP_OR_SET_DOWN, player);
        assertFalse(helper, menu.getCarried().isEmpty(), "original real-bucket recipe must remain craftable");
    }

    private static List<Recipe<?>> testRecipes() {
        return List.of(
                shapeless(BLACKLISTED_RECIPE_ID, new ItemStack(Items.DIAMOND),
                        Ingredient.of(Items.WATER_BUCKET), Ingredient.of(Items.STICK)),
                shapeless(ALLOWED_TAGGED_RECIPE_ID, new ItemStack(Items.EMERALD),
                        Ingredient.of(ae2fct$waterBucketTag()), Ingredient.of(Items.PAPER)),
                shapeless(BLACKLISTED_OVERLAP_RECIPE_ID, new ItemStack(Items.GOLD_INGOT),
                        Ingredient.of(Items.WATER_BUCKET), Ingredient.of(Items.SUGAR)),
                shapeless(ALLOWED_OVERLAP_RECIPE_ID, new ItemStack(Items.IRON_INGOT),
                        Ingredient.of(Items.WATER_BUCKET), Ingredient.of(Items.SUGAR))
        );
    }

    private static ShapelessRecipe shapeless(ResourceLocation id, ItemStack result, Ingredient... ingredients) {
        NonNullList<Ingredient> inputs = NonNullList.create();
        inputs.addAll(List.of(ingredients));
        return new ShapelessRecipe(id, "", CraftingBookCategory.MISC, result, inputs);
    }

    private static TagKey<Item> ae2fct$waterBucketTag() {
        return ItemTags.create(Ae2fct.makeId("gametest_water_buckets"));
    }

    private static CraftingRecipe findRecipe(Level level, ItemStack... inputs) {
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingInput(inputs), level).orElse(null);
    }

    private static CraftingContainer craftingInput(ItemStack... inputs) {
        CraftingContainer container = new TransientCraftingContainer(new AutoCraftingMenu(), 3, 3);
        for (int slot = 0; slot < inputs.length; slot++) {
            container.setItem(slot, inputs[slot].copy());
        }
        return container;
    }

    private static void setVirtualFluidRecipeBlacklist(ResourceLocation... recipeIds) {
        List<String> entries = new ArrayList<>();
        for (ResourceLocation recipeId : recipeIds) {
            entries.add(recipeId.toString());
        }
        FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(entries);
    }

    private static void assertPatternEncode(GameTestHelper helper, Level level, ItemStack firstInput,
            ItemStack secondInput, boolean expectedEncoded, String message) {
        assertTrue(helper, PatternDetailsHelper.isEncodedPattern(encodePattern(helper, level, firstInput, secondInput))
                == expectedEncoded, message);
    }

    private static ItemStack encodePattern(GameTestHelper helper, Level level, ItemStack firstInput,
            ItemStack secondInput) {
        PatternEncodingTermMenu menu = createPatternMenu(helper, level);
        menu.getCraftingGridSlots()[0].set(firstInput.copy());
        menu.getCraftingGridSlots()[1].set(secondInput.copy());
        menu.getSlots(SlotSemantics.BLANK_PATTERN).get(0).set(AEItems.BLANK_PATTERN.stack(1));
        menu.encode();
        return menu.getSlots(SlotSemantics.ENCODED_PATTERN).get(0).getItem().copy();
    }

    private static PatternEncodingTermMenu createPatternMenu(GameTestHelper helper, Level level) {
        Player player = FakePlayerFactory.getMinecraft((ServerLevel) level);
        return new PatternEncodingTermMenu(1, player.getInventory(), new TestPatternHost(player, level)) {
            @Override
            public void broadcastChanges() {
            }
        };
    }

    private static void assertRecipe(GameTestHelper helper, ResourceLocation expected, CraftingRecipe actual,
            String message) {
        if (expected == null) {
            assertTrue(helper, actual == null, message + ": expected no recipe, got " + recipeId(actual));
        } else {
            assertTrue(helper, actual != null, message + ": expected " + expected + ", got no recipe");
            assertEquals(helper, expected, actual.getId(), message);
        }
    }

    private static ResourceLocation recipeId(CraftingRecipe recipe) {
        return recipe == null ? null : recipe.getId();
    }

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) {
            helper.fail(message, BlockPos.ZERO);
        }
    }

    private static void assertFalse(GameTestHelper helper, boolean condition, String message) {
        assertTrue(helper, !condition, message);
    }

    private static void assertEquals(GameTestHelper helper, Object expected, Object actual, String message) {
        assertTrue(helper, Objects.equals(expected, actual),
                message + ": expected=" + expected + ", actual=" + actual);
    }

    private static final class TestPatternHost extends ItemMenuHost
            implements IPatternTerminalMenuHost, IPatternTerminalLogicHost, IPortableTerminal, ISegmentedInventory {
        private final CountingStorage storage = new CountingStorage();
        private final InternalInventory craftingGrid = new AppEngInternalInventory(9);
        private final IConfigManager configManager = new ConfigManager(() -> {});
        private final Level level;
        private final PatternEncodingLogic logic;

        private TestPatternHost(Player player, Level level) {
            super(player, null, new ItemStack(Items.STICK));
            this.level = level;
            this.logic = new PatternEncodingLogic(this);
        }

        @Override
        public PatternEncodingLogic getLogic() {
            return logic;
        }

        @Override
        public Level getLevel() {
            return level;
        }

        @Override
        public void markForSave() {
        }

        @Override
        public MEStorage getInventory() {
            return storage;
        }

        @Override
        public InternalInventory getSubInventory(ResourceLocation id) {
            return CraftingTerminalPart.INV_CRAFTING.equals(id) ? craftingGrid : null;
        }

        @Override
        public IConfigManager getConfigManager() {
            return configManager;
        }

        @Override
        public void returnToMainMenu(Player player, ISubMenu subMenu) {
        }

        @Override
        public ItemStack getMainMenuIcon() {
            return ItemStack.EMPTY;
        }

        @Override
        public double extractAEPower(double amount, Actionable mode, PowerMultiplier usePowerMultiplier) {
            return amount;
        }
    }

    private static final class OffhandSynchronizer implements ContainerSynchronizer {
        private ItemStack offhand = ItemStack.EMPTY;

        @Override
        public void sendInitialData(AbstractContainerMenu menu, NonNullList<ItemStack> items,
                ItemStack carried, int[] data) {
            offhand = items.get(InventoryMenu.SHIELD_SLOT).copy();
        }

        @Override
        public void sendSlotChange(AbstractContainerMenu menu, int slot, ItemStack stack) {
            if (slot == InventoryMenu.SHIELD_SLOT) {
                offhand = stack.copy();
            }
        }

        @Override
        public void sendCarriedChange(AbstractContainerMenu menu, ItemStack stack) {
        }

        @Override
        public void sendDataChange(AbstractContainerMenu menu, int slot, int value) {
        }
    }

    private static final class CountingStorage implements MEStorage {
        private long inserted;
        private boolean acceptInserts = true;

        @Override
        public Component getDescription() {
            return Component.literal("AE2FCT GameTest storage");
        }

        @Override
        public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
            if (!acceptInserts) {
                return 0;
            }
            if (mode == Actionable.MODULATE) {
                inserted += amount;
            }
            return amount;
        }
    }

    @PrefixGameTestTemplate(false)
    @GameTest(templateNamespace = Ae2fct.MODID, template = "empty3x3x3")
    public static void networkAmountBounds(GameTestHelper helper) {
        FluidTransferRegression.networkAmountBounds(helper);
    }

    @PrefixGameTestTemplate(false)
    @GameTest(templateNamespace = Ae2fct.MODID, template = "empty3x3x3")
    public static void networkPartialExtractionPreservesRemainder(GameTestHelper helper) {
        FluidTransferRegression.partialNetworkExtraction(helper);
    }

    @PrefixGameTestTemplate(false)
    @GameTest(templateNamespace = Ae2fct.MODID, template = "empty3x3x3")
    public static void networkPartialInsertionPreservesRemainder(GameTestHelper helper) {
        FluidTransferRegression.partialNetworkInsertion(helper);
    }

    @SuppressWarnings("deprecation")
    private static void previewReservations(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        var saved = new ArrayList<ItemStack>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            saved.add(player.getInventory().getItem(i).copy());
            player.getInventory().setItem(i, ItemStack.EMPTY);
        }
        boolean oldQol = FluidCraftingConfig.COMMON.QoL.get();
        var oldBlacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();
        try {
            FluidCraftingConfig.COMMON.QoL.set(true);
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(List.of());
            var menu = new CraftingTermMenu(14, player.getInventory(), new TestPatternHost(player, helper.getLevel()));
            var bucket = Ingredient.of(Items.WATER_BUCKET);
            assertTrue(helper, !FluidCraftingTerminalIntegration.hasFluidIngredient(bucket, previewRepo(1000, false)),
                    "legacy fluid query must retain its viewer context guard");
            for (long amount : new long[]{999, 1000, 2000}) {
                var repo = previewRepo(amount, false);
                boolean available = FluidCraftingTerminalIntegration.withRecipeViewerContext(menu, ALLOWED_TAGGED_RECIPE_ID,
                        () -> FluidCraftingTerminalIntegration.hasFluidIngredient(bucket, repo));
                assertTrue(helper, available == (amount >= 1000), "legacy fluid query must require one whole bucket");
            }
            assertTrue(helper, !FluidCraftingTerminalIntegration.withRecipeViewerContext(menu, ALLOWED_TAGGED_RECIPE_ID,
                            () -> FluidCraftingTerminalIntegration.hasFluidIngredient(bucket, null)),
                    "legacy fluid query must reject a missing client repo");
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(List.of(ALLOWED_TAGGED_RECIPE_ID.toString()));
            assertTrue(helper, !FluidCraftingTerminalIntegration.withRecipeViewerContext(menu, ALLOWED_TAGGED_RECIPE_ID,
                            () -> FluidCraftingTerminalIntegration.hasFluidIngredient(bucket, previewRepo(1000, false))),
                    "legacy fluid query must retain blacklist rejection");
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(List.of());
            assertTrue(helper, FluidCraftingTerminalIntegration.withRecipeViewerContext(menu, ALLOWED_TAGGED_RECIPE_ID,
                            () -> FluidCraftingHelper.hasAvailableFluidInClientRepo(bucket, previewRepo(Long.MAX_VALUE, false), Long.MAX_VALUE)),
                    "legacy long-amount availability query must not narrow to an integer reservation");
            var ingredients = new java.util.LinkedHashMap<Integer, Ingredient>();
            ingredients.put(0, Ingredient.of(Items.WATER_BUCKET, Items.LAVA_BUCKET));
            ingredients.put(1, Ingredient.of(Items.WATER_BUCKET));
            for (long amount : new long[]{1000, 2000}) {
                menu.setClientRepo(previewRepo(amount, false));
                var result = FluidCraftingTerminalIntegration.withRecipeViewerContext(menu, ALLOWED_TAGGED_RECIPE_ID,
                        () -> menu.findMissingIngredients(ingredients));
                assertEquals(helper, amount == 1000 ? 1 : 0, result.missingSlots().size(),
                        "different ingredients sharing water must reserve it only once");
                assertTrue(helper, result.craftableSlots().isEmpty(), "stored-only fluid is not craftable");
                var again = FluidCraftingTerminalIntegration.withRecipeViewerContext(menu, ALLOWED_TAGGED_RECIPE_ID,
                        () -> menu.findMissingIngredients(ingredients));
                assertEquals(helper, result.missingSlots().size(), again.missingSlots().size(),
                        "reservations must reset for each preview");
            }
            menu.setClientRepo(previewRepo(1000, true));
            var craftable = FluidCraftingTerminalIntegration.withRecipeViewerContext(menu, ALLOWED_TAGGED_RECIPE_ID,
                    () -> menu.findMissingIngredients(ingredients));
            assertTrue(helper, craftable.missingSlots().isEmpty() && craftable.craftableSlots().size() == 1,
                    "the exhausted second slot must remain craftable rather than available");
            menu.setClientRepo(previewRepo(1000, false));
            player.getInventory().setItem(0, new ItemStack(Items.WATER_BUCKET));
            var mixed = FluidCraftingTerminalIntegration.withRecipeViewerContext(menu, ALLOWED_TAGGED_RECIPE_ID,
                    () -> menu.findMissingIngredients(ingredients));
            assertTrue(helper, !mixed.anyMissingOrCraftable(), "a real bucket must not reserve network fluid");
        } finally {
            FluidCraftingConfig.COMMON.QoL.set(oldQol);
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(oldBlacklist);
            for (int i = 0; i < saved.size(); i++) player.getInventory().setItem(i, saved.get(i));
        }
    }

    private static appeng.menu.me.common.IClientRepo previewRepo(long amount, boolean craftable) {
        return new appeng.menu.me.common.IClientRepo() {
            @Override
            public void handleUpdate(boolean fullUpdate, List<appeng.menu.me.common.GridInventoryEntry> entries) {
            }

            @Override
            public java.util.Set<appeng.menu.me.common.GridInventoryEntry> getAllEntries() {
                return java.util.Set.of(new appeng.menu.me.common.GridInventoryEntry(1, AEFluidKey.of(Fluids.WATER),
                        amount, 0, craftable));
            }

            @Override
            public java.util.Collection<appeng.menu.me.common.GridInventoryEntry> getByIngredient(Ingredient ingredient) {
                return List.of();
            }
        };
    }
    @PrefixGameTestTemplate(false)
    @GameTest(templateNamespace = Ae2fct.MODID, template = "empty3x3x3")
    public static void persistentLifecycle(GameTestHelper helper) {
        FluidTransferRegression.persistentLifecycle(helper);
    }

    @PrefixGameTestTemplate(false)
    @GameTest(templateNamespace = Ae2fct.MODID, template = "empty3x3x3")
    public static void topologyAndReentrancy(GameTestHelper helper) {
        FluidTransferRegression.topologyAndReentrancy(helper);
    }

    @PrefixGameTestTemplate(false)
    @GameTest(templateNamespace = Ae2fct.MODID, template = "empty3x3x3")
    public static void partLifecycle(GameTestHelper helper) {
        FluidTransferRegression.partLifecycle(helper);
    }

}
