package com.backpackery.item;

import com.backpackery.inventory.BackpackContainer;
import com.backpackery.menu.BackpackMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

public class BackpackItem extends Item {
	private final int tier;
	private final int slots;
	private final int upgradeSlots;

	public BackpackItem(int tier, int slots, int upgradeSlots, Properties properties) {
		super(properties);
		this.tier = tier;
		this.slots = slots;
		this.upgradeSlots = upgradeSlots;
	}

	public int tier() {
		return this.tier;
	}

	public int slots() {
		return this.slots;
	}

	public int upgradeSlots() {
		return this.upgradeSlots;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
								Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.translatable("tooltip.backpackery.storage", this.slots)
			.withStyle(ChatFormatting.GRAY));
		tooltip.accept(Component.translatable("tooltip.backpackery.upgrades", this.upgradeSlots)
			.withStyle(ChatFormatting.GRAY));
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			BackpackContainer storage = BackpackContainer.storage(stack, this.slots);
			BackpackContainer upgrades = BackpackContainer.upgrades(stack, this.upgradeSlots);
			player.openMenu(new SimpleMenuProvider(
				(syncId, inventory, p) -> new BackpackMenu(syncId, inventory, this.tier, this.slots, this.upgradeSlots,
					storage, upgrades, stack),
				stack.getHoverName()));
		}
		return InteractionResult.SUCCESS;
	}
}