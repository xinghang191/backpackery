package com.backpackery.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A slot whose maximum stack size follows the backpack's stack upgrades. */
public class StorageSlot extends Slot {
	/** {@code ItemStack.CODEC} rejects counts above this. */
	public static final int MAX_TAKE = 99;

	public StorageSlot(Container container, int slot, int x, int y) {
		super(container, slot, x, y);
	}

	@Override
	public int getMaxStackSize() {
		return this.container.getMaxStackSize();
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return this.container.getMaxStackSize(stack);
	}

	/**
	 * A stack bigger than 99 cannot be saved by vanilla once it leaves the backpack
	 * ({@code ItemStack.CODEC} allows 1..99), so taking out is capped and the rest stays inside.
	 */
	@Override
	public ItemStack remove(int amount) {
		return super.remove(Math.min(amount, MAX_TAKE));
	}

	/**
	 * Vanilla merges through {@code Slot.getMaxStackSize(stack)} (see {@code moveItemStackTo}), so
	 * the widened limit above is all that is needed — no {@code max_stack_size} component, which
	 * vanilla refuses to save above 99.
	 */
	@Override
	public boolean mayPlace(ItemStack stack) {
		return this.container.canPlaceItem(this.getContainerSlot(), stack);
	}
}
