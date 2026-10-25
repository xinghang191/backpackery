package com.backpackery;

import com.backpackery.item.BackpackItem;
import com.backpackery.item.AdvancedFeedingUpgradeItem;
import com.backpackery.item.CompactingUpgradeItem;
import com.backpackery.item.CookingUpgradeItem;
import com.backpackery.item.FeedingUpgradeItem;
import com.backpackery.item.FilterUpgradeItem;
import com.backpackery.item.MagnetUpgradeItem;
import com.backpackery.item.PickupUpgradeItem;
import com.backpackery.item.StackUpgradeItem;
import com.backpackery.item.VoidUpgradeItem;
import com.backpackery.item.WorkstationUpgradeItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
	public static final List<Item> ALL = new ArrayList<>();

	public static final String[] TIER_IDS = {
		"backpack", "copper_backpack", "iron_backpack", "gold_backpack", "diamond_backpack", "netherite_backpack"
	};

	/**
	 * Storage slots of the six backpack tiers, mirroring Sophisticated Backpacks' defaults:
	 * leather 27, copper 45, iron 54, gold 81, diamond 108, netherite 120.
	 */
	public static final int[] TIER_SLOTS = {27, 45, 54, 81, 108, 120};

	/** Upgrade slots per tier: 1, 1, 2, 3, 5, 7 - again the Sophisticated Backpacks defaults. */
	public static final int[] TIER_UPGRADES = {1, 1, 2, 3, 5, 7};

	public static final int[] TIER_COLS = new int[TIER_IDS.length];
	public static final int[] TIER_ROWS = new int[TIER_IDS.length];

	public static Item UPGRADE_BASE;
	public static Item PICKUP_UPGRADE;
	public static Item ADVANCED_PICKUP_UPGRADE;
	public static Item FILTER_UPGRADE;
	public static Item ADVANCED_FILTER_UPGRADE;
	public static Item MAGNET_UPGRADE;
	public static Item ADVANCED_MAGNET_UPGRADE;
	public static Item VOID_UPGRADE;
	public static Item ADVANCED_VOID_UPGRADE;
	public static Item COMPACTING_UPGRADE;
	public static Item ADVANCED_COMPACTING_UPGRADE;
	public static Item FEEDING_UPGRADE;
	public static Item ADVANCED_FEEDING_UPGRADE;
	public static Item CRAFTING_UPGRADE;
	public static Item STONECUTTER_UPGRADE;
	public static Item SMITHING_UPGRADE;
	public static Item ANVIL_UPGRADE;
	public static Item SMELTING_UPGRADE;
	public static Item BLASTING_UPGRADE;
	public static Item SMOKING_UPGRADE;
	public static Item AUTO_SMELTING_UPGRADE;
	public static Item AUTO_BLASTING_UPGRADE;
	public static Item AUTO_SMOKING_UPGRADE;
	public static final Item[] STACK_UPGRADES = new Item[4];

	static {
		for (int tier = 0; tier < TIER_IDS.length; tier++) {
			TIER_COLS[tier] = colsFor(TIER_SLOTS[tier]);
			TIER_ROWS[tier] = rowsFor(TIER_SLOTS[tier]);
		}
	}

	public static void init() {
		ModComponents.UPGRADES.getClass();

		for (int tier = 0; tier < TIER_IDS.length; tier++) {
			registerBackpack(tier);
		}

		UPGRADE_BASE = register("upgrade_base", new Item(props("upgrade_base", 16)));
		PICKUP_UPGRADE = register("pickup_upgrade", new PickupUpgradeItem(props("pickup_upgrade", 1)));
		ADVANCED_PICKUP_UPGRADE = register("advanced_pickup_upgrade",
			new PickupUpgradeItem(5.0D, props("advanced_pickup_upgrade", 1)));
		FILTER_UPGRADE = register("filter_upgrade", new FilterUpgradeItem(props("filter_upgrade", 1)));
		ADVANCED_FILTER_UPGRADE = register("advanced_filter_upgrade",
			new FilterUpgradeItem(FilterUpgradeItem.ADVANCED_FILTER_SLOTS, props("advanced_filter_upgrade", 1)));
		MAGNET_UPGRADE = register("magnet_upgrade", new MagnetUpgradeItem(props("magnet_upgrade", 1)));
		ADVANCED_MAGNET_UPGRADE = register("advanced_magnet_upgrade",
			new MagnetUpgradeItem(16.0D, props("advanced_magnet_upgrade", 1)));
		VOID_UPGRADE = register("void_upgrade", new VoidUpgradeItem(props("void_upgrade", 1)));
		ADVANCED_VOID_UPGRADE = register("advanced_void_upgrade",
			new VoidUpgradeItem(FilterUpgradeItem.ADVANCED_FILTER_SLOTS, props("advanced_void_upgrade", 1)));
		COMPACTING_UPGRADE = register("compacting_upgrade",
			new CompactingUpgradeItem(2, props("compacting_upgrade", 1)));
		ADVANCED_COMPACTING_UPGRADE = register("advanced_compacting_upgrade",
			new CompactingUpgradeItem(3, props("advanced_compacting_upgrade", 1)));
		FEEDING_UPGRADE = register("feeding_upgrade", new FeedingUpgradeItem(props("feeding_upgrade", 1)));
		ADVANCED_FEEDING_UPGRADE = register("advanced_feeding_upgrade",
			new AdvancedFeedingUpgradeItem(props("advanced_feeding_upgrade", 1)));
		CRAFTING_UPGRADE = register("crafting_upgrade",
			new WorkstationUpgradeItem(WorkstationUpgradeItem.Kind.CRAFTING, props("crafting_upgrade", 1)));
		STONECUTTER_UPGRADE = register("stonecutter_upgrade",
			new WorkstationUpgradeItem(WorkstationUpgradeItem.Kind.STONECUTTER, props("stonecutter_upgrade", 1)));
		SMITHING_UPGRADE = register("smithing_upgrade",
			new WorkstationUpgradeItem(WorkstationUpgradeItem.Kind.SMITHING, props("smithing_upgrade", 1)));
		ANVIL_UPGRADE = register("anvil_upgrade",
			new WorkstationUpgradeItem(WorkstationUpgradeItem.Kind.ANVIL, props("anvil_upgrade", 1)));
		SMELTING_UPGRADE = register("smelting_upgrade",
			new CookingUpgradeItem(CookingUpgradeItem.Kind.SMELTING, props("smelting_upgrade", 1)));
		BLASTING_UPGRADE = register("blasting_upgrade",
			new CookingUpgradeItem(CookingUpgradeItem.Kind.BLASTING, props("blasting_upgrade", 1)));
		SMOKING_UPGRADE = register("smoking_upgrade",
			new CookingUpgradeItem(CookingUpgradeItem.Kind.SMOKING, props("smoking_upgrade", 1)));
		AUTO_SMELTING_UPGRADE = register("auto_smelting_upgrade",
			new CookingUpgradeItem(CookingUpgradeItem.Kind.AUTO_SMELTING, props("auto_smelting_upgrade", 1)));
		AUTO_BLASTING_UPGRADE = register("auto_blasting_upgrade",
			new CookingUpgradeItem(CookingUpgradeItem.Kind.AUTO_BLASTING, props("auto_blasting_upgrade", 1)));
		AUTO_SMOKING_UPGRADE = register("auto_smoking_upgrade",
			new CookingUpgradeItem(CookingUpgradeItem.Kind.AUTO_SMOKING, props("auto_smoking_upgrade", 1)));

		for (int tier = 1; tier <= 4; tier++) {
			STACK_UPGRADES[tier - 1] = register("stack_upgrade_" + tier,
				new StackUpgradeItem(tier, props("stack_upgrade_" + tier, 1)));
		}

		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Backpackery.id("main"),
			CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
				.title(Component.translatable("itemGroup.backpackery"))
				.icon(() -> new ItemStack(ALL.get(0)))
				.displayItems((params, output) -> ALL.forEach(output::accept))
				.build());
	}

	/** Nine columns up to gold, twelve for diamond and netherite - the Sophisticated layout. */
	public static int colsFor(int slots) {
		return slots <= 81 ? 9 : 12;
	}

	public static int rowsFor(int slots) {
		int cols = colsFor(slots);
		return (slots + cols - 1) / cols;
	}

	private static Item.Properties props(String name, int stackSize) {
		return new Item.Properties().setId(key(name)).stacksTo(stackSize);
	}

	private static ResourceKey<Item> key(String name) {
		return ResourceKey.create(Registries.ITEM, Backpackery.id(name));
	}

	private static Item register(String name, Item item) {
		Registry.register(BuiltInRegistries.ITEM, key(name), item);
		ALL.add(item);
		return item;
	}

	private static void registerBackpack(int tier) {
		String name = TIER_IDS[tier];
		register(name, new BackpackItem(tier, TIER_SLOTS[tier], TIER_UPGRADES[tier],
			new Item.Properties().setId(key(name)).stacksTo(1)));
	}

	public static int slotsOf(int tier) {
		return TIER_SLOTS[tier];
	}

	private ModItems() {
	}
}