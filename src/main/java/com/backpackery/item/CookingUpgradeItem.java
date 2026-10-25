package com.backpackery.item;

public class CookingUpgradeItem extends UpgradeItem {
	public enum Kind {
		SMELTING("smelting_upgrade"),
		BLASTING("blasting_upgrade"),
		SMOKING("smoking_upgrade"),
		AUTO_SMELTING("auto_smelting_upgrade"),
		AUTO_BLASTING("auto_blasting_upgrade"),
		AUTO_SMOKING("auto_smoking_upgrade");

		private final String id;

		Kind(String id) {
			this.id = id;
		}

		public String id() {
			return this.id;
		}
	}

	private final Kind kind;

	public CookingUpgradeItem(Kind kind, Properties properties) {
		super(properties);
		this.kind = kind;
	}

	public Kind kind() {
		return this.kind;
	}

	public boolean automatic() {
		return this.kind == Kind.AUTO_SMELTING || this.kind == Kind.AUTO_BLASTING || this.kind == Kind.AUTO_SMOKING;
	}
}