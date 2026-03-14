package dev.shardshopblock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Set;

public class ShopBlockListener implements Listener {

    private final ShardShopBlock plugin;

    // All shulker box materials
    private static final Set<Material> SHULKER_BOXES = Set.of(
        Material.SHULKER_BOX,
        Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX,
        Material.MAGENTA_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX,
        Material.YELLOW_SHULKER_BOX, Material.LIME_SHULKER_BOX,
        Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX,
        Material.LIGHT_GRAY_SHULKER_BOX, Material.CYAN_SHULKER_BOX,
        Material.PURPLE_SHULKER_BOX, Material.BLUE_SHULKER_BOX,
        Material.BROWN_SHULKER_BOX, Material.GREEN_SHULKER_BOX,
        Material.RED_SHULKER_BOX, Material.BLACK_SHULKER_BOX
    );

    public ShopBlockListener(ShardShopBlock plugin) {
        this.plugin = plugin;
    }

    // ── Right-click → open ShardShop ─────────────────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!SHULKER_BOXES.contains(block.getType())) return;
        if (!plugin.isShopBlock(block.getLocation())) return;

        // Cancel the normal shulker box opening
        event.setCancelled(true);

        Player player = event.getPlayer();

        // Check if ShardSystem is available
        org.bukkit.plugin.Plugin shardSystem = plugin.getServer().getPluginManager().getPlugin("ShardSystem");
        if (shardSystem == null || !shardSystem.isEnabled()) {
            player.sendMessage(Component.text(
                "✗ ShardSystem is not loaded. Cannot open the Shard Shop.",
                NamedTextColor.RED));
            return;
        }

        // Dispatch /shardshop as the player
        plugin.getServer().dispatchCommand(player, "shardshop");
    }

    // ── Prevent breaking registered shop blocks ──────────────────────────────
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!plugin.isShopBlock(event.getBlock().getLocation())) return;

        Player player = event.getPlayer();

        // OPs and admins with the permission may still break it (with a warning)
        if (player.hasPermission("shardshopblock.admin") || player.isOp()) {
            player.sendMessage(Component.text(
                "⚠ This is a registered Shard Shop block! Use ",
                NamedTextColor.YELLOW)
                .append(Component.text("/shopblock remove", NamedTextColor.WHITE))
                .append(Component.text(" first to unregister it.", NamedTextColor.YELLOW))
            );
            event.setCancelled(true);
            return;
        }

        // Normal players cannot break it
        event.setCancelled(true);
        player.sendMessage(Component.text(
            "✗ You cannot break a Shard Shop block.", NamedTextColor.RED));
    }

    // ── Protect against explosions ───────────────────────────────────────────
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(b -> plugin.isShopBlock(b.getLocation()));
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(b -> plugin.isShopBlock(b.getLocation()));
    }
}
