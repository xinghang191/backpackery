package com.backpackery;

import com.backpackery.inventory.BackpackContents;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.ItemContainerContents;

public final class ModComponents {
	/** Installed upgrades, one stack per slot, always count 1. */
	public static final DataComponentType<ItemContainerContents> UPGRADES = register(
		"upgrades",
		builder -> builder
			.persistent(ItemContainerContents.CODEC)
			.networkSynchronized(ItemContainerContents.STREAM_CODEC)
			.cacheEncoding());

	/** Backpack storage; count is kept outside {@link net.minecraft.world.item.ItemStack#CODEC}. */
	// no cacheEncoding(): counts above 99 are legal here but not in the cached patch form
	public static final DataComponentType<BackpackContents> STORAGE = register(
		"storage",
		builder -> builder
			.persistent(BackpackContents.CODEC)
			.networkSynchronized(BackpackContents.STREAM_CODEC));

	/** Filter mode: false = whitelist, true = blacklist. */
	public static final DataComponentType<Boolean> FILTER_BLACKLIST = register(
		"filter_blacklist",
		builder -> builder
			.persistent(Codec.BOOL)
			.networkSynchronized(ByteBufCodecs.BOOL));

	private static <T> DataComponentType<T> register(String name,
													 java.util.function.UnaryOperator<DataComponentType.Builder<T>> operator) {
		return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Backpackery.id(name), operator.apply(
			DataComponentType.builder()).build());
	}

	private ModComponents() {
	}
}
