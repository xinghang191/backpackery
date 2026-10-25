package com.backpackery.menu;

import com.backpackery.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class FilterMenu extends AbstractContainerMenu {
	public static final int BASIC_SLOTS = 9;
	public static final int ADVANCED_SLOTS = 27;

	private final int invY;
	private final int filterSlots;
	private final int rows;

	public FilterMenu(int syncId, Inventory inventory, Container filters) {
		this(syncId, inventory, filters, BASIC_SLOTS);
	}

	public FilterMenu(int syncId, Inventory inventory, Container filters, int filterSlots) {
		super(ModMenus.filterFor(filterSlots), syncId);
		this.filterSlots = filterSlots;
		this.rows = (filterSlots + 8) / 9;

		for (int i = 0; i < filterSlots; i++) {
			int row = i / 9;
			int col = i % 9;
			this.addSlot(new Slot(filters, i, 8 + col * 18, 18 + row * 18));
		}

		this.invY = 17 + this.rows * 18 + 14;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(inventory, 9 + row * 9 + col, 8 + col * 18, this.invY + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(inventory, col, 8 + col * 18, this.invY + 58));
		}
	}

	public static FilterMenu client(int syncId, Inventory inventory) {
		return new FilterMenu(syncId, inventory, new SimpleContainer(BASIC_SLOTS), BASIC_SLOTS);
	}

	public static FilterMenu clientAdvanced(int syncId, Inventory inventory) {
		return new FilterMenu(syncId, inventory, new SimpleContainer(ADVANCED_SLOTS), ADVANCED_SLOTS);
	}

	public int invY() {
		return invY;
	}

	public int filterSlots() {
		return filterSlots;
	}

	public int rows() {
		return rows;
	}

	public int topHeight() {
		return 17 + this.rows * 18;
	}

	public int panelHeight() {
		return topHeight() + 96;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index < this.filterSlots) {
			if (!this.moveItemStackTo(stack, this.filterSlots, this.slots.size(), true)) return ItemStack.EMPTY;
		} else if (!this.moveItemStackTo(stack, 0, this.filterSlots, false)) {
			return ItemStack.EMPTY;
		}
		if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
		return copy;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}
}