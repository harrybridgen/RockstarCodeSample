# Rockstar Games Sample Code

## Welcome! This repository contains code samples from two personal game development projects:

- **Blothera** - a multiplayer, systems-driven Minecraft server using Java, SQL and Minecraft Paper API.
- **MiniQuest** - a standalone 2D RPG engine built in Python + Pygame.

## Note
> The Blothera code samples shown here are from just 2 of over 30 original plugins.
> The MiniQuest samples represent approximately 10% of the full project.
> This repo only scratches the surface. Many design decisions, including algorithms, data structures, and system architecture, are best discussed live.  
> If you'd like to look deeper, I'm happy to walk through the code and concepts in a quick online call.


---

## Repository Structure
- BlotheraNations/ # Persistent nation-building system (Java)
- BlotheraTrains/ # Modular train system (Java)
- MiniQuest/ # Full 2D RPG engine with combat, quests, AI (Python)

---

## BlotheraNations (Java, Spigot)

A modular system that powers persistent **nations** on a live Minecraft server:

- Custom **nation formation and leadership election**
- Persistent storage using **DAOs + SQL**
- Event-driven architecture (`Listener`s and `Tasks`)
- Taxation system using real-world time intervals
- Designed to integrate cleanly with Towny and other plugins

Example Classes:
- `NationDAO.java` - SQL-backed nation data manager
- `NationCapitalTransferListener.java` - Event handler for capital changes
- `LecternInsertListener.java` - Custom interaction mechanics with in-world items

---

## BlotheraTrains (Java, Paper API)

Experimental module for a **train simulation** inside Minecraft:

- Creates dynamic, linkable, moving trains using Minecarts.
- Uses location vectors, physics-inspired movement.
- Focused on interaction and immersion.
- Loads chunks as they move, meaning they can move even if a player is not present. The Minecraft community has been asking Mojang for this, for over a decade.

---

## MiniQuest (Python, Pygame)

A standalone, feature-rich **2D RPG engine** built from scratch:

- **Custom game loop** with tick-based update/draw
- **Day/night cycle** with lighting transitions
- Robust **combat system** with projectiles, enemies, and particles
- **Quest system** with dialogue UI and NPC interaction
- **Dynamic map loader/saver** using Tiled (.tmx) and pickle state saves
- Custom **camera system**, health bars, inventory, and equipment

Example Files:
- `Game.py` - Core game loop, event handling, rendering, logic
- `Map.py` - World data, tile layer loading, enemy/NPC spawning
- `Particle.py` - All particle effects (walk, fireball, teleport, healing)

---

## Purpose

These systems were developed and used in real, playable environments:

- **Blothera** is a long-running live Minecraft server
- **MiniQuest** is a single-player engine used for narrative prototypes and gameplay experiments

They showcase:
- System architecture
- Game logic
- Extensibility and modularity
- Real-time performance considerations

---

# Going Forward
If you would like to see or learn more or even see it in action, please feel free to schedual an online meeting.
