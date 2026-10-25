package com.backpackery.item;

import net.minecraft.world.item.Item;

/** Compacts picked-up items using vanilla 2x2 (basic) or 3x3 (advanced) crafting recipes. */
public class CompactingUpgradeItem extends UpgradeItem {
	private final int gridSize;

	public CompactingUpgradeItem(int gridSize, Item.Properties properties) {
		super(properties);
		this.gridSize = gridSize;
	}

	public int gridSize() {
		return this.gridSize;
	}
}