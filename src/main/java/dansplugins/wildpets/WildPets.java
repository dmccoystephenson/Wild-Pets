package dansplugins.wildpets;

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

    private final String version = "v0.18";

    @Override
    public void onEnable() {
        instance = this;

        // create/load config
        if (!(new File("./plugins/WildPets/config.yml").exists())) {
            EntityConfigManager.getInstance().initializeWithDefaults();
            ConfigManager.getInstance().saveConfigDefaults();
        }
        else {
            // pre load compatibility checks
            if (isVersionMismatched()) {
                ConfigManager.getInstance().handleVersionMismatch();
            }
            reloadConfig();
            EntityConfigManager.getInstance().initializeWithConfig();
        }

        EventRegistry.getInstance().registerEvents();

        StorageManager.getInstance().load();
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

    private boolean isVersionMismatched() {
        return !getConfig().getString("version").equalsIgnoreCase(getVersion());
    }
}