package me.enhancecombat.listeners;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
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
    private final Map<UUID, Integer> comboCount = new HashMap<>();
    private final Map<UUID, Long> parryWindow = new HashMap<>();
    private final Map<UUID, UUID> lastDamager = new HashMap<>();
    private final Set<UUID> parrySuccess = new HashSet<>();

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        if (!(event.getEntity() instanceof LivingEntity)) return;

        Player attacker = (Player) event.getDamager();
        LivingEntity target = (LivingEntity) event.getEntity();

        long now = System.currentTimeMillis();

        if (parrySuccess.contains(target.getUniqueId())) {
            event.setCancelled(true);
            parrySuccess.remove(target.getUniqueId());
            return;
        }

        if (lastAttackTime.containsKey(attacker.getUniqueId())) {
            long last = lastAttackTime.get(attacker.getUniqueId());
            if (now - last < 700) {
                event.setCancelled(true);
                return;
            }
        }

        // Combo tracking
        int currentCombo = comboCount.getOrDefault(attacker.getUniqueId(), 0);
        long lastHit = lastAttackTime.getOrDefault(attacker.getUniqueId(), 0L);

        if (now - lastHit <= 800) {
            currentCombo++;
        } else {
            currentCombo = 1;
        }

        if (currentCombo >= 3) {
            stun(target, 2);
            attacker.sendMessage("§aCombo Stun!");
            currentCombo = 0;
        }

        comboCount.put(attacker.getUniqueId(), currentCombo);
        lastAttackTime.put(attacker.getUniqueId(), now);

        attacker.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent("§eCombo: " + currentCombo + "/3"));

        if (target instanceof Player) {
            Player victim = (Player) target;
            parryWindow.put(victim.getUniqueId(), now);
            lastDamager.put(victim.getUniqueId(), attacker.getUniqueId());
            victim.spawnParticle(Particle.CRIT, victim.getLocation().add(0, 1.5, 0), 10);
            victim.playSound(victim.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
            victim.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent("§b>> Parry Window Active <<"));
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
                player.spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 20);
                player.spawnParticle(Particle.FLASH, player.getLocation().add(0, 1.2, 0), 10);
                player.playSound(player.getLocation(), "custom.parry_success", 1.0f, 1.0f);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent("§aParry Sukses!"));

                UUID attackerId = lastDamager.get(player.getUniqueId());
                if (attackerId != null) {
                    Entity attacker = Bukkit.getEntity(attackerId);
                    if (attacker instanceof LivingEntity) {
                        stun((LivingEntity) attacker, 5);
                    }
                }

                parrySuccess.add(player.getUniqueId());
                parryWindow.remove(player.getUniqueId());
                lastDamager.remove(player.getUniqueId());
            } else {
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent("§cParry Gagal!"));
            }
        }
    }

    private void stun(LivingEntity target, int seconds) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, seconds * 20, 255));
        target.getWorld().spawnParticle(Particle.SPELL_WITCH, target.getLocation().add(0, 1, 0), 20);
        if (target instanceof Player) {
            ((Player) target).playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0f, 1.0f);
            ((Player) target).spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent("§cYou are STUNNED!"));
        }
    }
}