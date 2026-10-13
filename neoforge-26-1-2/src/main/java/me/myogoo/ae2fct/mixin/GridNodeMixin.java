package me.myogoo.ae2fct.mixin;

import appeng.me.GridNode;
import me.myogoo.ae2fct.storage.FluidRemainders;
import me.myogoo.ae2fct.storage.RemainderAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = GridNode.class, remap = false)
public abstract class GridNodeMixin implements RemainderAccess.Node {
    @Unique private FluidRemainders.Slot ae2fct$slot;

    @Override
    public FluidRemainders.Slot ae2fct$slot() {
        return ae2fct$slot;
    }

    @Override
    public void ae2fct$slot(FluidRemainders.Slot slot) {
        ae2fct$slot = slot;
    }
}
