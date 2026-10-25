package com.backpackery.item;

public class MagnetUpgradeItem extends UpgradeItem {
	private final double radius;

	public MagnetUpgradeItem(Properties properties) {
		this(10.0D, properties);
	}

	public MagnetUpgradeItem(double radius, Properties properties) {
		super(properties);
		this.radius = radius;
	}

	public double radius() {
		return this.radius;
	}
}