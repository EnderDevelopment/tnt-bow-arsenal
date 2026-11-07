package com.enderdevelopment.plugin;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Main extends JavaPlugin implements Listener {
    private NamespacedKey bowKey;
    private NamespacedKey projectileKey;

    @Override
    public void onEnable() {
        bowKey = new NamespacedKey(this, "tnt_bow");
        projectileKey = new NamespacedKey(this, "tnt_projectile");
        Bukkit.getPluginManager().registerEvents(this, this);
        getCommand("tntbow").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("This command can only be used by a player.");
                return true;
            }
            if (!player.hasPermission("tntbow.use")) {
                player.sendMessage(Component.text("You do not have permission to use this.", NamedTextColor.RED));
                return true;
            }
            player.getInventory().addItem(createBow());
            player.sendMessage(Component.text("TNT Bow added to your inventory.", NamedTextColor.GOLD));
            return true;
        });
    }

    private ItemStack createBow() {
        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("TNT Bow", NamedTextColor.RED));
            meta.lore(List.of(Component.text("Shoots explosive TNT arrows", NamedTextColor.GRAY),
                    Component.text("Sneak and right-click to throw TNT", NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(bowKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isTntBow(ItemStack item) {
        if (item == null || item.getType() != Material.BOW || !item.hasItemMeta()) {
            return false;
        }
        Byte marked = item.getItemMeta().getPersistentDataContainer().get(bowKey, PersistentDataType.BYTE);
        return marked != null && marked == (byte) 1;
    }

    @EventHandler
    public void onShootBow(EntityShootBowEvent event) {
        if (isTntBow(event.getBow())) {
            event.getProjectile().getPersistentDataContainer().set(projectileKey,
                    PersistentDataType.STRING, "arrow");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!isTntBow(event.getItem()) || !event.getPlayer().isSneaking()) {
            return;
        }
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        Snowball charge = player.launchProjectile(Snowball.class);
        charge.setItem(new ItemStack(Material.TNT));
        charge.setVelocity(player.getLocation().getDirection().multiply(1.25));
        charge.getPersistentDataContainer().set(projectileKey, PersistentDataType.STRING, "thrown_tnt");
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        String marker = projectile.getPersistentDataContainer().get(projectileKey, PersistentDataType.STRING);
        if ("arrow".equals(marker) || "thrown_tnt".equals(marker)) {
            Entity shooter = projectile.getShooter() instanceof Entity entity ? entity : null;
            projectile.getWorld().createExplosion(projectile.getLocation(), 3.0f, false, true, shooter);
            projectile.remove();
        }
    }
}
