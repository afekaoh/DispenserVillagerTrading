package org.adam.dispenserVillagerTrading.Listeners;

import org.adam.dispenserVillagerTrading.Models.VillagerOnlyPressurePlate;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityInteractEvent;

public class PressurePlatesListeners implements Listener {
	private final VillagerOnlyPressurePlate villagerOnlyPressurePlate;
	
	public PressurePlatesListeners(VillagerOnlyPressurePlate villagerOnlyPressurePlate) {
		this.villagerOnlyPressurePlate = villagerOnlyPressurePlate;
	}
	
	@EventHandler
	public void onStepPlate(EntityInteractEvent event) {
		
		var blockVector = event.getBlock().getLocation().toVector().toBlockVector();
		if (!villagerOnlyPressurePlate.isVillagerPlate(blockVector))
			return;
		
		if (event.getEntity() instanceof Villager) {
			return;
		}
		event.setCancelled(true);
		
	}
	
	@EventHandler
	public void onVillagerPlatePlacement(BlockPlaceEvent event) {
		var itemInHand = event.getItemInHand();
		if (itemInHand.isSimilar(this.villagerOnlyPressurePlate.createVillagerPlateItem())) {
			var blockVector = event.getBlock().getLocation().toVector().toBlockVector();
			villagerOnlyPressurePlate.addVillagerPlate(blockVector);
		}
	}
	
	@EventHandler
	public void onVillagerPlateBreak(BlockBreakEvent event) {
		var blockVector = event.getBlock().getLocation().toVector().toBlockVector();
		if (villagerOnlyPressurePlate.isVillagerPlate(blockVector)) {
			villagerOnlyPressurePlate.removeVillagerPlate(blockVector);
		}
	}
}
