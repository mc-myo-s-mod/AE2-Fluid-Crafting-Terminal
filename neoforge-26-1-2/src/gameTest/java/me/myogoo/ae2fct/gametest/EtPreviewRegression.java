package me.myogoo.ae2fct.gametest;

import appeng.api.parts.IPartItem;
import appeng.api.stacks.AEFluidKey;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.me.common.IClientRepo;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.items.CraftingTermMenu;
import me.myogoo.ae2fct.config.FluidCraftingConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class EtPreviewRegression {
    static void run(GameTestHelper helper) {
        var pos = new BlockPos(1, 1, 1);
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        var saved = new java.util.ArrayList<ItemStack>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            saved.add(player.getInventory().getItem(slot).copy());
            player.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        boolean qol = FluidCraftingConfig.COMMON.QoL.get();
        var blacklist = FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.get();
        try {
            FluidCraftingConfig.COMMON.QoL.set(true);
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(List.of());
            helper.setBlock(pos, AEBlocks.CABLE_BUS.block());
            var host = (CableBusBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            var partItem = (IPartItem<?>) BuiltInRegistries.ITEM.get(net.minecraft.resources.Identifier.fromNamespaceAndPath("extendedterminal", "et_terminal")).orElseThrow().value();
            var part = host.addPart(partItem, Direction.NORTH, null);
            host.getCableBus().addToWorld();
            // Resolve the optional ET constructor only after the loaded-mod guard in the caller.
            var menuClass = Class.forName("me.myogoo.extendedterminal.menu.extendedterminal.ETTerminalMenu");
            var hostClass = Class.forName("me.myogoo.extendedterminal.api.host.IETTerminalHost");
            var menu = (MEStorageMenu) menuClass.getConstructor(net.minecraft.world.inventory.MenuType.class,
                    int.class, net.minecraft.world.entity.player.Inventory.class, hostClass)
                    .newInstance(menuClass.getField("TYPE").get(null), 14, player.getInventory(), part);
            var ingredients = Map.of(0, Ingredient.of(Items.WATER_BUCKET),
                    1, Ingredient.of(Items.WATER_BUCKET, Items.LAVA_BUCKET));
            var recipeId = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, net.minecraft.resources.Identifier.fromNamespaceAndPath("ae2fct", "gametest/et_preview"));
            for (long amount : new long[]{1000, 2000}) {
                menu.setClientRepo(new FluidRepo(amount));
                var result = me.myogoo.ae2fct.util.VirtualFluidRecipePolicy.withRecipe(menu, recipeId, () -> preview(menu, ingredients));
                if (result.missingSlots().size() != (amount == 1000 ? 1 : 0)
                        || !result.craftableSlots().isEmpty()) {
                    throw new AssertionError("ET must reserve stored water only once across two bucket slots");
                }
            }
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(List.of(recipeId.identifier().toString()));
            if (me.myogoo.ae2fct.util.VirtualFluidRecipePolicy.withRecipe(menu, recipeId, () -> preview(menu, ingredients)).missingSlots().size() != 2) {
                throw new AssertionError("ET must preserve both missing slots for a blacklisted recipe");
            }
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(List.of());
            FluidCraftingConfig.COMMON.QoL.set(false);
            if (me.myogoo.ae2fct.util.VirtualFluidRecipePolicy.withRecipe(menu, recipeId, () -> preview(menu, ingredients)).missingSlots().size() != 2) {
                throw new AssertionError("ET must require the fluid card or QoL setting");
            }
            System.out.println("FCT ET preview assertions passed");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("ET preview must use the actual registered part and menu", e);
        } finally {
            FluidCraftingConfig.COMMON.QoL.set(qol);
            FluidCraftingConfig.COMMON.virtualFluidRecipeBlacklist.set(blacklist);
            for (int slot = 0; slot < saved.size(); slot++) player.getInventory().setItem(slot, saved.get(slot));
            helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
        }
    }

    private static CraftingTermMenu.MissingIngredientSlots preview(MEStorageMenu menu, Map<Integer, Ingredient> ingredients) {
        try {
            return (CraftingTermMenu.MissingIngredientSlots) menu.getClass()
                    .getMethod("findMissingIngredients", Map.class).invoke(menu, ingredients);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("ET preview invocation failed", e);
        }
    }

    private record FluidRepo(long stored) implements IClientRepo {
        public void handleUpdate(boolean full, List<GridInventoryEntry> entries) { }
        public Set<GridInventoryEntry> getAllEntries() {
            return Set.of(new GridInventoryEntry(1, AEFluidKey.of(Fluids.WATER), stored, 0, false));
        }
        public Collection<GridInventoryEntry> getByIngredient(Ingredient ingredient) { return List.of(); }
    }
}
