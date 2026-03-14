package dev.shardshopblock;

import org.bukkit.Location;
import org.bukkit.Particle;

public class ParticleTask implements Runnable {

    private final ShardShopBlock plugin;
    private double angle = 0;

    public ParticleTask(ShardShopBlock plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Location loc : plugin.getShopLocations()) {
            if (loc.getWorld() == null) continue;

            // Spawn END_ROD particles in a rotating ring above the block
            double radius = 0.6;
            double y = loc.getBlockY() + 1.6;

            for (int i = 0; i < 4; i++) {
                double a = angle + (Math.PI / 2.0 * i);
                double x = loc.getBlockX() + 0.5 + Math.cos(a) * radius;
                double z = loc.getBlockZ() + 0.5 + Math.sin(a) * radius;
                loc.getWorld().spawnParticle(
                    Particle.END_ROD,
                    x, y, z,
                    1,       // count
                    0, 0, 0, // offset
                    0,       // extra (speed)
                    null,
                    false
                );
            }
        }
        angle += 0.15;
        if (angle > Math.PI * 2) angle = 0;
    }
}
