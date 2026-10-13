package me.myogoo.ae2fct.gametest;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.networking.EnergyCellBlockEntity;
import appeng.core.definitions.AEBlocks;
import me.myogoo.ae2fct.util.FluidStorageTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import org.slf4j.LoggerFactory;

/** Run write, then read in a second JVM using the same isolated GameTest world. */
public final class PersistentWorldGameTest {
    private static final BlockPos POS = new BlockPos(65536, 70, 65536);

    public static void realChunkAndRestart(GameTestHelper helper) {
        var level = helper.getLevel();
        var fluid = new FluidStack(Fluids.WATER, 1000);
        fluid.set(DataComponents.CUSTOM_NAME, Component.literal("FCT restart exact key"));
        var key = AEFluidKey.of(fluid);
        boolean write = "write".equals(System.getProperty("ae2fct.persistenceSmoke"));
        EnergyCellBlockEntity[] previous = {null};
        level.setChunkForced(POS.getX() >> 4, POS.getZ() >> 4, true);
        if (write) {
            helper.assertTrue(level.getBlockState(POS).isAir(), "write requires a fresh isolated fixture location");
            level.setBlockAndUpdate(POS, AEBlocks.ENERGY_CELL.block().defaultBlockState());
        }
        var sequence = helper.startSequence().thenWaitUntil(() -> {
            helper.assertTrue(level.getBlockEntity(POS) instanceof EnergyCellBlockEntity,
                    "registered physical owner must survive disk load");
            var cell = (EnergyCellBlockEntity) level.getBlockEntity(POS);
            helper.assertTrue(cell.getMainNode().getNode() != null, "physical node must become ready");
        });
        if (write) {
            sequence.thenExecute(() -> {
                var cell = (EnergyCellBlockEntity) level.getBlockEntity(POS);
                previous[0] = cell;
                var inventory = cell.getMainNode().getGrid().getStorageService().getInventory();
                var tank = new FluidTransferRegression.Tank(key, 0, 0, 1500);
                ((appeng.me.storage.NetworkStorage) inventory).mount(0, tank);
                helper.assertTrue(inventory.insert(AEItemKey.of(FluidStorageTransfer.createItem(key, 1000)),
                        2, Actionable.MODULATE, IActionSource.empty()) == 2,
                        "two virtual items must be accounted for");
                helper.assertTrue(tank.getAvailableStacks().get(key) == 1500
                                && inventory.getAvailableStacks().get(key) == 2000,
                        "physical owner must hold the remaining 500 mB");
                level.setChunkForced(POS.getX() >> 4, POS.getZ() >> 4, false);
            }).thenWaitUntil(() -> helper.assertTrue(previous[0].isRemoved()
                            && level.getChunkSource().getChunkNow(POS.getX() >> 4, POS.getZ() >> 4) == null,
                    "real chunk manager must unload the physical owner"))
                    .thenExecute(() -> level.setChunkForced(POS.getX() >> 4, POS.getZ() >> 4, true));
        }
        sequence.thenWaitUntil(() -> {
            var cell = level.getBlockEntity(POS);
            helper.assertTrue(cell instanceof EnergyCellBlockEntity
                            && ((EnergyCellBlockEntity) cell).getMainNode().getNode() != null,
                    "reloaded registered owner must recreate its node");
        }).thenExecuteAfter(5, () -> {
            var cell = (EnergyCellBlockEntity) level.getBlockEntity(POS);
            helper.assertTrue(cell != previous[0], "reload must create a new physical owner");
            var inventory = cell.getMainNode().getGrid().getStorageService().getInventory();
            helper.assertTrue(inventory.getAvailableStacks().get(key) == 500,
                    "exact component remainder must survive unload and disk restart");
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(POS).inflate(2)).isEmpty(),
                    "unloading or restarting must not eject a duplicate item");
            if (!write) {
                helper.assertTrue(inventory.extract(key, 500, Actionable.MODULATE, IActionSource.empty()) == 500
                                && inventory.getAvailableStacks().get(key) == 0,
                        "restarted balance must be recoverable exactly once");
                level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
            }
            // Finish unloading while the GameTest server is still ticking normally.
            previous[0] = cell;
            level.setChunkForced(POS.getX() >> 4, POS.getZ() >> 4, false);
        }).thenWaitUntil(() -> helper.assertTrue(previous[0].isRemoved() &&
                level.getChunkSource().getChunkNow(POS.getX() >> 4, POS.getZ() >> 4) == null,
                "fixture chunk must unload before server shutdown")).thenExecute(() -> {
            LoggerFactory.getLogger(PersistentWorldGameTest.class).info("FCT_PERSISTENCE_PASS phase={}", write ? "write" : "read");
        }).thenSucceed();
    }
}
