package org.adam.dispenserVillagerTrading;

import org.adam.dispenserVillagerTrading.Listeners.DispernserVillagerListener;
import org.adam.dispenserVillagerTrading.Listeners.PressurePlatesListeners;
import org.adam.dispenserVillagerTrading.Models.VillagerOnlyPressurePlate;
import org.adam.dispenserVillagerTrading.Models.VillagerPlateYAMLPersistence;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;

public final class DispenserVillagerTrading extends JavaPlugin {
	private VillagerOnlyPressurePlate villagerOnlyPressurePlate;
	private VillagerPlateYAMLPersistence persistence;
	
	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		this.villagerOnlyPressurePlate = new VillagerOnlyPressurePlate(null);
		this.persistence = new VillagerPlateYAMLPersistence(this);
		
		if (persistence.load()) getLogger().info("Loaded villager pressure plates from file.");
		else getLogger().info("No villager pressure plates file found, starting fresh.");
		
		addVillagerPressurePlatesRecipe(persistence);
		
		// Register event listeners
		
		// Dispenser villager interaction listener
		getServer()
			.getPluginManager()
			.registerEvents(
				new DispernserVillagerListener(this.getConfig().getBoolean("allowVillagerDispenserInteractions",
				                                                           false)),
				this);
		
		// Pressure plates listeners
		getServer()
			.getPluginManager()
			.registerEvents(new PressurePlatesListeners(villagerOnlyPressurePlate), this);
		
	}
	
	private void addVillagerPressurePlatesRecipe(VillagerPlateYAMLPersistence persistence) {
		this.villagerOnlyPressurePlate = new VillagerOnlyPressurePlate(persistence.getPlates());
		ItemStack villagerPlateItem = villagerOnlyPressurePlate.createVillagerPlateItem();
		NamespacedKey key = new NamespacedKey(this, "VillagerOnlyPressurePlate");
		if (getServer().getRecipe(key) == null) {
			ShapelessRecipe recipe = new ShapelessRecipe(key, villagerPlateItem);
			recipe.addIngredient(1, Material.STONE_PRESSURE_PLATE);
			recipe.addIngredient(1, Material.EMERALD);
			getServer().addRecipe(recipe);
		}
	}
	
	@Override
	public void onDisable() {
		// Save villager pressure plates on shutdown
		persistence = new VillagerPlateYAMLPersistence(this);
		persistence.updatePlates(villagerOnlyPressurePlate.getVillagerPlates());
		persistence.save();
	}
}
