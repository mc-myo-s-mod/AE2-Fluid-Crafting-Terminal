package me.myogoo.ae2fct.storage;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.ArrayList;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridService;
import appeng.api.networking.IGridServiceProvider;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public final class FluidRemainderService implements IGridService, IGridServiceProvider, MEStorage {
    private final IStorageService storage;
    private final Map<IGridNode, FluidRemainders.Slot> slots = new IdentityHashMap<>();

    public FluidRemainderService(IStorageService storage) {
        this.storage = storage;
        ((RemainderAccess.Network) storage.getInventory()).ae2fct$service(this);
        storage.addGlobalStorageProvider(mounts -> mounts.mount(this, Integer.MAX_VALUE));
    }

    @Override
    public void addNode(IGridNode node, CompoundTag savedData) {
        var slot = ((RemainderAccess.Node) node).ae2fct$slot();
        if (slot != null && slot.node == node) {
            slots.put(node, slot);
            slot.service = this;
            changed();
        }
    }

    @Override
    public void removeNode(IGridNode node) {
        var slot = slots.remove(node);
        if (slot != null && slot.service == this) slot.service = null;
        changed();
    }

    void changed() {
        storage.invalidateCache();
    }

    public long convert(AEFluidKey key, int unit, long count, Actionable mode, IActionSource source,
            boolean insertion) {
        if (count <= 0) return 0;
        long requested = FluidAccounting.requested(count, unit);
        var inventory = storage.getInventory();
        long simulated = insertion ? inventory.insert(key, requested, Actionable.SIMULATE, source)
                : inventory.extract(key, requested, Actionable.SIMULATE, source);
        long items = simulated / unit;
        if (mode == Actionable.SIMULATE || items == 0) return items;
        FluidRemainders.Slot reserved = null;
        // Prefer an existing same-key balance so successive fractions can become a whole item.
        for (int pass = 0; pass < 2 && reserved == null; pass++) {
            for (var slot : slots.values()) {
                if (slot.service == this && (pass == 0) == (slot.amount() != 0)
                        && slot.reserve(key, unit, insertion)) {
                    reserved = slot;
                    break;
                }
            }
        }
        if (reserved == null) return 0;
        try {
            long target = items * unit;
            long before = reserved.amount();
            if (insertion) {
                long actual = inventory.insert(key, target, Actionable.MODULATE, source);
                var result = FluidAccounting.inserted(actual, unit, before);
                reserved.set(key, result.remainder());
                return result.items();
            }
            long buffered = Math.min(before, target);
            long actual = buffered + (target == buffered ? 0
                    : inventory.extract(key, target - buffered, Actionable.MODULATE, source));
            var result = FluidAccounting.extracted(actual, unit, before - buffered);
            reserved.set(key, result.remainder());
            return result.items();
        } finally {
            reserved.release();
        }
    }

    @Override
    public long extract(AEKey key, long amount, Actionable mode, IActionSource source) {
        long extracted = 0;
        for (var slot : new ArrayList<>(slots.values())) {
            if (slot.service != this || !slot.available() || !key.equals(slot.key())) continue;
            long moved = Math.min(Math.max(0, amount - extracted), slot.amount());
            if (moved > 0 && mode == Actionable.MODULATE) slot.set(slot.key(), slot.amount() - moved);
            extracted += moved;
        }
        return extracted;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        for (var slot : slots.values()) {
            if (slot.service == this && slot.available() && slot.amount() > 0) out.add(slot.key(), slot.amount());
        }
    }

    @Override
    public Component getDescription() {
        return Component.literal("Fluid conversion remainders");
    }
}
