package com.backpackery.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class StackUpgradeItem extends UpgradeItem {
	private final int tier;

	public StackUpgradeItem(int tier, Properties properties) {
		super(properties);
		this.tier = tier;
	}

	public int tier() {
		return tier;
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.translatable("item.backpackery.stack_upgrade." + this.tier);
	}
}
