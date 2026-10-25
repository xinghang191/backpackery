package com.backpackery.client;

import com.backpackery.menu.FilterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class FilterScreen extends AbstractContainerScreen<FilterMenu> {
	private static final Identifier CHEST = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");

	public FilterScreen(FilterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, menu.panelHeight());
		this.titleLabelX = 8;
		this.titleLabelY = 6;
		this.inventoryLabelX = 8;
		this.inventoryLabelY = menu.invY() - 11;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);

		int x = this.leftPos;
		int y = this.topPos;
		int top = this.menu.topHeight();

		graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, x, y, 0.0F, 0.0F, 176, top, 256, 256);
		graphics.blit(RenderPipelines.GUI_TEXTURED, CHEST, x, y + top, 0.0F, 126.0F, 176, 96, 256, 256);
	}
}