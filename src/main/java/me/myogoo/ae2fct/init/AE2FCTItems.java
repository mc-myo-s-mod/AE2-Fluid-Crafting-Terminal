package me.myogoo.ae2fct.init;

import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.item.TerminalFluidInteractCardItem;
import me.myogoo.ae2fct.item.VirtualFluidItem;
import me.myogoo.myotus.api.MyotusAPI;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class AE2FCTItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Ae2fct.MODID);

    public static final RegistryObject<VirtualFluidItem> VIRTUAL_FLUID_ITEM = ITEMS.register("virtual_fluid_item",
            VirtualFluidItem::new);
    public static final RegistryObject<TerminalFluidInteractCardItem> TERMINAL_FLUID_INTERACT_CARD = registerItem(
            "terminal_fluid_interact_card",
            TerminalFluidInteractCardItem::new);

    private AE2FCTItems() {
    }

    private static <T extends Item> RegistryObject<T> registerItem(String name, Supplier<T> supplier) {
        var item = ITEMS.register(name, supplier);
        MyotusAPI.creativeTabRegistrar().creativeTabItem(item);
        return item;
    }
}
