package org.adam.dispenserVillagerTrading.Listeners;

import org.adam.dispenserVillagerTrading.Models.VillagerOnlyPressurePlate;
import org.bukkit.Location;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class PressurePlatesListeners implements Listener{
    private final VillagerOnlyPressurePlate villagerOnlyPressurePlate;

    public PressurePlatesListeners(VillagerOnlyPressurePlate villagerOnlyPressurePlate){
        this.villagerOnlyPressurePlate = villagerOnlyPressurePlate;
    }

    @EventHandler
    public void onStepPlateEntity(EntityInteractEvent event){

        var blockVector = event.getBlock().getLocation().toVector().toBlockVector();
        if(!villagerOnlyPressurePlate.isVillagerPlate(blockVector))
            return;

        if(!(event.getEntity() instanceof Villager)) event.setCancelled(true);


    }


    // Prevent players from activating villager-only pressure plates
    @EventHandler
    public void onStepPlatePlayer(PlayerInteractEvent event){
        if(event.getAction() != Action.PHYSICAL) return;

        var blockVector = event.getClickedBlock().getLocation().toVector().toBlockVector();

        if(!villagerOnlyPressurePlate.isVillagerPlate(blockVector)) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onVillagerPlatePlacement(BlockPlaceEvent event){

        var itemInHand = event.getItemInHand();
        if(itemInHand.isSimilar(this.villagerOnlyPressurePlate.createVillagerPlateItem())){
            var blockVector = event.getBlock().getLocation().toVector().toBlockVector();
            villagerOnlyPressurePlate.addVillagerPlate(blockVector);
        }
    }

    @EventHandler
    public void onVillagerPlateBreak(BlockBreakEvent event){
        Location blockLocation = event.getBlock().getLocation();
        var blockVector = blockLocation.toVector().toBlockVector();
        if(villagerOnlyPressurePlate.isVillagerPlate(blockVector)){
            villagerOnlyPressurePlate.removeVillagerPlate(blockVector);
        }
        // drop the villager plate item
        event.getBlock().getWorld().dropItemNaturally(blockLocation,
                                                      villagerOnlyPressurePlate.createVillagerPlateItem());
        event.setDropItems(false); // prevent default drops
    }
}
