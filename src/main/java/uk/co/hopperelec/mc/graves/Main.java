package uk.co.hopperelec.mc.graves;

import org.bukkit.Bukkit;
import org.bukkit.Location;
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
        EntityEquipment equipment = event.getEntity().getEquipment();
        if (equipment == null) return;
        List<ItemStack> mainChestItems = event.getDrops().subList(0,(int)(event.getDrops().size()-Arrays.stream(equipment.getArmorContents()).filter(Objects::nonNull).count())-1);

        if (mainChestItems.size() > 0) {
            ArmorStand grave = (ArmorStand) player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
            List<ItemStack> newDrops = new ArrayList<>();

            EntityEquipment armorStandEquipment = grave.getEquipment();
            if (armorStandEquipment == null) {
                newDrops.addAll(Arrays.asList(equipment.getArmorContents()));
                return;
            }
            armorStandEquipment.setArmorContents(equipment.getArmorContents());

            if (mainChestItems.size() > 27) {
                ItemStack extraChest = new ItemStack(Material.CHEST);
                ItemMeta extraChestMeta = extraChest.getItemMeta();
                List<ItemStack> extraChestItems = mainChestItems.subList(26, mainChestItems.size());
                if (extraChestMeta == null) {
                    newDrops.addAll(extraChestItems);
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
                newDrops.addAll(mainChestItems);
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
            Location loc = grave.getLocation();
            player.sendMessage("A grave has been made for your stuff at "+(int)loc.getX()+" "+(int)loc.getY()+" "+(int)loc.getZ());
            event.getDrops().clear();
            event.getDrops().addAll(newDrops);
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
            if (Arrays.stream(equipment.getArmorContents()).noneMatch(i -> i.getType()!= Material.AIR) && equipment.getItemInMainHand().getType().equals(Material.AIR) && equipment.getItemInOffHand().getType().equals(Material.AIR)) event.getRightClicked().remove();
        });
    }
}
