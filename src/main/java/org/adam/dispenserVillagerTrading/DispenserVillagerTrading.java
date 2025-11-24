package org.adam.dispenserVillagerTrading;

import org.adam.dispenserVillagerTrading.Listeners.DispernserVillagerListener;
import org.adam.dispenserVillagerTrading.Listeners.PressurePlatesListeners;
import org.adam.dispenserVillagerTrading.Models.VillagerOnlyPressurePlate;
import org.adam.dispenserVillagerTrading.Models.VillagerPlateYAMLPersistence;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.bukkit.plugin.java.JavaPlugin;

public final class DispenserVillagerTrading extends JavaPlugin{
    private VillagerOnlyPressurePlate villagerOnlyPressurePlate;
    private VillagerPlateYAMLPersistence persistence;
    private static final String CONFIG_FILE_PATH = "config.properties";


    @Override
    public void onEnable(){
        // making sure config file exists
        this.saveDefaultConfig();

        Properties constants = this.getConstants();
        Boolean allowVillagerDispenserInteractions = this.getConfig()
                                                         .getBoolean("allowVillagerDispenserInteractions",
                                                                     false);
        constants.setProperty("allow.villager.dispenser.interactions", allowVillagerDispenserInteractions.toString());

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
        try(FileReader reader = new FileReader(CONFIG_FILE_PATH)) {
            loadedConfig.load(reader);
        } catch(IOException e){
            this.getLogger().severe("Could not load config.properties file.");
            this.getLogger().severe("Using default constants.");
            loadedConfig.setProperty("namespacedKey.xp.name", "storedXp");
        }
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
