package me.myogoo.ae2fct.gametest;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.menuobjects.IPortableTerminal;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.ILinkStatus;
import appeng.api.storage.MEStorage;
import appeng.api.util.IConfigManager;
import appeng.core.definitions.AEItems;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.helpers.InventoryAction;
import appeng.menu.ISubMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.locator.MenuLocators;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.me.common.IClientRepo;
import appeng.menu.me.crafting.CraftConfirmMenu;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.me.items.PatternEncodingTermMenu;
import appeng.menu.slot.CraftingTermSlot;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.parts.reporting.CraftingTerminalPart;
import appeng.util.inv.AppEngInternalInventory;
import dev.shadowsoffire.fastsuite.AuxRecipeManager;
import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import me.myogoo.ae2fct.integration.recipeviewer.VirtualFluidBlacklistRecipes;
import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.item.VirtualFluidItem;
import me.myogoo.ae2fct.util.FluidCraftingHelper;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@GameTestHolder(Ae2fct.MODID)
@PrefixGameTestTemplate(false)
public final class VirtualFluidRecipePolicyGameTest {

    private static final ResourceLocation BLACKLISTED_RECIPE =
            ResourceLocation.fromNamespaceAndPath("immersiveengineering", "crafting/redstone_acid");
    private static final ResourceLocation UNLISTED_RECIPE =
            ResourceLocation.fromNamespaceAndPath("ae2fct", "unlisted_virtual_fluid_test_recipe");
    private static final ResourceLocation LOOKUP_BLACKLISTED_RECIPE =
            ResourceLocation.fromNamespaceAndPath(Ae2fct.MODID, "gametest/blacklisted_virtual_bucket");
    private static final ResourceLocation LOOKUP_ALLOWED_TAGGED_RECIPE =
            ResourceLocation.fromNamespaceAndPath(Ae2fct.MODID, "gametest/allowed_tagged_bucket");
    private static final ResourceLocation LOOKUP_SHAPED_NARROW_RECIPE =
            ResourceLocation.fromNamespaceAndPath(Ae2fct.MODID, "gametest/shaped_narrow_bucket");
    private static final ResourceLocation LOOKUP_BLACKLISTED_OVERLAP_RECIPE =
            ResourceLocation.fromNamespaceAndPath(Ae2fct.MODID, "gametest/aaa_blacklisted_overlap");
    private static final ResourceLocation LOOKUP_ALLOWED_OVERLAP_RECIPE =
            ResourceLocation.fromNamespaceAndPath(Ae2fct.MODID, "gametest/zzz_allowed_overlap");

    private VirtualFluidRecipePolicyGameTest() {
    }

    @GameTest(template = "empty")
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
            client.offhand = player.getOffhandItem().copy();

            menu.removed(player);
            assertTrue(helper, player.getOffhandItem().isEmpty(), "returned offhand fluid must be removed");
            assertEquals(helper, 1L, host.storage.inserted, "menu close must return exactly one virtual fluid");
            player.inventoryMenu.transferState(menu);
            player.inventoryMenu.broadcastChanges();
            assertTrue(helper, client.offhand.isEmpty(), "menu close must clear the client-predicted offhand too");
            menu.removed(player);
            assertEquals(helper, 1L, host.storage.inserted, "closing again must not return fluid twice");

            player.getInventory().setItem(Inventory.SLOT_OFFHAND, new ItemStack(Items.STICK, 3));
            menu.removed(player);
            assertEquals(helper, 3, player.getOffhandItem().getCount(), "ordinary offhand items must stay untouched");
            assertEquals(helper, 1L, host.storage.inserted, "ordinary offhand items must not enter ME");

            host.storage.acceptInserts = false;
            player.getInventory().setItem(Inventory.SLOT_OFFHAND, VirtualFluidItem.createItemStack(Fluids.WATER));
            menu.removed(player);
            assertTrue(helper, FluidCraftingHelper.isVirtualFluidItem(player.getOffhandItem()),
                    "rejected fluid must remain in the offhand");
            assertEquals(helper, 1L, host.storage.inserted, "rejected fluid must not enter ME");
        } finally {
            player.inventoryMenu.setSynchronizer(null);
            player.containerMenu = previousMenu;
            player.getInventory().setItem(0, previousHotbar);
            player.getInventory().setItem(Inventory.SLOT_OFFHAND, previousOffhand);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void blacklistedRecipeRejectsVirtualButAllowsRealBucket(GameTestHelper helper) {
        validateMixinTargetsLoad();

        FluidCraftingHelper.clearFluidCraftingEnabled();

        Ingredient waterBucket = Ingredient.of(Items.WATER_BUCKET);
        ItemStack virtualWater = VirtualFluidItem.createItemStack(Fluids.WATER);

        assertTrue(helper, waterBucket.test(virtualWater), "virtual water matches bucket ingredient by default");
        assertTrue(helper, waterBucket.test(Items.WATER_BUCKET.getDefaultInstance()),
                "real water bucket matches bucket ingredient by default");

        try {
            FluidCraftingHelper.withVirtualFluidRecipePolicy(null, BLACKLISTED_RECIPE, () -> {
                assertFalse(helper, waterBucket.test(virtualWater),
                        "blacklisted recipe must reject virtual water");
                assertTrue(helper, waterBucket.test(Items.WATER_BUCKET.getDefaultInstance()),
                        "blacklisted recipe must still allow a real water bucket");
                return null;
            });
        } finally {
            FluidCraftingHelper.clearFluidCraftingEnabled();
        }

        assertTrue(helper, waterBucket.test(virtualWater), "blacklist context must be cleared after the check");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void immersiveEngineeringCreosoteRequiresFullBucket(GameTestHelper helper) {
        if (!ModList.get().isLoaded("immersiveengineering")) {
            helper.succeed();
            return;
        }

        var level = helper.getLevel();
        var recipeId = ResourceLocation.fromNamespaceAndPath("immersiveengineering",
                "crafting/treated_wood_horizontal");
        var recipe = (CraftingRecipe) level.getRecipeManager().byKey(recipeId).orElseThrow().value();
        var creosote = BuiltInRegistries.FLUID.get(
                ResourceLocation.fromNamespaceAndPath("immersiveengineering", "creosote"));
        ItemStack[] ingredients = new ItemStack[9];
        java.util.Arrays.fill(ingredients, new ItemStack(Items.OAK_PLANKS));
        ingredients[4] = new ItemStack(creosote.getBucket());
        assertTrue(helper, recipe.matches(craftingInput(ingredients), level), "real creosote bucket control");

        ItemStack virtual = VirtualFluidItem.createItemStack(creosote);
        ingredients[4] = virtual;
        assertTrue(helper, recipe.matches(craftingInput(ingredients), level),
                "IE treated-wood recipe accepts a full virtual creosote bucket");

        ItemStack insufficient = virtual.copy();
        insufficient.set(AE2FCTDataComponent.VIRTUAL_FLUID,
                new VirtualFluid(new FluidStack(creosote, 999), 1000));
        ingredients[4] = insufficient;
        assertFalse(helper, recipe.matches(craftingInput(ingredients), level),
                "IE treated-wood recipe rejects less than one bucket of creosote");

        var previousBlacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();
        Player player = FakePlayerFactory.getMinecraft(level);
        try {
            setVirtualFluidRecipeBlacklist();
            CraftingTermMenu menu = createCraftingMenu(player, level);
            var grid = menu.getSlots(SlotSemantics.CRAFTING_GRID);
            var output = (CraftingTermSlot) menu.getSlots(SlotSemantics.CRAFTING_RESULT).getFirst();
            for (int slot = 0; slot < 9; slot++) {
                grid.get(slot).set(slot == 4 ? virtual.copy() : new ItemStack(Items.OAK_PLANKS));
            }
            output.doClick(InventoryAction.PICKUP_OR_SET_DOWN, player);
            assertEquals(helper, 8, menu.getCarried().getCount(), "virtual creosote produces eight treated planks");
            assertTrue(helper, grid.stream().allMatch(slot -> slot.getItem().isEmpty()),
                    "crafting consumes the virtual bucket without returning an empty bucket");

            menu.setCarried(ItemStack.EMPTY);
            setVirtualFluidRecipeBlacklist(recipeId);
            for (int slot = 0; slot < 9; slot++) {
                grid.get(slot).set(slot == 4 ? virtual.copy() : new ItemStack(Items.OAK_PLANKS));
            }
            output.doClick(InventoryAction.PICKUP_OR_SET_DOWN, player);
            assertTrue(helper, menu.getCarried().isEmpty(), "blacklisted treated wood requires a real bucket");
            assertTrue(helper, FluidCraftingHelper.isVirtualFluidItem(grid.get(4).getItem()),
                    "blocked crafting preserves virtual creosote");
            grid.get(4).set(new ItemStack(creosote.getBucket()));
            output.doClick(InventoryAction.PICKUP_OR_SET_DOWN, player);
            assertEquals(helper, 8, menu.getCarried().getCount(), "blacklisted recipe still accepts real creosote");
            assertTrue(helper, grid.get(4).getItem().is(Items.BUCKET), "real creosote leaves an empty bucket");
        } finally {
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(previousBlacklist);
            FluidCraftingHelper.clearFluidCraftingEnabled();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void unlistedTagIngredientAllowsVirtualFluid(GameTestHelper helper) {
        FluidCraftingHelper.clearFluidCraftingEnabled();

        Ingredient waterBucketTag = Ingredient.of(Tags.Items.BUCKETS_WATER);
        ItemStack virtualWater = VirtualFluidItem.createItemStack(Fluids.WATER);

        try {
            FluidCraftingHelper.withVirtualFluidRecipePolicy(null, UNLISTED_RECIPE, () -> {
                assertTrue(helper, waterBucketTag.test(virtualWater),
                        "unlisted tag ingredient must still allow virtual water");
                return null;
            });
        } finally {
            FluidCraftingHelper.clearFluidCraftingEnabled();
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeLookupUsesRecipeBlacklist(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<? extends String> previousBlacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();

        try {
            setVirtualFluidRecipeBlacklist(LOOKUP_BLACKLISTED_RECIPE, LOOKUP_BLACKLISTED_OVERLAP_RECIPE);

            ItemStack virtualWater = VirtualFluidItem.createItemStack(Fluids.WATER);
            assertFalse(helper, findPolicyRecipe(level, virtualWater, new ItemStack(Items.STICK)).isPresent(),
                    "blacklisted recipe with virtual bucket fails through policy lookup");
            assertRecipe(helper, LOOKUP_BLACKLISTED_RECIPE,
                    level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,
                            craftingInput(new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.STICK)), level)
                            .orElse(null),
                    "real bucket fixture recipe is loaded by the recipe manager");
            assertRecipe(helper, LOOKUP_BLACKLISTED_RECIPE,
                    findPolicyRecipe(level, new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.STICK)).orElse(null),
                    "blacklisted recipe still allows real bucket through policy lookup");
            assertRecipe(helper, LOOKUP_ALLOWED_TAGGED_RECIPE,
                    findPolicyRecipe(level, virtualWater, new ItemStack(Items.PAPER)).orElse(null),
                    "unlisted tag ingredient recipe accepts virtual bucket");
            assertRecipe(helper, LOOKUP_ALLOWED_OVERLAP_RECIPE,
                    findPolicyRecipe(level, virtualWater, new ItemStack(Items.SUGAR)).orElse(null),
                    "overlapping blacklisted recipe is skipped in favor of allowed alternative");
        } finally {
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(previousBlacklist);
            FluidCraftingHelper.clearFluidCraftingEnabled();
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void transferPreviewBlacklistLeavesRealBucketMissingWhenFluidAvailable(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<? extends String> previousBlacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();

        try {
            Player player = FakePlayerFactory.getMinecraft(helper.getLevel());
            CraftingTermMenu menu = createCraftingMenu(player, level);
            Ingredient waterBucket = Ingredient.of(Items.WATER_BUCKET);
            IClientRepo storedWater = clientRepo(new GridInventoryEntry(1, AEFluidKey.of(Fluids.WATER),
                    AEFluidKey.AMOUNT_BUCKET, 0, false));
            IClientRepo craftableWater = clientRepo(new GridInventoryEntry(2, AEFluidKey.of(Fluids.WATER),
                    0, AEFluidKey.AMOUNT_BUCKET, true));

            setVirtualFluidRecipeBlacklist(LOOKUP_BLACKLISTED_RECIPE);
            var blockedStored = FluidCraftingHelper.withVirtualFluidRecipePolicy(menu, LOOKUP_BLACKLISTED_RECIPE,
                    () -> FluidCraftingHelper.checkFluidAvailabilityInClientRepo(waterBucket, storedWater));
            assertFalse(helper, blockedStored.available(),
                    "blacklisted transfer preview must leave stored fluid bucket ingredient missing");
            assertFalse(helper, blockedStored.craftable(),
                    "blacklisted transfer preview must not mark stored fluid bucket ingredient craftable");
            var blockedCraftable = FluidCraftingHelper.withVirtualFluidRecipePolicy(menu, LOOKUP_BLACKLISTED_RECIPE,
                    () -> FluidCraftingHelper.checkFluidAvailabilityInClientRepo(waterBucket, craftableWater));
            assertFalse(helper, blockedCraftable.available(),
                    "blacklisted transfer preview must leave craftable fluid bucket ingredient missing");
            assertFalse(helper, blockedCraftable.craftable(),
                    "blacklisted transfer preview must not promote craftable fluid to bucket craftable");

            setVirtualFluidRecipeBlacklist();
            var allowedStored = FluidCraftingHelper.withVirtualFluidRecipePolicy(menu, LOOKUP_BLACKLISTED_RECIPE,
                    () -> FluidCraftingHelper.checkFluidAvailabilityInClientRepo(waterBucket, storedWater));
            assertTrue(helper, allowedStored.available(),
                    "unlisted transfer preview may satisfy bucket ingredient from stored fluid");
            var allowedCraftable = FluidCraftingHelper.withVirtualFluidRecipePolicy(menu, LOOKUP_BLACKLISTED_RECIPE,
                    () -> FluidCraftingHelper.checkFluidAvailabilityInClientRepo(waterBucket, craftableWater));
            assertTrue(helper, allowedCraftable.craftable(),
                    "unlisted transfer preview may mark bucket ingredient craftable from fluid autocrafting");
        } finally {
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(previousBlacklist);
            FluidCraftingHelper.clearFluidCraftingEnabled();
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void recipeViewerBlacklistCategoryUsesLocalConfig(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<? extends String> previousBlacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();

        try {
            setVirtualFluidRecipeBlacklist(LOOKUP_BLACKLISTED_RECIPE);
            List<RecipeHolder<CraftingRecipe>> recipes =
                    VirtualFluidBlacklistRecipes.fromLocalConfig(level.getRecipeManager());
            assertTrue(helper, recipes.size() == 1, "viewer category exposes one locally configured recipe");
            RecipeHolder<CraftingRecipe> recipe = recipes.getFirst();
            assertRecipe(helper, LOOKUP_BLACKLISTED_RECIPE, recipe,
                    "viewer category uses the actual loaded crafting recipe");
            assertTrue(helper, recipe.value().getIngredients().getFirst().test(new ItemStack(Items.WATER_BUCKET)),
                    "viewer category keeps the original real bucket ingredient");
            assertTrue(helper, VirtualFluidBlacklistRecipes.result(recipe, level.registryAccess()).is(Items.OBSIDIAN),
                    "viewer category keeps the original recipe output");

            setVirtualFluidRecipeBlacklist(LOOKUP_SHAPED_NARROW_RECIPE);
            RecipeHolder<CraftingRecipe> narrowRecipe =
                    VirtualFluidBlacklistRecipes.fromLocalConfig(level.getRecipeManager()).getFirst();
            assertRecipe(helper, LOOKUP_SHAPED_NARROW_RECIPE, narrowRecipe,
                    "viewer category loads narrow shaped recipe");
            assertTrue(helper, VirtualFluidBlacklistRecipes.recipeWidth(narrowRecipe) == 1,
                    "viewer category preserves shaped recipe width");
            assertTrue(helper, VirtualFluidBlacklistRecipes.recipeHeight(narrowRecipe) == 2,
                    "viewer category preserves shaped recipe height");
            assertTrue(helper, VirtualFluidBlacklistRecipes.gridSlot(narrowRecipe, 0) == 0,
                    "first narrow shaped ingredient remains in first grid slot");
            assertTrue(helper, VirtualFluidBlacklistRecipes.gridSlot(narrowRecipe, 1) == 3,
                    "second narrow shaped ingredient remains in first column second row");
            assertTrue(helper, VirtualFluidBlacklistRecipes.syntheticViewerId(narrowRecipe).equals(
                            ResourceLocation.fromNamespaceAndPath(Ae2fct.MODID,
                                    "virtual_fluid_blacklist/ae2fct/gametest/shaped_narrow_bucket")),
                    "EMI viewer id is category-local and does not collide with the real crafting recipe id");
        } finally {
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(previousBlacklist);
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void manualCraftingMenuRejectsBlacklistedVirtualFluidPickup(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<? extends String> previousBlacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();

        try {
            setVirtualFluidRecipeBlacklist();

            Player player = FakePlayerFactory.getMinecraft(helper.getLevel());
            CraftingTermMenu menu = createCraftingMenu(player, level);
            var grid = menu.getSlots(SlotSemantics.CRAFTING_GRID);
            CraftingTermSlot output = (CraftingTermSlot) menu.getSlots(SlotSemantics.CRAFTING_RESULT).getFirst();

            grid.getFirst().set(VirtualFluidItem.createItemStack(Fluids.WATER));
            grid.get(1).set(new ItemStack(Items.STICK));
            assertRecipe(helper, LOOKUP_BLACKLISTED_RECIPE, menu.getCurrentRecipe(),
                    "virtual bucket recipe is selected before it is blacklisted");
            assertFalse(helper, output.getItem().isEmpty(), "unlisted virtual bucket recipe must show output");

            setVirtualFluidRecipeBlacklist(LOOKUP_BLACKLISTED_RECIPE);
            output.doClick(InventoryAction.PICKUP_OR_SET_DOWN, player);
            assertTrue(helper, menu.getCarried().isEmpty(),
                    "cached blacklisted virtual-bucket recipe must not be picked up");
            assertTrue(helper, FluidCraftingHelper.isVirtualFluidItem(menu.getCraftingMatrix().getStackInSlot(0)),
                    "blocked cached crafting must not consume the virtual fluid input");
            grid.getFirst().set(ItemStack.EMPTY);
            grid.getFirst().set(VirtualFluidItem.createItemStack(Fluids.WATER));
            assertTrue(helper, output.getItem().isEmpty(),
                    "blacklisted virtual-bucket recipe must hide the crafting output");

            grid.getFirst().set(new ItemStack(Items.WATER_BUCKET));
            assertRecipe(helper, LOOKUP_BLACKLISTED_RECIPE, menu.getCurrentRecipe(),
                    "real bucket recipe remains selected after blacklist");
            assertFalse(helper, output.getItem().isEmpty(), "blacklisted real-bucket recipe must still show output");
            grid.getFirst().set(VirtualFluidItem.createItemStack(Fluids.WATER));
            assertTrue(helper, output.getItem().isEmpty(),
                    "replacing a real bucket with virtual fluid must clear the cached output");
            grid.getFirst().set(new ItemStack(Items.WATER_BUCKET));
            output.doClick(InventoryAction.PICKUP_OR_SET_DOWN, player);
            assertFalse(helper, menu.getCarried().isEmpty(),
                    "blacklisted recipe must remain craftable with a real bucket");
        } finally {
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(previousBlacklist);
            FluidCraftingHelper.clearFluidCraftingEnabled();
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void patternEncodingMenuUsesRecipeBlacklist(GameTestHelper helper) {
        Level level = helper.getLevel();
        List<? extends String> previousBlacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();

        try {
            setVirtualFluidRecipeBlacklist(LOOKUP_BLACKLISTED_RECIPE);
            ItemStack virtualWater = VirtualFluidItem.createItemStack(Fluids.WATER);
            assertPatternEncode(helper, level, virtualWater, new ItemStack(Items.STICK), false,
                    "manual pattern ghost virtual bucket cannot encode blacklisted recipe");
            assertPatternEncode(helper, level, new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.STICK),
                    true, "manual pattern real bucket can encode blacklisted recipe");

            setVirtualFluidRecipeBlacklist();
            PatternEncodingTermMenu cachedMenu = createPatternMenu(helper, level);
            setCraftingGrid(cachedMenu, virtualWater, new ItemStack(Items.STICK));
            cachedMenu.getSlots(SlotSemantics.BLANK_PATTERN).getFirst().set(AEItems.BLANK_PATTERN.stack(1));
            cachedMenu.encode();
            var cachedEncodedSlot = cachedMenu.getSlots(SlotSemantics.ENCODED_PATTERN).getFirst();
            assertTrue(helper, PatternDetailsHelper.isEncodedPattern(cachedEncodedSlot.getItem()),
                    "virtual bucket encodes before recipe is blacklisted");
            ItemStack encodedBeforeBlacklist = cachedEncodedSlot.getItem().copy();
            var decodedBeforeBlacklist = PatternDetailsHelper.decodePattern(encodedBeforeBlacklist, level);
            assertTrue(helper, decodedBeforeBlacklist instanceof AECraftingPattern,
                    "encoded virtual bucket pattern decodes before recipe is blacklisted");

            setVirtualFluidRecipeBlacklist(LOOKUP_BLACKLISTED_RECIPE);
            assertTrue(helper, ((AECraftingPattern) decodedBeforeBlacklist)
                    .assemble(craftingInput(virtualWater, new ItemStack(Items.STICK)), level).isEmpty(),
                    "decoded cached virtual pattern assemble rejects after blacklist update");
            assertTrue(helper, PatternDetailsHelper.decodePattern(encodedBeforeBlacklist, level) == null,
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

    @GameTest(template = "empty")
    public static void recipePolicyContextUsesRecipeBlacklist(GameTestHelper helper) {
        FluidCraftingHelper.clearFluidCraftingEnabled();

        Ingredient waterBucket = Ingredient.of(Items.WATER_BUCKET);
        ItemStack virtualWater = VirtualFluidItem.createItemStack(Fluids.WATER);

        try {
            Boolean allowedCachedMatch = FluidCraftingHelper.withVirtualFluidRecipePolicy(null, UNLISTED_RECIPE,
                    () -> waterBucket.test(virtualWater));
            Boolean blacklistedCachedMatch = FluidCraftingHelper.withVirtualFluidRecipePolicy(null, BLACKLISTED_RECIPE,
                    () -> waterBucket.test(virtualWater));

            assertTrue(helper, allowedCachedMatch, "unlisted cached recipe match must allow virtual water");
            assertFalse(helper, blacklistedCachedMatch, "blacklisted cached recipe match must reject virtual water");
        } finally {
            FluidCraftingHelper.clearFluidCraftingEnabled();
        }

        helper.succeed();
    }

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        helper.assertTrue(condition, message);
    }

    private static void assertFalse(GameTestHelper helper, boolean condition, String message) {
        helper.assertTrue(!condition, message);
    }

    private static void assertEquals(GameTestHelper helper, Object expected, Object actual, String message) {
        helper.assertTrue(Objects.equals(expected, actual), message + ": expected=" + expected + ", actual=" + actual);
    }

    private static void assertRecipe(GameTestHelper helper, ResourceLocation expected,
            RecipeHolder<?> actual, String message) {
        assertTrue(helper, actual != null, message + ": expected recipe=" + expected);
        helper.assertTrue(Objects.equals(expected, actual.id()), message + ": expected=" + expected
                + ", actual=" + actual.id());
    }

    private static CraftingInput craftingInput(ItemStack... stacks) {
        NonNullList<ItemStack> items = NonNullList.withSize(9, ItemStack.EMPTY);
        for (int i = 0; i < stacks.length; i++) {
            items.set(i, stacks[i].copy());
        }
        return CraftingInput.of(3, 3, items);
    }

    private static Optional<RecipeHolder<CraftingRecipe>> findPolicyRecipe(Level level, ItemStack... stacks) {
        return level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingInput(stacks), level);
    }

    private static void setVirtualFluidRecipeBlacklist(ResourceLocation... recipeIds) {
        List<String> entries = new ArrayList<>();
        for (ResourceLocation recipeId : recipeIds) {
            entries.add(recipeId.toString());
        }
        FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(entries);
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
                // The mock connection has no AE2 payload negotiation; exercise encoding without network sends.
            }
        };
    }

    private static CraftingTermMenu createCraftingMenu(Player player, Level level) {
        var host = new TestPatternHost(player, level);
        return new CraftingTermMenu(4, player.getInventory(), host) {
            @Override
            public void broadcastChanges() {
                // The mock connection has no AE2 payload negotiation; exercise slot crafting without network sends.
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

    private static void validateMixinTargetsLoad() {
        try {
            ensureInitialized(CraftingTermMenu.class);
            ensureInitialized(CraftingTermSlot.class);
            ensureInitialized(PatternEncodingTermMenu.class);
            ensureInitialized(CraftConfirmMenu.class);
            ensureInitialized(EncodingHelper.class);
            ensureInitialized(AECraftingPattern.class);
            if (ModList.get().isLoaded("fastsuite")) {
                ensureInitialized(AuxRecipeManager.class);
            }
        } catch (Throwable e) {
            throw new AssertionError("Expected mixin target class to initialize", e);
        }
    }

    private static void ensureInitialized(Class<?> type) throws IllegalAccessException {
        MethodHandles.lookup().ensureInitialized(type);
    }

    private static final class TestPatternHost extends ItemMenuHost<Item>
            implements IPatternTerminalMenuHost, IPatternTerminalLogicHost, IPortableTerminal, ISegmentedInventory {
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
        public InternalInventory getSubInventory(ResourceLocation id) {
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
        private long inserted;
        private boolean acceptInserts = true;

        @Override
        public long insert(AEKey key, long amount, Actionable mode, IActionSource source) {
            if (!acceptInserts) {
                return 0;
            }
            if (mode == Actionable.MODULATE) {
                inserted += amount;
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
}
