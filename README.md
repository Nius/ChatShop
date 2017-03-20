# ChatShop
A chat-based shop plugin for Minecraft.

## Purpose
This plugin creates a virtual shop with which players interact solely through chat commands. The plugin uses a database to store market listings and a complete transaction history, and integrates with economy providers via milkbowl.Vault. Players can interact with the shop from anywhere at any time (subject to configured limitations) and are notified every time a player creates or updates a new sell offer.

## Setup
ChatShop automatically creates the tables it needs, all prefixed with "ChatShop_" in order to prevent table collisions. Administrators need only point the ChatShop to an accessible database in its config.yml and the plugin will handle the rest.
ChatShop will generate a config.yml file on first run, complete with default colors and rules.
ChatShop will also generate a default items.csv file on first run (see below).

## Items Dictionary
The chatshop uses a CSV file to understand Minecraft items. This file (automatically generated on first run or if missing) is fully populated with all vanilla Minecraft items, their ID:DMG values, and at least one alias. This file can be edited to add or remove item aliases and rules, such as allowing users to refer to "iron ore" as "iore" or banning bedrock from the market.
Items not listed in ChatShop.ItemManager will not be at all understood and the ChatShop will refuse to interact with them in any way.
This file contains its own formatting documentation and advisories where necessary.

## Developer Features
* Due to Minecraft's hints about moving away from the ID:DMG system, ChatShop's ItemManager is designed to interact with both ID:DMG and ItemStack(MATERIAL,DMG) and the entire plugin is built to prefer the new ItemStack(MATERIAL,DMG) system.
* ChatShop comes with a full compliment of JavaDoc markup which can be generated at your discretion.
* ChatShop's ItemManager (along with items.csv) is a robust tool which could easily be ported to other projects.
* Bukkit has not played nicely with Potions since Minecraft 1.9, because Potions now use NBT rather than damage values to differentiate themselves. ChatShop has constructed a way around this problem, interfacing cleanly with Spigot and providing full support for all potions and tipped arrows.

## Limitations
* ChatShop currently does not support non-vanilla items.
* No part of this plugin is self-updating, nor will it notify you when an update is available.
