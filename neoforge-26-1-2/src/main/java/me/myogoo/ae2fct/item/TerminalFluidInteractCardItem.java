package me.myogoo.ae2fct.item;

import me.myogoo.myotus.api.ITerminalUpgradeCard;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class TerminalFluidInteractCardItem extends Item implements ITerminalUpgradeCard {
    public TerminalFluidInteractCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
            Consumer<Component> tooltipComponents,
            TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipComponents, tooltipFlag);
        tooltipComponents.accept(Component.translatable("item.ae2fct.terminal_fluid_interact_card.desc")
                .withStyle(ChatFormatting.GRAY));
    }
}
