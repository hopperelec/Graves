package uk.co.hopperelec.mc.graves;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.block.*;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class Main extends JavaPlugin implements Listener {
    ArrayList<ArmorStand> graves = new ArrayList<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this,this);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        List<ItemStack> mainChestItems = event.getDrops();

        if (mainChestItems.size() > 0) {
            ArmorStand grave = (ArmorStand) player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
            event.getDrops().clear();

            EntityEquipment armorStandEquipment = grave.getEquipment();
            if (armorStandEquipment == null) {
                event.getDrops().addAll(Arrays.asList(player.getInventory().getArmorContents()));
                return;
            }
            armorStandEquipment.setArmorContents(player.getInventory().getArmorContents());

            if (mainChestItems.size() > 27) {
                ItemStack extraChest = new ItemStack(Material.CHEST);
                ItemMeta extraChestMeta = extraChest.getItemMeta();
                List<ItemStack> extraChestItems = mainChestItems.subList(26, (int) (mainChestItems.size() - Arrays.stream(armorStandEquipment.getArmorContents()).filter(Objects::nonNull).count()));
                if (extraChestMeta == null) {
                    event.getDrops().addAll(extraChestItems);
                    return;
                }
                extraChestMeta.setDisplayName("Rest of your inventory");
                BlockStateMeta extraChestBlockStateMeta = (BlockStateMeta) extraChestMeta;
                BlockState extraChestBlockState = extraChestBlockStateMeta.getBlockState();
                ((Chest) extraChestBlockState).getBlockInventory().setContents(extraChestItems.toArray(new ItemStack[0]));
                extraChestBlockStateMeta.setBlockState(extraChestBlockState);
                extraChest.setItemMeta(extraChestMeta);
                mainChestItems = mainChestItems.subList(0, 26);
                mainChestItems.add(extraChest);
            }
            ItemStack mainChest = new ItemStack(Material.CHEST);
            ItemMeta mainChestMeta = mainChest.getItemMeta();
            if (mainChestMeta == null) {
                event.getDrops().addAll(mainChestItems);
                return;
            }
            mainChestMeta.setDisplayName("Your inventory");
            BlockStateMeta mainChestBlockStateMeta = (BlockStateMeta) mainChestMeta;
            BlockState mainChestBlockState = mainChestBlockStateMeta.getBlockState();
            ((Chest) mainChestBlockState).getBlockInventory().setContents(mainChestItems.toArray(new ItemStack[0]));
            mainChestBlockStateMeta.setBlockState(mainChestBlockState);
            mainChest.setItemMeta(mainChestMeta);
            armorStandEquipment.setItemInMainHand(mainChest);

            grave.setCustomName(event.getDeathMessage());
            grave.setCustomNameVisible(true);
            grave.setInvulnerable(true);
            grave.setBasePlate(false);
            grave.setGravity(false);
            grave.setGlowing(true);

            graves.add(grave);
            player.sendMessage("A grave has been made for your stuff at " + grave.getLocation().toString());
        }
    }

    @EventHandler
    public void onArmorStandInteract(PlayerArmorStandManipulateEvent event) {
        EntityEquipment equipment = event.getRightClicked().getEquipment();
        if (equipment == null) {
            event.getRightClicked().remove();
            return;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(this,() -> {
            if (Arrays.stream(equipment.getArmorContents()).noneMatch(i -> i.getType().equals(Material.AIR)) && equipment.getItemInMainHand().getType().equals(Material.AIR) && equipment.getItemInOffHand().getType().equals(Material.AIR)) event.getRightClicked().remove();
        });
    }
}
