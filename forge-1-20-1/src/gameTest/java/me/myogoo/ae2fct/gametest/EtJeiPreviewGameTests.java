package me.myogoo.ae2fct.gametest;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.me.common.IClientRepo;
import appeng.helpers.InventoryAction;
import appeng.menu.AutoCraftingMenu;
import appeng.menu.slot.CraftingTermSlot;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.implementations.menuobjects.IPortableTerminal;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.MEStorage;
import appeng.api.util.IConfigManager;
import appeng.menu.ISubMenu;
import appeng.util.ConfigManager;
import appeng.util.inv.AppEngInternalInventory;
import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.codec.VirtualFluid;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import me.myogoo.ae2fct.item.VirtualFluidItem;
import me.myogoo.ae2fct.util.VirtualFluidStorage;
import me.myogoo.extendedterminal.api.host.IETTerminalHost;
import me.myogoo.extendedterminal.menu.ETMenuType;
import me.myogoo.extendedterminal.menu.extendedterminal.ETTerminalMode;
import me.myogoo.extendedterminal.menu.extendedterminal.ETTerminalMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static net.minecraft.world.item.Items.CRAFTING_TABLE;

@PrefixGameTestTemplate(false)
public final class EtJeiPreviewGameTests {
    private static final ResourceLocation ALLOWED_RECIPE = Ae2fct.makeId("et_jei_preview_allowed");
    private static final ResourceLocation BLOCKED_RECIPE = Ae2fct.makeId("et_jei_preview_blocked");

    private EtJeiPreviewGameTests() {
    }

    @GameTest(templateNamespace = Ae2fct.MODID, template = "empty3x3x3")
    public static void mobGrindingUtilsFeedConsumesFourVirtualBuckets(GameTestHelper helper) {
        if (!ModList.get().isLoaded("mob_grinding_utils")) {
            helper.succeed();
            return;
        }

        var level = helper.getLevel();
        var id = new ResourceLocation("mob_grinding_utils", "recipe_cursed_feed");
        var recipe = (CraftingRecipe) level.getRecipeManager().byKey(id).orElseThrow();
        var xp = BuiltInRegistries.FLUID.get(new ResourceLocation("mob_grinding_utils", "fluid_xp"));
        var feed = BuiltInRegistries.ITEM.get(new ResourceLocation("mob_grinding_utils", "gm_chicken_feed_cursed"));
        var virtual = VirtualFluidItem.createItemStack(xp);
        var bucket = FluidUtil.getFilledBucket(new FluidStack(xp, 1000));
        ItemStack[] inputs = { bucket, new ItemStack(Items.SPIDER_EYE), bucket,
                new ItemStack(Items.ROTTEN_FLESH), new ItemStack(Items.WHEAT_SEEDS), new ItemStack(Items.BONE),
                bucket, new ItemStack(Items.GUNPOWDER), bucket };
        int[] corners = {0, 2, 6, 8};
        var grid = new TransientCraftingContainer(new AutoCraftingMenu(), 3, 3);
        var previousBlacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();
        Player player = new FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "mgu-crafting-test"));
        var previousMenu = player.containerMenu;
        try {
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(List.of());
            for (int slot = 0; slot < inputs.length; slot++) {
                grid.setItem(slot, inputs[slot].copy());
            }
            helper.assertTrue(recipe.matches(grid, level), "MGU real XP buckets must match");
            for (int corner : corners) {
                inputs[corner] = virtual;
                grid.setItem(corner, virtual.copy());
            }
            helper.assertTrue(recipe.matches(grid, level), "MGU must accept four virtual XP buckets");
            helper.assertTrue(recipe.getRemainingItems(grid).stream().allMatch(ItemStack::isEmpty),
                    "virtual XP must not leave empty buckets");
            helper.assertTrue(!FluidUtil.getFluidHandler(virtual).isPresent(),
                    "virtual XP must not expose a drainable fluid capability");
            grid.setItem(0, VirtualFluidItem.createItemStack(Fluids.WATER));
            helper.assertTrue(!recipe.matches(grid, level), "MGU must reject fluid outside its experience tag");
            for (int amount : new int[]{999, 1001}) {
                var invalid = virtual.copy();
                VirtualFluidStorage.set(invalid, new VirtualFluid(new FluidStack(xp, amount), 1000));
                grid.setItem(0, invalid);
                helper.assertTrue(!recipe.matches(grid, level), "MGU must require exactly one virtual bucket");
            }

            var menu = new ETTerminalMenu(ETTerminalMenu.TYPE, 2, player.getInventory(), new TestHost(player));
            player.containerMenu = menu;
            var slots = menu.getSlots(ETMenuType.ET_TERMINAL.getSlotSemanticGrid());
            var output = (CraftingTermSlot) menu.getSlots(ETMenuType.ET_TERMINAL.getSlotSemanticResult()).get(0);
            for (int slot = 0; slot < inputs.length; slot++) {
                slots.get(slot).set(inputs[slot].copy());
            }
            helper.assertTrue(output.getItem().is(feed), "ET must preview cursed feed from virtual XP");
            output.doClick(InventoryAction.PICKUP_OR_SET_DOWN, player);
            helper.assertTrue(menu.getCarried().is(feed) && menu.getCarried().getCount() == 1,
                    "ET must allow taking one cursed feed");
            helper.assertTrue(slots.stream().allMatch(slot -> slot.getItem().isEmpty()),
                    "crafting must consume four 1000 mB virtual buckets and all five ordinary ingredients");

            menu.setCarried(ItemStack.EMPTY);
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(List.of(id.toString()));
            for (int slot = 0; slot < inputs.length; slot++) {
                slots.get(slot).set(inputs[slot].copy());
            }
            helper.assertTrue(output.getItem().isEmpty(), "blacklisted MGU recipe must have no virtual output");
            output.doClick(InventoryAction.PICKUP_OR_SET_DOWN, player);
            helper.assertTrue(menu.getCarried().isEmpty(), "blacklisted MGU output must not be obtainable");
            helper.assertTrue(slots.stream().noneMatch(slot -> slot.getItem().isEmpty()),
                    "blocked crafting must preserve every input");
            for (int corner : corners) {
                slots.get(corner).set(bucket.copy());
            }
            output.doClick(InventoryAction.PICKUP_OR_SET_DOWN, player);
            helper.assertTrue(menu.getCarried().is(feed), "blacklist must still allow real XP bucket crafting");
            helper.assertTrue(slots.stream().filter(slot -> slot.getItem().is(Items.BUCKET)).count() == 4,
                    "only the four real XP buckets should leave empty buckets");
        } finally {
            player.containerMenu = previousMenu;
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(previousBlacklist);
            FluidCraftingTerminalIntegration.restoreRecipeContext(null);
        }
        helper.succeed();
    }

    @GameTest(templateNamespace = Ae2fct.MODID, template = "empty3x3x3", timeoutTicks = 20)
    public static void extendedTerminalJeiPreviewResolvesFluidStorage(GameTestHelper helper) {
        boolean oldQol = FluidCraftingConfig.COMMON.QoL.get();
        List<? extends String> oldBlacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();
        try {
            FluidCraftingConfig.COMMON.QoL.set(true);
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(List.of(BLOCKED_RECIPE.toString()));

            Player player = new FakePlayer(helper.getLevel(),
                    new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "et-preview-test"));
            TestHost host = new TestHost(player);
            ETTerminalMenu menu = new ETTerminalMenu(ETTerminalMenu.TYPE, 1, player.getInventory(), host);
            menu.setClientRepo(new FluidRepo());

            Ingredient bucket = Ingredient.of(Items.WATER_BUCKET);
            var ingredients = java.util.Map.of(0, bucket);

            var withoutViewer = menu.findMissingIngredients(ingredients);
            helper.assertTrue(withoutViewer.missingSlots().contains(0),
                    "ET preview must not resolve fluids without JEI recipe context");

            var allowed = FluidCraftingTerminalIntegration.withRecipeViewerContext(menu, ALLOWED_RECIPE,
                    () -> menu.findMissingIngredients(ingredients));
            helper.assertTrue(allowed.missingSlots().isEmpty(),
                    "ET JEI preview must resolve a stored fluid bucket ingredient");

            var repeated = java.util.Map.of(0, bucket, 1, Ingredient.of(Items.WATER_BUCKET, Items.LAVA_BUCKET));
            var reserved = FluidCraftingTerminalIntegration.withRecipeViewerContext(menu, ALLOWED_RECIPE,
                    () -> menu.findMissingIngredients(repeated));
            helper.assertTrue(reserved.missingSlots().size() == 1,
                    "ET final preview correction must not count the same 1000 mB twice");

            var blocked = FluidCraftingTerminalIntegration.withRecipeViewerContext(menu, BLOCKED_RECIPE,
                    () -> menu.findMissingIngredients(ingredients));
            helper.assertTrue(blocked.missingSlots().contains(0),
                    "ET JEI preview must preserve a blacklisted fluid recipe as missing");

            FluidCraftingConfig.COMMON.QoL.set(false);
            var withoutUpgrade = FluidCraftingTerminalIntegration.withRecipeViewerContext(menu, ALLOWED_RECIPE,
                    () -> menu.findMissingIngredients(ingredients));
            helper.assertTrue(withoutUpgrade.missingSlots().contains(0),
                    "ET JEI preview must not resolve fluids without the required upgrade or QoL setting");
            helper.assertTrue(menu.findMissingIngredients(ingredients).missingSlots().contains(0),
                    "JEI recipe context must be cleared after preview");
            helper.succeed();
        } finally {
            FluidCraftingConfig.COMMON.QoL.set(oldQol);
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(oldBlacklist);
        }
    }

    private static final class FluidRepo implements IClientRepo {
        private final Set<GridInventoryEntry> entries = Set.of(new GridInventoryEntry(
                1L, AEFluidKey.of(Fluids.WATER), AEFluidKey.AMOUNT_BUCKET, 0L, false));

        @Override
        public void handleUpdate(boolean fullUpdate, List<GridInventoryEntry> entries) {
        }

        @Override
        public Set<GridInventoryEntry> getAllEntries() {
            return entries;
        }

        @Override
        public java.util.Collection<GridInventoryEntry> getByIngredient(Ingredient ingredient) {
            // The ordinary item lookup must not pretend the stored water is a real bucket item.
            return Set.of();
        }
    }

    private static final class TestHost extends ItemMenuHost
            implements IPortableTerminal, IETTerminalHost, ISegmentedInventory {
        private final AppEngInternalInventory crafting = new AppEngInternalInventory(null,
                ETMenuType.ET_TERMINAL.getGridSize());
        private final ConfigManager config = new ConfigManager(() -> { });
        private final MEStorage storage = new MEStorage() {
            private final Map<AEKey, Long> values = new HashMap<>();

            @Override
            public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
                if (mode == Actionable.MODULATE) {
                    values.merge(what, amount, Long::sum);
                }
                return amount;
            }

            @Override
            public void getAvailableStacks(appeng.api.stacks.KeyCounter out) {
                values.forEach(out::add);
            }

            @Override
            public Component getDescription() {
                return Component.literal("ET JEI preview test");
            }
        };
        private ETTerminalMode mode = ETTerminalMode.CRAFTING;

        private TestHost(Player player) {
            super(player, null, new ItemStack(CRAFTING_TABLE));
        }

        @Override
        public MEStorage getInventory() {
            return storage;
        }

        @Override
        public InternalInventory getSubInventory(ResourceLocation id) {
            return ETMenuType.ET_TERMINAL.getCraftingInventory().equals(id)
                    ? crafting : InternalInventory.empty();
        }

        @Override
        public double extractAEPower(double amount, Actionable mode, PowerMultiplier multiplier) {
            return amount;
        }

        @Override
        public IConfigManager getConfigManager() {
            return config;
        }

        @Override
        public void returnToMainMenu(Player player, ISubMenu subMenu) {
        }

        @Override
        public ItemStack getMainMenuIcon() {
            return new ItemStack(CRAFTING_TABLE);
        }

        @Override
        public ETTerminalMode getMode() {
            return mode;
        }

        @Override
        public void setMode(ETTerminalMode mode) {
            this.mode = mode;
        }

        @Override
        public ResourceLocation getStoneCutterRecipeId() {
            return null;
        }

        @Override
        public void setStoneCutterRecipeId(ResourceLocation stonecuttingRecipeId) {
        }

        public me.myogoo.myotus.api.experience.ExperienceMath.ExperienceSource
        getRememberedAnvilExperienceSource() {
            return null;
        }

        public void setRememberedAnvilExperienceSource(
                me.myogoo.myotus.api.experience.ExperienceMath.ExperienceSource source) {
        }
    }
}
