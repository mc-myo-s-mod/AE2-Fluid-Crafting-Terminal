package me.myogoo.ae2fct.gametest;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.me.storage.NetworkStorage;
import me.myogoo.ae2fct.util.FluidStorageTransfer;
import me.myogoo.ae2fct.init.AE2FCTDataComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.ArrayList;

final class FluidTransferRegression {
    static void persistentLifecycle(GameTestHelper helper) {
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        var fluid = new FluidStack(Fluids.WATER, 1000);
        fluid.set(DataComponents.CUSTOM_NAME, Component.literal("persistent exact key"));
        var original = new me.myogoo.ae2fct.codec.VirtualFluid(fluid, 1000);
        var ops = helper.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var encoded = me.myogoo.ae2fct.codec.VirtualFluid.CODEC.encodeStart(ops, original).getOrThrow();
        var decoded = me.myogoo.ae2fct.codec.VirtualFluid.CODEC.parse(ops, encoded).getOrThrow();
        check(original.equals(decoded) && original.hashCode() == decoded.hashCode(), "shared NBT codec preserves key/amount/capacity");
        var buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            me.myogoo.ae2fct.codec.VirtualFluid.STREAM_CODEC.encode(buffer, original);
            check(original.equals(me.myogoo.ae2fct.codec.VirtualFluid.STREAM_CODEC.decode(buffer)), "shared stream codec round trip");
        } finally {
            buffer.release();
        }
        var key = AEFluidKey.of(fluid);
        var fixture = new PersistentNetworkFixture(helper, pos, true, null);
        var tank = new Tank(key, 0, 0, 1500);
        fixture.network.mount(0, tank);
        var item = AEItemKey.of(FluidStorageTransfer.createItem(key, 1000));
        var before = fixture.host.getPersistentData().copy();
        int changes = fixture.host.changes;
        check(fixture.network.insert(item, 2, Actionable.SIMULATE, IActionSource.empty()) == 2,
                "physical grid simulation stays generic");
        check(before.equals(fixture.host.getPersistentData()) && changes == fixture.host.changes,
                "simulation must neither allocate saved slots nor dirty the host");
        check(fixture.network.insert(item, 2, Actionable.MODULATE, IActionSource.empty()) == 2
                        && tank.stored == 1500 && fixture.host.changes > changes,
                "actual 1500 insertion consumes two items and dirties its 500 remainder owner");
        var saved = fixture.host.saveWithFullMetadata(helper.getLevel().registryAccess());
        check(saved.equals(fixture.host.saveWithFullMetadata(helper.getLevel().registryAccess())),
                "repeated saves must not consume balances");
        fixture.chunk.clearAllBlockEntities();
        check(recovery(helper, pos) == 0, "actual clearAllBlockEntities unload path must not drop");
        var loaded = new PersistentNetworkFixture(helper, pos, true, saved);
        check(loaded.network.getAvailableStacks().get(key) == 500,
                "inherited ordinary BE load must preserve exact component key and balance");
        loaded.host.loadWithComponents(saved.copy(), helper.getLevel().registryAccess());
        loaded.host.loadWithComponents(saved.copy(), helper.getLevel().registryAccess());
        check(loaded.network.getAvailableStacks().get(key) == 500, "repeat load replaces rather than adds the saved balance");
        check(loaded.network.extract(key, 100, Actionable.MODULATE, IActionSource.empty()) == 100
                        && loaded.network.getAvailableStacks().get(key) == 400,
                "remainders must be ordinary extractable fluid");
        loaded.chunk.removeBlockEntity(loaded.host.getBlockPos());
        var recoveredItems = recoveryItems(helper, pos);
        check(recoveredItems.size() == 1 && AEItemKey.of(recoveredItems.getFirst())
                        .equals(AEItemKey.of(FluidStorageTransfer.createItem(key, 400))),
                "last physical host removal recovers the fraction with its exact key");
        loaded.chunk.removeBlockEntity(loaded.host.getBlockPos());
        check(recovery(helper, pos) == 0, "repeated destruction cannot duplicate a recovery");

        for (int edge = 0; edge < 4; edge++) {
            var pending = new PersistentNetworkFixture(helper, pos, edge != 2, saved);
            if (edge == 2) {
                pending.host.setRemoved();
                pending.chunk.getBlockEntities().remove(pending.host.getBlockPos());
            } else {
                pending.chunk.clearAllBlockEntities();
            }
            if (edge == 0) {
                var dormant = new PersistentNetworkFixture.Host(pending.host.getBlockPos());
                dormant.setLevel(helper.getLevel());
                dormant.loadWithComponents(saved.copy(), helper.getLevel().registryAccess());
                pending.chunk.setBlockEntity(dormant);
                pending.chunk.removeBlockEntity(dormant.getBlockPos());
            } else {
                pending.chunk.setBlockEntityNbt(saved.copy());
                if (edge == 1) pending.chunk.removeBlockEntity(pending.host.getBlockPos());
                if (edge == 2) pending.chunk.setBlockState(pending.host.getBlockPos(),
                        net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), false);
                if (edge == 3) pending.chunk.setBlockEntity(new PersistentNetworkFixture.Host(pending.host.getBlockPos()));
            }
            check(recovery(helper, pos) == 500, "dormant/pending removal edge " + edge + " must recover exactly once");
        }
        var replacement = new PersistentNetworkFixture(helper, pos, true, saved);
        replacement.chunk.setBlockEntity(new PersistentNetworkFixture.Host(replacement.host.getBlockPos()));
        check(recovery(helper, pos) == 500, "direct live BE replacement must retire the displaced owner");
        var recipient = new PersistentNetworkFixture(helper, pos, true, null);
        var receivingTank = new Tank(key, 0, 0, 400);
        recipient.network.mount(0, receivingTank);
        check(recipient.network.insert(AEItemKey.of(recoveredItems.getFirst()), 1,
                        Actionable.MODULATE, IActionSource.empty()) == 1 && receivingTank.stored == 400,
                "the existing fractional virtual item must reinsert with its exact key");
        helper.succeed();
    }

    static void topologyAndReentrancy(GameTestHelper helper) {
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        var key = AEFluidKey.of(Fluids.WATER);
        var item = AEItemKey.of(FluidStorageTransfer.createItem(key, 1000));
        var a = new PersistentNetworkFixture(helper, pos, true, null);
        var b = new PersistentNetworkFixture(helper, new net.minecraft.core.BlockPos(2, 1, 1), true, null);
        for (var fixture : new PersistentNetworkFixture[]{a, b}) {
            var tank = new Tank(key, 0, 0, 500);
            fixture.network.mount(0, tank);
            check(fixture.network.insert(item, 1, Actionable.MODULATE, IActionSource.empty()) == 1,
                    "1000 to 500 insertion must keep the other half");
            fixture.network.unmount(tank);
        }
        for (int i = 0; i < 2; i++) {
            var connection = appeng.api.networking.GridHelper.createConnection(a.main.getNode(), b.main.getNode());
            check(a.main.getGrid().getStorageService().getInventory().getAvailableStacks().get(key) == 1000,
                    "merge must expose existing holders once");
            connection.destroy();
            check(a.main.getGrid().getStorageService().getInventory().getAvailableStacks().get(key) == 500
                            && b.main.getGrid().getStorageService().getInventory().getAvailableStacks().get(key) == 500,
                    "split/remerge must never copy balances");
        }
        var outer = a.host.node("outer");
        var outerInventory = outer.getGrid().getStorageService().getInventory();
        check(outerInventory.getAvailableStacks().get(key) == 0, "outer node must not mount the main balance");
        var outerTank = new Tank(key, 0, 0, 500);
        ((NetworkStorage) outerInventory).mount(0, outerTank);
        check(outerInventory.insert(item, 1, Actionable.MODULATE, IActionSource.empty()) == 1, "outer has an independent slot");
        var duplicate = a.host.node("main");
        check(duplicate.getGrid().getStorageService().getInventory().getAvailableStacks().get(key) == 0,
                "duplicate live tag cannot expose a second handle");
        a.chunk.removeBlockEntity(a.host.getBlockPos());
        check(recovery(helper, pos) == 1000, "main and outer balances retire independently, duplicate main retires nothing");

        var splitA = new PersistentNetworkFixture(helper, pos, true, null);
        var splitB = new PersistentNetworkFixture(helper, new net.minecraft.core.BlockPos(2, 1, 1), true, null);
        var link = appeng.api.networking.GridHelper.createConnection(splitA.main.getNode(), splitB.main.getNode());
        var mergingInventory = (NetworkStorage) splitA.main.getGrid().getStorageService().getInventory();
        var splittingTank = new Tank(key, 0, 0, 500);
        mergingInventory.mount(0, splittingTank);
        splittingTank.afterMutation = link::destroy;
        check(mergingInventory.insert(item, 1, Actionable.MODULATE, IActionSource.empty()) == 1,
                "reentrant split must finish its pinned transfer");
        mergingInventory.unmount(splittingTank);
        check(splitA.main.getGrid().getStorageService().getInventory().getAvailableStacks().get(key)
                        + splitB.main.getGrid().getStorageService().getInventory().getAvailableStacks().get(key) == 500,
                "a pin moves with its physical node across a reentrant split without copying");
        var fraction = new PersistentNetworkFixture(helper, pos, true, null);
        var limited = new Tank(key, 2000, 500, 0);
        fraction.network.mount(0, limited);
        check(fraction.network.extract(item, 1, Actionable.MODULATE, IActionSource.empty()) == 0,
                "500 actual extraction must not invent a whole item");
        check(fraction.network.extract(item, 1, Actionable.MODULATE, IActionSource.empty()) == 1
                        && limited.stored == 1000 && fraction.network.getAvailableStacks().get(key) == 1000,
                "two 500 extractions combine without requiring a refund");

        var occupied = new PersistentNetworkFixture(helper, pos, true, null);
        var water = new Tank(key, 0, 0, 500);
        var lavaKey = AEFluidKey.of(Fluids.LAVA);
        var lava = new Tank(lavaKey, 0, 0, 500);
        occupied.network.mount(0, water);
        occupied.network.mount(0, lava);
        occupied.network.insert(item, 1, Actionable.MODULATE, IActionSource.empty());
        check(occupied.network.insert(AEItemKey.of(FluidStorageTransfer.createItem(lavaKey, 1000)), 1,
                        Actionable.MODULATE, IActionSource.empty()) == 0 && lava.stored == 0,
                "a different key cannot overwrite a full one-key slot or consume the caller item");

        for (boolean insertion : new boolean[]{true, false}) {
            var dying = new PersistentNetworkFixture(helper, pos, true, null);
            var tank = new Tank(key, insertion ? 0 : 2000, 1500, 1500);
            dying.network.mount(0, tank);
            tank.afterMutation = () -> dying.chunk.removeBlockEntity(dying.host.getBlockPos());
            long result = insertion ? dying.network.insert(item, 2, Actionable.MODULATE, IActionSource.empty())
                    : dying.network.extract(item, 2, Actionable.MODULATE, IActionSource.empty());
            check(result == (insertion ? 2 : 1) && recovery(helper, pos) == 500,
                    "reentrant destruction must wait for actual " + (insertion ? "insertion" : "extraction"));
            check(recovery(helper, pos) == 0, "in-flight recovery must be single use");
        }
        helper.succeed();
    }

    private static long recovery(GameTestHelper helper, net.minecraft.core.BlockPos relative) {
        return recoveryItems(helper, relative).stream().mapToLong(FluidTransferRegression::amount).sum();
    }

    private static java.util.List<ItemStack> recoveryItems(GameTestHelper helper, net.minecraft.core.BlockPos relative) {
        var items = new ArrayList<ItemStack>();
        var box = new net.minecraft.world.phys.AABB(helper.absolutePos(relative)).inflate(0.25);
        for (var entity : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
            long fluid = amount(entity.getItem());
            if (fluid > 0) {
                items.add(entity.getItem().copy());
                entity.discard();
            }
        }
        return items;
    }

    static void partLifecycle(GameTestHelper helper) {
        var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        var side = net.minecraft.core.Direction.NORTH;
        var key = AEFluidKey.of(Fluids.WATER);
        var item = AEItemKey.of(FluidStorageTransfer.createItem(key, 1000));
        for (boolean reading : new boolean[]{false, true}) {
            helper.setBlock(pos, appeng.core.definitions.AEBlocks.CABLE_BUS.block());
            var host = (appeng.blockentity.networking.CableBusBlockEntity) helper.getBlockEntity(pos);
            var part = host.addPart(appeng.core.definitions.AEParts.INTERFACE.get(), side, null);
            host.getCableBus().addToWorld();
            var main = (NetworkStorage) part.getGridNode().getGrid().getStorageService().getInventory();
            var tank = new Tank(key, 0, 0, 500);
            main.mount(0, tank);
            check(main.insert(item, 1, Actionable.MODULATE, IActionSource.empty()) == 1,
                    "part main node must acquire its physical side slot");
            var outer = appeng.api.networking.GridHelper.createManagedNode(part,
                    (owner, node) -> host.setChanged()).setTagName("test_outer");
            outer.create(helper.getLevel(), helper.absolutePos(pos));
            var outerNetwork = (NetworkStorage) outer.getGrid().getStorageService().getInventory();
            check(outerNetwork.getAvailableStacks().get(key) == 0, "part outer must not copy main balance");
            var outerTank = new Tank(key, 0, 0, 500);
            outerNetwork.mount(0, outerTank);
            outerNetwork.insert(item, 1, Actionable.MODULATE, IActionSource.empty());
            if (reading) {
                host.getCableBus().removeFromWorld();
                host.getCableBus().readFromNBT(new net.minecraft.nbt.CompoundTag(), helper.getLevel().registryAccess());
                check(recovery(helper, pos) == 0, "NBT reconstruction must not eject part balances");
            } else {
                host.removePartFromSide(side);
                check(recovery(helper, pos) == 1000, "part removal must retire all main/outer tags for that side");
            }
            helper.getLevel().getChunkAt(helper.absolutePos(pos)).removeBlockEntity(helper.absolutePos(pos));
            check(recovery(helper, pos) == (reading ? 1000 : 0),
                    "whole block removal consumes dormant balances and never repeats part recovery");
            outer.destroy();
            helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
        }

        helper.setBlock(pos, appeng.core.definitions.AEBlocks.CABLE_BUS.block());
        var host = (appeng.blockentity.networking.CableBusBlockEntity) helper.getBlockEntity(pos);
        var part = host.addPart(appeng.core.definitions.AEParts.INTERFACE.get(), side, null);
        host.getCableBus().addToWorld();
        var oldNetwork = (NetworkStorage) part.getGridNode().getGrid().getStorageService().getInventory();
        var oldTank = new Tank(key, 0, 0, 500);
        oldNetwork.mount(0, oldTank);
        NetworkStorage[] replacement = {null};
        oldTank.afterMutation = () -> {
            var next = host.replacePart(appeng.core.definitions.AEParts.INTERFACE.get(), side, null, null);
            replacement[0] = (NetworkStorage) next.getGridNode().getGrid().getStorageService().getInventory();
            replacement[0].mount(0, new Tank(key, 0, 0, 500));
            check(replacement[0].insert(item, 1, Actionable.MODULATE, IActionSource.empty()) == 1,
                    "replacement part has its own reservation");
        };
        check(oldNetwork.insert(item, 1, Actionable.MODULATE, IActionSource.empty()) == 1,
                "old transaction must finish its actual accounting");
        check(recovery(helper, pos) == 500 && replacement[0].getAvailableStacks().get(key) == 1000,
                "retired pin must not erase or duplicate replacement part balance");
        helper.getLevel().getChunkAt(helper.absolutePos(pos)).removeBlockEntity(helper.absolutePos(pos));
        check(recovery(helper, pos) == 500, "replacement part persisted its own balance");
        helper.succeed();
    }

    static void networkAmountBounds(GameTestHelper helper) {
        var key = AEFluidKey.of(Fluids.WATER);
        var tank = new Tank(key, Long.MAX_VALUE, 0, 0);
        var network = PersistentNetworkFixture.network(helper);
        network.mount(0, tank);
        var item = AEItemKey.of(FluidStorageTransfer.createItem(key, 1000));
        long largestSafeCount = Long.MAX_VALUE / 1000;
        for (long count : new long[]{0, -1, Long.MIN_VALUE}) {
            for (var mode : Actionable.values()) {
                check(network.insert(item, count, mode, IActionSource.empty()) == 0,
                        "nonpositive insertion must return zero");
                check(network.extract(item, count, mode, IActionSource.empty()) == 0,
                        "nonpositive extraction must return zero");
            }
        }
        check(tank.calls == 0 && tank.stored == Long.MAX_VALUE,
                "invalid item counts must be rejected before querying or mutating a backend");
        for (long count : new long[]{largestSafeCount, largestSafeCount + 1, Long.MAX_VALUE}) {
            check(network.insert(item, count, Actionable.SIMULATE, IActionSource.empty()) == largestSafeCount,
                    "large insertion requests must be bounded without disabling insert-all callers");
            check(network.extract(item, count, Actionable.SIMULATE, IActionSource.empty()) == largestSafeCount
                            && tank.stored == Long.MAX_VALUE,
                    "large extraction simulations must remain supported and read-only");
        }
        tank = new Tank(key, 0, 0, Long.MAX_VALUE);
        network = PersistentNetworkFixture.network(helper);
        network.mount(0, tank);
        check(network.insert(item, Long.MAX_VALUE, Actionable.MODULATE, IActionSource.empty()) == largestSafeCount
                        && tank.stored == largestSafeCount * 1000,
                "large full insertion must report only the bounded actual whole-item count");
        tank = new Tank(key, largestSafeCount * 1000, Long.MAX_VALUE, 0);
        network = PersistentNetworkFixture.network(helper);
        network.mount(0, tank);
        check(network.extract(item, Long.MAX_VALUE, Actionable.MODULATE, IActionSource.empty()) == largestSafeCount
                        && tank.stored == 0,
                "large full extraction must not overflow or invent items");
        helper.succeed();
    }


    static void partialNetworkExtraction(GameTestHelper helper) {
        var key = AEFluidKey.of(Fluids.WATER);
        var tank = new Tank(key, 2000, 1500, 0);
        var network = PersistentNetworkFixture.network(helper);
        network.mount(0, tank);
        var item = AEItemKey.of(FluidStorageTransfer.createItem(key, 1000));
        check(network.extract(item, 2, Actionable.SIMULATE, IActionSource.empty()) == 2 && tank.stored == 2000,
                "simulation must not move any fluid");
        long extracted = network.extract(item, 2, Actionable.MODULATE, IActionSource.empty());
        check(extracted == 1, "1500 mB must not be reported as two whole items");
        check(network.getAvailableStacks().get(key) + extracted * 1000 == 2000,
                "a refused 500 mB rollback must remain owned and extractable");
        helper.succeed();
    }

    static void partialNetworkInsertion(GameTestHelper helper) {
        var key = AEFluidKey.of(Fluids.WATER);
        var tank = new Tank(key, 0, 0, 1500);
        var network = PersistentNetworkFixture.network(helper);
        network.mount(0, tank);
        var item = AEItemKey.of(FluidStorageTransfer.createItem(key, 1000));
        check(network.insert(item, 2, Actionable.SIMULATE, IActionSource.empty()) == 2 && tank.stored == 0,
                "simulation must not insert any fluid");
        long inserted = network.insert(item, 2, Actionable.MODULATE, IActionSource.empty());
        check(inserted >= 0 && inserted <= 2, "insertion return must stay within the offered item count");
        check(network.getAvailableStacks().get(key) + (2 - inserted) * 1000 == 2000,
                "partial insertion with refused rollback must retain every offered mB");
        helper.succeed();
    }

    @SuppressWarnings("deprecation")
    private static void legacyExtractionHelpers(GameTestHelper helper) {
        var key = AEFluidKey.of(Fluids.WATER);
        IEnergySource power = (amount, mode, multiplier) -> amount;
        for (int actual : new int[]{0, 500, 1000}) {
            var tank = new Tank(key, 1000, actual, 0);
            var template = new ItemStack(net.minecraft.world.item.Items.WATER_BUCKET, 2);
            template.set(DataComponents.CUSTOM_NAME, Component.literal("legacy template"));
            var result = me.myogoo.ae2fct.util.FluidCraftingHelper.tryExtractFluidForTemplate(
                    template, tank, power, IActionSource.empty());
            check(tank.stored + amount(result) == 1000 && amount(result) == actual,
                    "legacy template extraction must return every actual mB, including partial/refused results");
            check(template.getCount() == 2 && !template.has(AE2FCTDataComponent.VIRTUAL_FLUID),
                    "legacy extraction must not mutate the provided template");
            if (actual > 0) {
                check(result.getCount() == 1 && result.get(AE2FCTDataComponent.VIRTUAL_FLUID).capacity() == 1000
                                && template.get(DataComponents.CUSTOM_NAME).equals(result.get(DataComponents.CUSTOM_NAME)),
                        "legacy results must preserve template data and requested capacity");
            }
            tank = new Tank(key, 1000, actual, 0);
            result = me.myogoo.ae2fct.util.FluidCraftingHelper.tryExtractFluidForIngredient(
                    net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.WATER_BUCKET),
                    tank, power, IActionSource.empty());
            check(tank.stored + amount(result) == 1000 && amount(result) == actual,
                    "legacy ingredient extraction must preserve partial/refused/full results");
        }
    }
    static void run(GameTestHelper helper) {
        legacyExtractionHelpers(helper);
        if (net.neoforged.fml.ModList.get().isLoaded("extendedterminal")) EtPreviewRegression.run(helper);
        var key = AEFluidKey.of(Fluids.WATER);
        IEnergySource power = (amount, mode, multiplier) -> amount;
        var emptySource = IActionSource.empty();
        var tank = new Tank(key, 1000, 500, 0);
        var carried = FluidStorageTransfer.extractToItem(tank, power, key, 1000, emptySource);
        check(tank.stored == 500 && amount(carried) == 500, "partial click must retain all extracted fluid");
        FluidStorageTransfer.returnToStorage(carried, 1, tank, power, emptySource,
                remainder -> { throw new AssertionError("single cursor owns its remainder"); });
        check(tank.stored == 500 && amount(carried) == 500,
                "refused refund must leave the actual extracted amount on the cursor");
        check(!net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.WATER_BUCKET).test(carried),
                "partial cursor must not become a craftable bucket");
        tank = new Tank(key, 999, 500, 0);
        check(FluidStorageTransfer.extractToItem(tank, power, key, 1000, emptySource).isEmpty()
                && tank.stored == 999, "insufficient simulated stock must not be touched");
        int[] simulations = {0};
        IEnergySource fadingPower = (requested, mode, multiplier) ->
                mode == Actionable.SIMULATE && ++simulations[0] > 1 ? requested / 2 : requested;
        tank = new Tank(key, 1000, 1000, 0);
        carried = FluidStorageTransfer.extractToItem(tank, fadingPower, key, 1000, emptySource);
        check(amount(carried) == 500 && tank.stored == 500, "power-limited extraction must preserve its actual result");

        for (int count : new int[]{1, 2}) {
            tank = new Tank(key, 0, 0, 500);
            var stack = FluidStorageTransfer.createItem(key, 1000);
            stack.setCount(count);
            var leftovers = new ArrayList<ItemStack>();
            FluidStorageTransfer.returnToStorage(stack, count, tank, power, emptySource, leftovers::add);
            check(tank.stored == 500 && tank.stored + amount(stack)
                            + leftovers.stream().mapToLong(FluidTransferRegression::amount).sum() == count * 1000,
                    "owned insertion must retain its fractional remainder for single and stacked items");
            tank.insertLimit = 0;
            long before = amount(stack);
            FluidStorageTransfer.returnToStorage(stack, count, tank, power, emptySource, leftovers::add);
            check(amount(stack) == before, "rejected owned insertion must not consume its item");
        }
        var a = new FluidStack(Fluids.WATER, 1000);
        var b = new FluidStack(Fluids.WATER, 1000);
        a.set(DataComponents.CUSTOM_NAME, Component.literal("A"));
        b.set(DataComponents.CUSTOM_NAME, Component.literal("B"));
        var aKey = AEFluidKey.of(a);
        var aItem = FluidStorageTransfer.createItem(aKey, 1000);
        var sameItem = FluidStorageTransfer.createItem(AEFluidKey.of(a.copy()), 1000);
        var bItem = FluidStorageTransfer.createItem(AEFluidKey.of(b), 1000);
        check(AEItemKey.of(aItem).equals(AEItemKey.of(sameItem))
                        && AEItemKey.of(aItem).hashCode() == AEItemKey.of(sameItem).hashCode(),
                "independent identical fluid contents must produce equal item keys");
        check(!AEItemKey.of(aItem).equals(AEItemKey.of(bItem)), "component variants must stay separate");
        check(!AEItemKey.of(aItem).equals(AEItemKey.of(FluidStorageTransfer.createItem(aKey, 500))),
                "different fluid amounts must stay separate");
        tank = new Tank(aKey, 1000, 1000, 1000);
        carried = FluidStorageTransfer.extractToItem(tank, power, aKey, 1000, emptySource);
        check(aKey.equals(AEFluidKey.of(carried.get(AE2FCTDataComponent.VIRTUAL_FLUID).fluid())), "click must preserve the original fluid key");
        FluidStorageTransfer.returnToStorage(carried, 1, tank, power, emptySource,
                remainder -> { throw new AssertionError("full insertion has no remainder"); });
        check(carried.isEmpty() && tank.stored == 1000, "component key must survive the owned round trip");
        var network = PersistentNetworkFixture.network(helper);
        network.mount(0, tank);
        var itemKey = AEItemKey.of(aItem);
        check(network.extract(itemKey, 1, Actionable.SIMULATE, emptySource) == 1 && tank.stored == 1000,
                "ownerless simulation must preserve component fluid");
        check(network.extract(itemKey, 1, Actionable.MODULATE, emptySource) == 1 && tank.stored == 0,
                "ownerless full extraction must preserve component fluid");
        check(network.insert(itemKey, 1, Actionable.SIMULATE, emptySource) == 1 && tank.stored == 0,
                "multi-mB virtual item insertion must remain supported");
        check(network.insert(itemKey, 1, Actionable.MODULATE, emptySource) == 1 && tank.stored == 1000,
                "ownerless full insertion must restore the exact component key");
    }

    private static long amount(ItemStack stack) {
        var fluid = stack.get(AE2FCTDataComponent.VIRTUAL_FLUID);
        return stack.isEmpty() || fluid == null ? 0 : (long) fluid.fluid().getAmount() * stack.getCount();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    static final class Tank implements MEStorage {
        private final AEFluidKey key;
        private long stored;
        private final long extractLimit;
        private long insertLimit;
        private int calls;
        private Runnable afterMutation = () -> {};

        Tank(AEFluidKey key, long stored, long extractLimit, long insertLimit) {
            this.key = key;
            this.stored = stored;
            this.extractLimit = extractLimit;
            this.insertLimit = insertLimit;
        }

        @Override
        public Component getDescription() {
            return Component.literal("Simulation is not modulation");
        }

        @Override
        public void getAvailableStacks(KeyCounter out) {
            out.add(key, stored);
        }

        @Override
        public long extract(AEKey key, long amount, Actionable mode, IActionSource source) {
            calls++;
            check(amount >= 0, "overflowed extraction");
            if (!this.key.equals(key)) return 0;
            long moved = Math.min(stored, amount);
            if (mode == Actionable.MODULATE) {
                moved = Math.min(moved, extractLimit);
                stored -= moved;
                var callback = afterMutation;
                afterMutation = () -> {};
                callback.run();
            }
            return moved;
        }

        @Override
        public long insert(AEKey key, long amount, Actionable mode, IActionSource source) {
            calls++;
            check(amount >= 0, "overflowed insertion");
            if (!this.key.equals(key)) return 0;
            if (mode == Actionable.SIMULATE) return amount;
            long moved = Math.min(amount, insertLimit);
            stored += moved;
            var callback = afterMutation;
            afterMutation = () -> {};
            callback.run();
            return moved;
        }
    }
}
