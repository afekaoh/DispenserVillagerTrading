package org.adam.dispenserVillagerTrading.Models;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BlockVector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class VillagerOnlyPressurePlate {
	private final Set<BlockVector> villagerPlates;
	
	public VillagerOnlyPressurePlate(Set<BlockVector> villagerPlates) {
		this.villagerPlates = new HashSet<>();
		if (villagerPlates != null) {
			this.villagerPlates.addAll(villagerPlates);
		}
	}
	
	public ItemStack createVillagerPlateItem() {
		ItemStack item = new ItemStack(Material.STONE_PRESSURE_PLATE);
		ItemMeta meta = item.getItemMeta();
		
		meta.customName(Component.text("Villager-Only Pressure Plate"));
		meta.lore(List.of(Component.text("Villager-Only Pressure Plate")));
		
		item.setItemMeta(meta);
		return item;
	}
	
	public boolean isVillagerPlate(BlockVector vector) {
		// Check if the given BlockVector is in the set of villager plates
		return villagerPlates.contains(vector);
	}
	
	public void addVillagerPlate(BlockVector vector) {
		villagerPlates.add(vector);
	}
	
	public void removeVillagerPlate(BlockVector vector) {
		villagerPlates.remove(vector);
	}
	
	public Set<BlockVector> getVillagerPlates() {
		return Set.copyOf(villagerPlates);
	}
	
}
