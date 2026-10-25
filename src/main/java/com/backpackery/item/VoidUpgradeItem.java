package com.backpackery.item;

import com.backpackery.inventory.BackpackContainer;
import com.backpackery.inventory.InstalledFilterContainer;
import com.backpackery.menu.FilterMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Void upgrade with its own filter, matching Sophisticated Backpacks: items that match the
 * filter are destroyed instead of entering the backpack. The advanced variant has 27 slots.
 */
public class VoidUpgradeItem extends UpgradeItem {
	private final int filterSlots;

	public VoidUpgradeItem(Properties properties) {
		this(FilterUpgradeItem.FILTER_SLOTS, properties);
	}

	public VoidUpgradeItem(int filterSlots, Properties properties) {
		super(properties);
		this.filterSlots = filterSlots;
	}

	public int filterSlots() {
		return this.filterSlots;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide()) {
			this.open(player, player.getItemInHand(hand));
		}
		return InteractionResult.SUCCESS;
	}

	public void open(Player player, ItemStack stack) {
		this.open(player, BackpackContainer.storage(stack, this.filterSlots));
	}

	public void openInstalled(Player player, ItemStack backpack, int upgradeIndex) {
		this.open(player, new InstalledFilterContainer(backpack, upgradeIndex, this.filterSlots));
	}

	private void open(Player player, Container filters) {
		if (player.level().isClientSide()) return;
		player.openMenu(new SimpleMenuProvider(
			(syncId, inventory, p) -> new FilterMenu(syncId, inventory, filters, this.filterSlots),
			Component.translatable("gui.backpackery.void_filter")));
	}
}