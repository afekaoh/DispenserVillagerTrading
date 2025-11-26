package org.adam.dispenserVillagerTrading.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class DispenserVillagerTradeChestEvent extends Event implements Cancellable{
    private static final HandlerList HANDLERS = new HandlerList();
    private final Inventory source;
    private final Inventory destination;
    private ItemStack item;
    private boolean cancelled;

    public DispenserVillagerTradeChestEvent(Inventory source, Inventory destination, ItemStack item){
        this.source = source;
        this.destination = destination;
        this.item = item;
    }

    public Inventory getSource(){return source;}

    public Inventory getDestination(){return destination;}

    public ItemStack getItem(){return item;}

    public void setItem(ItemStack item){this.item = item;}

    @Override
    public boolean isCancelled(){return cancelled;}

    @Override
    public void setCancelled(boolean cancel){this.cancelled = cancel;}

    @Override
    public HandlerList getHandlers(){return HANDLERS;}

    public static HandlerList getHandlerList(){return HANDLERS;}

}
