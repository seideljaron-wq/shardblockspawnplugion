package dev.shardshopblock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.JavaPlugin;
import org.bukkit.plugin.Plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.*;

public class ShardShopBlock extends JavaPlugin {

    private static ShardShopBlock instance;

    private final Set<Location>       shopLocations  = new HashSet<>();
    // key = "world,x,y,z"  →  list of ArmorStand UUIDs (one per hologram line)
    private final Map<String, List<UUID>> hologramMap = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadLocations();

        // Spawn holograms 1 tick after enable so worlds are fully loaded
        Bukkit.getScheduler().runTaskLater(this, this::spawnAllHolograms, 1L);

        getServer().getPluginManager().registerEvents(new ShopBlockListener(this), this);
        ShopBlockCommand cmd = new ShopBlockCommand(this);
        getCommand("shopblock").setExecutor(cmd);
        getCommand("shopblock").setTabCompleter(cmd);

        if (getConfig().getBoolean("show-particles", true)) {
            Bukkit.getScheduler().runTaskTimer(this, new ParticleTask(this), 10L, 10L);
        }

        getLogger().info("ShardShopBlock enabled! " + shopLocations.size() + " shop block(s) loaded.");
    }

    @Override
    public void onDisable() {
        removeAllHolograms();
        getLogger().info("ShardShopBlock disabled.");
    }

    public static ShardShopBlock getInstance() { return instance; }

    // ── ShardSystem admin check ───────────────────────────────────────────────
    // Reads the ShardSystem config directly – no hard dependency needed

    public boolean isShardAdmin(UUID uuid) {
        Plugin ss = Bukkit.getPluginManager().getPlugin("ShardSystem");
        if (ss instanceof JavaPlugin shardSystem) {
            List<String> admins = shardSystem.getConfig().getStringList("admins");
            for (String s : admins) {
                try { if (UUID.fromString(s).equals(uuid)) return true; }
                catch (IllegalArgumentException ignored) {}
            }
        }
        return false;
    }

    // ── Location management ───────────────────────────────────────────────────

    public Set<Location> getShopLocations() { return shopLocations; }

    public boolean isShopBlock(Location loc) {
        Location b = loc.getBlock().getLocation();
        for (Location l : shopLocations) {
            if (l.getWorld().equals(b.getWorld())
                    && l.getBlockX() == b.getBlockX()
                    && l.getBlockY() == b.getBlockY()
                    && l.getBlockZ() == b.getBlockZ()) return true;
        }
        return false;
    }

    public boolean addShopBlock(Location loc) {
        Location block = loc.getBlock().getLocation();
        if (isShopBlock(block)) return false;
        shopLocations.add(block);
        saveLocations();
        spawnHologram(block);
        return true;
    }

    public boolean removeShopBlock(Location loc) {
        Location block = loc.getBlock().getLocation();
        Location found = null;
        for (Location l : shopLocations) {
            if (l.getWorld().equals(block.getWorld())
                    && l.getBlockX() == block.getBlockX()
                    && l.getBlockY() == block.getBlockY()
                    && l.getBlockZ() == block.getBlockZ()) { found = l; break; }
        }
        if (found == null) return false;
        shopLocations.remove(found);
        removeHologram(locationKey(found));
        saveLocations();
        return true;
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private void loadLocations() {
        shopLocations.clear();
        for (String s : getConfig().getStringList("shop-blocks")) {
            Location loc = parseLocation(s);
            if (loc != null) shopLocations.add(loc);
        }
    }

    private void saveLocations() {
        List<String> raw = new ArrayList<>();
        shopLocations.forEach(l -> raw.add(serializeLocation(l)));
        getConfig().set("shop-blocks", raw);
        saveConfig();
    }

    public String serializeLocation(Location loc) {
        return loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
    }

    private Location parseLocation(String s) {
        try {
            String[] p = s.split(",");
            World w = Bukkit.getWorld(p[0]);
            if (w == null) return null;
            return new Location(w, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
        } catch (Exception e) {
            getLogger().warning("Could not parse location: " + s);
            return null;
        }
    }

    private String locationKey(Location loc) { return serializeLocation(loc); }

    // ── Hologram (multi-line ArmorStand stack) ────────────────────────────────
    //
    // Layout (top → bottom, each line = one invisible ArmorStand):
    //
    //   Line 0  │  ✦ SHARD SHOP ✦          (bold, light purple)
    //   Line 1  │  RUNEMC.ORG              (italic, dark purple / gray)
    //   Line 2  │  (empty gap)
    //   Line 3  │  ▶ Right Click to Open!  (yellow arrow + white text)
    //
    // ArmorStands are stacked 0.28 blocks apart (vanilla hologram spacing).

    private static final double LINE_SPACING = 0.28;
    // Y offset of the TOP line above the block surface (block top = +1.0)
    private static final double TOP_Y_OFFSET = 2.55;

    private void spawnAllHolograms() {
        shopLocations.forEach(this::spawnHologram);
    }

    public void spawnHologram(Location blockLoc) {
        String key = locationKey(blockLoc);
        removeHologram(key);

        List<Component> lines = buildHologramLines();
        List<UUID> ids = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            double yOffset = TOP_Y_OFFSET - (i * LINE_SPACING);
            Location standLoc = blockLoc.clone().add(0.5, yOffset, 0.5);

            ArmorStand stand = (ArmorStand) standLoc.getWorld().spawnEntity(standLoc, EntityType.ARMOR_STAND);
            stand.setInvisible(true);
            stand.setSmall(true);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setInvulnerable(true);
            stand.setPersistent(true);
            stand.setCustomNameVisible(true);
            stand.customName(lines.get(i));

            ids.add(stand.getUniqueId());
        }

        hologramMap.put(key, ids);
    }

    /**
     * Builds the hologram lines to match the style in the screenshot:
     *
     *   ✦ SHARD SHOP ✦          → bold light_purple  (like "TOOLS CRATE")
     *   RANDOMIZED ITEMS style  → we use "RuneMC.org" small gray italic
     *   (gap line – empty)
     *   ▶ Right Click to Open!  → yellow ▶ + white text  (like in screenshot)
     */
    private List<Component> buildHologramLines() {
        List<Component> lines = new ArrayList<>();

        // Line 0 – main title: bold light purple  "✦ SHARD SHOP ✦"
        lines.add(
            Component.text("✦ SHARD SHOP ✦", NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.BOLD)
        );

        // Line 1 – subtitle: italic dark purple  "RUNEMC.ORG"
        lines.add(
            Component.text("RUNEMC.ORG", NamedTextColor.DARK_PURPLE)
                .decorate(TextDecoration.ITALIC)
        );

        // Line 2 – empty gap (space so it looks like the screenshot)
        lines.add(Component.empty());

        // Line 3 – action hint: yellow ▶ + white "Right Click to Open!"
        lines.add(
            Component.text("▶ ", NamedTextColor.YELLOW)
                .append(Component.text("Right Click ", NamedTextColor.WHITE))
                .append(Component.text("to Open!", NamedTextColor.YELLOW))
        );

        return lines;
    }

    public void removeHologram(String key) {
        List<UUID> ids = hologramMap.remove(key);
        if (ids == null) return;
        for (UUID uid : ids) {
            Entity e = Bukkit.getEntity(uid);
            if (e != null) e.remove();
        }
    }

    private void removeAllHolograms() {
        hologramMap.values().forEach(ids ->
            ids.forEach(uid -> { Entity e = Bukkit.getEntity(uid); if (e != null) e.remove(); })
        );
        hologramMap.clear();
    }
}
