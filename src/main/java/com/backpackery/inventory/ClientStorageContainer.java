package com.backpackery.inventory;

import com.backpackery.logic.StackRules;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Client-side mirror of a backpack's storage.
 *
 * <p>{@link SimpleContainer#setItem} clamps every incoming stack to
 * {@link SimpleContainer#getMaxStackSize(ItemStack)}. The client menu is built from a plain
 * {@code SimpleContainer}, so without this override a server-synced stack of 247 items would be
 * silently shortened to the vanilla limit of 64 — even though the server still holds all 247.
 * Reporting the absolute backpack ceiling keeps synced counts intact; the server remains
 * authoritative for what may actually be inserted or taken.</p>
 */
public class ClientStorageContainer extends SimpleContainer {
	public ClientStorageContainer(int size) {
		super(size);
	}

	@Override
	public int getMaxStackSize() {
		return StackRules.HARD_CAP;
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return StackRules.HARD_CAP;
	}
}