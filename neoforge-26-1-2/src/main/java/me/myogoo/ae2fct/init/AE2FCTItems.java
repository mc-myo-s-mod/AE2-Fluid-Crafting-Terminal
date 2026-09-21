package me.myogoo.ae2fct.init;

import me.myogoo.ae2fct.Ae2fct;
import me.myogoo.ae2fct.item.TerminalFluidInteractCardItem;
import me.myogoo.ae2fct.item.VirtualFluidItem;
import me.myogoo.myotus.api.MyotusAPI;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Function;

public class AE2FCTItems {
    public static final DeferredRegister.Items REGISTER = DeferredRegister.createItems(Ae2fct.MODID);

    public static final DeferredItem<VirtualFluidItem> VIRTUAL_FLUID_ITEM = REGISTER.registerItem(
            "virtual_fluid_item", VirtualFluidItem::new);
    public static final DeferredItem<TerminalFluidInteractCardItem> TERMINAL_FLUID_INTERACT_CARD = registerItem(
            "terminal_fluid_interact_card",
            TerminalFluidInteractCardItem::new);

    private static <T extends Item> DeferredItem<T> registerItem(String name,
            Function<Item.Properties, T> factory) {
        var item = REGISTER.registerItem(name, factory);
        MyotusAPI.creativeTabs().registerCreativeTabItem(item);
        return item;
    }
}
