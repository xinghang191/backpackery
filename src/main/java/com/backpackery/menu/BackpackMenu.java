package com.backpackery.menu;

import com.backpackery.ModMenus;
import com.backpackery.inventory.ClientStorageContainer;
import com.backpackery.item.FilterUpgradeItem;
import com.backpackery.item.UpgradeItem;
import com.backpackery.item.VoidUpgradeItem;
import com.backpackery.item.WorkstationUpgradeItem;
import com.backpackery.logic.PickupLogic;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BackpackMenu extends AbstractContainerMenu {
	public static final int INVENTORY_WIDTH = 176;

	/** Sort button ids understood by {@link #clickMenuButton}. */
	public static final int BUTTON_SORT = 0;
	public static final int BUTTON_TRANSFER_TO_STORAGE = 1;
	public static final int BUTTON_TRANSFER_TO_INVENTORY = 2;
	/** 3..5 select the sort key and sort right away. */
	public static final int BUTTON_SORT_BY_NAME = 3;
	/** 100 + upgrade slot index: right-clicking an installed workstation upgrade opens it. */
	public static final int BUTTON_OPEN_UPGRADE = 100;

	private static final int SORT_BY_NAME = 0;
	private static final int SORT_BY_MOD = 1;
	private static final int SORT_BY_COUNT = 2;

	private final int cols;
	private final int rows;
	private final int upgradeSlots;
	private final Container storage;
	private final Container upgrades;
	private final ItemStack host;
	private final int invOffset;
	private final int invY;
	private int sortBy = SORT_BY_NAME;

	public BackpackMenu(int syncId, Inventory inventory, int tier, int slots, int upgradeSlots,
						Container storage, Container upgrades, ItemStack host) {
		super(ModMenus.forTier(tier), syncId);
		this.cols = slots <= 81 ? 9 : 12;
		this.rows = (slots + this.cols - 1) / this.cols;
		this.upgradeSlots = upgradeSlots;
		this.storage = storage;
		this.upgrades = upgrades;
		this.host = host;

		for (int i = 0; i < slots; i++) {
			this.addSlot(new StorageSlot(storage, i, 8 + (i % this.cols) * 18, 18 + (i / this.cols) * 18));
		}

		// Upgrade slots live in the vertical strip attached to the left of the panel,
		// exactly where Sophisticated Backpacks puts them.
		for (int u = 0; u < upgradeSlots; u++) {
			this.addSlot(new UpgradeSlot(upgrades, u, -15, 6 + u * 16));
		}

		this.invOffset = this.cols == 12 ? 27 : 0;
		this.invY = panelHeight() - 94 + 12;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlot(new Slot(inventory, 9 + row * 9 + col,
					this.invOffset + 8 + col * 18, this.invY + row * 18));
			}
		}
		for (int col = 0; col < 9; col++) {
			this.addSlot(new Slot(inventory, col, this.invOffset + 8 + col * 18, this.invY + 58));
		}
	}

	public static BackpackMenu client(int syncId, Inventory inventory, int tier, int slots, int upgradeSlots) {
		return new BackpackMenu(syncId, inventory, tier, slots, upgradeSlots,
			new ClientStorageContainer(slots), new SimpleContainer(upgradeSlots), ItemStack.EMPTY);
	}

	public int cols() {
		return cols;
	}

	public int rows() {
		return rows;
	}

	public int upgradeSlots() {
		return upgradeSlots;
	}

	public int invOffset() {
		return invOffset;
	}

	public int invY() {
		return invY;
	}

	public int panelWidth() {
		return cols * 18 + 14;
	}

	public int panelHeight() {
		return 114 + rows * 18;
	}

	public int sortBy() {
		return sortBy;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (id >= BUTTON_OPEN_UPGRADE && id < BUTTON_OPEN_UPGRADE + this.upgradeSlots) {
			ItemStack upgrade = this.upgrades.getItem(id - BUTTON_OPEN_UPGRADE);
			if (upgrade.getItem() instanceof WorkstationUpgradeItem workstation) {
				workstation.open(player);
				return true;
			}
			if (upgrade.getItem() instanceof FilterUpgradeItem filter) {
				filter.openInstalled(player, this.host, id - BUTTON_OPEN_UPGRADE);
				return true;
			}
			if (upgrade.getItem() instanceof VoidUpgradeItem voidUpgrade) {
				voidUpgrade.openInstalled(player, this.host, id - BUTTON_OPEN_UPGRADE);
				return true;
			}
			return false;
		}
		switch (id) {
			case BUTTON_SORT -> sort();
			case BUTTON_TRANSFER_TO_STORAGE -> transferToStorage();
			case BUTTON_TRANSFER_TO_INVENTORY -> transferToInventory();
			case BUTTON_SORT_BY_NAME -> setSortByAndSort(SORT_BY_NAME);
			case BUTTON_SORT_BY_NAME + 1 -> setSortByAndSort(SORT_BY_MOD);
			case BUTTON_SORT_BY_NAME + 2 -> setSortByAndSort(SORT_BY_COUNT);
			default -> {
				return false;
			}
		}
		return true;
	}

	private void setSortByAndSort(int mode) {
		this.sortBy = mode;
		sort();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = this.slots.get(index);
		if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		int storageEnd = this.cols * this.rows;
		int upgradeEnd = storageEnd + this.upgradeSlots;

		if (index < upgradeEnd) {
			if (!this.moveItemStackTo(stack, upgradeEnd, this.slots.size(), true)) return ItemStack.EMPTY;
		} else if (stack == this.host) {
			return ItemStack.EMPTY; // never put the open backpack inside itself
		} else if (stack.getItem() instanceof UpgradeItem) {
			if (!this.moveItemStackTo(stack, storageEnd, upgradeEnd, false)
				&& !this.moveItemStackTo(stack, 0, storageEnd, false)) return ItemStack.EMPTY;
		} else if (!this.moveItemStackTo(stack, 0, storageEnd, false)) {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.set(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return copy;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public void removed(Player player) {
		super.removed(player);
		if (this.storage instanceof com.backpackery.inventory.BackpackContainer c) c.setChanged();
		if (this.upgrades instanceof com.backpackery.inventory.BackpackContainer u) u.setChanged();
	}

	// ------------------------------------------------------------------ buttons

	private void transferToStorage() {
		int storageEnd = this.cols * this.rows;
		int inventoryStart = storageEnd + this.upgradeSlots;
		for (int i = inventoryStart; i < this.slots.size(); i++) {
			Slot slot = this.slots.get(i);
			if (!slot.hasItem() || slot.getItem() == this.host) continue;
			this.moveItemStackTo(slot.getItem(), 0, storageEnd, false);
			if (!slot.hasItem()) slot.set(ItemStack.EMPTY);
			else slot.setChanged();
		}
		this.broadcastChanges();
	}

	private void transferToInventory() {
		int storageEnd = this.cols * this.rows;
		int inventoryStart = storageEnd + this.upgradeSlots;
		for (int i = 0; i < storageEnd; i++) {
			Slot slot = this.slots.get(i);
			if (!slot.hasItem()) continue;
			this.moveItemStackTo(slot.getItem(), inventoryStart, this.slots.size(), true);
			if (!slot.hasItem()) slot.set(ItemStack.EMPTY);
			else slot.setChanged();
		}
		this.broadcastChanges();
	}

	/**
	 * Sorts the storage area, merging partial stacks up to the slot capacity that the stack
	 * upgrades allow and rebuilding the slot list in place.
	 */
	private void sort() {
		int storageEnd = this.cols * this.rows;
		List<ItemStack> sorted = new ArrayList<>();
		for (int i = 0; i < storageEnd; i++) {
			ItemStack stack = this.storage.getItem(i);
			if (!stack.isEmpty()) sorted.add(stack.copy());
		}
		sorted.sort(comparator());

		List<ItemStack> packed = new ArrayList<>();
		for (ItemStack stack : sorted) {
			int remaining = stack.getCount();
			int capacity = Math.max(1, this.storage.getMaxStackSize(stack));
			for (ItemStack existing : packed) {
				if (remaining <= 0) break;
				if (existing.getCount() >= capacity) continue;
				if (!PickupLogic.sameForMerge(existing, stack)) continue;
				int move = Math.min(remaining, capacity - existing.getCount());
				existing.grow(move);
				remaining -= move;
			}
			while (remaining > 0) {
				ItemStack part = stack.copyWithCount(Math.min(remaining, capacity));
				packed.add(part);
				remaining -= part.getCount();
			}
		}
		if (packed.size() > storageEnd) return; // never lose items, skip the sort instead

		for (int i = 0; i < storageEnd; i++) {
			this.storage.setItem(i, i < packed.size() ? packed.get(i) : ItemStack.EMPTY);
		}
		this.storage.setChanged();
		this.broadcastChanges();
	}

	private Comparator<ItemStack> comparator() {
		return switch (this.sortBy) {
			case SORT_BY_COUNT -> Comparator.<ItemStack>comparingInt(ItemStack::getCount).reversed()
				.thenComparing(this::itemKey).thenComparing(this::componentsKey);
			case SORT_BY_MOD -> Comparator.<ItemStack, String>comparing(s -> namespaceOf(itemKey(s)))
				.thenComparing(this::itemKey).thenComparing(this::componentsKey);
			default -> Comparator.<ItemStack, String>comparing(this::itemKey)
				.thenComparing(this::componentsKey)
				.thenComparing(Comparator.<ItemStack>comparingInt(ItemStack::getCount).reversed());
		};
	}

	private String itemKey(ItemStack stack) {
		return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
	}

	private String componentsKey(ItemStack stack) {
		return stack.getComponents().toString();
	}

	private static String namespaceOf(String id) {
		int colon = id.indexOf(':');
		return colon < 0 ? id : id.substring(0, colon);
	}
}