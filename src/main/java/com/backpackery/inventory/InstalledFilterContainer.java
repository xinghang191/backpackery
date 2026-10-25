package com.backpackery.inventory;

import com.backpackery.logic.StackRules;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Filter slots of an upgrade that is installed inside a backpack. Reads and writes go through the
 * backpack's upgrade component, so editing an installed filter actually persists.
 */
public class InstalledFilterContainer implements Container {
	private final ItemStack backpack;
	private final int upgradeIndex;
	private final int size;

	public InstalledFilterContainer(ItemStack backpack, int upgradeIndex, int size) {
		this.backpack = backpack;
		this.upgradeIndex = upgradeIndex;
		this.size = size;
	}

	private ItemStack upgrade() {
		return BackpackContainer.upgrades(this.backpack, StackRules.MAX_UPGRADE_SLOTS).getItem(this.upgradeIndex);
	}

	private void store(ItemStack upgrade) {
		BackpackContainer.upgrades(this.backpack, StackRules.MAX_UPGRADE_SLOTS).setItem(this.upgradeIndex, upgrade);
	}

	@Override
	public int getContainerSize() {
		return this.size;
	}

	@Override
	public boolean isEmpty() {
		for (int i = 0; i < this.size; i++) {
			if (!this.getItem(i).isEmpty()) return false;
		}
		return true;
	}

	@Override
	public ItemStack getItem(int slot) {
		return BackpackContainer.storage(this.upgrade(), this.size).getItem(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int amount) {
		ItemStack upgrade = this.upgrade();
		BackpackContainer storage = BackpackContainer.storage(upgrade, this.size);
		ItemStack result = storage.removeItem(slot, amount);
		this.store(upgrade);
		return result;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		ItemStack upgrade = this.upgrade();
		BackpackContainer storage = BackpackContainer.storage(upgrade, this.size);
		ItemStack result = storage.removeItemNoUpdate(slot);
		this.store(upgrade);
		return result;
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		ItemStack upgrade = this.upgrade();
		BackpackContainer.storage(upgrade, this.size).setItem(slot, stack);
		this.store(upgrade);
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public void setChanged() {
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return true;
	}

	@Override
	public void clearContent() {
		ItemStack upgrade = this.upgrade();
		BackpackContainer.storage(upgrade, this.size).clearContent();
		this.store(upgrade);
	}
}