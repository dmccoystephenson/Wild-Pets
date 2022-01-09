package dansplugins.wildpets.commands;

import dansplugins.wildpets.data.EphemeralData;
import dansplugins.wildpets.objects.Pet;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.abs.AbstractPluginCommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author Daniel McCoy Stephenson
 */
public class FollowCommand extends AbstractPluginCommand {

    public FollowCommand() {
        super(new ArrayList<>(Arrays.asList("follow")), new ArrayList<>(Arrays.asList("wp.follow")));
    }

    public boolean execute(CommandSender sender) {

        if (!(sender instanceof Player)) {
            return false;
        }

        Player player = (Player) sender;

        Pet pet = EphemeralData.getInstance().getPetSelectionForPlayer(player.getUniqueId());

        if (pet == null) {
            player.sendMessage(ChatColor.RED + "No pet selected.");
            return false;
        }

        pet.setFollowing();
        player.sendMessage(ChatColor.GREEN + pet.getName() + " is now following you.");
        return true;
    }

    @Override
    public boolean execute(CommandSender commandSender, String[] strings) {
        return execute(commandSender);
    }
}