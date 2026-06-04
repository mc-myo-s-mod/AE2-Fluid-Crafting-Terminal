package me.myogoo.ae2fct.init;

import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.item.TerminalFluidInteractCardItem;
import me.myogoo.ae2fct.item.VirtualFluidItem;
import me.myogoo.myotus.api.MyotusAPI;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class AE2FCTItems {
    public static final DeferredRegister<Item> REGISTER = DeferredRegister.create(BuiltInRegistries.ITEM, Ae2fct.MODID);

    public static final Supplier<VirtualFluidItem> VIRTUAL_FLUID_ITEM = REGISTER.register("virtual_fluid_item",
            VirtualFluidItem::new);
    public static final Supplier<TerminalFluidInteractCardItem> TERMINAL_FLUID_INTERACT_CARD = registerItem(
            "terminal_fluid_interact_card",
            TerminalFluidInteractCardItem::new);

    private static <T extends Item> Supplier<T> registerItem(String name, Supplier<T> supplier) {
        var item = REGISTER.register(name, supplier);
        MyotusAPI.creativeTabRegistrar().creativeTabItem(item);
        return item;
    }
}
