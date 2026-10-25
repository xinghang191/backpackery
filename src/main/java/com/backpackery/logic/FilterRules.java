package com.backpackery.logic;

import com.backpackery.ModComponents;
import com.backpackery.item.FilterUpgradeItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

public final class FilterRules {
	/** True when the backpack has a filter upgrade that blocks this item (whitelist miss or blacklist hit). */
	public static boolean blocked(ItemStack backpack, ItemStack candidate) {
		ItemContainerContents upgrades = backpack.get(ModComponents.UPGRADES);
		if (upgrades == null) return false;

		for (ItemStack upgrade : upgrades.nonEmptyItemCopyStream().toList()) {
			if (!(upgrade.getItem() instanceof FilterUpgradeItem filterUpgrade)) continue;
			List<ItemStack> entries = contents(upgrade, filterUpgrade.filterSlots());
			if (entries.isEmpty()) continue;

			boolean listed = false;
			for (ItemStack entry : entries) {
				if (matches(entry, candidate)) {
					listed = true;
					break;
				}
			}
			boolean blacklist = upgrade.getOrDefault(ModComponents.FILTER_BLACKLIST, false);
			if (blacklist ? listed : !listed) return true;
		}
		return false;
	}

	public static boolean accepts(ItemStack backpack, ItemStack candidate) {
		return !blocked(backpack, candidate);
	}

	public static List<ItemStack> collect(ItemStack backpack) {
		List<ItemStack> out = new ArrayList<>();
		ItemContainerContents upgrades = backpack.get(ModComponents.UPGRADES);
		if (upgrades == null) return out;
		for (ItemStack upgrade : upgrades.nonEmptyItemCopyStream().toList()) {
			if (!(upgrade.getItem() instanceof FilterUpgradeItem filterUpgrade)) continue;
			out.addAll(contents(upgrade, filterUpgrade.filterSlots()));
		}
		return out;
	}

	public static boolean isBlacklist(ItemStack filterUpgrade) {
		return filterUpgrade.getOrDefault(ModComponents.FILTER_BLACKLIST, false);
	}

	/** Filter contents live in {@code backpackery:storage}; older items used {@code minecraft:container}. */
	public static List<ItemStack> contents(ItemStack stack, int slots) {
		List<ItemStack> out = new ArrayList<>();
		var own = stack.get(ModComponents.STORAGE);
		if (own != null) {
			for (ItemStack stored : own.slots()) {
				if (!stored.isEmpty()) out.add(stored);
			}
			return out;
		}
		ItemContainerContents legacy = stack.get(DataComponents.CONTAINER);
		if (legacy != null) legacy.nonEmptyItemCopyStream().forEach(out::add);
		return out;
	}

	public static boolean matches(ItemStack filter, ItemStack candidate) {
		return ItemStack.isSameItem(filter, candidate);
	}

	private FilterRules() {
	}
}
