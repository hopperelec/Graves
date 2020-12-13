package uk.co.hopperelec.mc.graves;

import org.bukkit.Location;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.block.*;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Main extends JavaPlugin implements Listener {
    ArrayList<StorageMinecart> graves = new ArrayList<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this,this);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        List<ItemStack> mainChestItems = event.getDrops();

        if (mainChestItems.size() > 0) {
            StorageMinecart grave = (StorageMinecart) player.getWorld().spawnEntity(player.getLocation(), EntityType.MINECART_CHEST);

            if (mainChestItems.size() > 27) {
                ItemStack extraChest = new ItemStack(Material.CHEST);
                ItemMeta extraChestMeta = extraChest.getItemMeta();
                List<ItemStack> extraChestItems = mainChestItems.subList(26, mainChestItems.size());
                if (extraChestMeta == null) return;
                extraChestMeta.setDisplayName("Rest of your inventory");
                BlockStateMeta extraChestBlockStateMeta = (BlockStateMeta) extraChestMeta;
                BlockState extraChestBlockState = extraChestBlockStateMeta.getBlockState();
                ((Chest) extraChestBlockState).getBlockInventory().setContents(extraChestItems.toArray(new ItemStack[0]));
                extraChestBlockStateMeta.setBlockState(extraChestBlockState);
                extraChest.setItemMeta(extraChestMeta);
                mainChestItems = mainChestItems.subList(0, 26);
                mainChestItems.add(extraChest);
            }
            grave.getInventory().setContents(mainChestItems.toArray(new ItemStack[0]));

            grave.setCustomName(event.getDeathMessage());
            grave.setCustomNameVisible(true);
            grave.setInvulnerable(true);
            grave.setGravity(false);
            grave.setGlowing(true);

            graves.add(grave);
            Location loc = grave.getLocation();
            player.sendMessage("A grave has been made for your stuff at "+(int)loc.getX()+" "+(int)loc.getY()+" "+(int)loc.getZ());
            event.getDrops().clear();
        }
    }

    @EventHandler
    public void onChestOpen(InventoryOpenEvent event) {
        for (StorageMinecart grave : graves) {
            if (event.getInventory().equals(grave.getInventory())) {
                event.setCancelled(true);
                for (ItemStack item : event.getInventory().getContents()) {
                    Location loc = event.getPlayer().getLocation();
                    if (loc.getWorld() == null) return;
                    loc.getWorld().dropItemNaturally(loc,item);
                    grave.remove();
                }
            }
        }
    }
}
