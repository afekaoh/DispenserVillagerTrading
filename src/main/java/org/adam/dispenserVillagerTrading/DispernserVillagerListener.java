package org.adam.dispenserVillagerTrading;

import io.papermc.paper.event.block.BlockPreDispenseEvent;
import io.papermc.paper.event.player.PlayerTradeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;

import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.data.Directional;

import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.*;

public class DispernserVillagerListener implements Listener {
	private final Plugin plugin;
	
	public DispernserVillagerListener(Plugin plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onBlockDispense(BlockPreDispenseEvent event) {
		// Check if the block involved is actually a dispenser
		var logger = plugin.getLogger();
		logger.info("BlockPreDispenseEvent triggered at " + event.getBlock().getState());
		if (!(event.getBlock().getState() instanceof Dispenser dispenser)) {
			return;
		}
		logger.info("Dispenser activated at " + dispenser.getLocation());
		Inventory dispenserInventory = dispenser.getInventory();
		var blockData = dispenser.getBlockData();
		// Check if the dispenser is facing a villager
		if (!(blockData instanceof Directional))
			return;
		var direction = ((Directional) blockData).getFacing();
		Location nextBlock = dispenser.getLocation().add(direction.getDirection());
		var villagers = nextBlock.getNearbyEntities(1, 1, 1)
		                         .stream()
		                         .filter(entity -> entity instanceof Villager)
		                         .map(entity -> (Villager) entity)
		                         .toList();
		if (villagers.isEmpty()) {
			logger.info("No villager found in front of dispenser at " + dispenser.getLocation());
			return;
		}
		event.setCancelled(true);
		Villager villager = villagers.stream().findFirst().get();
		// check if the villager has trades
		if (villager.getProfession() == Villager.Profession.NONE)
			return;
		// get all the trades of the villager
		List<MerchantRecipe> trades = villager.getRecipes();
		trades.forEach(trade -> logger.info("Villager trade: " + trade.toString()));
		
		// remove empty item stacks from the dispenser inventory
		var dispenserItems = Arrays.stream(dispenserInventory.getContents()).toList();
		
		List<MerchantRecipe> matchingTrades = trades
			.stream()
			.filter(trade -> trade.getUses() < trade.getMaxUses())
			.filter(trade -> {
				// group the required items by material and sum their amounts
				Map<Material, Integer> requiredItems = Map.of(
					trade.getIngredients().get(0).getType(), trade.getIngredients().get(0).getAmount()
				                                             );
				if (trade.getIngredients().size() > 1) {
					var secondIngredient = trade.getIngredients().get(1);
					requiredItems = trade.getIngredients().stream()
					                     .collect(
						                     groupingBy(
							                     ItemStack::getType,
							                     mapping(ItemStack::getAmount, summingInt(Integer::intValue))));
				}
				// group the dispenser items by material and sum their amounts
				Map<Material, Integer> dispenserItemMap = dispenserItems.stream()
				                                                        .filter(item -> item != null && item.getType() != Material.AIR)
				                                                        .collect(
					                                                        groupingBy(
						                                                        ItemStack::getType,
						                                                        mapping(itemStack -> itemStack != null ? itemStack.getAmount() : 0,
						                                                                summingInt(Integer::intValue))));
				// check if the dispenser has enough items for the trade
				for (Map.Entry<Material, Integer> entry : requiredItems.entrySet()) {
					Material material = entry.getKey();
					int requiredAmount = entry.getValue();
					int availableAmount = dispenserItemMap.getOrDefault(material, 0);
					if (availableAmount < requiredAmount) {
						return false;
					}
				}
				return true;
			}).toList();
		if (matchingTrades.isEmpty()) {
			return;
		}
		// pick a random trade from the matching trades
		MerchantRecipe selectedTrade = matchingTrades.get((int) (Math.random() * matchingTrades.size()));
		// remove the required items from the dispenser inventory
		for (ItemStack ingredient : selectedTrade.getIngredients()) {
			int amountToRemove = ingredient.getAmount();
			for (int i = 0; i < dispenserInventory.getSize(); i++) {
				ItemStack item = dispenserInventory.getItem(i);
				if (item != null && item.getType() == ingredient.getType()) {
					if (item.getAmount() > amountToRemove) {
						item.setAmount(item.getAmount() - amountToRemove);
						dispenserInventory.setItem(i, item);
						break;
					} else {
						amountToRemove -= item.getAmount();
						dispenserInventory.setItem(i, null);
					}
				}
			}
		}
		// drop the result item at the dispenser back face
		Location dropLocation = dispenser.getLocation().add(direction.getOppositeFace().getDirection().multiply(0.5));
		// check if the location is occupied by a chest
		boolean chestPresent = dropLocation.getBlock().getType() == Material.CHEST;
		if (chestPresent) {
			// add the item to the chest
			Chest chest = (Chest) dropLocation.getBlock().getState();
			var remainingItems = chest.getInventory().addItem(selectedTrade.getResult());
			// if there are remaining items, drop them in the world
			for (ItemStack item : remainingItems.values()) {
				dispenser.getWorld().dropItemNaturally(dropLocation, item);
			}
		} else {
			// drop the item in the world
			dispenser.getWorld().dropItemNaturally(dropLocation, selectedTrade.getResult());
		}
		// execute the trade on the villager to update its trades
		Player dummyPlayer = Bukkit.getServer().getOnlinePlayers().stream().findFirst().orElse(null);
		if (dummyPlayer == null) {
			logger.warning("No online players found to execute villager trade.");
			return;
		}
		// apply the trade
		int tradeIndex = villager.getRecipes().indexOf(selectedTrade);
		selectedTrade.setUses(selectedTrade.getUses() + 1);
		villager.setRecipe(tradeIndex, selectedTrade);
		// check if the villager can level up
		int tradeExp = selectedTrade.getVillagerExperience();
		int currentExp = villager.getVillagerExperience();
		villager.setVillagerExperience(currentExp + tradeExp);
		if (canLevelUp(villager))
			villager.increaseLevel(1);
		villager.updateDemand();
		logger.info("Executed villager trade: " +
			            selectedTrade.getIngredients().getFirst().getType() +
			            " number of uses: " +
			            selectedTrade.getUses() +
			            " out of " +
			            selectedTrade.getMaxUses() +
			            " Villager experience: " +
			            villager.getVillagerExperience() +
			            " Villager level: " +
			            villager.getVillagerLevel());
		// Call PlayerTradeEvent to simulate the trade
		var tradeEvent = new PlayerTradeEvent(dummyPlayer, villager, selectedTrade, false, true);
		tradeEvent.callEvent();
		logger.info("second check: " +
			            selectedTrade.getIngredients().getFirst().getType() +
			            " number of uses: " +
			            selectedTrade.getUses() +
			            " out of " +
			            selectedTrade.getMaxUses() +
			            " Villager experience: " +
			            villager.getVillagerExperience() +
			            " Villager level: " +
			            villager.getVillagerLevel());
	}
	
	public static boolean canLevelUp(Villager villager) {
		int currentLevel = villager.getVillagerLevel();
		int currentExp = villager.getVillagerExperience();
		
		// Master villagers (level 5) cannot level up further
		if (currentLevel >= 5) {
			return false;
		}
		
		int requiredExpForNextLevel = getExpRequiredForLevel(currentLevel + 1);
		
		return currentExp >= requiredExpForNextLevel;
	}
	
	// Helper method to get the total XP threshold for a given level
	private static int getExpRequiredForLevel(int level) {
		return switch (level) {
			case 2 -> 10;
			case 3 -> 70;
			case 4 -> 150;
			case 5 -> 250;
			default -> Integer.MAX_VALUE; // For maxed out or invalid levels
		};
	}
}
