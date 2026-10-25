package com.backpackery.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/**
 * Backpack contents.
 *
 * <p>Vanilla cannot persist a stack of more than 99: {@link ItemStack#CODEC} limits {@code count} to
 * 1–99, and so does the {@code minecraft:max_stack_size} component. This component therefore writes
 * every stored stack with a count of 1 and keeps the real count in a sibling int field.</p>
 *
 * <p>The slot list is kept as plain {@link ItemStack}s (no {@code ItemStackTemplate} indirection),
 * so nothing downstream can normalise the counts.</p>
 */
public record BackpackContents(List<ItemStack> slots) {
	public static final Codec<BackpackContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.INT.fieldOf("size").forGetter(contents -> contents.slots().size()),
		Stored.CODEC.listOf().fieldOf("items").forGetter(BackpackContents::stored)
	).apply(instance, BackpackContents::fromStored));

	public static final StreamCodec<RegistryFriendlyByteBuf, BackpackContents> STREAM_CODEC =
		new StreamCodec<>() {
			@Override
			public BackpackContents decode(RegistryFriendlyByteBuf buf) {
				int size = buf.readVarInt();
				List<ItemStack> slots = emptySlots(size);
				int stored = buf.readVarInt();
				for (int i = 0; i < stored; i++) {
					int slot = buf.readVarInt();
					ItemStack stack = ItemStack.STREAM_CODEC.decode(buf);
					if (slot >= 0 && slot < size) slots.set(slot, stack);
				}
				return new BackpackContents(slots);
			}

			@Override
			public void encode(RegistryFriendlyByteBuf buf, BackpackContents value) {
				List<ItemStack> slots = value.slots();
				buf.writeVarInt(slots.size());
				int stored = 0;
				for (ItemStack stack : slots) {
					if (!stack.isEmpty()) stored++;
				}
				buf.writeVarInt(stored);
				for (int slot = 0; slot < slots.size(); slot++) {
					ItemStack stack = slots.get(slot);
					if (stack.isEmpty()) continue;
					buf.writeVarInt(slot);
					ItemStack.STREAM_CODEC.encode(buf, stack);
				}
			}
		};

	public static BackpackContents of(List<ItemStack> items) {
		return new BackpackContents(List.copyOf(items));
	}

	public static BackpackContents empty(int size) {
		return new BackpackContents(emptySlots(size));
	}

	/** Items from older builds stored their contents in {@code minecraft:container}. */
	public static BackpackContents fromLegacy(ItemContainerContents legacy, int size) {
		NonNullList<ItemStack> items = NonNullList.withSize(size, ItemStack.EMPTY);
		legacy.copyInto(items);
		return of(items);
	}

	public void copyInto(NonNullList<ItemStack> target) {
		for (int i = 0; i < target.size() && i < this.slots.size(); i++) {
			target.set(i, this.slots.get(i));
		}
	}

	private static List<ItemStack> emptySlots(int size) {
		List<ItemStack> slots = new ArrayList<>(Math.max(size, 0));
		for (int i = 0; i < size; i++) slots.add(ItemStack.EMPTY);
		return slots;
	}

	private List<Stored> stored() {
		List<Stored> out = new ArrayList<>();
		for (int slot = 0; slot < this.slots.size(); slot++) {
			ItemStack stack = this.slots.get(slot);
			if (!stack.isEmpty()) out.add(new Stored(slot, stack));
		}
		return out;
	}

	private static BackpackContents fromStored(int size, List<Stored> stored) {
		int slots = Math.max(size, 0);
		for (Stored entry : stored) slots = Math.max(slots, entry.slot() + 1);
		List<ItemStack> items = emptySlots(slots);
		for (Stored entry : stored) {
			if (entry.slot() >= 0 && entry.slot() < items.size()) items.set(entry.slot(), entry.stack());
		}
		return new BackpackContents(items);
	}

	/** One slot: the stack (written with a count of 1) plus its real count. */
	private record Stored(int slot, ItemStack stack) {
		static final Codec<Stored> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.INT.fieldOf("slot").forGetter(Stored::slot),
			ItemStack.CODEC.fieldOf("item").forGetter(stored -> stored.stack().copyWithCount(1)),
			Codec.INT.fieldOf("count").forGetter(stored -> stored.stack().getCount())
		).apply(instance, (slot, item, count) -> new Stored(slot, item.copyWithCount(count))));
	}
}
