package com.backpackery.logic;

import com.backpackery.ModComponents;
import com.backpackery.item.VoidUpgradeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class VoidRules {
	public static boolean shouldVoid(ItemStack backpack, ItemStack candidate) {
		ItemContainerContents upgrades = backpack.get(ModComponents.UPGRADES);
		if (upgrades == null) return false;

		for (ItemStack upgrade : upgrades.nonEmptyItemCopyStream().toList()) {
			if (!(upgrade.getItem() instanceof VoidUpgradeItem voidUpgrade)) continue;
			var entries = FilterRules.contents(upgrade, voidUpgrade.filterSlots());
			if (entries.stream().anyMatch(entry -> FilterRules.matches(entry, candidate))) {
				return true;
			}
		}
		return false;
	}

	public static boolean hasVoid(ItemStack backpack) {
		ItemContainerContents upgrades = backpack.get(ModComponents.UPGRADES);
		if (upgrades == null) return false;
		return upgrades.nonEmptyItemCopyStream().anyMatch(s -> s.getItem() instanceof VoidUpgradeItem);
	}

	private VoidRules() {
	}
}