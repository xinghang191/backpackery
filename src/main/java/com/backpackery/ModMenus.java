package com.backpackery;

import com.backpackery.menu.BackpackMenu;
import com.backpackery.menu.FilterMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
	public static final int TIERS = 6;

	@SuppressWarnings("unchecked")
	private static final MenuType<BackpackMenu>[] BY_TIER = new MenuType[TIERS];
	public static MenuType<FilterMenu> FILTER;
	public static MenuType<FilterMenu> FILTER_ADVANCED;

	public static void init() {
		for (int tier = 0; tier < TIERS; tier++) {
			final int t = tier;
			final int slots = ModItems.TIER_SLOTS[tier];
			final int upgrades = ModItems.TIER_UPGRADES[tier];
			MenuType<BackpackMenu> type = new MenuType<>(
				(syncId, inventory) -> BackpackMenu.client(syncId, inventory, t, slots, upgrades),
				FeatureFlags.VANILLA_SET);
			Registry.register(BuiltInRegistries.MENU, Backpackery.id("backpack_" + slots), type);
			BY_TIER[tier] = type;
		}

		FILTER = Registry.register(BuiltInRegistries.MENU, Backpackery.id("filter"),
			new MenuType<>((syncId, inventory) -> FilterMenu.client(syncId, inventory), FeatureFlags.VANILLA_SET));
		FILTER_ADVANCED = Registry.register(BuiltInRegistries.MENU, Backpackery.id("filter_advanced"),
			new MenuType<>((syncId, inventory) -> FilterMenu.clientAdvanced(syncId, inventory), FeatureFlags.VANILLA_SET));
	}

	public static MenuType<FilterMenu> filterFor(int slots) {
		return slots > FilterMenu.BASIC_SLOTS ? FILTER_ADVANCED : FILTER;
	}

	public static MenuType<BackpackMenu> forTier(int tier) {
		return BY_TIER[tier];
	}

	private ModMenus() {
	}
}