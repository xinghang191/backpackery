package com.backpackery.item;

import com.backpackery.ModComponents;
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

public class FilterUpgradeItem extends UpgradeItem {
	public static final int FILTER_SLOTS = 9;
	public static final int ADVANCED_FILTER_SLOTS = 27;

	private final int filterSlots;

	public FilterUpgradeItem(Properties properties) {
		this(FILTER_SLOTS, properties);
	}

	public FilterUpgradeItem(int filterSlots, Properties properties) {
		super(properties);
		this.filterSlots = filterSlots;
	}

	public int filterSlots() {
		return this.filterSlots;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);

		if (player.isShiftKeyDown()) {
			if (!level.isClientSide()) {
				boolean blacklist = !stack.getOrDefault(ModComponents.FILTER_BLACKLIST, false);
				stack.set(ModComponents.FILTER_BLACKLIST, blacklist);
				player.sendSystemMessage(Component.translatable(
					blacklist ? "message.backpackery.filter.blacklist" : "message.backpackery.filter.whitelist"));
			}
			return InteractionResult.SUCCESS;
		}

		if (!level.isClientSide()) {
			this.open(player, stack);
		}
		return InteractionResult.SUCCESS;
	}

	public void open(Player player, ItemStack stack) {
		this.open(player, BackpackContainer.storage(stack, this.filterSlots), stack);
	}

	public void openInstalled(Player player, ItemStack backpack, int upgradeIndex) {
		this.open(player, new InstalledFilterContainer(backpack, upgradeIndex, this.filterSlots),
			BackpackContainer.upgrades(backpack, upgradeIndex + 1).getItem(upgradeIndex));
	}

	private void open(Player player, Container filters, ItemStack stack) {
		if (player.level().isClientSide()) return;
		boolean blacklist = stack.getOrDefault(ModComponents.FILTER_BLACKLIST, false);
		Component title = Component.translatable(
			blacklist ? "gui.backpackery.filter.blacklist" : "gui.backpackery.filter.whitelist");
		player.openMenu(new SimpleMenuProvider(
			(syncId, inventory, p) -> new FilterMenu(syncId, inventory, filters, this.filterSlots), title));
	}
}