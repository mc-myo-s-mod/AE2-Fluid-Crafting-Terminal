package me.myogoo.ae2fct.gametest;

import java.util.ArrayList;
import java.util.List;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.IManagedGridNode;
import appeng.me.storage.NetworkStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/** An ordinary external BE, with inherited loader persistence and real AE2 services. */
final class PersistentNetworkFixture {
    final Host host;
    final IManagedGridNode main;
    final NetworkStorage network;
    final LevelChunk chunk;

    PersistentNetworkFixture(GameTestHelper helper, BlockPos relative, boolean isolated, CompoundTag saved) {
        var pos = helper.absolutePos(relative);
        if (isolated) {
            chunk = new LevelChunk(helper.getLevel(), new ChunkPos(pos));
            chunk.setLoaded(true);
            chunk.getSections()[chunk.getSectionIndex(pos.getY())]
                    .setBlockState(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, Blocks.CHEST.defaultBlockState());
        } else {
            helper.setBlock(relative, Blocks.CHEST);
            chunk = helper.getLevel().getChunkAt(pos);
        }
        host = new Host(pos);
        host.setLevel(helper.getLevel());
        // Loader load retains the tag; a separate reload must receive a fresh snapshot.
        if (saved != null) host.loadWithComponents(saved.copy(), helper.getLevel().registryAccess());
        chunk.setBlockEntity(host);
        main = host.node("main");
        network = (NetworkStorage) main.getGrid().getStorageService().getInventory();
    }

    static NetworkStorage network(GameTestHelper helper) {
        return new PersistentNetworkFixture(helper, new BlockPos(1, 1, 1), false, null).network;
    }

    static final class Host extends ChestBlockEntity implements IInWorldGridNodeHost {
        final List<IManagedGridNode> nodes = new ArrayList<>();
        int changes;

        Host(BlockPos pos) {
            super(pos, Blocks.CHEST.defaultBlockState());
        }

        IManagedGridNode node(String tag) {
            var node = GridHelper.createManagedNode(this, (owner, gridNode) -> owner.setChanged()).setTagName(tag);
            nodes.add(node);
            node.create(getLevel(), getBlockPos());
            return node;
        }

        @Override
        public IGridNode getGridNode(Direction side) {
            return nodes.isEmpty() ? null : nodes.getFirst().getNode();
        }

        @Override
        public void setChanged() {
            changes++;
            super.setChanged();
        }

        @Override
        public void setRemoved() {
            nodes.forEach(IManagedGridNode::destroy);
            super.setRemoved();
        }
    }
}
