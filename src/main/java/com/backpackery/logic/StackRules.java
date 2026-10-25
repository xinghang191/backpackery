package com.backpackery.logic;

import com.backpackery.ModComponents;
import com.backpackery.inventory.BackpackContents;
import com.backpackery.item.StackUpgradeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/**
 * Slot limits for backpack storage.
 *
 * <p>Mirrors Sophisticated Core: the storage slot limit is {@code 64 × product(all stack upgrade
 * multipliers)} ({@code StackUpgradeItem#getInventorySlotLimit}), and an item is never pushed past
 * {@code itsOwnMaxStackSize × multiplier} — which is exactly the invariant their
 * {@code isMultiplierHighEnough} guard checks before letting a stack upgrade be removed.</p>
 */
public final class StackRules {
	/** Multiplier per upgrade tier; index 0 = no stack upgrade. */
	public static final int[] MULTIPLIER = {1, 2, 4, 8, 16};
	/** Highest multiplier a backpack can reach, so a slot never explodes into silly numbers. */
	public static final double MAX_MULTIPLIER = 64;
	/** Absolute per-slot ceiling. */
	public static final int HARD_CAP = 4096;

	/** Multiplier contributed by one upgrade stack (1 when it is not a stack upgrade). */
	public static int multiplierOf(ItemStack upgrade) {
		if (upgrade == null || upgrade.isEmpty()) return 1;
		if (!(upgrade.getItem() instanceof StackUpgradeItem stackUpgrade)) return 1;
		int index = Math.max(0, Math.min(stackUpgrade.tier(), MULTIPLIER.length - 1));
		return MULTIPLIER[index];
	}

	/** Largest upgrade-slot count of any tier (netherite); the actual count lives on the item. */
	public static final int MAX_UPGRADE_SLOTS = 7;

	public static double multiplier(ItemStack backpack) {
		return clampProduct(product(backpack, -1, ItemStack.EMPTY));
	}

	/** Multiplier the backpack would have if the upgrade in {@code skipSlot} were not installed. */
	public static double multiplierWithout(ItemStack backpack, int skipSlot) {
		return clampProduct(product(backpack, skipSlot, ItemStack.EMPTY));
	}

	/** Multiplier the backpack would have if the upgrade in {@code slot} were swapped for {@code replacement}. */
	public static double multiplierReplacing(ItemStack backpack, int slot, ItemStack replacement) {
		return clampProduct(product(backpack, slot, ItemStack.EMPTY) * multiplierOf(replacement));
	}

	private static double product(ItemStack backpack, int skipSlot, ItemStack ignored) {
		ItemContainerContents upgrades = backpack.get(ModComponents.UPGRADES);
		if (upgrades == null) return 1.0;
		List<ItemStack> slots = upgrades.itemCopies().toList();
		double product = 1.0;
		for (int i = 0; i < slots.size(); i++) {
			if (i == skipSlot) continue;
			product *= multiplierOf(slots.get(i));
		}
		return product;
	}

	private static double clampProduct(double product) {
		return Math.max(1.0, Math.min(product, MAX_MULTIPLIER));
	}

	/**
	 * Capacity of one slot for this particular item.
	 * <p>Unstackable items (tools, armour, potions) are never widened — a stack of two swords would
	 * be item duplication.</p>
	 */
	public static int capacityFor(ItemStack backpack, ItemStack stack) {
		int base = baseLimit(stack);
		if (base <= 1) return 1;
		return Math.min((int) (base * multiplier(backpack)), HARD_CAP);
	}

	/** Capacity used when no item is at hand. */
	public static int genericCapacity(ItemStack backpack) {
		return Math.min((int) (64 * multiplier(backpack)), HARD_CAP);
	}

	public static int baseLimit(ItemStack stack) {
		return Math.max(1, stack.getItem().getDefaultMaxStackSize());
	}

	/** How many times the item's own stack limit a stored stack occupies, rounded up. */
	public static int neededMultiplier(ItemStack stack) {
		int base = baseLimit(stack);
		if (base <= 1 || stack.getCount() <= 1) return 1;
		return (int) Math.ceil(stack.getCount() / (double) base);
	}

	/** Slots whose contents no longer fit into a storage with {@code multiplier}. */
	public static List<Integer> oversizedSlots(ItemStack backpack, double multiplier) {
		List<Integer> out = new ArrayList<>();
		BackpackContents contents = backpack.get(ModComponents.STORAGE);
		if (contents == null) return out;
		List<ItemStack> slots = contents.slots();
		for (int slot = 0; slot < slots.size(); slot++) {
			ItemStack stack = slots.get(slot);
			if (stack.isEmpty() || stack.getCount() <= 1) continue;
			if (neededMultiplier(stack) > multiplier) out.add(slot);
		}
		return out;
	}

	private StackRules() {
	}
}
