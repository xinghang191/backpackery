package com.backpackery.item;

public class PickupUpgradeItem extends UpgradeItem {
	private final double radius;

	public PickupUpgradeItem(Properties properties) {
		this(2.0D, properties);
	}

	public PickupUpgradeItem(double radius, Properties properties) {
		super(properties);
		this.radius = radius;
	}

	public double radius() {
		return this.radius;
	}
}