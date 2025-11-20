package org.adam.dispenserVillagerTrading.Models;

import org.adam.dispenserVillagerTrading.DispenserVillagerTrading;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.BlockVector;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class VillagerPlateYAMLPersistence {
	private final File villagerPlatesFile;
	
	
	private final Set<BlockVector> plates = new HashSet<>();
	
	public VillagerPlateYAMLPersistence(DispenserVillagerTrading plugin) {
		
		File dataFolder = plugin.getDataFolder();
		if (!dataFolder.exists()) dataFolder.mkdirs();
		
		this.villagerPlatesFile = new File(dataFolder, "villager_plates.yml");
	}
	
	// Loading
	public Boolean load() {
				var plateList = YamlConfiguration.loadConfiguration(villagerPlatesFile);
		plates.clear();
		
		var list = plateList.getList("plates");
		if (list == null) return false;
		
		for (Object obj : list) {
			if (obj instanceof BlockVector vec) {
				plates.add(vec);
			}
		}
		return true;
	}
	
	// Saving
	public void save() {
		var plateList = YamlConfiguration.loadConfiguration(villagerPlatesFile);
		plateList.set("plates", plates.stream().toList());
		
		try {
			plateList.save(villagerPlatesFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// Right now i'm updating the list only in enable/disable of the plugin
	// but in the future I might want to save after each change
	public Set<BlockVector> getPlates() {
		return Set.copyOf(plates);
	}
	
	public void addPlate(BlockVector vector) {
		plates.add(vector);
	}
	
	public void removePlate(BlockVector vector) {
		plates.remove(vector);
	}
	
	public void updatePlates(Set<BlockVector> newPlates) {
		plates.clear();
		plates.addAll(newPlates);
	}
	
}
