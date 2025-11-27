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

import java.util.Properties;

public final class DispenserVillagerTrading extends JavaPlugin{
    private VillagerOnlyPressurePlate villagerOnlyPressurePlate;
    private VillagerPlateYAMLPersistence persistence;
    private static final String NAMESPACEDKEY_XP_NAME = "storedXp";


    @Override
    public void onEnable(){
        // making sure config file exists
        this.saveDefaultConfig();

        Properties constants = this.getConstants();

        // Load villager pressure plates from file
        this.persistence = new VillagerPlateYAMLPersistence(this);
        if(persistence.load()) getLogger().info("Loaded villager pressure plates from file.");
        else getLogger().info("No villager pressure plates file found, starting fresh.");
        this.villagerOnlyPressurePlate = new VillagerOnlyPressurePlate(persistence.getPlates());

        addVillagerPressurePlatesRecipe();

        // Register event listeners

        // Dispenser villager interaction listener
        getServer()
                .getPluginManager()
                .registerEvents(
                        new DispernserVillagerListener(this, constants),
                        this);

        // Pressure plates listeners
        getServer()
                .getPluginManager()
                .registerEvents(new PressurePlatesListeners(villagerOnlyPressurePlate), this);


        // create NamespacedKey for stored XP in the plugin namespace
        new NamespacedKey(this, constants.getProperty("namespacedKey.xp.name"));

    }


    private Properties getConstants(){
        Properties loadedConfig = new Properties();
        boolean allowVillagerDispenserInteractions = this.getConfig()
                                                         .getBoolean("allowVillagerDispenserInteractions",
                                                                     false);
        loadedConfig.setProperty("allow.villager.dispenser.interactions", Boolean.toString(allowVillagerDispenserInteractions));
        loadedConfig.setProperty("namespacedKey.xp.name", NAMESPACEDKEY_XP_NAME);
        return loadedConfig;
    }

    private void addVillagerPressurePlatesRecipe(){
        ItemStack villagerPlateItem = villagerOnlyPressurePlate.createVillagerPlateItem();
        NamespacedKey key = new NamespacedKey(this, "VillagerOnlyPressurePlate");
        if(getServer().getRecipe(key) == null){
            ShapelessRecipe recipe = new ShapelessRecipe(key, villagerPlateItem);
            recipe.addIngredient(1, Material.STONE_PRESSURE_PLATE);
            recipe.addIngredient(1, Material.EMERALD);
            getServer().addRecipe(recipe);
        }
    }

    @Override
    public void onDisable(){
        // Save villager pressure plates on shutdown
        persistence = new VillagerPlateYAMLPersistence(this);
        persistence.updatePlates(villagerOnlyPressurePlate.getVillagerPlates());
        persistence.save();
    }
}
