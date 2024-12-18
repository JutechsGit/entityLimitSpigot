package de.jutechs.entityLimitSpigot;


import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class Main extends JavaPlugin {

    private int renderDistance;
    private int checkIntervalTicks;
    private int entityLimit;
    private int tickCounter;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        Bukkit.getScheduler().runTaskTimer(this, this::checkEntities, 0L, 1L);
    }

    private void loadConfig() {
        FileConfiguration config = getConfig();
        renderDistance = config.getInt("renderDistance", 10);
        checkIntervalTicks = config.getInt("checkIntervalTicks", 200);
        entityLimit = config.getInt("entityLimit", 1000);
        tickCounter = 0;
    }

    private void checkEntities() {
        tickCounter++;
        if (tickCounter < checkIntervalTicks) {
            return;
        }
        tickCounter = 0;

        for (World world : Bukkit.getWorlds()) {
            for (Player player : world.getPlayers()) {
                checkChunksAroundPlayer(player, world);
            }
        }
    }

    private void checkChunksAroundPlayer(Player player, World world) {
        int playerChunkX = player.getLocation().getChunk().getX();
        int playerChunkZ = player.getLocation().getChunk().getZ();

        Map<EntityType, Integer> globalEntityCountMap = new HashMap<>();
        int totalEntities = 0;

        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                int chunkX = playerChunkX + dx;
                int chunkZ = playerChunkZ + dz;
                Chunk chunk = world.getChunkAt(chunkX, chunkZ);

                for (Entity entity : chunk.getEntities()) {
                    EntityType entityType = entity.getType();
                    if (entityType == EntityType.PLAYER || entityType == EntityType.ITEM) {
                        continue; // Skip players and items
                    }

                    globalEntityCountMap.put(entityType, globalEntityCountMap.getOrDefault(entityType, 0) + 1);
                    totalEntities++;
                }
            }
        }

        // getLogger().info("Total entities in checked area: " + totalEntities);
        for (Map.Entry<EntityType, Integer> entry : globalEntityCountMap.entrySet()) {
        //    getLogger().info("Entity Type: " + entry.getKey() + ", Count: " + entry.getValue());
        }

        if (totalEntities > entityLimit) {
            removeMostCommonEntities(world, playerChunkX, playerChunkZ, globalEntityCountMap);
        }
    }

    private void removeMostCommonEntities(World world, int playerChunkX, int playerChunkZ, Map<EntityType, Integer> globalEntityCountMap) {
        EntityType mostCommonEntity = null;
        int maxCount = 0;

        for (Map.Entry<EntityType, Integer> entry : globalEntityCountMap.entrySet()) {
            if (entry.getValue() > maxCount) {
                mostCommonEntity = entry.getKey();
                maxCount = entry.getValue();
            }
        }

        if (mostCommonEntity != null) {
            // getLogger().info("Removing entities of type " + mostCommonEntity + " from the checked area.");
            for (int dx = -renderDistance; dx <= renderDistance; dx++) {
                for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                    Chunk chunk = world.getChunkAt(playerChunkX + dx, playerChunkZ + dz);
                    for (Entity entity : chunk.getEntities()) {
                        if (entity.getType() == mostCommonEntity) {
                            entity.remove();
                        }
                    }
                }
            }
        }
    }
}
