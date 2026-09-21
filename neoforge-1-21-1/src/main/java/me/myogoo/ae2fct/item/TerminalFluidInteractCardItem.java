package me.myogoo.ae2fct.item;

import me.myogoo.myotus.api.ITerminalUpgradeCard;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class TerminalFluidInteractCardItem extends Item implements ITerminalUpgradeCard {
    public TerminalFluidInteractCardItem() {
        super(new Properties());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("item.ae2fct.terminal_fluid_interact_card.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
