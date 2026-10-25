package com.backpackery.logic;

import com.backpackery.Backpackery;
import com.backpackery.ModItems;
import com.backpackery.ModMenus;
import com.backpackery.inventory.BackpackContainer;
import com.backpackery.logic.StackRules;
import com.backpackery.item.BackpackItem;
import com.backpackery.item.FilterUpgradeItem;
import com.backpackery.item.PickupUpgradeItem;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.SimpleContainer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class SelfTestCommand {
	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
			dispatcher.register(Commands.literal("backpackery")
				.then(Commands.literal("selftest").executes(context -> {
					String report = run(context.getSource());
					context.getSource().sendSuccess(() -> Component.literal(report), false);
					Backpackery.LOGGER.info("[backpackery][selftest] {}", report);
					return 1;
				}))
				));
		if (Boolean.getBoolean("backpackery.selftest")) {
			ServerLifecycleEvents.SERVER_STARTED.register(server -> {
				try {
					String report = run(server.createCommandSourceStack());
					Backpackery.LOGGER.info("[backpackery][selftest:auto] {}", report);
				} catch (Throwable throwable) {
					Backpackery.LOGGER.error("[backpackery][selftest:auto] crashed", throwable);
				} finally {
					server.halt(false);
				}
			});
		}
	}

	public static String run(CommandSourceStack source) {
		List<String> out = new ArrayList<>();
		boolean ok = true;

		List<String> expectedRecipes = List.of(
			"backpack", "copper_backpack", "iron_backpack", "gold_backpack", "diamond_backpack", "netherite_backpack",
			"upgrade_base",
			"pickup_upgrade", "advanced_pickup_upgrade",
			"filter_upgrade", "advanced_filter_upgrade",
			"magnet_upgrade", "advanced_magnet_upgrade",
			"void_upgrade", "advanced_void_upgrade",
			"compacting_upgrade", "advanced_compacting_upgrade",
			"feeding_upgrade", "advanced_feeding_upgrade",
			"crafting_upgrade", "stonecutter_upgrade", "smithing_upgrade", "anvil_upgrade",
			"smelting_upgrade", "blasting_upgrade", "smoking_upgrade",
			"auto_smelting_upgrade", "auto_blasting_upgrade", "auto_smoking_upgrade",
			"stack_upgrade_1", "stack_upgrade_2", "stack_upgrade_3", "stack_upgrade_4"
		);
		var loadedRecipes = new HashSet<String>();
		source.getServer().getRecipeManager().getRecipes()
			.forEach(holder -> loadedRecipes.add(holder.id().identifier().getPath()));
		List<String> missingRecipes = expectedRecipes.stream().filter(path -> !loadedRecipes.contains(path)).toList();
		boolean allRecipes = missingRecipes.isEmpty();
		ok &= allRecipes;
		out.add("allRecipes=" + allRecipes + (missingRecipes.isEmpty() ? "" : ",missing=" + missingRecipes));

		ItemStack stack = new ItemStack(ModItems.ALL.get(5));
		BackpackContainer storage = BackpackContainer.storage(stack, ModItems.slotsOf(5));
		storage.setItem(0, new ItemStack(Items.DIRT, 32));
		storage.setItem(1, new ItemStack(Items.STONE, 16));

		BackpackContainer reopened = BackpackContainer.storage(stack, ModItems.slotsOf(5));
		boolean roundTrip = reopened.getItem(0).getCount() == 32 && reopened.getItem(0).is(Items.DIRT)
			&& reopened.getItem(1).getCount() == 16 && reopened.getItem(1).is(Items.STONE)
			&& reopened.getItem(2).isEmpty();
		ok &= roundTrip;
		out.add("roundtrip=" + roundTrip);
		boolean hasStorage = stack.has(com.backpackery.ModComponents.STORAGE);
		ok &= hasStorage;
		out.add("storageComponent=" + hasStorage);

		BackpackContainer upgrades = BackpackContainer.upgrades(stack, StackRules.MAX_UPGRADE_SLOTS);
		upgrades.setItem(2, new ItemStack(ModItems.PICKUP_UPGRADE));
		boolean upgradeRoundTrip = BackpackContainer.upgrades(stack, StackRules.MAX_UPGRADE_SLOTS).getItem(2).getItem() instanceof PickupUpgradeItem;
		ok &= upgradeRoundTrip;
		out.add("upgradeRoundtrip=" + upgradeRoundTrip);
		boolean detected = PickupLogic.hasPickup(stack);
		ok &= detected;
		out.add("pickupDetected=" + detected);

		ServerPlayer testPlayer = source.getPlayer();
		if (testPlayer != null) {
			ItemStack carried = new ItemStack(ModItems.ALL.get(1));
			BackpackContainer.upgrades(carried, StackRules.MAX_UPGRADE_SLOTS)
				.setItem(0, new ItemStack(ModItems.PICKUP_UPGRADE));
			ItemStack previous = testPlayer.getInventory().getItem(0);
			testPlayer.getInventory().setItem(0, carried);
			boolean inventoryDetected = PickupLogic.inventoryBackpacks(testPlayer).stream()
				.anyMatch(candidate -> PickupLogic.radius(candidate) > 0.0);
			ok &= inventoryDetected;
			out.add("inventoryPickupDetected=" + inventoryDetected);
			testPlayer.getInventory().setItem(0, previous);
		}

		ItemStack leftover = PickupLogic.insert(reopened, new ItemStack(Items.DIRT, 40));
		int dirt = reopened.getItem(0).getCount() + reopened.getItem(2).getCount();
		boolean merged = leftover.isEmpty() && dirt == 72 && reopened.getItem(0).getCount() == 64;
		ok &= merged;
		out.add("mergeInsert=" + merged + "(slot0=" + reopened.getItem(0).getCount() + ",slot2=" + reopened.getItem(2).getCount() + ")");

		ItemStack full = new ItemStack(ModItems.ALL.get(0));
		BackpackContainer tiny = BackpackContainer.storage(full, ModItems.slotsOf(0));
		for (int i = 0; i < tiny.getContainerSize(); i++) tiny.setItem(i, new ItemStack(Items.COBBLESTONE, 64));
		ItemStack overflow = PickupLogic.insert(tiny, new ItemStack(Items.COBBLESTONE, 10));
		boolean overflowKept = overflow.getCount() == 10;
		ok &= overflowKept;
		out.add("overflowKept=" + overflowKept);

		// filters ---------------------------------------------------------
		ItemStack filtered = new ItemStack(ModItems.ALL.get(3));
		ItemStack filterUpgrade = new ItemStack(ModItems.FILTER_UPGRADE);
		BackpackContainer filterContents = BackpackContainer.storage(filterUpgrade, FilterUpgradeItem.FILTER_SLOTS);
		filterContents.setItem(0, new ItemStack(Items.DIRT));
		BackpackContainer.upgrades(filtered, StackRules.MAX_UPGRADE_SLOTS).setItem(0, filterUpgrade);
		out.add("filterDebug(stored=" + filterContents.getItem(0) + ",collected=" + FilterRules.collect(filtered).size()
			+ ",upgrades=" + upgradesComponentSize(filtered) + ")");
		boolean allowDirt = FilterRules.accepts(filtered, new ItemStack(Items.DIRT));
		boolean denyStone = !FilterRules.accepts(filtered, new ItemStack(Items.STONE));
		ok &= allowDirt && denyStone;
		out.add("filterAllowDirt=" + allowDirt + ",filterDenyStone=" + denyStone);
		boolean emptyFilterAllowsAll = FilterRules.accepts(new ItemStack(ModItems.ALL.get(0)), new ItemStack(Items.STONE));
		ok &= emptyFilterAllowsAll;
		out.add("noFilterAllowsAll=" + emptyFilterAllowsAll);

		ItemStack blacklister = new ItemStack(ModItems.ALL.get(4));
		ItemStack blacklistUpgrade = new ItemStack(ModItems.FILTER_UPGRADE);
		BackpackContainer blacklistContents = BackpackContainer.storage(blacklistUpgrade, FilterUpgradeItem.FILTER_SLOTS);
		blacklistContents.setItem(0, new ItemStack(Items.COBBLESTONE));
		blacklistUpgrade.set(com.backpackery.ModComponents.FILTER_BLACKLIST, true);
		BackpackContainer.upgrades(blacklister, StackRules.MAX_UPGRADE_SLOTS).setItem(0, blacklistUpgrade);
		boolean blacklistPassesOther = FilterRules.accepts(blacklister, new ItemStack(Items.DIRT));
		boolean blacklistBlocksListed = !FilterRules.accepts(blacklister, new ItemStack(Items.COBBLESTONE));
		ok &= blacklistPassesOther && blacklistBlocksListed;
		out.add("blacklistPassDirt=" + blacklistPassesOther + ",blacklistBlockCobble=" + blacklistBlocksListed);

		// radii -----------------------------------------------------------
		ItemStack magnetised = new ItemStack(ModItems.ALL.get(1));
		BackpackContainer.upgrades(magnetised, StackRules.MAX_UPGRADE_SLOTS).setItem(0, new ItemStack(ModItems.MAGNET_UPGRADE));
		boolean magnetRadius = PickupLogic.radius(magnetised) == PickupLogic.MAGNET_RADIUS;
		boolean noUpgradeRadius = PickupLogic.radius(new ItemStack(ModItems.ALL.get(0))) == 0.0;
		boolean pickupTight = PickupLogic.PICKUP_RADIUS <= 2.5
			&& PickupLogic.MAGNET_RADIUS >= 9.5 && PickupLogic.MAGNET_RADIUS <= 10.5;
		ok &= magnetRadius && noUpgradeRadius && pickupTight;
		out.add("magnetRadius=" + magnetRadius + ",idleRadius=" + noUpgradeRadius
			+ ",radii(pickup=" + PickupLogic.PICKUP_RADIUS + ",magnet=" + PickupLogic.MAGNET_RADIUS + ")");

		// void ------------------------------------------------------------
		ItemStack voider = new ItemStack(ModItems.ALL.get(2));
		BackpackContainer voiderUpgrades = BackpackContainer.upgrades(voider, StackRules.MAX_UPGRADE_SLOTS);
		ItemStack voidUpgrade = new ItemStack(ModItems.VOID_UPGRADE);
		BackpackContainer voidContents = BackpackContainer.storage(voidUpgrade, FilterUpgradeItem.FILTER_SLOTS);
		voidContents.setItem(0, new ItemStack(Items.COBBLESTONE));
		voiderUpgrades.setItem(0, voidUpgrade);
		boolean voidKeepsDirt = !VoidRules.shouldVoid(voider, new ItemStack(Items.DIRT));
		boolean voidVoidsCobble = VoidRules.shouldVoid(voider, new ItemStack(Items.COBBLESTONE, 5));
		ok &= voidKeepsDirt && voidVoidsCobble;
		out.add("void keepsDirt=" + voidKeepsDirt + ",voidsCobble=" + voidVoidsCobble);

		ItemStack advancedVoider = new ItemStack(ModItems.ALL.get(2));
		BackpackContainer advancedVoidUpgrades = BackpackContainer.upgrades(advancedVoider, StackRules.MAX_UPGRADE_SLOTS);
		ItemStack advancedVoidUpgrade = new ItemStack(ModItems.ADVANCED_VOID_UPGRADE);
		BackpackContainer advancedVoidContents = BackpackContainer.storage(advancedVoidUpgrade,
			FilterUpgradeItem.ADVANCED_FILTER_SLOTS);
		advancedVoidContents.setItem(26, new ItemStack(Items.DIRT));
		advancedVoidUpgrades.setItem(0, advancedVoidUpgrade);
		boolean advancedVoidSlot26 = VoidRules.shouldVoid(advancedVoider, new ItemStack(Items.DIRT));
		boolean advancedVoidKeepsCobble = !VoidRules.shouldVoid(advancedVoider, new ItemStack(Items.COBBLESTONE));
		ok &= advancedVoidSlot26 && advancedVoidKeepsCobble;
		out.add("advancedVoid(slot26=" + advancedVoidSlot26 + ",keepsCobble=" + advancedVoidKeepsCobble + ")");

		boolean noVoidWithoutFilter = !VoidRules.shouldVoid(new ItemStack(ModItems.ALL.get(0)), new ItemStack(Items.DIRT));
		ok &= noVoidWithoutFilter;
		out.add("noVoidWithoutFilter=" + noVoidWithoutFilter);

		// stack upgrades ---------------------------------------------------
		ItemStack bigBag = new ItemStack(ModItems.ALL.get(5));
		BackpackContainer plainStore = BackpackContainer.storage(bigBag, ModItems.slotsOf(5));
		boolean baseLimit = plainStore.getMaxStackSize(new ItemStack(Items.DIRT)) == 64;
		boolean swordLimitPlain = plainStore.getMaxStackSize(new ItemStack(Items.DIAMOND_SWORD)) == 1;

		BackpackContainer bigUps = BackpackContainer.upgrades(bigBag, StackRules.MAX_UPGRADE_SLOTS);
		bigUps.setItem(0, new ItemStack(ModItems.STACK_UPGRADES[1]));   // T2
		// limits multiply (checked by the 'multiply' assertion below), so use a single T2 here
		BackpackContainer t2Store = BackpackContainer.storage(bigBag, ModItems.slotsOf(5));
		int dirtCap = t2Store.getMaxStackSize(new ItemStack(Items.DIRT));
		int pearlCap = t2Store.getMaxStackSize(new ItemStack(Items.ENDER_PEARL));
		int swordCap = t2Store.getMaxStackSize(new ItemStack(Items.DIAMOND_SWORD));
		boolean perItem = dirtCap == 256 && pearlCap == 64 && swordCap == 1;
		ok &= baseLimit && swordLimitPlain && perItem;
		out.add("capacity(base=64,swordBase=1,t2dirt=" + dirtCap + ",t2pearl=" + pearlCap + ",t2sword=" + swordCap + ")");

		// compacting upgrade: detect its grid and actually compact through the recipe manager ----
		ItemStack compactBag = new ItemStack(ModItems.ALL.get(2));
		BackpackContainer.upgrades(compactBag, StackRules.MAX_UPGRADE_SLOTS)
			.setItem(0, new ItemStack(ModItems.COMPACTING_UPGRADE));
		boolean compactGrid = CompactingRules.gridSize(compactBag) == 2;

		SimpleContainer compactor = new SimpleContainer(9);
		compactor.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
		CompactingRules.compact(compactor, source.getServer().overworld(), 2);
		boolean planksCompacted = compactor.getItem(0).is(Items.CRAFTING_TABLE)
			&& compactor.getItem(0).getCount() == 1;

		compactor.setItem(0, new ItemStack(Items.IRON_INGOT, 9));
		CompactingRules.compact(compactor, source.getServer().overworld(), 3);
		boolean ironCompacted = compactor.getItem(0).is(Items.IRON_BLOCK)
			&& compactor.getItem(0).getCount() == 1;

		boolean compactOk = compactGrid && planksCompacted && ironCompacted
			&& ModItems.FEEDING_UPGRADE != null;
		ok &= compactOk;
		out.add("compacting(grid2=" + compactGrid + ",planks=" + planksCompacted
			+ ",iron=" + ironCompacted + ",feedingItem=" + (ModItems.FEEDING_UPGRADE != null) + ")");

		// multiple stack upgrades multiply (reference: 64 x product of multipliers) -------
		ItemStack multiBag = new ItemStack(ModItems.ALL.get(5));
		BackpackContainer multiUpgrades = BackpackContainer.upgrades(multiBag, StackRules.MAX_UPGRADE_SLOTS);
		multiUpgrades.setItem(0, new ItemStack(ModItems.STACK_UPGRADES[0]));
		multiUpgrades.setItem(1, new ItemStack(ModItems.STACK_UPGRADES[0]));
		double multi = StackRules.multiplier(multiBag);
		int multiDirt = StackRules.capacityFor(multiBag, new ItemStack(Items.DIRT));
		boolean product = multi == 4.0 && multiDirt == 256;
		ok &= product;
		out.add("multiply(2x2=" + multi + ",dirt=" + multiDirt + ")");

		// guard: a stack upgrade cannot be pulled while a stack still needs its multiplier
		ItemStack guardBag = new ItemStack(ModItems.ALL.get(5));
		BackpackContainer guardUpgrades = BackpackContainer.upgrades(guardBag, StackRules.MAX_UPGRADE_SLOTS);
		guardUpgrades.setItem(0, new ItemStack(ModItems.STACK_UPGRADES[0]));
		BackpackContainer guardStore = BackpackContainer.storage(guardBag, ModItems.slotsOf(5));
		guardStore.setItem(0, new ItemStack(Items.DIRT, 100));
		boolean guardBlocks = !new com.backpackery.menu.UpgradeSlot(
			BackpackContainer.upgrades(guardBag, StackRules.MAX_UPGRADE_SLOTS), 0, 0, 0).mayPickup(null);
		guardStore.setItem(0, new ItemStack(Items.DIRT, 64));
		boolean guardAllows = new com.backpackery.menu.UpgradeSlot(
			BackpackContainer.upgrades(guardBag, StackRules.MAX_UPGRADE_SLOTS), 0, 0, 0).mayPickup(null);
		ok &= guardBlocks && guardAllows;
		out.add("upgradeGuard(blocks100=" + guardBlocks + ",allows64=" + guardAllows + ")");

		// two holders of the same stack must see one truth (guards against dupe/loss) ------
		ItemStack sharedBag = new ItemStack(ModItems.ALL.get(5));
		BackpackContainer viewA = BackpackContainer.storage(sharedBag, ModItems.slotsOf(5));
		BackpackContainer viewB = BackpackContainer.storage(sharedBag, ModItems.slotsOf(5));
		viewA.setItem(0, new ItemStack(Items.DIRT, 5));
		boolean shared = viewB.getItem(0).getCount() == 5;
		viewB.setItem(1, new ItemStack(Items.STONE, 3));
		shared &= viewA.getItem(1).getCount() == 3;
		ok &= shared;
		out.add("sharedView=" + shared);

		// swords must never merge, that would duplicate items
		t2Store.setItem(0, new ItemStack(Items.DIAMOND_SWORD));
		ItemStack swordRest = PickupLogic.insert(t2Store, new ItemStack(Items.DIAMOND_SWORD));
		boolean swordsStaySingle = swordRest.isEmpty()
			&& t2Store.getItem(0).getCount() == 1 && t2Store.getItem(1).getCount() == 1;
		ok &= swordsStaySingle;
		out.add("swordsDontMerge=" + swordsStaySingle);

		ItemStack restBig = PickupLogic.insert(t2Store, new ItemStack(Items.DIRT, 200));
		boolean bigInserted = restBig.isEmpty() && t2Store.getItem(2).getCount() == 200;
		boolean noWidenComponent = t2Store.getItem(2).getOrDefault(DataComponents.MAX_STACK_SIZE, 64) == 64;
		boolean limitComponent = t2Store.getItem(2).getCount() == 200 && noWidenComponent;
		ItemStack restOver = PickupLogic.insert(t2Store, new ItemStack(Items.DIRT, 100));
		boolean bigSpill = restOver.isEmpty() && t2Store.getItem(2).getCount() == 256
			&& t2Store.getItem(3).getCount() == 44;
		boolean bigPersists = BackpackContainer.storage(bigBag, ModItems.slotsOf(5)).getItem(2).getCount() == 256;
		ok &= bigInserted && limitComponent && bigSpill && bigPersists;
		out.add("bigStack(200=" + bigInserted + ",limit=" + limitComponent + ",spill=" + bigSpill + ",persist=" + bigPersists + ")");

		// the client menu must not clamp synced counts to the vanilla 64 limit ----------
		com.backpackery.inventory.ClientStorageContainer clientStore =
			new com.backpackery.inventory.ClientStorageContainer(ModItems.slotsOf(5));
		clientStore.setItem(0, new ItemStack(Items.STONE, 247));
		boolean clientCount = clientStore.getItem(0).getCount() == 247;
		ok &= clientCount;
		out.add("clientCount247=" + clientCount);

		// a stack that leaves the backpack with a normal count loses the widened limit
		ItemStack leaving = t2Store.removeItem(3, t2Store.getItem(3).getCount());
		boolean trimOnExit = leaving.getCount() == 44
			&& leaving.getOrDefault(DataComponents.MAX_STACK_SIZE, 64) == 64
			&& com.backpackery.menu.StorageSlot.MAX_TAKE == 99;
		ok &= trimOnExit;
		out.add("trimOnExit=" + trimOnExit);

		// layout / registry -------------------------------------------------
		boolean everyTierHasMenu = true;
		StringBuilder tiers = new StringBuilder();
		for (Item item : ModItems.ALL) {
			if (!(item instanceof BackpackItem backpack)) continue;
			boolean okTier = backpack.tier() >= 0 && backpack.tier() < ModMenus.TIERS
				&& ModMenus.forTier(backpack.tier()) != null;
			everyTierHasMenu &= okTier;
			tiers.append(ModItems.colsFor(backpack.slots())).append('x').append(ModItems.rowsFor(backpack.slots()))
				.append('=').append(backpack.slots()).append(' ');
		}
		ok &= everyTierHasMenu;
		out.add("tiers[" + tiers.toString().trim() + "],allMenus=" + everyTierHasMenu);

		// how far do vanilla codecs allow a widened stack to travel? -----------------
		var ops = RegistryOps.create(NbtOps.INSTANCE, source.getServer().registryAccess());
		ItemStack probe = new ItemStack(Items.DIRT, 256);
		var encodedItem = ItemStack.CODEC.encodeStart(ops, probe);
		int codecCount = encodedItem.result()
			.map(tag -> ItemStack.CODEC.parse(ops, tag).result().orElse(ItemStack.EMPTY).getCount())
			.orElse(-1);
		out.add("itemCodec(256->" + codecCount + ",err=" + encodedItem.error().map(Object::toString).orElse("-") + ")");

		ItemStack componentProbe = new ItemStack(Items.DIRT, 1);
		componentProbe.set(DataComponents.MAX_STACK_SIZE, 256);
		ItemContainerContents probeContents = ItemContainerContents.fromItems(List.of(componentProbe));
		var encodedContents = ItemContainerContents.CODEC.encodeStart(ops, probeContents);
		int containerCount = encodedContents.result()
			.map(tag -> ItemContainerContents.CODEC.parse(ops, tag).result()
				.map(c -> c.nonEmptyItemCopyStream().findFirst().orElse(ItemStack.EMPTY).getCount()).orElse(-1))
			.orElse(-1);
		out.add("containerCodec(256->" + containerCount + ",err=" + encodedContents.error().map(Object::toString).orElse("-") + ")");

		var encA = ItemStack.CODEC.encodeStart(ops, componentProbe);
		out.add("probeA(count1+component256)=" + encA.result().isPresent()
			+ " err=" + encA.error().map(Object::toString).orElse("-"));
		ItemStack countOnly = new ItemStack(Items.DIRT, 99);
		var encB = ItemStack.CODEC.encodeStart(ops, countOnly);
		out.add("probeB(count99,noComponent)=" + encB.result().isPresent());

		com.backpackery.inventory.BackpackContents own = com.backpackery.inventory.BackpackContents.of(List.of(probe));
		var encodedOwn = com.backpackery.inventory.BackpackContents.CODEC.encodeStart(ops, own);
		int ownCount = encodedOwn.result()
			.map(tag -> com.backpackery.inventory.BackpackContents.CODEC.parse(ops, tag).result()
				.map(bc -> bc.slots().stream().filter(st -> !st.isEmpty()).findFirst()
					.orElse(ItemStack.EMPTY).getCount())
				.orElse(-1))
			.orElse(-1);
		boolean ownCodec = ownCount == 256;
		ok &= ownCodec;
		out.add("backpackCodec(256->" + ownCount + ",err=" + encodedOwn.error().map(Object::toString).orElse("-") + ")");

		// the backpack item itself must survive a save with a 256 stack inside
		boolean hostSaves = ItemStack.CODEC.encodeStart(ops, bigBag).result().isPresent();
		ok &= hostSaves;
		out.add("backpackItemSaves=" + hostSaves);

		net.minecraft.network.RegistryFriendlyByteBuf buf = new net.minecraft.network.RegistryFriendlyByteBuf(
			io.netty.buffer.Unpooled.buffer(), source.getServer().registryAccess());
		ItemStack.STREAM_CODEC.encode(buf, probe.copy());
		int streamCount = ItemStack.STREAM_CODEC.decode(buf).getCount();
		out.add("streamCodec(256->" + streamCount + ")");

		return (ok ? "PASS " : "FAIL ") + out;
	}

	private static String upgradesComponentSize(ItemStack stack) {
		ItemContainerContents u = stack.get(com.backpackery.ModComponents.UPGRADES);
		if (u == null) return "none";
		StringBuilder sb = new StringBuilder();
		u.nonEmptyItemCopyStream().forEach(st -> sb.append(st.getItem()).append('/')
			.append(st.get(com.backpackery.ModComponents.STORAGE) != null ? "withStorage" : "noStorage").append(' '));
		return sb.length() == 0 ? "empty" : sb.toString().trim();
	}

	private SelfTestCommand() {
	}
}
