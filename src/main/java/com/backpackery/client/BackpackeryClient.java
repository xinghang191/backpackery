package com.backpackery.client;

import com.backpackery.ModMenus;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class BackpackeryClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		for (int tier = 0; tier < ModMenus.TIERS; tier++) {
			MenuScreens.register(ModMenus.forTier(tier), BackpackScreen::new);
		}
		MenuScreens.register(ModMenus.FILTER, FilterScreen::new);
		MenuScreens.register(ModMenus.FILTER_ADVANCED, FilterScreen::new);
	}
}