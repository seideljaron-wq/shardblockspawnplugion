package dev.shardshopblock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;

public class ShopBlockCommand implements CommandExecutor, TabCompleter {

    private static final Set<Material> SHULKER_BOXES = Set.of(
        Material.SHULKER_BOX, Material.WHITE_SHULKER_BOX, Material.ORANGE_SHULKER_BOX,
        Material.MAGENTA_SHULKER_BOX, Material.LIGHT_BLUE_SHULKER_BOX, Material.YELLOW_SHULKER_BOX,
        Material.LIME_SHULKER_BOX, Material.PINK_SHULKER_BOX, Material.GRAY_SHULKER_BOX,
        Material.LIGHT_GRAY_SHULKER_BOX, Material.CYAN_SHULKER_BOX, Material.PURPLE_SHULKER_BOX,
        Material.BLUE_SHULKER_BOX, Material.BROWN_SHULKER_BOX, Material.GREEN_SHULKER_BOX,
        Material.RED_SHULKER_BOX, Material.BLACK_SHULKER_BOX
    );

    private final ShardShopBlock plugin;

    public ShopBlockCommand(ShardShopBlock plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // Console can only use /shopblock list
        if (!(sender instanceof Player player)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("list")) listBlocks(sender);
            else sender.sendMessage("Usage (console): /shopblock list");
            return true;
        }

        // ── Permission: only ShardSystem admins (or OPs as fallback) ─────────
        if (!player.isOp() && !plugin.isShardAdmin(player.getUniqueId())) {
            player.sendMessage(Component.text(
                "✗ Only Shard Admins can manage shop blocks.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) { sendHelp(player); return true; }

        switch (args[0].toLowerCase()) {
            case "set"    -> handleSet(player);
            case "remove" -> handleRemove(player);
            case "list"   -> listBlocks(player);
            default       -> sendHelp(player);
        }
        return true;
    }

    // ── /shopblock set ────────────────────────────────────────────────────────

    private void handleSet(Player player) {
        Block target = player.getTargetBlockExact(5);

        if (target == null || !SHULKER_BOXES.contains(target.getType())) {
            player.sendMessage(Component.text(
                "✗ Look directly at a Shulker Box (within 5 blocks).", NamedTextColor.RED));
            return;
        }

        if (plugin.isShopBlock(target.getLocation())) {
            player.sendMessage(Component.text(
                "⚠ This Shulker Box is already registered.", NamedTextColor.YELLOW));
            return;
        }

        plugin.addShopBlock(target.getLocation());

        player.sendMessage(
            Component.text("✔ Shulker Box at ", NamedTextColor.GREEN)
                .append(Component.text(
                    target.getX() + ", " + target.getY() + ", " + target.getZ(),
                    NamedTextColor.AQUA))
                .append(Component.text(" registered as Shard Shop!", NamedTextColor.GREEN))
        );
        player.sendMessage(Component.text(
            "Hologram spawned. Players can right-click to open the shop.",
            NamedTextColor.GRAY));
    }

    // ── /shopblock remove ─────────────────────────────────────────────────────

    private void handleRemove(Player player) {
        Block target = player.getTargetBlockExact(5);

        if (target == null || !SHULKER_BOXES.contains(target.getType())) {
            player.sendMessage(Component.text(
                "✗ Look directly at a registered Shulker Box (within 5 blocks).", NamedTextColor.RED));
            return;
        }

        if (!plugin.isShopBlock(target.getLocation())) {
            player.sendMessage(Component.text(
                "⚠ This Shulker Box is not registered.", NamedTextColor.YELLOW));
            return;
        }

        plugin.removeShopBlock(target.getLocation());

        player.sendMessage(
            Component.text("✔ Shulker Box at ", NamedTextColor.GREEN)
                .append(Component.text(
                    target.getX() + ", " + target.getY() + ", " + target.getZ(),
                    NamedTextColor.AQUA))
                .append(Component.text(" unregistered. Hologram removed.", NamedTextColor.GREEN))
        );
    }

    // ── /shopblock list ───────────────────────────────────────────────────────

    private void listBlocks(CommandSender sender) {
        if (plugin.getShopLocations().isEmpty()) {
            sender.sendMessage(Component.text(
                "⚠ No Shulker Box shop blocks registered.", NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text("── Registered Shop Blocks ──", NamedTextColor.GOLD));
        int i = 1;
        for (org.bukkit.Location loc : plugin.getShopLocations()) {
            sender.sendMessage(
                Component.text("  " + i++ + ". ", NamedTextColor.GRAY)
                    .append(Component.text(loc.getWorld().getName(), NamedTextColor.AQUA))
                    .append(Component.text(
                        " @ " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ(),
                        NamedTextColor.WHITE))
            );
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(Component.text("── ShardShopBlock ──", NamedTextColor.GOLD));
        player.sendMessage(Component.text("  /shopblock set    ", NamedTextColor.AQUA)
            .append(Component.text("Look at Shulker Box → register", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /shopblock remove ", NamedTextColor.AQUA)
            .append(Component.text("Look at Shulker Box → unregister + delete hologram", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("  /shopblock list   ", NamedTextColor.AQUA)
            .append(Component.text("List all registered blocks", NamedTextColor.GRAY)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("set", "remove", "list");
        return List.of();
    }
}
