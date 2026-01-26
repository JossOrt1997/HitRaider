package com.analiticasoft.hitraider.combat.weapons;

import com.analiticasoft.hitraider.combat.CombatSystem;
import com.analiticasoft.hitraider.combat.Faction;
import com.analiticasoft.hitraider.combat.Projectile;
import com.analiticasoft.hitraider.combat.ProjectileSystem;
import com.analiticasoft.hitraider.entities.Player;
import com.analiticasoft.hitraider.relics.RelicManager;
import com.badlogic.gdx.physics.box2d.World;

/**
 * WeaponRuntime:
 * - Bridges WeaponDefinition -> real systems (CombatSystem/ProjectileSystem)
 * - Applies relic bonuses consistently
 * - GameplayScreen should NOT implement weapon behavior; it calls WeaponRuntime
 */
public final class WeaponRuntime {

    private final World world;
    private final CombatSystem combat;
    private final ProjectileSystem projectiles;
    private final RelicManager relics;

    public WeaponRuntime(World world, CombatSystem combat, ProjectileSystem projectiles, RelicManager relics) {
        this.world = world;
        this.combat = combat;
        this.projectiles = projectiles;
        this.relics = relics;
    }

    /** Execute weapon primary action (melee or ranged). */
    public void usePrimary(Player player, WeaponType type, int aimY) {
        WeaponDefinition def = WeaponRegistry.get(type);
        if (def == null || player == null) return;

        if (def.melee) {
            int dmg = def.baseDamage;
            combat.spawnMeleeHitbox(
                player.body,
                player,
                player.getFaction(),
                player.getFacingDir(),
                aimY,
                dmg
            );
            return;
        }

        // Ranged: Bolter
        int dmg = def.baseDamage + relics.getBonusProjectileDamage();

        float sx = player.getXpx() + player.getFacingDir() * 14f;
        float sy = player.getYpx() + 10f;

        Projectile p = new Projectile(
            world,
            Faction.PLAYER,
            dmg,
            sx, sy,
            player.getFacingDir() * def.projectileSpeedMps,
            0f,
            def.projectileLifetimeSec
        );

        p.piercesLeft = def.basePierce + relics.getPiercingShots();

        projectiles.spawn(p);
    }

    /** Cooldown including relic modifiers (Phase A: fire rate affects ranged). */
    public float cooldownFor(WeaponType type) {
        WeaponDefinition def = WeaponRegistry.get(type);
        if (def == null) return 0.25f;

        float base = def.cooldownSec;
        if (!def.melee) base = base * relics.getFireRateMultiplier();
        return base;
    }
}
