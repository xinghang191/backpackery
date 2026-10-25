package com.backpackery.logic;

import com.backpackery.ModComponents;
import com.backpackery.inventory.BackpackContainer;
import com.backpackery.item.BackpackItem;
import com.backpackery.item.MagnetUpgradeItem;
import com.backpackery.item.PickupUpgradeItem;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;

public final class PickupLogic {
	public static final double PICKUP_RADIUS = 2.0;
	public static final double MAGNET_RADIUS = 10.0;
	private static final int INTERVAL = 10;

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % INTERVAL != 0) return;
			boolean feed = server.getTickCount() % 20 == 0;
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				// Pickup / magnet upgrades work from anywhere in the player's inventory, not only
				// while the backpack is held. The selected slot and the offhand are part of this
				// inventory, so no separate hand pass is needed.
				for (ItemStack stack : inventoryBackpacks(player)) {
					BackpackItem backpack = (BackpackItem) stack.getItem();
					double radius = radius(stack);
					if (radius <= 0) continue;
					pullIn(player, stack, backpack.slots(), radius);
				}
				if (feed) {
					FeedingRules.tick(player);
					CookingRules.tick(player);
				}
			}
		});
	}

	public static double radius(ItemStack stack) {
		ItemContainerContents upgrades = stack.get(ModComponents.UPGRADES);
		if (upgrades == null) return 0.0;
		double radius = 0.0;
		for (ItemStack upgrade : upgrades.nonEmptyItemCopyStream().toList()) {
			if (upgrade.getItem() instanceof MagnetUpgradeItem magnet) {
				radius = Math.max(radius, magnet.radius());
			} else if (upgrade.getItem() instanceof PickupUpgradeItem pickup) {
				radius = Math.max(radius, pickup.radius());
			}
		}
		return radius;
	}

	public static boolean hasUpgrade(ItemStack stack, Class<?> type) {
		ItemContainerContents upgrades = stack.get(ModComponents.UPGRADES);
		if (upgrades == null) return false;
		return upgrades.nonEmptyItemCopyStream().anyMatch(s -> type.isInstance(s.getItem()));
	}

	public static boolean hasPickup(ItemStack stack) {
		return radius(stack) > 0;
	}

	/** Every backpack carried anywhere in the player's inventory (hotbar, main, offhand, armor). */
	public static List<ItemStack> inventoryBackpacks(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		List<ItemStack> out = new java.util.ArrayList<>();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (stack.getItem() instanceof BackpackItem) out.add(stack);
		}
		return out;
	}

	private static void pullIn(ServerPlayer player, ItemStack stack, int slots, double radius) {
		List<ItemEntity> drops = player.level().getEntitiesOfClass(
			ItemEntity.class,
			player.getBoundingBox().inflate(radius),
			entity -> entity.isAlive() && !entity.getItem().isEmpty());
		if (drops.isEmpty()) return;

		BackpackContainer container = BackpackContainer.storage(stack, slots);
		boolean changed = false;

		for (ItemEntity drop : drops) {
			ItemStack candidate = drop.getItem();

			if (VoidRules.shouldVoid(stack, candidate)) {
				drop.discard();
				changed = true;
				continue;
			}
			if (!FilterRules.accepts(stack, candidate)) continue;

			ItemStack remaining = insert(container, candidate);
			changed = true;
			if (remaining.isEmpty()) {
				drop.discard();
			} else {
				drop.setItem(remaining);
			}
		}
		if (changed) {
			container.setChanged();
			int grid = CompactingRules.gridSize(stack);
			if (grid > 0 && player.level() instanceof ServerLevel serverLevel) {
				CompactingRules.compact(container, serverLevel, grid);
			}
		}
	}

	public static ItemStack insert(Container container, ItemStack stack) {
		ItemStack remaining = stack.copy();
		int capacity = container.getMaxStackSize(stack);
		for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
			for (int i = 0; i < container.getContainerSize() && !remaining.isEmpty(); i++) {
				ItemStack existing = container.getItem(i);
				if (pass == 0) {
					if (!existing.isEmpty()
						&& sameForMerge(existing, remaining)
						&& existing.getCount() < capacity) {
						int move = Math.min(remaining.getCount(), capacity - existing.getCount());
						existing.grow(move);
						remaining.shrink(move);
						container.setItem(i, existing);
					}
				} else if (existing.isEmpty()) {
					ItemStack placed = remaining.copy();
					container.setItem(i, placed);
					remaining = ItemStack.EMPTY;
				}
			}
		}
		return remaining;
	}

	/**
	 * Widened stacks carry a {@code minecraft:max_stack_size} component, so a plain stack of the
	 * same item would otherwise look like a different item and refuse to merge.
	 */
	public static boolean sameForMerge(ItemStack a, ItemStack b) {
		if (!ItemStack.isSameItem(a, b)) return false;
		ItemStack x = a.copy();
		ItemStack y = b.copy();
		x.remove(DataComponents.MAX_STACK_SIZE);
		y.remove(DataComponents.MAX_STACK_SIZE);
		return ItemStack.isSameItemSameComponents(x, y);
	}

	private PickupLogic() {
	}
}
