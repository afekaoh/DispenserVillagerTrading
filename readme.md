# Dispenser Villager Trading

Dispenser Villager Trading is a Paper plugin that allows dispensers to perform villager trades automatically.  
When a dispenser faces a villager and contains the required ingredients for one of the villager’s trades, it will automatically:

- Select a valid trade (at random, preserving vanilla dispenser behavior),
- Pay the cost using items from its inventory,
- Drop the result behind the dispenser (or insert it into a chest located directly behind it),
- Update villager experience and trade usage as if a player traded manually.

This plugin also adds a **villager-only pressure plate** that only villagers can activate.

---

## Features
- **Dispenser trading**: Dispensers can fulfill any available villager trade — including two-ingredient trades — using their own inventory. Villagers gain experience and trades lock/unlock as normal.
- **Player Experience**: Players gain experience as if they traded manually when dispensers perform trades. Same as Furnace Experience.
- **Villager-only pressure plate**: A custom craftable pressure plate (stone pressure plate + emerald) that only villagers can activate.

---

## Requirements
- **Paper 1.21.10** (or any Paper build targeting the `1.21` API)
- **Java 21** runtime on the server

---

## Installation
1. Build the plugin JAR (see below) or download one from the releases page.
2. Place the JAR into your server’s `plugins/` directory.
3. Start or reload your Paper server.

---

## Building
This project uses Gradle. Common tasks:

- `./gradlew build` — Builds the plugin JAR under `build/libs/`.
- `./gradlew copyToServer` — Builds the JAR and copies it to the server directory specified by `SERVER_LOCATION` in `secrets.properties`.
- `./gradlew runServer` — Starts a local Paper server for testing with the configured Minecraft version.

---

## Configuration
The generated `config.yml` provides the following option:

- **`allowVillagerDispenserInteractions`** (boolean):  
  If `false`, dispensers will *not* fire when a villager stands in front of them *unless* a matching trade exists.  
  Defaults to `false`.

---

## Usage Tips
- Place a dispenser facing a villager, load it with the required trade ingredients, and power it. The plugin selects a valid trade, consumes the payment, and outputs the result behind the dispenser.
- Craft the villager-only pressure plate (stone pressure plate + emerald) to allow villagers — and optionally players — to activate contraptions while preventing other mobs from triggering them.
- Place a chest directly behind the dispenser to automatically collect traded items.

---

## Notes
The plugin is designed specifically for **Paper** and may not function correctly on other server types.

It aims to preserve core vanilla behavior while adding helpful mechanics:

- Trades are selected randomly, matching vanilla dispenser behavior.
- Villager experience and trade usage update normally.
- Result items are dispensed naturally, as if fired by a dispenser.
- Additional quality-of-life features:
    - Traded items are dispensed *behind* the dispenser so villagers don’t pick them up.
    - Chests placed behind the dispenser collect trade outputs automatically.
    - A villager-only pressure plate allows villager-driven redstone without mob interference.

---

## Future Plans
- Selecting a specific trade via redstone signal strength.
- Moving the villager-only pressure plate to a dedicated plugin. Or adding more pressure plate types.

---

## Feedback and Contributions
Feedback, issues, and pull requests are welcome!  
Visit the GitHub repository: [DispenserVillagerTrading](https://github.com/afekaoh/DispenserVillagerTrading)
