package dansplugins.wildpets.managers;

import dansplugins.wildpets.objects.EntityConfig;
import org.bukkit.Material;
import org.bukkit.entity.Entity;

import java.util.ArrayList;

public class EntityConfigManager {

    private static EntityConfigManager instance;

    private ArrayList<EntityConfig> entityConfigs = new ArrayList<>();

    private EntityConfigManager() {
        initialize();
    }

    public static EntityConfigManager getInstance() {
        if (instance == null) {
            instance = new EntityConfigManager();
        }
        return instance;
    }

    public EntityConfig acquireConfiguration(Entity entity) {
        for (EntityConfig entityConfig : entityConfigs) {
            if (entity.getType().name().equalsIgnoreCase(entityConfig.getType())) {
                return entityConfig;
            }
        }
        return getDefaultConfiguration();
    }

    private void initialize() {
        entityConfigs.add(new EntityConfig("Axilotl", 0.10, Material.KELP, 20));
        entityConfigs.add(new EntityConfig("Bat", 0.10, Material.PUMPKIN_PIE, 1));
        entityConfigs.add(new EntityConfig("Cat", 0.10, Material.SALMON, 5));
        entityConfigs.add(new EntityConfig("Chicken", 0.10, Material.WHEAT_SEEDS, 10));
        entityConfigs.add(new EntityConfig("Cod", 0.10, Material.KELP, 20));
        entityConfigs.add(new EntityConfig("Cow", 0.10, Material.WHEAT, 30));
        entityConfigs.add(new EntityConfig("Donkey", 0.10, Material.CARROT, 5));
        entityConfigs.add(new EntityConfig("Fox", 0.10, Material.SWEET_BERRIES, 10));
        entityConfigs.add(new EntityConfig("Glow_Squid", 0.10, Material.KELP, 20));
        entityConfigs.add(new EntityConfig("Horse", 0.10, Material.APPLE, 5));
        entityConfigs.add(new EntityConfig("Mooshroom", 0.10, Material.RED_MUSHROOM, 10));
        entityConfigs.add(new EntityConfig("Mule", 0.10, Material.APPLE, 5));

    }

    private EntityConfig getDefaultConfiguration() {
        return new EntityConfig("default", 0.25, Material.WHEAT, 10);
    }

}
