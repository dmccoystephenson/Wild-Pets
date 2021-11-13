package dansplugins.wildpets;

import dansplugins.wildpets.bstats.Metrics;
import dansplugins.wildpets.managers.ConfigManager;
import dansplugins.wildpets.managers.EntityConfigManager;
import dansplugins.wildpets.managers.StorageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class WildPets extends JavaPlugin {
  
    private static WildPets instance;

    public static WildPets getInstance() {
        return instance;
    }

    private final String version = "v1.1-alpha-5";

    @Override
    public void onEnable() {
        instance = this;

        // create/load config
        if (!(new File("./plugins/WildPets/config.yml").exists())) {
            EntityConfigManager.getInstance().initializeWithDefaults();
            ConfigManager.getInstance().saveMissingConfigDefaultsIfNotPresent();
        }
        else {
            // pre load compatibility checks
            if (isVersionMismatched()) {
                ConfigManager.getInstance().saveMissingConfigDefaultsIfNotPresent();
            }
            reloadConfig();
            EntityConfigManager.getInstance().initializeWithConfig();
        }

        // schedule auto save
        Scheduler.getInstance().scheduleAutosave();

        // register events
        EventRegistry.getInstance().registerEvents();

        // load save files
        StorageManager.getInstance().load();

        // bStats
        int pluginId = 12332;
        Metrics metrics = new Metrics(this, pluginId);
    }

    @Override
    public void onDisable() {
        StorageManager.getInstance().save();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        CommandInterpreter commandInterpreter = new CommandInterpreter();
        return commandInterpreter.interpretCommand(sender, label, args);
    }

    public String getVersion() {
        return version;
    }

    public boolean isDebugEnabled() {
        return getConfig().getBoolean("configOptions.debugMode");
    }

    private boolean isVersionMismatched() {
        return !getConfig().getString("version").equalsIgnoreCase(getVersion());
    }
}