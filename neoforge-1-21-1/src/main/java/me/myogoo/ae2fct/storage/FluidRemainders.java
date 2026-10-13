package me.myogoo.ae2fct.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import appeng.api.networking.IGridNode;
import appeng.api.parts.IPart;
import appeng.api.parts.PartHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import me.myogoo.ae2fct.util.FluidStorageTransfer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Physical ownership survives node removal, grid splits and chunk unloading. */
public final class FluidRemainders {
    public static final String TAG = "ae2fct:remainders";
    private final BlockEntity host;
    private final Map<String, Slot> slots = new HashMap<>();
    private boolean retired;

    public FluidRemainders(BlockEntity host) {
        this.host = host;
    }

    public static void bind(IGridNode node, Level level, BlockPos pos, String tagName) {
        if (level == null || level.isClientSide()) return;
        if (node.getOwner() instanceof BlockEntity host) {
            bind(node, host, "block", tagName);
        } else if (node.getOwner() instanceof IPart part && pos != null) {
            var partHost = PartHelper.getPartHost(level, pos);
            if (partHost == null) return;
            if (partHost.getPart(null) == part) {
                bind(node, partHost.getBlockEntity(), "center", tagName);
            } else {
                for (var side : Direction.values()) {
                    if (partHost.getPart(side) == part) {
                        bind(node, partHost.getBlockEntity(), side.getName(), tagName);
                        break;
                    }
                }
            }
        }
    }

    private static void bind(IGridNode node, BlockEntity host, String scope, String tagName) {
        var owner = ((RemainderAccess.Host) host).ae2fct$remainders();
        if (owner.retired || host.isRemoved()) return;
        var slot = owner.slot(scope + ":" + tagName);
        // A live duplicate tag must never expose the same balance to two grids.
        if (slot.node != null && slot.node != node && slot.service != null) return;
        slot.node = node;
        ((RemainderAccess.Node) node).ae2fct$slot(slot);
    }

    private Slot slot(String id) {
        return slots.computeIfAbsent(id, Slot::new);
    }

    public static boolean hasSavedBalance(CompoundTag tag) {
        return tag != null && tag.getCompound("NeoForgeData").contains(TAG);
    }

    public void reload() {
        for (var slot : slots.values()) {
            if (!slot.pinned && !slot.retired) {
                slot.read();
                if (slot.service != null) slot.service.changed();
            }
        }
    }

    public void retire(String scope) {
        if (host.getLevel() == null || host.getLevel().isClientSide()) return;
        if (scope == null) retired = true;
        var saved = host.getPersistentData().getCompound(TAG);
        for (var id : saved.getAllKeys()) slot(id);
        for (var slot : new ArrayList<>(slots.values())) {
            if (scope == null || slot.id.startsWith(scope + ":")) {
                slots.remove(slot.id);
                slot.retired = true;
                slot.persist(true);
                if (!slot.pinned) slot.recover();
            }
        }
    }

    public final class Slot {
        private final String id;
        private AEFluidKey key;
        private long amount;
        IGridNode node;
        FluidRemainderService service;
        private boolean pinned;
        private boolean retired;

        private Slot(String id) {
            this.id = id;
            read();
        }

        private void read() {
            key = null;
            amount = 0;
            var saved = host.getPersistentData().getCompound(TAG).getCompound(id);
            if (!saved.isEmpty()) {
                var stack = GenericStack.readTag(host.getLevel().registryAccess(), saved);
                if (stack == null || !(stack.what() instanceof AEFluidKey) || stack.amount() <= 0
                        || stack.amount() > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Invalid FCT remainder at " + host.getBlockPos());
                }
                key = (AEFluidKey) stack.what();
                amount = stack.amount();
            }
        }

        boolean available() {
            return !retired && !pinned && !host.isRemoved();
        }

        AEFluidKey key() {
            return key;
        }

        long amount() {
            return amount;
        }

        boolean reserve(AEFluidKey key, int unit, boolean insertion) {
            if (!available() || (amount != 0 && !key.equals(this.key))) return false;
            if (insertion && amount > Integer.MAX_VALUE - (long) unit + 1) return false;
            pinned = true;
            return true;
        }

        void set(AEFluidKey key, long amount) {
            if (amount < 0 || amount > Integer.MAX_VALUE) throw new IllegalArgumentException("FCT remainder capacity");
            this.key = amount == 0 ? null : key;
            this.amount = amount;
            persist(false);
        }

        private void persist(boolean retiring) {
            // A retired in-flight part must not erase a replacement part's new slot.
            if (retired && !retiring) return;
            var data = host.getPersistentData();
            var saved = data.getCompound(TAG);
            if (retired || amount == 0) {
                saved.remove(id);
            } else {
                saved.put(id, GenericStack.writeTag(host.getLevel().registryAccess(), new GenericStack(key, amount)));
            }
            if (saved.isEmpty()) data.remove(TAG);
            else data.put(TAG, saved);
            host.setChanged();
            if (service != null) service.changed();
        }

        void release() {
            pinned = false;
            if (retired) recover();
        }

        private void recover() {
            if (amount == 0) return;
            var stack = FluidStorageTransfer.createItem(key, (int) amount);
            var pos = host.getBlockPos();
            var entity = new ItemEntity(host.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            entity.setDefaultPickUpDelay();
            if (!host.getLevel().addFreshEntity(entity)) {
                throw new IllegalStateException("Unable to recover FCT remainder at " + pos);
            }
            amount = 0;
            key = null;
        }
    }
}
