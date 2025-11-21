package uk.co.hopperelec.mc.graves;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class GravesPlugin extends JavaPlugin implements Listener {
    final NamespacedKey graveKey = new NamespacedKey(this, "hopperelec-grave");
    final NamespacedKey overflowKey = new NamespacedKey(this, "hopperelec-grave-overflow");

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        final Player player = event.getEntity();
        List<ItemStack> drops = event.getDrops();
        if (drops.isEmpty()) return;

        final StorageMinecart grave = (StorageMinecart) player.getWorld().spawnEntity(player.getLocation(), EntityType.CHEST_MINECART);

        if (drops.size() > 27) {
            final ItemStack overflowItem = new ItemStack(Material.CHEST);
            final BlockStateMeta overflowItemMeta = (BlockStateMeta) overflowItem.getItemMeta();
            if (overflowItemMeta != null) {
                final BlockState state = overflowItemMeta.getBlockState();
                if (state instanceof Chest overflowItemState) {
                    final List<ItemStack> overflowItems = new ArrayList<>(drops.subList(26, drops.size()));
                    overflowItemState.getInventory().setContents(overflowItems.toArray(new ItemStack[0]));
                    overflowItemMeta.setBlockState(state);
                    overflowItemMeta.displayName(Component.text("Rest of your inventory"));
                    overflowItemMeta.getPersistentDataContainer().set(overflowKey, PersistentDataType.BYTE, (byte) 1);
                    overflowItem.setItemMeta(overflowItemMeta);
                    drops = drops.subList(0, 26);
                    drops.add(overflowItem);
                }
            }
        }

        grave.getInventory().setContents(drops.toArray(new ItemStack[0]));
        event.getDrops().clear();
        grave.getPersistentDataContainer().set(graveKey, PersistentDataType.BYTE, (byte) 1);

        grave.setCustomNameVisible(true);
        grave.setInvulnerable(true);
        grave.setGlowing(true);
        if (event.deathMessage() != null) {
            grave.customName(event.deathMessage());
        } else {
            grave.customName(Component.text(player.getName() + "'s Grave"));
        }

        final Location loc = grave.getLocation();
        player.sendMessage(Component.text("A grave has been made for your stuff at " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()));
    }

    void dropInventoryContents(@NotNull Location loc, @NotNull Inventory inventory) {
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                if (item.getItemMeta() != null) {
                    if (item.getItemMeta().getPersistentDataContainer().has(overflowKey, PersistentDataType.BYTE)) {
                        // If it's the overflow chest, drop its contents too
                        final BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
                        if (meta != null) {
                            final BlockState state = meta.getBlockState();
                            if (state instanceof Chest overflowChest) {
                                dropInventoryContents(loc, overflowChest.getInventory());
                                continue; // Skip dropping the chest item itself
                            }
                        }
                    }
                }
                loc.getWorld().dropItemNaturally(loc, item);
            }
        }
        inventory.clear();
    }

    @EventHandler
    public void onInventoryOpen(@NotNull InventoryOpenEvent event) {
        if (event.getInventory().getHolder() instanceof StorageMinecart minecart) {
            if (minecart.getPersistentDataContainer().has(graveKey, PersistentDataType.BYTE)) {
                event.setCancelled(true);
                dropInventoryContents(minecart.getLocation(), minecart.getInventory());
                minecart.remove();
            }
        }
    }

    @EventHandler
    public void onVehicleMove(@NotNull VehicleMoveEvent event) {
        // Prevent graves from being moved by anything other than gravity
        if (event.getVehicle() instanceof StorageMinecart minecart) {
            if (minecart.getPersistentDataContainer().has(graveKey, PersistentDataType.BYTE)) {
                if (event.getFrom().getY() > event.getTo().getY()) {
                    // Allow movement downwards (gravity)
                    return;
                }
                minecart.teleport(event.getFrom());
            }
        }
    }

    @EventHandler
    public void onInventoryMoveItem(@NotNull InventoryMoveItemEvent event) {
        // Prevent hoppers from taking items out of graves
        if (event.getSource().getHolder() instanceof StorageMinecart minecart) {
            if (minecart.getPersistentDataContainer().has(graveKey, PersistentDataType.BYTE)) {
                event.setCancelled(true);
                return;
            }
        }
        // Prevent hoppers from putting items into graves
        if (event.getDestination().getHolder() instanceof StorageMinecart minecart) {
            if (minecart.getPersistentDataContainer().has(graveKey, PersistentDataType.BYTE)) {
                event.setCancelled(true);
            }
        }
    }
}
