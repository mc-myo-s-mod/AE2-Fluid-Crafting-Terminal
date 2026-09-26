package me.myogoo.ae2fct.gametest;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.me.common.IClientRepo;
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
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import me.myogoo.ae2fct.integration.FluidCraftingTerminalIntegration;
import me.myogoo.extendedterminal.api.host.IETTerminalHost;
import me.myogoo.extendedterminal.menu.ETMenuType;
import me.myogoo.extendedterminal.menu.extendedterminal.ETTerminalMode;
import me.myogoo.extendedterminal.menu.extendedterminal.ETTerminalMenu;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

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
