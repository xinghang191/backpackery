package com.backpackery.logic;

import com.backpackery.inventory.BackpackContainer;
import com.backpackery.item.BackpackItem;
import com.backpackery.item.CookingUpgradeItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.List;

/**
 * Cooking upgrades. The basic and auto variants process one stored item per second while the
 * backpack is anywhere in the player's inventory. Fuel is consumed one item at a time, which is
 * intentionally simple but keeps the upgrade useful without exposing a furnace GUI.
 */
public final class CookingRules {
	public static void tick(ServerPlayer player) {
		for (ItemStack backpack : backpacks(player)) {
			if (!(backpack.getItem() instanceof BackpackItem item)) continue;
			CookingUpgradeItem cooking = cookingUpgrade(backpack);
			if (cooking == null) continue;
			process(player, backpack, item.slots(), cooking.kind());
		}
	}

	private static List<ItemStack> backpacks(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		List<ItemStack> out = new java.util.ArrayList<>();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.getItem() instanceof BackpackItem) out.add(stack);
		}
		return out;
	}

	private static CookingUpgradeItem cookingUpgrade(ItemStack backpack) {
		var upgrades = backpack.get(com.backpackery.ModComponents.UPGRADES);
		if (upgrades == null) return null;
		for (ItemStack upgrade : upgrades.nonEmptyItemCopyStream().toList()) {
			if (upgrade.getItem() instanceof CookingUpgradeItem cooking) return cooking;
		}
		return null;
	}

	private static void process(ServerPlayer player, ItemStack backpack, int slots, CookingUpgradeItem.Kind kind) {
		BackpackContainer storage = BackpackContainer.storage(backpack, slots);
		for (int inputSlot = 0; inputSlot < storage.getContainerSize(); inputSlot++) {
			ItemStack input = storage.getItem(inputSlot);
			if (input.isEmpty()) continue;

			ItemStack single = input.copyWithCount(1);
			ItemStack result = result(player, single, kind);
			if (result.isEmpty()) continue;

			int fuelSlot = findFuel(storage, inputSlot);
			if (fuelSlot < 0) continue;
			if (!hasRoom(storage, result)) continue;

			ItemStack fuel = storage.getItem(fuelSlot);
			fuel.shrink(1);
			storage.setItem(fuelSlot, fuel.isEmpty() ? ItemStack.EMPTY : fuel);
			input.shrink(1);
			storage.setItem(inputSlot, input.isEmpty() ? ItemStack.EMPTY : input);

			ItemStack leftover = PickupLogic.insert(storage, result.copy());
			if (!leftover.isEmpty()) {
				// hasRoom() guaranteed a place; put the result back rather than deleting it.
				storage.setItem(inputSlot, leftover);
			}
			storage.setChanged();
			return;
		}
	}

	private static ItemStack result(ServerPlayer player, ItemStack input, CookingUpgradeItem.Kind kind) {
		ServerLevel level = player.level();
		SingleRecipeInput recipeInput = new SingleRecipeInput(input);
		var manager = player.level().getServer().getRecipeManager();

		return switch (kind) {
			case SMELTING, AUTO_SMELTING -> manager.getRecipeFor(RecipeType.SMELTING, recipeInput, level)
				.map(holder -> holder.value().assemble(recipeInput)).orElse(ItemStack.EMPTY);
			case BLASTING, AUTO_BLASTING -> manager.getRecipeFor(RecipeType.BLASTING, recipeInput, level)
				.map(holder -> holder.value().assemble(recipeInput)).orElse(ItemStack.EMPTY);
			case SMOKING, AUTO_SMOKING -> manager.getRecipeFor(RecipeType.SMOKING, recipeInput, level)
				.map(holder -> holder.value().assemble(recipeInput)).orElse(ItemStack.EMPTY);
		};
	}

	private static int findFuel(BackpackContainer storage, int inputSlot) {
		for (int i = 0; i < storage.getContainerSize(); i++) {
			if (i == inputSlot) continue;
			ItemStack stack = storage.getItem(i);
			if (!stack.isEmpty() && fuelTicks(stack) > 0) return i;
		}
		return -1;
	}

	private static boolean hasRoom(BackpackContainer storage, ItemStack result) {
		int remaining = result.getCount();
		for (int i = 0; i < storage.getContainerSize() && remaining > 0; i++) {
			ItemStack existing = storage.getItem(i);
			if (existing.isEmpty()) return true;
			if (!PickupLogic.sameForMerge(existing, result)) continue;
			int capacity = storage.getMaxStackSize(existing);
			if (existing.getCount() < capacity) remaining -= capacity - existing.getCount();
		}
		return remaining <= 0;
	}

	private static int fuelTicks(ItemStack stack) {
		if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) return 1600;
		if (stack.is(Items.COAL_BLOCK)) return 16000;
		if (stack.is(Items.BLAZE_ROD)) return 2400;
		if (stack.is(Items.LAVA_BUCKET)) return 20000;
		if (stack.is(Items.DRIED_KELP_BLOCK)) return 4001;
		if (stack.is(Items.STICK)) return 100;
		if (stack.is(Items.BAMBOO)) return 50;
		if (stack.is(Items.CRAFTING_TABLE) || stack.is(Items.CHEST) || stack.is(Items.BOOKSHELF)) return 300;
		if (stack.is(Items.OAK_PLANKS) || stack.is(Items.SPRUCE_PLANKS) || stack.is(Items.BIRCH_PLANKS)
			|| stack.is(Items.JUNGLE_PLANKS) || stack.is(Items.ACACIA_PLANKS) || stack.is(Items.DARK_OAK_PLANKS)
			|| stack.is(Items.MANGROVE_PLANKS) || stack.is(Items.CHERRY_PLANKS) || stack.is(Items.BAMBOO_PLANKS)
			|| stack.is(Items.CRIMSON_PLANKS) || stack.is(Items.WARPED_PLANKS)) return 300;
		if (stack.is(Items.OAK_LOG) || stack.is(Items.SPRUCE_LOG) || stack.is(Items.BIRCH_LOG)
			|| stack.is(Items.JUNGLE_LOG) || stack.is(Items.ACACIA_LOG) || stack.is(Items.DARK_OAK_LOG)
			|| stack.is(Items.MANGROVE_LOG) || stack.is(Items.CHERRY_LOG) || stack.is(Items.CRIMSON_STEM)
			|| stack.is(Items.WARPED_STEM)) return 300;
		return 0;
	}

	private CookingRules() {
	}
}