package dansplugins.wildpets.eventhandlers;

import dansplugins.wildpets.WildPets;
import dansplugins.wildpets.data.EphemeralData;
import dansplugins.wildpets.data.PersistentData;
import dansplugins.wildpets.objects.EntityConfig;
import dansplugins.wildpets.objects.Pet;
import dansplugins.wildpets.services.LocalConfigService;
import dansplugins.wildpets.services.LocalEntityConfigService;
import dansplugins.wildpets.utils.Scheduler;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.Random;
import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 */
public class InteractionHandler implements Listener {

    @EventHandler()
    public void handle(PlayerInteractEntityEvent event) {
        Entity clickedEntity = event.getRightClicked();

        EntityConfig entityConfig = LocalEntityConfigService.getInstance().acquireConfiguration(clickedEntity);

        Player player = event.getPlayer();
        
        Pet pet = PersistentData.getInstance().getPet(clickedEntity);

        if (EphemeralData.getInstance().isPlayerTaming(player.getUniqueId())) {
            setRightClickCooldown(player, 1);

            if (clickedEntity instanceof Player) {
                player.sendMessage(ChatColor.RED + "You can't tame players.");
                return;
            }

            if (!(clickedEntity instanceof LivingEntity)) {
                player.sendMessage(ChatColor.RED + "You can only tame living entities.");
                EphemeralData.getInstance().setPlayerAsNotTaming(player.getUniqueId());
                return;
            }

            if (pet != null) {
                player.sendMessage(ChatColor.RED + "That entity is already a pet.");
                EphemeralData.getInstance().setPlayerAsNotTaming(player.getUniqueId());
                return;
            }

            if (WildPets.getInstance().isDebugEnabled() && entityConfig.getType().equalsIgnoreCase("default")) {
                player.sendMessage(ChatColor.BLUE + "[DEBUG] This entity doesn't have a configuration.");
            }

            if (!entityConfig.isEnabled()) {
                player.sendMessage(ChatColor.RED + "Taming has been disabled for this entity.");
                return;
            }

            int numPets = PersistentData.getInstance().getPetList(player.getUniqueId()).getNumPets();
            int petLimit = LocalConfigService.getInstance().getInt("petLimit");
            if (WildPets.getInstance().isDebugEnabled()) {
                System.out.println("[DEBUG] Number of pets: " + numPets);
                System.out.println("[DEBUG] Pet Limit: " + petLimit);
            }
            if (numPets >= petLimit) {
                player.sendMessage(ChatColor.RED + "You have reached your pet limit.");
                EphemeralData.getInstance().setPlayerAsNotTaming(player.getUniqueId());
                return;
            }

            ItemStack itemStack = player.getInventory().getItemInMainHand();
            Material requiredMaterial = entityConfig.getRequiredTamingItem();
            int requiredAmount = entityConfig.getTamingItemAmount();
            if (itemStack.getType() != requiredMaterial || itemStack.getAmount() < requiredAmount) {
                player.sendMessage(ChatColor.RED + "You need to use " + requiredAmount + " " + requiredMaterial.name().toLowerCase() + " to tame this entity.");
                EphemeralData.getInstance().setPlayerAsNotTaming(player.getUniqueId());
                return;
            }

            // handle chance to tame
            if (!rollDice(entityConfig.getChanceToSucceed())) {
                player.sendMessage(ChatColor.RED + "Taming failed.");
                if (LocalConfigService.getInstance().getBoolean("cancelTamingAfterFailedAttempt")) {
                    EphemeralData.getInstance().setPlayerAsNotTaming(player.getUniqueId());
                }
                if (itemStack.getAmount() > requiredAmount) {
                    player.getInventory().setItemInMainHand(new ItemStack(itemStack.getType(), itemStack.getAmount() - requiredAmount));
                }
                else {
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                }
                return;
            }

            PersistentData.getInstance().addNewPet(player, clickedEntity);
            player.sendMessage(ChatColor.GREEN + "Tamed.");
            EphemeralData.getInstance().setPlayerAsNotTaming(player.getUniqueId());

            if (itemStack.getAmount() > requiredAmount) {
                player.getInventory().setItemInMainHand(new ItemStack(itemStack.getType(), itemStack.getAmount() - requiredAmount));
            }
            else {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            }

            EphemeralData.getInstance().selectPetForPlayer(PersistentData.getInstance().getPet(clickedEntity), player.getUniqueId());
        }
        else if (EphemeralData.getInstance().isPlayerSelecting(player.getUniqueId())) {
            setRightClickCooldown(player, 1);

            if (pet == null) {
                player.sendMessage(ChatColor.RED + "That entity is not a pet.");
                EphemeralData.getInstance().setPlayerAsNotSelecting(player.getUniqueId());
                return;
            }

            if (PersistentData.getInstance().getPlayersPet(player, clickedEntity) == null) {
                player.sendMessage(ChatColor.RED + "That entity is not your pet.");
                EphemeralData.getInstance().setPlayerAsNotSelecting(player.getUniqueId());
                return;
            }

            EphemeralData.getInstance().selectPetForPlayer(pet, player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + pet.getName() + " selected.");
            EphemeralData.getInstance().setPlayerAsNotSelecting(player.getUniqueId());
        }
        else if (EphemeralData.getInstance().isPlayerLocking(player.getUniqueId())) {

            if (pet == null) {
                player.sendMessage(ChatColor.RED + "This entity isn't a pet.");
                EphemeralData.getInstance().setPlayerAsNotLocking(player.getUniqueId());
                return;
            }

            if (!pet.getOwnerUUID().equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "This is not your pet.");
                EphemeralData.getInstance().setPlayerAsNotLocking(player.getUniqueId());
                return;
            }

            boolean locked = pet.isLocked();
            if (locked) {
                player.sendMessage(ChatColor.RED + "This pet is already locked.");
                EphemeralData.getInstance().setPlayerAsNotLocking(player.getUniqueId());
                return;
            }
            pet.setLocked(true);
            player.sendMessage(ChatColor.GREEN + "This pet has been locked.");
            EphemeralData.getInstance().setPlayerAsNotLocking(player.getUniqueId());
            event.setCancelled(true);
        }
        else if (EphemeralData.getInstance().isPlayerUnlocking(player.getUniqueId())) {

            if (pet == null) {
                player.sendMessage(ChatColor.RED + "This entity isn't a pet.");
                EphemeralData.getInstance().setPlayerAsNotUnlocking(player.getUniqueId());
                return;
            }

            if (!pet.getOwnerUUID().equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "This is not your pet.");
                EphemeralData.getInstance().setPlayerAsNotUnlocking(player.getUniqueId());
                return;
            }

            boolean locked = pet.isLocked();
            if (!locked) {
                player.sendMessage(ChatColor.RED + "This pet is already unlocked.");
                EphemeralData.getInstance().setPlayerAsNotUnlocking(player.getUniqueId());
                return;
            }
            pet.setLocked(false);
            player.sendMessage(ChatColor.GREEN + "This pet has been unlocked.");
            EphemeralData.getInstance().setPlayerAsNotUnlocking(player.getUniqueId());
            event.setCancelled(true);
        }
        else if (EphemeralData.getInstance().isPlayerCheckingAccess(player.getUniqueId())) {

            if (pet == null) {
                player.sendMessage(ChatColor.RED + "This entity isn't a pet.");
                EphemeralData.getInstance().setPlayerAsNotCheckingAccess(player.getUniqueId());
                EphemeralData.getInstance().setPlayerAsNotCheckingAccess(player.getUniqueId());
                return;
            }

            boolean locked = pet.isLocked();
            if (!locked) {
                player.sendMessage(ChatColor.RED + "This pet isn't locked.");
                EphemeralData.getInstance().setPlayerAsNotCheckingAccess(player.getUniqueId());
                return;
            }

            if (pet.getAccessList().size() == 0) {
                player.sendMessage(ChatColor.RED + "No one has access to this pet.");
                EphemeralData.getInstance().setPlayerAsNotCheckingAccess(player.getUniqueId());
                return;
            }

            if (pet.getAccessList().size() == 1 && pet.getAccessList().get(0).equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "No one has access to this pet but you.");
                EphemeralData.getInstance().setPlayerAsNotCheckingAccess(player.getUniqueId());
                return;
            }

            player.sendMessage(ChatColor.AQUA + "The following players have access to this pet:");

            UUIDChecker uuidChecker = new UUIDChecker();

            for (UUID uuid : pet.getAccessList()) {
                String playerName = uuidChecker.findPlayerNameBasedOnUUID(uuid);
                if (playerName != null) {
                    player.sendMessage(ChatColor.AQUA + playerName);
                }
            }
            EphemeralData.getInstance().setPlayerAsNotCheckingAccess(player.getUniqueId());
            event.setCancelled(true);
        }
        else if (EphemeralData.getInstance().isPlayerGrantingAccess(player.getUniqueId())) {
            // TODO: implement
        }
        else if (EphemeralData.getInstance().isPlayerRevokingAccess(player.getUniqueId())) {
            // TODO: implement
        }
        else {

            if (!EphemeralData.getInstance().hasRightClickCooldown(player.getUniqueId())) {
                if (pet == null) {
                    return;
                }

                setRightClickCooldown(player, LocalConfigService.getInstance().getInt("rightClickViewCooldown"));

                pet.sendInfoToPlayer(player);

                if (LocalConfigService.getInstance().getBoolean("rightClickToSelect")) {

                    if (!pet.getOwnerUUID().equals(player.getUniqueId())) {
                        return;
                    }

                    Pet petSelection = EphemeralData.getInstance().getPetSelectionForPlayer(player.getUniqueId());
                    if (petSelection == null || !petSelection.getUniqueID().equals(pet.getUniqueID())) {
                        EphemeralData.getInstance().selectPetForPlayer(pet, player.getUniqueId());
                        player.sendMessage(ChatColor.GREEN + pet.getName() + " selected.");
                    }

                }
            }

        }

        if (pet == null) {
            return;
        }

        if (pet.isLocked() && !pet.getOwnerUUID().equals(player.getUniqueId()) && !pet.getAccessList().contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You don't have access to this pet.");
            event.setCancelled(true);
        }

    }

    @EventHandler()
    public void handle(EntityMountEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player) entity;

        Entity mount = event.getMount();
        Pet pet = PersistentData.getInstance().getPet(mount);
        if (pet == null) {
            return;
        }

        if (!pet.isLocked()) {
            return;
        }

        if (!pet.getOwnerUUID().equals(player.getUniqueId()) && !pet.getAccessList().contains(player.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You don't have access to this pet.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "You mount " + pet.getName());
    }

    private void setRightClickCooldown(Player player, int seconds) {
        EphemeralData.getInstance().setRightClickCooldown(player.getUniqueId(), true);
        Scheduler.scheduleRightClickCooldownSetter(player, seconds);
    }

    private boolean rollDice(double chanceToSucceed) {
        double chanceToFail = 1 - chanceToSucceed;
        if (WildPets.getInstance().isDebugEnabled()) { System.out.println("Rolling dice! Chance to fail: " + chanceToFail * 100 + "%"); }
        Random random = new Random();
        double generatedNumber = random.nextDouble();
        if (WildPets.getInstance().isDebugEnabled()) { System.out.println("Dice landed on " + generatedNumber * 100 + ". " + chanceToFail * 100 + " was required."); }
        return generatedNumber > chanceToFail;
    }
}