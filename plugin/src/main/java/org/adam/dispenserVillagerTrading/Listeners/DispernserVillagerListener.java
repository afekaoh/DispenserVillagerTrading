package org.adam.dispenserVillagerTrading.Listeners;

import io.papermc.paper.event.block.BlockPreDispenseEvent;
import org.adam.dispenserVillagerTrading.events.DispenserVillagerTradeChestEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;

import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.util.stream.Collectors.*;

public class DispernserVillagerListener implements Listener{

    private final Properties constants;
    private final NamespacedKey xpNamedSpacedKey;


    public DispernserVillagerListener(JavaPlugin plugin, Properties constants){
        this.constants = constants;
        this.xpNamedSpacedKey = new NamespacedKey(plugin, this.constants.getProperty("namespacedKey.xp.name"));
    }

    @EventHandler
    public void onBlockPreDispense(BlockPreDispenseEvent event){
        // Check if the block involved is actually a dispenser
        if(!(event.getBlock().getState() instanceof Dispenser dispenser)){
            return;
        }

        var blockData = dispenser.getBlockData();
        // Check if the dispenser is facing a villager
        if(!(blockData instanceof Directional directionalBlockData))
            return;
        var direction = directionalBlockData.getFacing();
        Location nextBlock = dispenser.getLocation().add(direction.getDirection());

        Villager villager = getVillagerFromBlock(nextBlock);
        if(villager == null) return;

        // the event is cancelled to prevent villagers from activating the dispenser normally and take the items
        // you can also use the added pressure plate to only allow villagers to activate the dispenser
        // users can enable/disable this feature in the config file

        boolean enableVillagerInteraction = Boolean.parseBoolean(
                constants.getProperty("allow.villager.dispenser.interactions", "false"));
        if(!enableVillagerInteraction)
            event.setCancelled(true);

        // check if the villager has trades
        if(villager.getProfession() == Villager.Profession.NONE)
            return;

        // get all the trades of the villager
        List<MerchantRecipe> trades = villager.getRecipes();

        // remove empty item stacks from the dispenser inventory
        Inventory dispenserInventory = dispenser.getInventory();
        var dispenserContent = Arrays.stream(dispenserInventory.getContents()).toList();

        // find all trades that can be fulfilled with the dispenser items
        List<MerchantRecipe> matchingTrades = getMatchingTrades(trades, dispenserContent);

        if(matchingTrades.isEmpty()){
            return;
        }
        // pick a random trade from the matching trades to keep in line with Dispenser randomness
        MerchantRecipe selectedTrade = matchingTrades.get((int) (Math.random() * matchingTrades.size()));

        // all the checks are done, process the trade
        processTrade(dispenser, selectedTrade, villager);

        // cancel the event if not already cancelled
        if(!event.isCancelled())
            event.setCancelled(true);
    }

    @EventHandler
    public void onDispenserBreak(BlockBreakEvent event){

        if(event.getBlock().getState() instanceof Dispenser dispenser){
            // when a dispenser is broken we can drop the stored XP as orbs
            var pdc = dispenser.getPersistentDataContainer();
            int storedXP = pdc.getOrDefault(xpNamedSpacedKey, PersistentDataType.INTEGER, 0);
            if(storedXP > 0){
                dispenser.getWorld().spawn(dispenser.getLocation().add(0.5, 0.5, 0.5),
                                           org.bukkit.entity.ExperienceOrb.class,
                                           orb -> orb.setExperience(storedXP));

                // reset the stored XP just as a safety measure (though the dispenser is being broken)
                pdc.set(xpNamedSpacedKey, PersistentDataType.INTEGER, 0);
                dispenser.update();
            }
        }
    }

    private @NotNull List<MerchantRecipe> getMatchingTrades(List<MerchantRecipe> trades,
                                                            List<ItemStack> dispenserContent){
        // since we want to include tades that require two ingredients we have to filter the trades based on the
        // dispenser contents
        return trades
                .stream()
                .filter(trade -> trade.getUses() < trade.getMaxUses())
                .filter(trade -> {

                    Map<Material, Integer> requiredIngredients = convertTradesToMap(trade);

                    Map<Material, Integer> dispenserItemMap = convertDispenserContentToMap(dispenserContent);

                    // check if the dispenser has enough items for the trade
                    for(Map.Entry<Material, Integer> tradeEntry : requiredIngredients.entrySet()){
                        Material material = tradeEntry.getKey();
                        int requiredAmount = tradeEntry.getValue();
                        int availableAmount = dispenserItemMap.getOrDefault(material, 0);
                        if(availableAmount < requiredAmount){
                            return false;
                        }
                    }
                    return true;
                })
                .toList();
    }

    private void processTrade(Dispenser dispenser,
                              MerchantRecipe selectedTrade,
                              Villager villager){

        var blockData = dispenser.getBlockData();
        var direction = ((Directional) blockData).getFacing();
        var dispenserInventory = dispenser.getInventory();

        // payment
        processDispenserPayment(selectedTrade, dispenserInventory);

        // receive the result item
        processTradeResult(dispenser, selectedTrade, direction);

        // apply the trade effects on the villager
        var hasVillagerLeveledUp = processVillagerTradeEffects(selectedTrade, villager);

        processPlayerExperienceEffect(dispenser, hasVillagerLeveledUp);
    }

    private void processPlayerExperienceEffect(Dispenser dispenser, boolean hasVillagerLeveledUp){
        var state = (Dispenser) dispenser.getBlock().getState(); // get a fresh state to avoid any caching issues

        var r = new java.util.Random();
        // experience calculation according to villager trading mechanics
        // each trade gives between 3 and 6 XP plus 5 XP if the villager leveled up @https://minecraft.fandom.com/wiki/Trading#Mechanics
        final int randomTradeXP = r.nextInt(3, 7);
        int expiranceGained = hasVillagerLeveledUp ? 5 : 0;
        expiranceGained += randomTradeXP;
        int existingXP = state.getPersistentDataContainer().getOrDefault(xpNamedSpacedKey,
                                                                         PersistentDataType.INTEGER, 0);
        expiranceGained += existingXP;
        // store the updated XP back in the dispenser persistent data container to avoid player experience manipulation
        state.getPersistentDataContainer().set(xpNamedSpacedKey, PersistentDataType.INTEGER, expiranceGained);
        state.update();
    }

    private boolean processVillagerTradeEffects(MerchantRecipe selectedTrade, Villager villager){
        // update the trade usage
        int tradeIndex = villager.getRecipes().indexOf(selectedTrade);
        selectedTrade.setUses(selectedTrade.getUses() + 1);
        villager.setRecipe(tradeIndex, selectedTrade);

        // update villager experience and level
        int tradeExp = selectedTrade.getVillagerExperience();
        int currentExp = villager.getVillagerExperience();
        villager.setVillagerExperience(currentExp + tradeExp);
        boolean shouldLevelUp = canLevelUp(villager);
        if(shouldLevelUp){
            villager.increaseLevel(1);
        }
        return shouldLevelUp;
    }

    private void processTradeResult(Dispenser dispenser, MerchantRecipe selectedTrade, BlockFace direction){
        // drop the result item at the dispenser back face to avoid item collection by the villager
        Location dropLocation = dispenser.getLocation().add(direction.getOppositeFace().getDirection());

        // adding an option to drop the item in a chest

        // check if the location is occupied by a chest
        boolean chestPresent = dropLocation.getBlock().getType() == Material.CHEST;
        if(chestPresent){
            // add the item to the chest
            Chest chest = (Chest) dropLocation.getBlock().getState();
            DispenserVillagerTradeChestEvent move = new DispenserVillagerTradeChestEvent(dispenser.getInventory(),
                                                                                         chest.getInventory(),
                                                                                         selectedTrade.getResult());
            Bukkit.getPluginManager().callEvent(move);
            if(move.isCancelled()){
                // Design choice: if the event is cancelled, we drop the item in the world
                dispenser.getWorld().dropItemNaturally(dropLocation, selectedTrade.getResult());
                return;
            }

            var remainingItems = chest.getInventory().addItem(selectedTrade.getResult());
            // if there are remaining items, drop them in the world
            for(ItemStack item : remainingItems.values()){
                dispenser.getWorld().dropItemNaturally(dropLocation, item);
            }
            return;
        }
        // drop the item in the world
        dispenser.getWorld().dropItemNaturally(dropLocation, selectedTrade.getResult());
    }

    private void processDispenserPayment(MerchantRecipe selectedTrade, Inventory dispenserInventory){
        // remove the required items from the dispenser inventory
        // we can safely assume that the dispenser has enough items for the trade since we already checked that before
        selectedTrade.getIngredients().forEach(dispenserInventory::removeItemAnySlot);
    }

    private @NotNull Map<Material, Integer> convertDispenserContentToMap(List<ItemStack> dispenserItems){
        return dispenserItems.stream()
                             .filter(item -> item != null && item.getType() != Material.AIR)
                             .collect(groupingBy(
                                     ItemStack::getType,
                                     mapping(ItemStack::getAmount, summingInt(Integer::intValue))));
    }

    private @NotNull Map<Material, Integer> convertTradesToMap(MerchantRecipe trade){
        return trade.getIngredients()
                    .stream()
                    .collect(groupingBy(
                            ItemStack::getType,
                            mapping(ItemStack::getAmount, summingInt(Integer::intValue))));
    }

    private @Nullable Villager getVillagerFromBlock(Location nextBlock){
        var villagers = nextBlock.getNearbyEntities(1, 1, 1)
                                 .stream()
                                 .filter(entity -> entity instanceof Villager)
                                 .map(entity -> (Villager) entity)
                                 .toList();
        if(villagers.isEmpty()){
            return null;
        }
        return villagers.stream().findFirst().get();
    }

    public boolean canLevelUp(Villager villager){
        int currentLevel = villager.getVillagerLevel();
        int currentExp = villager.getVillagerExperience();

        // Master villagers (level 5) cannot level up further
        if(currentLevel >= 5){
            return false;
        }

        int requiredExpForNextLevel = getExpRequiredForLevel(currentLevel + 1);

        return currentExp >= requiredExpForNextLevel;
    }

    // Helper method to get the total XP threshold for a given level
    private int getExpRequiredForLevel(int level){
        return switch(level){
            case 2 -> 10;
            case 3 -> 70;
            case 4 -> 150;
            case 5 -> 250;
            default -> Integer.MAX_VALUE; // For maxed out or invalid levels
        };
    }
}
