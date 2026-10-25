package com.backpackery.inventory;

import com.backpackery.ModComponents;
import com.backpackery.item.BackpackItem;
import com.backpackery.logic.StackRules;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/**
 * Storage / upgrade slots of one backpack stack.
 *
 * <p>Every read and write goes straight to the stack's components instead of working on a private
 * snapshot. That matters because several holders can exist at the same time (the open menu, the
 * pickup pass, the filter rules): with snapshots the last writer wins and items vanish or get
 * duplicated. Here the item stack is the single source of truth.</p>
 */
public class BackpackContainer implements Container {
	private final ItemStack host;
	private final int size;
	private final boolean storage;

	private BackpackContainer(ItemStack host, int size, boolean storage) {
		this.host = host;
		this.size = size;
		this.storage = storage;
	}

	public static BackpackContainer storage(ItemStack host, int size) {
		return new BackpackContainer(host, size, true);
	}

	public static BackpackContainer upgrades(ItemStack host, int size) {
		return new BackpackContainer(host, size, false);
	}

	public ItemStack host() {
		return this.host;
	}

	private boolean isStorageOfBackpack() {
		return this.storage && this.host.getItem() instanceof BackpackItem;
	}

	/** Current contents as a mutable, exactly {@link #size} long list. Never null. */
	private List<ItemStack> snapshot() {
		List<ItemStack> slots = new ArrayList<>(this.size);
		for (int i = 0; i < this.size; i++) slots.add(ItemStack.EMPTY);
		if (this.storage) {
			BackpackContents contents = this.host.get(ModComponents.STORAGE);
			if (contents != null) {
				fill(slots, contents.slots());
			} else {
				ItemContainerContents legacy = this.host.get(DataComponents.CONTAINER);
				if (legacy != null) fill(slots, legacySlots(legacy));
			}
		} else {
			ItemContainerContents upgrades = this.host.get(ModComponents.UPGRADES);
			if (upgrades != null) fill(slots, legacySlots(upgrades));
		}
		return slots;
	}

	private static List<ItemStack> legacySlots(ItemContainerContents contents) {
		List<ItemStack> slots = new ArrayList<>();
		contents.itemCopies().forEach(slots::add);
		return slots;
	}

	private static void fill(List<ItemStack> target, List<ItemStack> source) {
		for (int i = 0; i < target.size() && i < source.size(); i++) {
			target.set(i, source.get(i));
		}
	}

	private void write(List<ItemStack> slots) {
		if (this.storage) {
			this.host.set(ModComponents.STORAGE, BackpackContents.of(slots));
			// pre-0.8 builds kept contents in minecraft:container; never keep two copies around
			if (this.host.has(DataComponents.CONTAINER)) this.host.remove(DataComponents.CONTAINER);
		} else {
			this.host.set(ModComponents.UPGRADES, ItemContainerContents.fromItems(slots));
		}
	}

	@Override
	public int getMaxStackSize() {
		return isStorageOfBackpack() ? StackRules.genericCapacity(this.host) : 64;
	}

	@Override
	public int getMaxStackSize(ItemStack stack) {
		return isStorageOfBackpack() ? StackRules.capacityFor(this.host, stack) : 64;
	}

	@Override
	public int getContainerSize() {
		return this.size;
	}

	@Override
	public boolean isEmpty() {
		for (int i = 0; i < this.size; i++) {
			if (!getItem(i).isEmpty()) return false;
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		if (slot < 0 || slot >= this.size) return ItemStack.EMPTY;
		if (this.storage) {
			BackpackContents contents = this.host.get(ModComponents.STORAGE);
			if (contents != null) {
				List<ItemStack> slots = contents.slots();
				return slot < slots.size() ? slots.get(slot) : ItemStack.EMPTY;
			}
			ItemContainerContents legacy = this.host.get(DataComponents.CONTAINER);
			if (legacy != null) return at(legacySlots(legacy), slot);
			return ItemStack.EMPTY;
		}
		ItemContainerContents upgrades = this.host.get(ModComponents.UPGRADES);
		return upgrades == null ? ItemStack.EMPTY : at(legacySlots(upgrades), slot);
	}

	private static ItemStack at(List<ItemStack> slots, int slot) {
		return slot < slots.size() ? slots.get(slot) : ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		if (slot < 0 || slot >= this.size) return ItemStack.EMPTY;
		List<ItemStack> slots = snapshot();
		ItemStack stack = slots.get(slot);
		if (stack.isEmpty()) return ItemStack.EMPTY;
		ItemStack taken = stack.split(Math.max(0, amount));
		slots.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
		write(slots);
		trimLimit(taken);
		return taken;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		if (slot < 0 || slot >= this.size) return ItemStack.EMPTY;
		List<ItemStack> slots = snapshot();
		ItemStack stack = slots.get(slot);
		slots.set(slot, ItemStack.EMPTY);
		write(slots);
		trimLimit(stack);
		return stack;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		if (slot < 0 || slot >= this.size) return;
		List<ItemStack> slots = snapshot();
		slots.set(slot, stack == null ? ItemStack.EMPTY : stack);
		trimLimit(slots.get(slot));
		write(slots);
	}

	/**
	 * A widened limit is expressed only through {@code getMaxStackSize(stack)}; if an older build
	 * left a {@code max_stack_size} component behind, drop it again — vanilla refuses to save a
	 * value above 99 and silently rewrites the stack.
	 */
	private void trimLimit(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return;
		int base = StackRules.baseLimit(stack);
		if (base <= 1) return;
		int limit = stack.getOrDefault(DataComponents.MAX_STACK_SIZE, base);
		if (limit == base) return;
		if (stack.getCount() > base) return;
		stack.remove(DataComponents.MAX_STACK_SIZE);
	}

	@Override
	public void setChanged() {
		// every mutation already writes through to the item stack
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return !(stack.getItem() instanceof BackpackItem);
	}

	@Override
	public void clearContent() {
		List<ItemStack> slots = new ArrayList<>(this.size);
		for (int i = 0; i < this.size; i++) slots.add(ItemStack.EMPTY);
		write(slots);
	}
}
