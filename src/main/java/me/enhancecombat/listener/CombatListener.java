package me.enhancecombat.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class CombatListener implements Listener {

    private final Map<UUID, Long> lastAttackTime = new HashMap<>();
    private final Map<UUID, Integer> comboLevel = new HashMap<>();
    private final Map<UUID, Long> parryWindow = new HashMap<>();

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player attacker = (Player) event.getDamager();

        long now = System.currentTimeMillis();
        if (lastAttackTime.containsKey(attacker.getUniqueId())) {
            long last = lastAttackTime.get(attacker.getUniqueId());
            if (now - last < 700) {
                event.setCancelled(true); // Delay serangan
                return;
            }
        }
        lastAttackTime.put(attacker.getUniqueId(), now);

        Entity target = event.getEntity();
        if (target instanceof Player) {
            Player victim = (Player) target;
            parryWindow.put(victim.getUniqueId(), now);
            victim.spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1.5, 0), 10);
            victim.playSound(victim.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        }

        // Combo tracking
        int level = comboLevel.getOrDefault(attacker.getUniqueId(), 0);
        if (now - lastAttackTime.getOrDefault(attacker.getUniqueId(), 0L) <= 500) {
            level = Math.min(level + 1, 3);
        } else {
            level = 1;
        }
        comboLevel.put(attacker.getUniqueId(), level);

        // If combo level 3, stun target
        if (level == 3 && target instanceof LivingEntity) {
            stun((LivingEntity) target, 2);
            attacker.sendMessage("§aStunned target!");
        }
    }

    @EventHandler
    public void onParry(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        long now = System.currentTimeMillis();

        if (parryWindow.containsKey(player.getUniqueId())) {
            long hitTime = parryWindow.get(player.getUniqueId());
            if (now - hitTime <= 200) {
                // Parry berhasil
                player.spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 20);
                player.spawnParticle(Particle.FLASH, player.getLocation().add(0, 1.2, 0), 10);
                player.playSound(player.getLocation(), "custom.parry_success", 1.0f, 1.0f);
                Entity attacker = getLastDamager(player);
                if (attacker instanceof LivingEntity) {
                    stun((LivingEntity) attacker, 5);
                }
                parryWindow.remove(player.getUniqueId());
            }
        }
    }

    private Entity getLastDamager(Player player) {
        // Simplified version; in real plugin use metadata/tracking
        return null;
    }

    private void stun(LivingEntity target, int seconds) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, seconds * 20, 255));
        target.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, seconds * 20, 250));
        target.getWorld().spawnParticle(Particle.SPELL_WITCH, target.getLocation().add(0, 1, 0), 20);
        if (target instanceof Player) {
            ((Player) target).playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 1.0f);
        }
    }
}