package com.backpackery.client;

import com.backpackery.Backpackery;
import com.backpackery.menu.BackpackMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import com.backpackery.item.FilterUpgradeItem;
import com.backpackery.item.VoidUpgradeItem;
import com.backpackery.item.WorkstationUpgradeItem;

/**
 * Backpack screen laid out like Sophisticated Backpacks:
 *
 * <ul>
 *   <li>9 storage columns up to gold, 12 for diamond / netherite;</li>
 *   <li>upgrade slots in a 25 px strip attached to the left edge;</li>
 *   <li>sort and transfer buttons in the panel corners;</li>
 *   <li>the same 9-slice storage background and slot sprite sheet.</li>
 * </ul>
 *
 * <p>The panel is {@code 114 + rows * 18} tall for every tier, which keeps the player inventory
 * in the exact place the background texture expects it.</p>
 */
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
	private static final Identifier BACKGROUND_9 = Backpackery.id("textures/gui/storage_background_9.png");
	private static final Identifier BACKGROUND_12 = Backpackery.id("textures/gui/storage_background_12.png");
	private static final Identifier SLOTS = Backpackery.id("textures/gui/slots_background.png");
	private static final Identifier CONTROLS = Backpackery.id("textures/gui/gui_controls.png");
	private static final Identifier ICONS = Backpackery.id("textures/gui/icons.png");
	private static final Identifier EMPTY_UPGRADE_SLOT = Backpackery.id("textures/gui/empty_upgrade_slot.png");

	private static final int BUTTON_SIZE = 12;
	private static final int SORT_ICON_U = 0;
	private static final int SORT_ICON_V = 144;
	private static final int SORT_NAME_U = 24;
	private static final int SORT_NAME_V = 144;
	private static final int SORT_COUNT_U = 36;
	private static final int SORT_COUNT_V = 144;
	private static final int SORT_MOD_U = 0;
	private static final int SORT_MOD_V = 156;
	private static final int STORE_ICON_U = 0;
	private static final int STORE_ICON_V = 156;
	private static final int TAKE_ICON_U = 12;
	private static final int TAKE_ICON_V = 156;

	private int sortBy;
	private int previousGuiScale = Integer.MIN_VALUE;

	public BackpackScreen(BackpackMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, menu.panelWidth(), menu.panelHeight());
		this.titleLabelX = 8;
		this.titleLabelY = 6;
		this.inventoryLabelX = menu.invOffset() + 8;
		this.inventoryLabelY = menu.invY() - 11;
		this.fitGuiScale();
	}

	/**
	 * A netherite backpack is 294 GUI pixels tall, which is taller than the 240 px that
	 * vanilla's automatic GUI scale assumes. Temporarily lower the GUI scale so the whole
	 * panel fits on screen; it is restored when the screen closes.
	 */
	private void fitGuiScale() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft == null) return;
		int maxScale = Math.min(
			(minecraft.getWindow().getScreenWidth() - 16) / this.imageWidth,
			(minecraft.getWindow().getScreenHeight() - 16) / this.imageHeight);
		maxScale = Math.max(1, Math.min(4, maxScale));

		int configured = minecraft.options.guiScale().get();
		int effective = configured == 0 ? minecraft.getWindow().getGuiScale() : configured;
		if (effective > maxScale) {
			this.previousGuiScale = configured;
			minecraft.options.guiScale().set(maxScale);
			minecraft.resizeGui();
		}
	}

	private void restoreGuiScale() {
		if (this.previousGuiScale == Integer.MIN_VALUE) return;
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.options.guiScale().set(this.previousGuiScale);
		minecraft.resizeGui();
		this.previousGuiScale = Integer.MIN_VALUE;
	}

	@Override
	public void removed() {
		super.removed();
		restoreGuiScale();
	}

	// ------------------------------------------------------------------ background

	@Override
	protected void slotClicked(Slot slot, int slotId, int button, ContainerInput action) {
		if (button == 1 && action == ContainerInput.PICKUP && slot != null) {
			int storageEnd = this.menu.cols() * this.menu.rows();
			int upgradeEnd = storageEnd + this.menu.upgradeSlots();
			if (slot.index >= storageEnd && slot.index < upgradeEnd) {
				ItemStack upgrade = slot.getItem();
				if (upgrade.getItem() instanceof WorkstationUpgradeItem
					|| upgrade.getItem() instanceof FilterUpgradeItem
					|| upgrade.getItem() instanceof VoidUpgradeItem) {
					this.minecraft.gameMode.handleInventoryButtonClick(
						this.menu.containerId, BackpackMenu.BUTTON_OPEN_UPGRADE + slot.getContainerSlot());
					return;
				}
			}
		}
		super.slotClicked(slot, slotId, button, action);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);

		int x = this.leftPos;
		int y = this.topPos;
		int width = this.imageWidth;
		int rows = this.menu.rows();
		int cols = this.menu.cols();
		Identifier background = cols == 12 ? BACKGROUND_12 : BACKGROUND_9;

		drawStorageBackground(graphics, background, x, y, width, rows * 18);
		graphics.blit(RenderPipelines.GUI_TEXTURED, SLOTS, x + 7, y + 17, 0.0F, 0.0F,
			cols * 18, rows * 18, 256, 256);

		drawUpgradeStrip(graphics, x, y, this.menu.upgradeSlots());
		drawButtons(graphics, mouseX, mouseY);
	}

	/** The exact two-part 9-slice Sophisticated Core uses for storage backgrounds. */
	private void drawStorageBackground(GuiGraphicsExtractor graphics, Identifier texture,
									   int x, int y, int width, int slotsHeight) {
		int half = Math.min(slotsHeight / 2, 150);
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F,
			width, 17 + half, 256, 256);
		int yOffset = 0;
		if (slotsHeight / 2 > 150) {
			int middleHeight = (slotsHeight / 2 - 150) * 2;
			graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y + 17 + half, 0.0F, 17.0F,
				width, middleHeight, 256, 256);
			yOffset = middleHeight;
		}
		int playerInventoryHeight = 97;
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture,
			x, y + yOffset + 17 + half, 0.0F, 256.0F - (playerInventoryHeight + half),
			width, playerInventoryHeight + half, 256, 256);
	}

	private void drawUpgradeStrip(GuiGraphicsExtractor graphics, int x, int y, int slots) {
		if (slots <= 0) return;
		int height = 6 + slots * 16;
		graphics.blit(RenderPipelines.GUI_TEXTURED, CONTROLS, x - 21, y, 0.0F, 0.0F, 26, 4, 256, 256);
		graphics.blit(RenderPipelines.GUI_TEXTURED, CONTROLS, x - 21, y + 4, 0.0F, 4.0F,
			25, height - 4, 256, 256);
		graphics.blit(RenderPipelines.GUI_TEXTURED, CONTROLS, x - 21, y + height, 0.0F, 198.0F,
			25, 6, 256, 256);
		for (int i = 0; i < slots; i++) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, EMPTY_UPGRADE_SLOT,
				x - 15, y + 6 + i * 16, 0.0F, 0.0F, 16, 16, 16, 16);
		}
	}

	// ------------------------------------------------------------------ buttons

	private int sortX() {
		return this.leftPos + this.imageWidth - 31;
	}

	private int sortY() {
		return this.topPos + 4;
	}

	private int transferX() {
		return this.leftPos + this.imageWidth - 31;
	}

	private int transferY() {
		return this.topPos + this.imageHeight - 2;
	}

	private void drawButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		boolean sortHovered = inButton(mouseX, mouseY, sortX(), sortY());
		boolean sortByHovered = inButton(mouseX, mouseY, sortX() + BUTTON_SIZE, sortY());
		boolean storeHovered = inButton(mouseX, mouseY, transferX(), transferY());
		boolean takeHovered = inButton(mouseX, mouseY, transferX() + BUTTON_SIZE, transferY());

		button(graphics, sortX(), sortY(), sortHovered, SORT_ICON_U, SORT_ICON_V);
		int iconU = switch (this.sortBy) {
			case 1 -> SORT_MOD_U;
			case 2 -> SORT_COUNT_U;
			default -> SORT_NAME_U;
		};
		int iconV = switch (this.sortBy) {
			case 1 -> SORT_MOD_V;
			case 2 -> SORT_COUNT_V;
			default -> SORT_NAME_V;
		};
		button(graphics, sortX() + BUTTON_SIZE, sortY(), sortByHovered, iconU, iconV);

		button(graphics, transferX(), transferY(), storeHovered, STORE_ICON_U, STORE_ICON_V);
		button(graphics, transferX() + BUTTON_SIZE, transferY(), takeHovered, TAKE_ICON_U, TAKE_ICON_V);

		if (sortHovered) {
			graphics.setTooltipForNextFrame(this.font,
				Component.translatable("gui.backpackery.button.sort"), mouseX, mouseY);
		} else if (sortByHovered) {
			Component key = switch (this.sortBy) {
				case 1 -> Component.translatable("gui.backpackery.sort_by.mod");
				case 2 -> Component.translatable("gui.backpackery.sort_by.count");
				default -> Component.translatable("gui.backpackery.sort_by.name");
			};
			graphics.setTooltipForNextFrame(this.font, key, mouseX, mouseY);
		} else if (storeHovered) {
			graphics.setTooltipForNextFrame(this.font,
				Component.translatable("gui.backpackery.button.transfer_to_storage"), mouseX, mouseY);
		} else if (takeHovered) {
			graphics.setTooltipForNextFrame(this.font,
				Component.translatable("gui.backpackery.button.transfer_to_inventory"), mouseX, mouseY);
		}
	}

	private void button(GuiGraphicsExtractor graphics, int x, int y, boolean hovered, int iconU, int iconV) {
		if (hovered) {
			graphics.blit(RenderPipelines.GUI_TEXTURED, CONTROLS, x, y, 41.0F, 18.0F,
				BUTTON_SIZE, BUTTON_SIZE, 256, 256);
		} else {
			graphics.blit(RenderPipelines.GUI_TEXTURED, CONTROLS, x, y, 29.0F, 18.0F,
				BUTTON_SIZE, BUTTON_SIZE, 256, 256);
		}
		graphics.blit(RenderPipelines.GUI_TEXTURED, ICONS, x, y, (float) iconU, (float) iconV,
			BUTTON_SIZE, BUTTON_SIZE, 256, 256);
	}

	private boolean inButton(double mouseX, double mouseY, int x, int y) {
		return mouseX >= x && mouseX < x + BUTTON_SIZE && mouseY >= y && mouseY < y + BUTTON_SIZE;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0 && this.minecraft != null && this.minecraft.gameMode != null) {
			int mouseX = (int) event.x();
			int mouseY = (int) event.y();
			if (inButton(mouseX, mouseY, sortX(), sortY())) {
				this.sendButton(BackpackMenu.BUTTON_SORT);
				return true;
			}
			if (inButton(mouseX, mouseY, sortX() + BUTTON_SIZE, sortY())) {
				this.sortBy = (this.sortBy + 1) % 3;
				this.sendButton(BackpackMenu.BUTTON_SORT_BY_NAME + this.sortBy);
				return true;
			}
			if (inButton(mouseX, mouseY, transferX(), transferY())) {
				this.sendButton(BackpackMenu.BUTTON_TRANSFER_TO_STORAGE);
				return true;
			}
			if (inButton(mouseX, mouseY, transferX() + BUTTON_SIZE, transferY())) {
				this.sendButton(BackpackMenu.BUTTON_TRANSFER_TO_INVENTORY);
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	private void sendButton(int id) {
		if (this.minecraft != null && this.minecraft.gameMode != null) {
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
		}
	}
}