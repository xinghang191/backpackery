package com.backpackery.menu;

import com.backpackery.inventory.BackpackContainer;
import com.backpackery.item.UpgradeItem;
import com.backpackery.logic.StackRules;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * One upgrade slot.
 *
 * <p>Mirrors {@code StackUpgradeItem#canRemoveUpgradeFrom} / {@code isMultiplierHighEnough} of
 * Sophisticated Core: a stack upgrade may not be pulled out (or swapped for a weaker one) while the
 * backpack still holds a stack that needs its multiplier — otherwise the contents would overflow
 * the slot limit, which is how item loss happens.</p>
 */
public class UpgradeSlot extends Slot {
	public UpgradeSlot(Container container, int slot, int x, int y) {
		super(container, slot, x, y);
	}

	@Override
	public boolean mayPlace(ItemStack stack) {
		if (!(stack.getItem() instanceof UpgradeItem)) return false;
		ItemStack backpack = backpack();
		if (backpack == null || backpack.isEmpty()) return true;
		// a swap must not leave the remaining contents above the new limit
		double after = StackRules.multiplierReplacing(backpack, this.getContainerSlot(), stack);
		return StackRules.oversizedSlots(backpack, after).isEmpty();
	}

	@Override
	public boolean mayPickup(Player player) {
		ItemStack backpack = backpack();
		if (backpack == null || backpack.isEmpty()) return true;
		if (StackRules.multiplierOf(getItem()) <= 1) return true;
		double after = StackRules.multiplierWithout(backpack, this.getContainerSlot());
		List<Integer> oversized = StackRules.oversizedSlots(backpack, after);
		if (oversized.isEmpty()) return true;
		if (player != null && !player.level().isClientSide()) {
			player.sendSystemMessage(Component.translatable(
				"message.backpackery.upgrade.contents_too_big", oversized.get(0) + 1));
		}
		return false;
	}

	private ItemStack backpack() {
		return this.container instanceof BackpackContainer backpack ? backpack.host() : null;
	}

	@Override
	public int getMaxStackSize() {
		return 1;
	}
}
