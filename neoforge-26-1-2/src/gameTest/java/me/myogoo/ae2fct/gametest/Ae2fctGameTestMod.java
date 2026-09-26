package me.myogoo.ae2fct.gametest;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.core.definitions.AEItems;
import appeng.crafting.RecipeAccess;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.ILinkStatus;
import appeng.api.storage.MEStorage;
import appeng.api.util.IConfigManager;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.helpers.InventoryAction;
import appeng.menu.ISubMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.me.common.IClientRepo;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.CraftingTermSlot;
import appeng.parts.reporting.CraftingTerminalPart;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.inv.AppEngInternalInventory;
import dev.shadowsoffire.fastsuite.CachedRecipeList;
import me.myogoo.ae2fct.api.IMEStorageMenu;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.integration.BlacklistRecipe;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.HashedPatchMap;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.HashOps;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RemoteSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.item.VirtualFluidItem;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import me.myogoo.ae2fct.util.VirtualFluidRecipePolicy;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

@Mod("ae2fct_gametest")
public final class Ae2fctGameTestMod {
    private static final Identifier OFFHAND_TEST_ID = Ae2fct.makeId("virtual_fluid_offhand_refund_regression");
    private static final Identifier TEST_ID = Ae2fct.makeId("virtual_fluid_ingredient_regression");
    private static final Identifier REFUND_TEST_ID = Ae2fct.makeId("virtual_fluid_pattern_ghost_refund_regression");
    private static final Identifier BLACKLIST_TEST_ID = Ae2fct.makeId("virtual_fluid_recipe_blacklist_regression");
    private static final Identifier FASTSUITE_TEST_ID = Ae2fct.makeId("virtual_fluid_fastsuite_cache_regression");
    private static final Identifier STRUCTURE = Ae2fct.makeId("empty");
    private static final Identifier BLACKLISTED_RECIPE_ID = Ae2fct.makeId("gametest/blacklisted_virtual_bucket");
    private static final Identifier ALLOWED_TAGGED_RECIPE_ID = Ae2fct.makeId("gametest/allowed_tagged_bucket");
    private static final Identifier BLACKLISTED_OVERLAP_RECIPE_ID = Ae2fct.makeId("gametest/aaa_blacklisted_overlap");
    private static final Identifier ALLOWED_OVERLAP_RECIPE_ID = Ae2fct.makeId("gametest/zzz_allowed_overlap");
    private static final ResourceKey<Recipe<?>> BLACKLISTED_RECIPE =
            ResourceKey.create(Registries.RECIPE, BLACKLISTED_RECIPE_ID);
    private static final ResourceKey<Recipe<?>> ALLOWED_TAGGED_RECIPE =
            ResourceKey.create(Registries.RECIPE, ALLOWED_TAGGED_RECIPE_ID);
    private static final ResourceKey<Recipe<?>> BLACKLISTED_OVERLAP_RECIPE =
            ResourceKey.create(Registries.RECIPE, BLACKLISTED_OVERLAP_RECIPE_ID);
    private static final ResourceKey<Recipe<?>> ALLOWED_OVERLAP_RECIPE =
            ResourceKey.create(Registries.RECIPE, ALLOWED_OVERLAP_RECIPE_ID);
    private static final Holder<TestEnvironmentDefinition<?>> ENVIRONMENT =
            Holder.direct(new TestEnvironmentDefinition.AllOf());
    private static final ResourceKey<Consumer<GameTestHelper>> TEST_FUNCTION =
            ResourceKey.create(Registries.TEST_FUNCTION, TEST_ID);
    private static final ResourceKey<Consumer<GameTestHelper>> OFFHAND_TEST_FUNCTION =
            ResourceKey.create(Registries.TEST_FUNCTION, OFFHAND_TEST_ID);
    private static final ResourceKey<Consumer<GameTestHelper>> REFUND_TEST_FUNCTION =
            ResourceKey.create(Registries.TEST_FUNCTION, REFUND_TEST_ID);
    private static final ResourceKey<Consumer<GameTestHelper>> BLACKLIST_TEST_FUNCTION =
            ResourceKey.create(Registries.TEST_FUNCTION, BLACKLIST_TEST_ID);
    private static final ResourceKey<Consumer<GameTestHelper>> FASTSUITE_TEST_FUNCTION =
            ResourceKey.create(Registries.TEST_FUNCTION, FASTSUITE_TEST_ID);

    public Ae2fctGameTestMod(IEventBus modEventBus) {
        modEventBus.addListener(Ae2fctGameTestMod::registerTestFunction);
        modEventBus.addListener(Ae2fctGameTestMod::registerGameTests);
    }

    private static void registerTestFunction(RegisterEvent event) {
        if (event.getRegistryKey() == Registries.TEST_FUNCTION) {
            event.register(Registries.TEST_FUNCTION, OFFHAND_TEST_ID,
                    () -> Ae2fctGameTestMod::virtualFluidOffhandRefundRegression);
            event.register(Registries.TEST_FUNCTION, TEST_ID,
                    () -> Ae2fctGameTestMod::virtualFluidIngredientRegression);
            event.register(Registries.TEST_FUNCTION, REFUND_TEST_ID,
                    () -> Ae2fctGameTestMod::virtualFluidPatternGhostRefundRegression);
            event.register(Registries.TEST_FUNCTION, BLACKLIST_TEST_ID,
                    () -> Ae2fctGameTestMod::virtualFluidRecipeBlacklistRegression);
            event.register(Registries.TEST_FUNCTION, FASTSUITE_TEST_ID,
                    () -> Ae2fctGameTestMod::virtualFluidFastSuiteCacheRegression);
        }
    }

    private static void registerGameTests(RegisterGameTestsEvent event) {
        event.registerTest(OFFHAND_TEST_ID, new FunctionGameTestInstance(OFFHAND_TEST_FUNCTION, new TestData<>(
                ENVIRONMENT,
                STRUCTURE,
                20,
                0,
                true,
                Rotation.NONE,
                false,
                1,
                1,
                false,
                0)));
        event.registerTest(TEST_ID, new FunctionGameTestInstance(TEST_FUNCTION, new TestData<>(
                ENVIRONMENT,
                STRUCTURE,
                20,
                0,
                true,
                Rotation.NONE,
                false,
                1,
                1,
                false,
                0)));
        event.registerTest(REFUND_TEST_ID, new FunctionGameTestInstance(REFUND_TEST_FUNCTION, new TestData<>(
                ENVIRONMENT,
                STRUCTURE,
                20,
                0,
                true,
                Rotation.NONE,
                false,
                1,
                1,
                false,
                0)));
        event.registerTest(BLACKLIST_TEST_ID, new FunctionGameTestInstance(BLACKLIST_TEST_FUNCTION, new TestData<>(
                ENVIRONMENT,
                STRUCTURE,
                20,
                0,
                true,
                Rotation.NONE,
                false,
                1,
                1,
                false,
                0)));
        event.registerTest(FASTSUITE_TEST_ID, new FunctionGameTestInstance(FASTSUITE_TEST_FUNCTION, new TestData<>(
                ENVIRONMENT,
                STRUCTURE,
                20,
                0,
                true,
                Rotation.NONE,
                false,
                1,
                1,
                false,
                0)));
    }

    private static void virtualFluidOffhandRefundRegression(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        var previousMenu = player.containerMenu;
        ItemStack previousHotbar = player.getInventory().getItem(0).copy();
        ItemStack previousOffhand = player.getOffhandItem().copy();
        try {
            var host = new TestPatternHost(player, helper.getLevel());
            var menu = new CraftingTermMenu(10, player.getInventory(), host);
            AEItemKey waterKey = AEItemKey.of(VirtualFluidItem.createItemStack(Fluids.WATER));
            player.containerMenu = menu;
            player.getInventory().setItem(0, VirtualFluidItem.createItemStack(Fluids.WATER));
            player.getInventory().setItem(Inventory.SLOT_OFFHAND, ItemStack.EMPTY);
            var client = new OffhandSynchronizer(player);
            player.inventoryMenu.setSynchronizer(client);

            menu.clicked(menu.getSlots(SlotSemantics.PLAYER_HOTBAR).getFirst().index,
                    Inventory.SLOT_OFFHAND, ContainerInput.SWAP, player);
            assertTrue(helper, FluidCraftingHelper.isVirtualFluidItem(player.getOffhandItem()),
                    "F swap should move virtual fluid into the offhand");
            client.offhand = player.getOffhandItem().copy();

            menu.removed(player);
            assertTrue(helper, player.getOffhandItem().isEmpty(), "returned offhand fluid must be removed");
            assertEquals(helper, 1L, host.storage.stored(waterKey), "menu close must return exactly one virtual fluid");
            player.inventoryMenu.transferState(menu);
            player.inventoryMenu.broadcastChanges();
            assertTrue(helper, client.offhand.isEmpty(), "menu close must clear the client-predicted offhand too");
            menu.removed(player);
            assertEquals(helper, 1L, host.storage.stored(waterKey), "closing again must not return fluid twice");

            player.getInventory().setItem(Inventory.SLOT_OFFHAND, new ItemStack(Items.STICK, 3));
            menu.removed(player);
            assertEquals(helper, 3, player.getOffhandItem().getCount(), "ordinary offhand items must stay untouched");
            assertEquals(helper, 1L, host.storage.stored(waterKey), "ordinary offhand items must not enter ME");

            host.storage.acceptInserts = false;
            player.getInventory().setItem(Inventory.SLOT_OFFHAND, VirtualFluidItem.createItemStack(Fluids.WATER));
            menu.removed(player);
            assertTrue(helper, FluidCraftingHelper.isVirtualFluidItem(player.getOffhandItem()),
                    "rejected fluid must remain in the offhand");
            assertEquals(helper, 1L, host.storage.stored(waterKey), "rejected fluid must not enter ME");
        } finally {
            player.initInventoryMenu();
            player.containerMenu = previousMenu;
            player.getInventory().setItem(0, previousHotbar);
            player.getInventory().setItem(Inventory.SLOT_OFFHAND, previousOffhand);
        }
        helper.succeed();
    }

    private static void virtualFluidIngredientRegression(GameTestHelper helper) {
        FluidCraftingHelper.clearFluidCraftingEnabled();

        Ingredient waterBucket = Ingredient.of(Items.WATER_BUCKET);
        KeyCounter storage = new KeyCounter();
        storage.add(AEFluidKey.of(Fluids.WATER), FluidType.BUCKET_VOLUME);

        try {
            var fluids = FluidCraftingHelper.getFluidsFromIngredient(waterBucket, helper.getLevel());
            assertEquals(helper, 1, fluids.size(), "water bucket ingredient fluid count");
            assertTrue(helper, fluids.contains(Fluids.WATER), "water bucket ingredient resolves water");

            ItemStack waterVirtual = VirtualFluidItem.createItemStack(Fluids.WATER);
            ItemStack lavaVirtual = VirtualFluidItem.createItemStack(Fluids.LAVA);
            var waterComponent = waterVirtual.get(AE2FCTDataComponent.VIRTUAL_FLUID);
            var sameWater = new VirtualFluid(new FluidStack(Fluids.WATER, 1000), 1000);
            assertEquals(helper, waterComponent, sameWater, "independent water components are equal");
            assertEquals(helper, waterComponent.hashCode(), sameWater.hashCode(), "equal component hashes");
            assertTrue(helper, !waterComponent.equals(lavaVirtual.get(AE2FCTDataComponent.VIRTUAL_FLUID)),
                    "different fluids are not equal");
            assertTrue(helper, !waterComponent.equals(new VirtualFluid(new FluidStack(Fluids.WATER, 500), 1000)),
                    "different fluid amounts are not equal");
            assertTrue(helper, !waterComponent.equals(new VirtualFluid(new FluidStack(Fluids.WATER, 1000), 2000)),
                    "different capacities are not equal");
            assertTrue(helper, waterBucket.test(waterVirtual), "water virtual item matches water bucket ingredient");
            ItemStack undersizedWater = waterVirtual.copy();
            undersizedWater.set(AE2FCTDataComponent.VIRTUAL_FLUID,
                    new VirtualFluid(new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME - 1), FluidType.BUCKET_VOLUME));
            assertTrue(helper, !waterBucket.test(undersizedWater),
                    "a 999 mB virtual fluid does not match a full-bucket ingredient");
            assertTrue(helper, !waterBucket.test(lavaVirtual),
                    "lava virtual item does not match water bucket ingredient");

            var contained = FluidUtil.getFirstStackContained(waterVirtual);
            assertEquals(helper, Fluids.WATER, contained.getFluid(), "virtual fluid item contains water");
            assertEquals(helper, FluidType.BUCKET_VOLUME, contained.getAmount(),
                    "virtual fluid item contains one bucket");

            var handler = VirtualFluidItem.createFluidHandler(waterVirtual);
            var water = FluidResource.of(Fluids.WATER);
            assertEquals(helper, 0, handler.extract(0, water, FluidType.BUCKET_VOLUME, null),
                    "virtual fluid handler is read-only");
            assertEquals(helper, FluidType.BUCKET_VOLUME, handler.getAmountAsInt(0),
                    "read-only extract does not drain virtual fluid");

            assertTrue(helper, FluidCraftingHelper.findBestMatchingFluidItemKeys(waterBucket, null, storage).isEmpty(),
                    "fluid candidates require helper context");

            FluidCraftingHelper.setFluidCraftingEnabled(true, null, helper.getLevel());
            var keys = FluidCraftingHelper.findBestMatchingFluidItemKeys(waterBucket, null, storage);
            assertEquals(helper, 1, keys.size(), "water fluid candidate count");
            assertEquals(helper, AEItemKey.of(waterVirtual), keys.getFirst(), "water candidate key");
        } finally {
            FluidCraftingHelper.clearFluidCraftingEnabled();
        }

        assertTrue(helper, !FluidCraftingHelper.isFluidCraftingEnabled(), "fluid crafting context cleared");
        assertTrue(helper, FluidCraftingHelper.findBestMatchingFluidItemKeys(waterBucket, null, storage).isEmpty(),
                "fluid crafting level ThreadLocal cleared");
        helper.succeed();
    }

    private static void virtualFluidRecipeBlacklistRegression(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<? extends String> previousBlacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();

        try {
            setVirtualFluidRecipeBlacklist(BLACKLISTED_RECIPE_ID, BLACKLISTED_OVERLAP_RECIPE_ID);

            var viewerRecipes = BlacklistRecipe.load(level);
            assertEquals(helper, 2, viewerRecipes.size(), "viewer only lists configured, present crafting recipes");
            var bucketRecipe = viewerRecipes.stream().filter(recipe -> recipe.id().equals(BLACKLISTED_RECIPE_ID))
                    .findFirst().orElseThrow();
            assertTrue(helper, bucketRecipe.inputs().stream().flatMap(List::stream)
                    .anyMatch(stack -> stack.is(Items.WATER_BUCKET)), "blacklist viewer keeps the real water bucket");
            assertTrue(helper, bucketRecipe.inputs().stream().flatMap(List::stream)
                    .noneMatch(FluidCraftingHelper::isVirtualFluidItem), "blacklist viewer does not substitute virtual fluids");
            assertTrue(helper, !bucketRecipe.outputs().isEmpty(), "blacklist viewer keeps the original output");
            var narrowRecipe = new BlacklistRecipe(BLACKLISTED_RECIPE_ID, 0,
                    List.of(List.of(new ItemStack(Items.STICK)), List.of(new ItemStack(Items.DIAMOND))),
                    List.of(new ItemStack(Items.OBSIDIAN)), 1, false);
            assertTrue(helper, narrowRecipe.inputAt(1).isEmpty() && narrowRecipe.inputAt(3).getFirst().is(Items.DIAMOND),
                    "narrow shaped recipes preserve empty columns in the 3x3 viewer grid");

            ItemStack virtualWater = VirtualFluidItem.createItemStack(Fluids.WATER);
            FluidCraftingHelper.setFluidCraftingEnabled(true, BLACKLISTED_RECIPE, level);
            assertTrue(helper, !FluidCraftingHelper.isFluidCraftingEnabled(),
                    "blacklisted recipe disables automatic virtual-fluid candidates");
            assertTrue(helper, !Ingredient.of(Items.WATER_BUCKET).test(virtualWater),
                    "blacklisted transfer cannot reuse an existing virtual fluid item");
            assertTrue(helper, Ingredient.of(Items.WATER_BUCKET).test(new ItemStack(Items.WATER_BUCKET)),
                    "blacklisted transfer still accepts real buckets");
            FluidCraftingHelper.clearFluidCraftingEnabled();
            assertTransferPreviewFluidRepoPolicy(helper, level, Ingredient.of(Items.WATER_BUCKET));
            assertEquals(helper, null, findCraftingRecipe(level, virtualWater, new ItemStack(Items.STICK)),
                    "listed recipe with virtual bucket fails via recipe manager");
            assertRecipe(helper, BLACKLISTED_RECIPE,
                    findCraftingRecipe(level, new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.STICK)),
                    "listed recipe with real bucket still succeeds");
            assertRecipe(helper, ALLOWED_TAGGED_RECIPE,
                    findCraftingRecipe(level, virtualWater, new ItemStack(Items.PAPER)),
                    "unlisted tag ingredient recipe accepts virtual bucket");
            assertRecipe(helper, ALLOWED_OVERLAP_RECIPE,
                    findCraftingRecipe(level, virtualWater, new ItemStack(Items.SUGAR)),
                    "overlapping blacklisted recipe is skipped in favor of allowed alternative");

            CraftingInput listedVirtualInput = craftingInput(virtualWater, new ItemStack(Items.STICK));
            RecipeHolder<CraftingRecipe> blacklisted = RecipeAccess.byKey(level, RecipeType.CRAFTING, BLACKLISTED_RECIPE);
            assertTrue(helper, blacklisted != null, "blacklisted fixture recipe is loaded");
            assertTrue(helper, level.recipeAccess().getRecipeFor(RecipeType.CRAFTING,
                    listedVirtualInput, level, blacklisted).isEmpty(),
                    "vanilla recipe manager cached holder rejects virtual input");
            assertEquals(helper, null, RecipeAccess.getRecipeFor(level, RecipeType.CRAFTING,
                    listedVirtualInput, BLACKLISTED_RECIPE),
                    "hinted recipe key rejects blacklisted virtual bucket");
            assertEquals(helper, null, RecipeAccess.getRecipeFor(level, RecipeType.CRAFTING,
                    listedVirtualInput, blacklisted),
                    "cached recipe holder rejects blacklisted virtual bucket");
            assertRecipe(helper, BLACKLISTED_RECIPE, RecipeAccess.getRecipeFor(level, RecipeType.CRAFTING,
                    craftingInput(new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.STICK)), blacklisted),
                    "cached recipe holder still allows real bucket");
            assertCraftingTermSlotRejectsCachedBlacklistedRecipe(helper, level);
            assertFastSuiteCachedRecipeList(helper, level, listedVirtualInput,
                    craftingInput(new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.STICK)),
                    craftingInput(virtualWater, new ItemStack(Items.PAPER)),
                    craftingInput(virtualWater, new ItemStack(Items.SUGAR)));

            assertPatternEncode(helper, level, VirtualFluidItem.createItemStack(Fluids.WATER), new ItemStack(Items.STICK),
                    false, "manual pattern ghost virtual bucket cannot encode blacklisted recipe");
            assertPatternEncode(helper, level, new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.STICK),
                    true, "manual pattern real bucket can encode blacklisted recipe");

            setVirtualFluidRecipeBlacklist();
            PatternEncodingTermMenu cachedMenu = createPatternMenu(helper, level);
            setCraftingGrid(cachedMenu, VirtualFluidItem.createItemStack(Fluids.WATER), new ItemStack(Items.STICK));
            cachedMenu.getSlots(SlotSemantics.BLANK_PATTERN).getFirst().set(AEItems.BLANK_PATTERN.stack(1));
            cachedMenu.encode();
            var cachedEncodedSlot = cachedMenu.getSlots(SlotSemantics.ENCODED_PATTERN).getFirst();
            assertTrue(helper, PatternDetailsHelper.isEncodedPattern(cachedEncodedSlot.getItem()),
                    "virtual bucket encodes before recipe is blacklisted");
            ItemStack encodedBeforeBlacklist = cachedEncodedSlot.getItem().copy();
            var decodedBeforeBlacklist = PatternDetailsHelper.decodePattern(encodedBeforeBlacklist, level);
            assertTrue(helper, decodedBeforeBlacklist instanceof AECraftingPattern,
                    "encoded virtual bucket pattern decodes before recipe is blacklisted");

            setVirtualFluidRecipeBlacklist(BLACKLISTED_RECIPE_ID);
            assertTrue(helper, ((AECraftingPattern) decodedBeforeBlacklist)
                    .assemble(craftingInput(virtualWater, new ItemStack(Items.STICK)), level).isEmpty(),
                    "decoded cached virtual pattern assemble rejects after blacklist update");
            assertEquals(helper, null, PatternDetailsHelper.decodePattern(encodedBeforeBlacklist, level),
                    "existing blacklisted virtual pattern no longer decodes");
            cachedMenu.encode();
            assertTrue(helper, !PatternDetailsHelper.isEncodedPattern(cachedEncodedSlot.getItem()),
                    "cached pattern recipe is blocked after blacklist update");
        } finally {
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(previousBlacklist);
            FluidCraftingHelper.clearFluidCraftingEnabled();
        }

        helper.succeed();
    }

    private static void virtualFluidFastSuiteCacheRegression(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<? extends String> previousBlacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();

        try {
            setVirtualFluidRecipeBlacklist(BLACKLISTED_RECIPE_ID, BLACKLISTED_OVERLAP_RECIPE_ID);
            ItemStack virtualWater = VirtualFluidItem.createItemStack(Fluids.WATER);
            assertFastSuiteCachedRecipeList(helper, level,
                    craftingInput(virtualWater, new ItemStack(Items.STICK)),
                    craftingInput(new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.STICK)),
                    craftingInput(virtualWater, new ItemStack(Items.PAPER)),
                    craftingInput(virtualWater, new ItemStack(Items.SUGAR)));
        } finally {
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(previousBlacklist);
        }

        helper.succeed();
    }

    private static void virtualFluidPatternGhostRefundRegression(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        var host = new TestPatternHost(player, helper.getLevel());
        var menu = new PatternEncodingTermMenu(2, player.getInventory(), host);
        assertTrue(helper, menu instanceof IMEStorageMenu, "MEStorageMenu mixin bridge must be applied");

        ItemStack ghostWater = VirtualFluidItem.createItemStack(Fluids.WATER);
        ItemStack carriedWater = VirtualFluidItem.createItemStack(Fluids.WATER);
        ItemStack inventoryWater = VirtualFluidItem.createItemStack(Fluids.WATER);
        ItemStack realSlotWater = VirtualFluidItem.createItemStack(Fluids.WATER);
        AEItemKey waterKey = AEItemKey.of(ghostWater);

        var fakeSlot = menu.getCraftingGridSlots()[0];
        var realSlot = menu.getSlots(SlotSemantics.BLANK_PATTERN).getFirst();
        fakeSlot.set(ghostWater);
        menu.setCarried(carriedWater);
        player.getInventory().setItem(0, inventoryWater);
        realSlot.set(realSlotWater);
        var previousMenu = player.containerMenu;
        player.containerMenu = menu;
        try {
            menu.removed(player);
        } finally {
            player.containerMenu = previousMenu;
        }

        assertTrue(helper, FluidCraftingHelper.isVirtualFluidItem(fakeSlot.getItem()),
                "pattern ghost virtual fluid must remain encoded");
        assertEquals(helper, 1, fakeSlot.getItem().getCount(), "pattern ghost virtual fluid count");
        assertTrue(helper, menu.getCarried().isEmpty(), "carried virtual fluid must be returned");
        assertTrue(helper, player.getInventory().getItem(0).isEmpty(), "player inventory virtual fluid must be returned");
        assertTrue(helper, realSlot.getItem().isEmpty(), "non-Fake menu slot virtual fluid must be returned");
        assertEquals(helper, 3L, host.storage.stored(waterKey),
                "only carried/player/non-Fake virtual fluids should be returned to storage");
        assertEquals(helper, 4L, host.storage.stored(waterKey) + fakeSlot.getItem().getCount(),
                "virtual fluid total must be preserved without duplicating pattern ghosts");
        helper.succeed();
    }

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        helper.assertTrue(condition, message);
    }

    private static void assertEquals(GameTestHelper helper, int expected, int actual, String message) {
        helper.assertTrue(expected == actual, message + ": expected=" + expected + ", actual=" + actual);
    }

    private static void assertEquals(GameTestHelper helper, long expected, long actual, String message) {
        helper.assertTrue(expected == actual, message + ": expected=" + expected + ", actual=" + actual);
    }

    private static void assertEquals(GameTestHelper helper, Object expected, Object actual, String message) {
        helper.assertTrue(Objects.equals(expected, actual), message + ": expected=" + expected + ", actual=" + actual);
    }

    private static CraftingInput craftingInput(ItemStack... stacks) {
        NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < stacks.length; i++) {
            items.set(i, stacks[i].copy());
        }
        return CraftingInput.of(3, 3, items);
    }

    private static RecipeHolder<CraftingRecipe> findCraftingRecipe(ServerLevel level, ItemStack... stacks) {
        return level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, craftingInput(stacks), level).orElse(null);
    }

    private static void assertCraftingTermSlotRejectsCachedBlacklistedRecipe(GameTestHelper helper, ServerLevel level) {
        List<? extends String> previousBlacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();
        ItemStack virtualWater = VirtualFluidItem.createItemStack(Fluids.WATER);
        ItemStack stick = new ItemStack(Items.STICK);

        try {
            setVirtualFluidRecipeBlacklist();
            RecipeHolder<CraftingRecipe> recipe = findCraftingRecipe(level, virtualWater, stick);
            assertRecipe(helper, BLACKLISTED_RECIPE, recipe,
                    "crafting terminal caches virtual bucket output before recipe is blacklisted");

            var player = FakePlayerFactory.getMinecraft(helper.getLevel());
            CraftingTermMenu menu = new CraftingTermMenu(4, player.getInventory(), new TestPatternHost(player, level)) {
                @Override
                public void broadcastChanges() {
                    // This mock connection has no AE2 payload negotiation; exercise slot behavior without network sends.
                }
            };
            menu.getSlots(SlotSemantics.CRAFTING_GRID).get(0).set(virtualWater.copy());
            menu.getSlots(SlotSemantics.CRAFTING_GRID).get(1).set(stick.copy());
            CraftingTermSlot output = (CraftingTermSlot) menu.getSlots(SlotSemantics.CRAFTING_RESULT).getFirst();
            assertEquals(helper, recipe.value().assemble(craftingInput(virtualWater, stick)).getItem(),
                    output.getItem().getItem(),
                    "crafting terminal result slot keeps the cached output");

            setVirtualFluidRecipeBlacklist(BLACKLISTED_RECIPE_ID);
            output.doClick(InventoryAction.PICKUP_OR_SET_DOWN, player);
            assertTrue(helper, menu.getCarried().isEmpty(),
                    "cached crafting terminal recipe must not bypass blacklist on pickup");
            assertTrue(helper, FluidCraftingHelper.isVirtualFluidItem(menu.getCraftingMatrix().getStackInSlot(0)),
                    "blocked cached crafting terminal recipe must not consume virtual fluid input");
            menu.getSlots(SlotSemantics.CRAFTING_GRID).getFirst().set(ItemStack.EMPTY);
            menu.getSlots(SlotSemantics.CRAFTING_GRID).getFirst().set(virtualWater.copy());
            assertTrue(helper, output.getItem().isEmpty(),
                    "blacklisted virtual-bucket recipe must hide the crafting output");

            CraftingTermMenu realBucketMenu = new CraftingTermMenu(5, player.getInventory(),
                    new TestPatternHost(player, level)) {
                @Override
                public void broadcastChanges() {
                    // This mock connection has no AE2 payload negotiation; exercise slot behavior without network sends.
                }
            };
            ItemStack realWaterBucket = new ItemStack(Items.WATER_BUCKET);
            RecipeHolder<CraftingRecipe> realBucketRecipe = findCraftingRecipe(level, realWaterBucket, stick);
            assertRecipe(helper, BLACKLISTED_RECIPE, realBucketRecipe,
                    "crafting terminal still finds blacklisted recipe with real bucket input");
            ItemStack expectedRealBucketOutput = realBucketRecipe.value().assemble(craftingInput(realWaterBucket, stick));

            realBucketMenu.getSlots(SlotSemantics.CRAFTING_GRID).get(0).set(realWaterBucket.copy());
            realBucketMenu.getSlots(SlotSemantics.CRAFTING_GRID).get(1).set(stick.copy());
            CraftingTermSlot realBucketOutput =
                    (CraftingTermSlot) realBucketMenu.getSlots(SlotSemantics.CRAFTING_RESULT).getFirst();
            assertEquals(helper, expectedRealBucketOutput.getItem(), realBucketOutput.getItem().getItem(),
                    "blacklisted crafting terminal recipe still shows real bucket output");
            realBucketMenu.getSlots(SlotSemantics.CRAFTING_GRID).getFirst().set(virtualWater.copy());
            assertTrue(helper, realBucketOutput.getItem().isEmpty(),
                    "replacing a real bucket with virtual fluid must clear the cached output");
            realBucketMenu.getSlots(SlotSemantics.CRAFTING_GRID).getFirst().set(realWaterBucket.copy());
            realBucketOutput.doClick(InventoryAction.PICKUP_OR_SET_DOWN, player);
            assertEquals(helper, expectedRealBucketOutput.getItem(), realBucketMenu.getCarried().getItem(),
                    "blacklisted crafting terminal recipe still crafts with a real bucket");
        } finally {
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(previousBlacklist);
        }
    }

    private static void assertRecipe(GameTestHelper helper, ResourceKey<Recipe<?>> expected,
            RecipeHolder<?> actual, String message) {
        assertTrue(helper, actual != null, message + ": expected recipe=" + expected.identifier());
        assertEquals(helper, expected, actual.id(), message);
    }

    private static void assertFastSuiteCachedRecipeList(GameTestHelper helper, ServerLevel level,
            CraftingInput listedVirtualInput, CraftingInput listedRealInput, CraftingInput taggedInput,
            CraftingInput overlapInput) {
        if (!ModList.get().isLoaded("fastsuite")) {
            return;
        }

        var cache = new CachedRecipeList<CraftingInput, CraftingRecipe>(RecipeType.CRAFTING,
                RecipeAccess.byType(level, RecipeType.CRAFTING));

        assertEquals(helper, null, cache.getRecipesFor(listedVirtualInput, level).findFirst().orElse(null),
                "FastSuite cached list rejects blacklisted virtual bucket");
        assertRecipe(helper, BLACKLISTED_RECIPE, cache.getRecipesFor(listedRealInput, level).findFirst().orElse(null),
                "FastSuite cached list still allows real bucket");
        assertRecipe(helper, ALLOWED_TAGGED_RECIPE, cache.getRecipesFor(taggedInput, level).findFirst().orElse(null),
                "FastSuite cached list accepts unlisted tagged virtual bucket");
        assertRecipe(helper, ALLOWED_OVERLAP_RECIPE, cache.getRecipesFor(overlapInput, level).findFirst().orElse(null),
                "FastSuite cached list skips blacklisted overlap recipe");
    }

    private static void setVirtualFluidRecipeBlacklist(Identifier... recipeIds) {
        List<String> entries = new ArrayList<>();
        for (Identifier recipeId : recipeIds) {
            entries.add(recipeId.toString());
        }
        FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(entries);
    }

    private static void assertTransferPreviewFluidRepoPolicy(GameTestHelper helper, Level level, Ingredient waterBucket) {
        Player player = FakePlayerFactory.getMinecraft((ServerLevel) level);
        CraftingTermMenu menu = createCraftingMenu(player, level);
        IClientRepo storedWater = clientRepo(new GridInventoryEntry(1, AEFluidKey.of(Fluids.WATER),
                AEFluidKey.AMOUNT_BUCKET, 0, false));
        IClientRepo craftableWater = clientRepo(new GridInventoryEntry(2, AEFluidKey.of(Fluids.WATER),
                0, AEFluidKey.AMOUNT_BUCKET, true));

        var blockedStored = VirtualFluidRecipePolicy.withRecipe(menu, BLACKLISTED_RECIPE,
                () -> FluidCraftingHelper.checkFluidAvailabilityInClientRepo(waterBucket, storedWater, level));
        assertTrue(helper, !blockedStored.available(),
                "blacklisted transfer preview must leave stored fluid bucket ingredient missing");
        assertTrue(helper, !blockedStored.craftable(),
                "blacklisted transfer preview must not mark stored fluid bucket ingredient craftable");
        var blockedCraftable = VirtualFluidRecipePolicy.withRecipe(menu, BLACKLISTED_RECIPE,
                () -> FluidCraftingHelper.checkFluidAvailabilityInClientRepo(waterBucket, craftableWater, level));
        assertTrue(helper, !blockedCraftable.available(),
                "blacklisted transfer preview must leave craftable fluid bucket ingredient missing");
        assertTrue(helper, !blockedCraftable.craftable(),
                "blacklisted transfer preview must not promote craftable fluid to bucket craftable");

        setVirtualFluidRecipeBlacklist();
        var allowedStored = VirtualFluidRecipePolicy.withRecipe(menu, BLACKLISTED_RECIPE,
                () -> FluidCraftingHelper.checkFluidAvailabilityInClientRepo(waterBucket, storedWater, level));
        assertTrue(helper, allowedStored.available(),
                "unlisted transfer preview may satisfy bucket ingredient from stored fluid");
        var allowedCraftable = VirtualFluidRecipePolicy.withRecipe(menu, BLACKLISTED_RECIPE,
                () -> FluidCraftingHelper.checkFluidAvailabilityInClientRepo(waterBucket, craftableWater, level));
        assertTrue(helper, allowedCraftable.craftable(),
                "unlisted transfer preview may mark bucket ingredient craftable from fluid autocrafting");

        boolean previousQoL = FluidCraftingConfig.COMMON.QoL.get();
        try {
            FluidCraftingConfig.COMMON.QoL.set(true);
            Map<Integer, Ingredient> ingredients = Map.of(0, waterBucket);
            var missing = new CraftingTermMenu.MissingIngredientSlots(Set.of(0), Set.of());

            setVirtualFluidRecipeBlacklist(BLACKLISTED_RECIPE_ID, BLACKLISTED_OVERLAP_RECIPE_ID);
            menu.setClientRepo(storedWater);
            var blockedResult = VirtualFluidRecipePolicy.withRecipe(menu, BLACKLISTED_RECIPE,
                    () -> FluidCraftingTerminalIntegration.resolveMissingIngredients(menu, missing, ingredients));
            assertTrue(helper, blockedResult.missingSlots().contains(0),
                    "blacklisted ET preview must keep the fluid ingredient missing");

            setVirtualFluidRecipeBlacklist();
            menu.setClientRepo(storedWater);
            var availableResult = VirtualFluidRecipePolicy.withRecipe(menu, BLACKLISTED_RECIPE,
                    () -> FluidCraftingTerminalIntegration.resolveMissingIngredients(menu, missing, ingredients));
            assertTrue(helper, availableResult.missingSlots().isEmpty(),
                    "stored fluid must remove the ET preview missing slot");

            menu.setClientRepo(craftableWater);
            var craftableResult = VirtualFluidRecipePolicy.withRecipe(menu, BLACKLISTED_RECIPE,
                    () -> FluidCraftingTerminalIntegration.resolveMissingIngredients(menu, missing, ingredients));
            assertTrue(helper, craftableResult.missingSlots().isEmpty()
                            && craftableResult.craftableSlots().contains(0),
                    "craftable fluid must promote the ET preview slot to craftable");

            menu.setClientRepo(storedWater);
            assertTrue(helper, FluidCraftingTerminalIntegration.resolveMissingIngredients(menu, missing, ingredients)
                            == missing,
                    "ET preview without recipe context must leave missing slots unchanged");

            FluidCraftingConfig.COMMON.QoL.set(false);
            var withoutUpgrade = VirtualFluidRecipePolicy.withRecipe(menu, BLACKLISTED_RECIPE,
                    () -> FluidCraftingTerminalIntegration.resolveMissingIngredients(menu, missing, ingredients));
            assertTrue(helper, withoutUpgrade == missing,
                    "ET preview without the fluid upgrade must leave missing slots unchanged");
        } finally {
            FluidCraftingConfig.COMMON.QoL.set(previousQoL);
            setVirtualFluidRecipeBlacklist(BLACKLISTED_RECIPE_ID, BLACKLISTED_OVERLAP_RECIPE_ID);
        }
    }

    private static CraftingTermMenu createCraftingMenu(Player player, Level level) {
        return new CraftingTermMenu(6, player.getInventory(), new TestPatternHost(player, level)) {
            @Override
            public void broadcastChanges() {
                // This mock connection has no AE2 payload negotiation; exercise preview behavior without network sends.
            }
        };
    }

    private static IClientRepo clientRepo(GridInventoryEntry entry) {
        return new TestClientRepo(Set.of(entry));
    }

    private static PatternEncodingTermMenu createPatternMenu(GameTestHelper helper, Level level) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        var host = new TestPatternHost(player, level);
        return new PatternEncodingTermMenu(3, player.getInventory(), host) {
            @Override
            public void broadcastChanges() {
                // This mock connection has no AE2 payload negotiation; exercise encoding without network sends.
            }
        };
    }

    private static void setCraftingGrid(PatternEncodingTermMenu menu, ItemStack... stacks) {
        var slots = menu.getCraftingGridSlots();
        for (int i = 0; i < slots.length; i++) {
            slots[i].set(i < stacks.length ? stacks[i].copy() : ItemStack.EMPTY);
        }
    }

    private static void assertPatternEncode(GameTestHelper helper, Level level, ItemStack first, ItemStack second,
            boolean expectedEncoded, String message) {
        PatternEncodingTermMenu menu = createPatternMenu(helper, level);
        setCraftingGrid(menu, first, second);
        menu.getSlots(SlotSemantics.BLANK_PATTERN).getFirst().set(AEItems.BLANK_PATTERN.stack(1));
        menu.encode();
        var encoded = menu.getSlots(SlotSemantics.ENCODED_PATTERN).getFirst().getItem();
        assertTrue(helper, PatternDetailsHelper.isEncodedPattern(encoded) == expectedEncoded, message);
    }

    private static final class TestPatternHost extends ItemMenuHost<Item>
            implements IPatternTerminalMenuHost, IPatternTerminalLogicHost, IEnergySource, ISegmentedInventory {
        private final CountingStorage storage = new CountingStorage();
        private final InternalInventory craftingGrid = new AppEngInternalInventory(9);
        private final IConfigManager configManager = IConfigManager.builder(() -> {}).build();
        private final Level level;
        private final PatternEncodingLogic logic;

        private TestPatternHost(Player player, Level level) {
            super(Items.STICK, player, MenuLocators.forStack(new ItemStack(Items.STICK)));
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
        public InternalInventory getSubInventory(Identifier id) {
            return CraftingTerminalPart.INV_CRAFTING.equals(id) ? craftingGrid : null;
        }

        @Override
        public ILinkStatus getLinkStatus() {
            return ILinkStatus.ofConnected();
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

    private record TestClientRepo(Set<GridInventoryEntry> entries) implements IClientRepo {
        @Override
        public void handleUpdate(boolean fullUpdate, List<GridInventoryEntry> entries) {
        }

        @Override
        public Set<GridInventoryEntry> getAllEntries() {
            return entries;
        }

        @Override
        public Collection<GridInventoryEntry> getByIngredient(Ingredient ingredient) {
            return List.of();
        }
    }

    private static final class CountingStorage implements MEStorage {
        private final Map<AEKey, Long> storedByKey = new LinkedHashMap<>();
        private boolean acceptInserts = true;

        private long stored(AEKey key) {
            return storedByKey.getOrDefault(key, 0L);
        }

        @Override
        public long insert(AEKey key, long amount, Actionable mode, IActionSource source) {
            if (!acceptInserts) {
                return 0;
            }
            if (mode == Actionable.MODULATE) {
                storedByKey.merge(key, amount, Long::sum);
            }
            return amount;
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            storedByKey.forEach((key, amount) -> {
                if (amount > 0) {
                    out.add(key, amount);
                }
            });
        }

        @Override
        public Component getDescription() {
            return Component.literal("AE2FCT GameTest storage");
        }
    }

    private static final class OffhandSynchronizer implements ContainerSynchronizer {
        private ItemStack offhand = ItemStack.EMPTY;
        private final HashedPatchMap.HashGenerator hashGenerator;

        private OffhandSynchronizer(Player player) {
            var hashOps = player.registryAccess().createSerializationContext(HashOps.CRC32C_INSTANCE);
            hashGenerator = component -> component.encodeValue(hashOps).getOrThrow().asInt();
        }

        @Override
        public void sendInitialData(AbstractContainerMenu menu, List<ItemStack> items, ItemStack carried, int[] data) {
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

        @Override
        public RemoteSlot createSlot() {
            return new RemoteSlot.Synchronized(hashGenerator);
        }
    }
}
