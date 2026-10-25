package com.backpackery.logic;

import com.backpackery.inventory.BackpackContainer;
import com.backpackery.item.BackpackItem;
import com.backpackery.item.FeedingUpgradeItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/** Feeding upgrade: eats food from the backpack when the player is hungry. */
public final class FeedingRules {
	public static void tick(ServerPlayer player) {
		if (player.getFoodData().getFoodLevel() >= 20) return;

		ItemStack backpack = findFeedingBackpack(player);
		if (backpack.isEmpty()) return;

		BackpackContainer storage = BackpackContainer.storage(backpack, ((BackpackItem) backpack.getItem()).slots());
		for (int i = 0; i < storage.getContainerSize(); i++) {
			ItemStack stack = storage.getItem(i);
			if (stack.isEmpty()) continue;
			FoodProperties food = stack.get(DataComponents.FOOD);
			if (food == null || food.nutrition() <= 0) continue;

			player.getFoodData().eat(food.nutrition(), food.saturation());
			stack.shrink(1);
			if (stack.isEmpty()) storage.setItem(i, ItemStack.EMPTY);
			else storage.setItem(i, stack);
			storage.setChanged();
			return;
		}
	}

	private static ItemStack findFeedingBackpack(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.getItem() instanceof BackpackItem && PickupLogic.hasUpgrade(stack, FeedingUpgradeItem.class)) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	private FeedingRules() {
	}
}