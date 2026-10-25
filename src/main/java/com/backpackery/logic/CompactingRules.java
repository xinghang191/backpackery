package com.backpackery.logic;

import com.backpackery.ModComponents;
import com.backpackery.item.CompactingUpgradeItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Compacting upgrade: when items are picked up, full 2x2 (basic) or 3x3 (advanced) vanilla
 * crafting recipes are applied to turn them into their compressed form.
 *
 * <p>Recipes are read from the server recipe manager, so datapack recipes work too. Only recipes
 * that fill the whole grid count as compacting recipes: a 7-ingot cauldron recipe must not eat
 * the other two ingots of a 9-ingot stack.</p>
 *
 * <p>Candidates are indexed by ingredient item, so a pickup only checks the handful of recipes
 * that can possibly match the picked-up stack.</p>
 */
public final class CompactingRules {
	private static final int MIN_GRID = 2;
	private static final int MAX_GRID = 3;

	private static Map<Integer, Map<Item, List<ShapedRecipe>>> cache = Map.of();
	private static ServerLevel cacheLevel;

	public static int gridSize(ItemStack backpack) {
		ItemContainerContents upgrades = backpack.get(ModComponents.UPGRADES);
		if (upgrades == null) return 0;
		int grid = 0;
		for (ItemStack upgrade : upgrades.nonEmptyItemCopyStream().toList()) {
			if (upgrade.getItem() instanceof CompactingUpgradeItem compacting) {
				grid = Math.max(grid, compacting.gridSize());
			}
		}
		return grid;
	}

	public static void compact(Container container, ServerLevel level, int grid) {
		Map<Item, List<ShapedRecipe>> byItem = recipes(level, grid);
		if (byItem.isEmpty()) return;

		boolean changed = false;
		for (int slot = 0; slot < container.getContainerSize(); slot++) {
			if (container.getItem(slot).getCount() < 4) continue;
			// Repeat for a slot that holds 64 items: every pass consumes 4 or 9 of them.
			for (int pass = 0; pass < 16; pass++) {
				if (!tryCompact(container, slot, byItem, grid, level)) break;
				changed = true;
			}
		}
		if (changed) container.setChanged();
	}

	private static boolean tryCompact(Container container, int slot, Map<Item, List<ShapedRecipe>> byItem,
									  int grid, ServerLevel level) {
		ItemStack stack = container.getItem(slot);
		if (stack.isEmpty() || stack.getCount() < 4) return false;

		for (ShapedRecipe recipe : byItem.getOrDefault(stack.getItem(), List.of())) {
			int cells = cellsOf(recipe);
			if (cells != grid * grid) continue;

			CraftingInput input = inputFor(recipe, stack, grid);
			if (input == null || !recipe.matches(input, level)) continue;

			if (stack.getCount() < cells) continue;

			ItemStack output = recipe.assemble(input);
			if (output.isEmpty() || output.getCount() <= 0) continue;

			List<ItemStack> simulation = snapshot(container);
			ItemStack remainingInput = simulation.get(slot).copy();
			remainingInput.shrink(cells);
			simulation.set(slot, remainingInput.isEmpty() ? ItemStack.EMPTY : remainingInput);

			ItemStack remainder = insert(simulation, output.copy(), container);
			if (!remainder.isEmpty()) continue; // no room for the compressed item: do not consume anything

			write(container, simulation);
			return true;
		}
		return false;
	}

	/** A grid of the recipe's shape with one item per non-empty pattern cell, or null if it cannot fit. */
	private static CraftingInput inputFor(ShapedRecipe recipe, ItemStack stack, int grid) {
		int width = recipe.getWidth();
		int height = recipe.getHeight();
		if (width > grid || height > grid) return null;

		List<Optional<Ingredient>> ingredients = recipe.getIngredients();
		List<ItemStack> items = new ArrayList<>(9);
		for (int i = 0; i < 9; i++) items.add(ItemStack.EMPTY);

		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				int index = row * width + col;
				if (index >= ingredients.size() || ingredients.get(index).isEmpty()) continue;
				Ingredient ingredient = ingredients.get(index).get();
				if (!ingredient.test(stack)) return null;
				items.set(row * 3 + col, stack.copyWithCount(1));
			}
		}
		return CraftingInput.of(3, 3, items);
	}

	private static int cellsOf(ShapedRecipe recipe) {
		int cells = 0;
		for (Optional<Ingredient> ingredient : recipe.getIngredients()) {
			if (ingredient.isPresent()) cells++;
		}
		return cells;
	}

	private static Map<Item, List<ShapedRecipe>> recipes(ServerLevel level, int grid) {
		if (cacheLevel != level) build(level);
		return cache.getOrDefault(grid, Map.of());
	}

	private static void build(ServerLevel level) {
		Map<Integer, Map<Item, List<ShapedRecipe>>> grids = new HashMap<>();
		for (int grid = MIN_GRID; grid <= MAX_GRID; grid++) {
			grids.put(grid, new HashMap<>());
		}

		for (RecipeHolder<?> holder : level.recipeAccess().getRecipes()) {
			if (!(holder.value() instanceof ShapedRecipe recipe)) continue;
			if (recipe.isSpecial()) continue;
			if (recipe.getWidth() > MAX_GRID || recipe.getHeight() > MAX_GRID) continue;

			int cells = cellsOf(recipe);
			Set<Item> items = itemsOf(recipe);
			if (items.isEmpty()) continue;

			for (int grid = MIN_GRID; grid <= MAX_GRID; grid++) {
				if (recipe.getWidth() > grid || recipe.getHeight() > grid) continue;
				if (cells != grid * grid) continue;
				Map<Item, List<ShapedRecipe>> byItem = grids.get(grid);
				for (Item item : items) {
					byItem.computeIfAbsent(item, key -> new ArrayList<>()).add(recipe);
				}
			}
		}

		Map<Integer, Map<Item, List<ShapedRecipe>>> frozen = new HashMap<>();
		for (Map.Entry<Integer, Map<Item, List<ShapedRecipe>>> entry : grids.entrySet()) {
			Map<Item, List<ShapedRecipe>> byItem = new HashMap<>();
			for (Map.Entry<Item, List<ShapedRecipe>> inner : entry.getValue().entrySet()) {
				byItem.put(inner.getKey(), List.copyOf(inner.getValue()));
			}
			frozen.put(entry.getKey(), Map.copyOf(byItem));
		}

		cache = Map.copyOf(frozen);
		cacheLevel = level;
	}

	private static Set<Item> itemsOf(ShapedRecipe recipe) {
		Set<Item> items = new HashSet<>();
		for (Optional<Ingredient> ingredient : recipe.getIngredients()) {
			if (ingredient.isEmpty()) continue;
			ingredient.get().items().forEach(holder -> items.add(holder.value()));
		}
		return items;
	}

	private static List<ItemStack> snapshot(Container container) {
		List<ItemStack> stacks = new ArrayList<>(container.getContainerSize());
		for (int i = 0; i < container.getContainerSize(); i++) {
			stacks.add(container.getItem(i).copy());
		}
		return stacks;
	}

	private static void write(Container container, List<ItemStack> stacks) {
		for (int i = 0; i < stacks.size(); i++) {
			container.setItem(i, stacks.get(i));
		}
	}

	private static ItemStack insert(List<ItemStack> stacks, ItemStack stack, Container container) {
		ItemStack remaining = stack.copy();
		int capacity = Math.max(1, container.getMaxStackSize(stack));
		for (int pass = 0; pass < 2 && !remaining.isEmpty(); pass++) {
			for (int i = 0; i < stacks.size() && !remaining.isEmpty(); i++) {
				ItemStack existing = stacks.get(i);
				if (pass == 0) {
					if (!existing.isEmpty()
						&& PickupLogic.sameForMerge(existing, remaining)
						&& existing.getCount() < capacity) {
						int move = Math.min(remaining.getCount(), capacity - existing.getCount());
						existing.grow(move);
						remaining.shrink(move);
					}
				} else if (existing.isEmpty()) {
					stacks.set(i, remaining.copy());
					remaining = ItemStack.EMPTY;
				}
			}
		}
		return remaining;
	}

	private CompactingRules() {
	}
}