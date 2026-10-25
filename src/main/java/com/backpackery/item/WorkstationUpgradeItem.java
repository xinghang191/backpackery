package com.backpackery.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * Portable workstation upgrades. They open the vanilla menu, so recipes and the server-side
 * recipe manager stay authoritative. Right-clicking the installed upgrade slot opens it from
 * inside the backpack.
 */
public class WorkstationUpgradeItem extends UpgradeItem {
	public enum Kind {
		CRAFTING("crafting_upgrade", "gui.backpackery.workstation.crafting"),
		STONECUTTER("stonecutter_upgrade", "gui.backpackery.workstation.stonecutter"),
		SMITHING("smithing_upgrade", "gui.backpackery.workstation.smithing"),
		ANVIL("anvil_upgrade", "gui.backpackery.workstation.anvil");

		private final String id;
		private final String titleKey;

		Kind(String id, String titleKey) {
			this.id = id;
			this.titleKey = titleKey;
		}

		public String id() {
			return this.id;
		}

		public String titleKey() {
			return this.titleKey;
		}
	}

	private final Kind kind;

	public WorkstationUpgradeItem(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	public Kind kind() {
		return this.kind;
	}

	public void open(Player player) {
		if (player.level().isClientSide()) return;
		Component title = Component.translatable(this.kind.titleKey());

		switch (this.kind) {
			case CRAFTING -> player.openMenu(new SimpleMenuProvider(
				(syncId, inventory, p) -> new CraftingMenu(syncId, inventory), title));
			case STONECUTTER -> player.openMenu(new SimpleMenuProvider(
				(syncId, inventory, p) -> new StonecutterMenu(syncId, inventory), title));
			case SMITHING -> player.openMenu(new SimpleMenuProvider(
				(syncId, inventory, p) -> new SmithingMenu(syncId, inventory), title));
			case ANVIL -> player.openMenu(new SimpleMenuProvider(
				(syncId, inventory, p) -> new AnvilMenu(syncId, inventory), title));
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide()) {
			this.open(player);
		}
		return InteractionResult.SUCCESS;
	}
}