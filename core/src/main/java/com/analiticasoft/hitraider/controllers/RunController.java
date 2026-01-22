package com.analiticasoft.hitraider.controllers;

import com.analiticasoft.hitraider.combat.*;
import com.analiticasoft.hitraider.config.PhysicsTuning;
import com.analiticasoft.hitraider.entities.MeleeEnemy;
import com.analiticasoft.hitraider.entities.Player;
import com.analiticasoft.hitraider.entities.RangedEnemy;
import com.analiticasoft.hitraider.physics.GameContactListener;
import com.analiticasoft.hitraider.physics.PhysicsConstants;
import com.analiticasoft.hitraider.physics.PhysicsWorld;
import com.analiticasoft.hitraider.render.CharacterAnimator;
import com.analiticasoft.hitraider.render.DebugAnimLibrary;
import com.analiticasoft.hitraider.relics.*;
import com.analiticasoft.hitraider.world.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import java.util.Random;

/**
 * Encapsula TODA la lógica de run/rooms/spawns/drops/choice.
 * GameplayScreen solo orquesta.
 */
public class RunController {

    // External dependencies (provided by GameplayScreen)
    public PhysicsWorld physics;
    public CombatSystem combat;
    public ProjectileSystem projectiles;
    public GameContactListener contactListener;

    public Array<LevelFactory.PlatformRect> platformRects;

    public Player player;

    public final Array<MeleeEnemy> meleeEnemies = new Array<>();
    public final Array<RangedEnemy> rangedEnemies = new Array<>();
    public final Array<CharacterAnimator> meleeAnims = new Array<>();
    public final Array<CharacterAnimator> rangedAnims = new Array<>();

    public final EncounterManager encounter = new EncounterManager();

    public final RelicManager relics = new RelicManager();
    public final Array<RelicPickup> pickups = new Array<>();

    // Run system
    public final RunManager run = new RunManager();
    public final RoomTemplateRegistry templates = new RoomTemplateRegistry();
    public final RoomInstanceGenerator generator = new RoomInstanceGenerator();
    public final DropRules dropRules = new DropRules();
    public Array<RoomInstance> runRooms;

    public boolean relicDroppedThisRoom = false;
    public boolean inChoiceRoom = false;

    public float shootCooldown = 0f;

    private final Random rng = new Random();

    public void buildTemplates() {
        templates.clear();

        RoomTemplate arena = new RoomTemplate("arena", 120f, 140f, 1120f)
            .addSpawn(520f, 220f).addSpawn(650f, 220f).addSpawn(820f, 220f).addSpawn(950f, 220f).addSpawn(1040f, 220f);

        RoomTemplate platforms = new RoomTemplate("platforms", 120f, 140f, 1120f)
            .addSpawn(520f, 260f).addSpawn(650f, 260f).addSpawn(820f, 220f).addSpawn(950f, 220f).addSpawn(1040f, 220f);

        RoomTemplate hall = new RoomTemplate("hall", 120f, 140f, 1120f)
            .addSpawn(520f, 220f).addSpawn(650f, 220f).addSpawn(760f, 220f).addSpawn(880f, 220f).addSpawn(1000f, 220f);

        templates.add(arena);
        templates.add(platforms);
        templates.add(hall);
    }

    public void startNewRun(boolean rebuildPhysics) {
        long seed = System.currentTimeMillis();
        int totalRooms = 12;

        runRooms = generator.generate(seed, totalRooms, templates);
        run.start(seed, totalRooms, runRooms);

        loadCurrentRoom(rebuildPhysics);
    }

    public void buildPhysicsIfNeeded(boolean rebuildPhysics) {
        if (!rebuildPhysics) return;

        // --- HARD RESET of old world (prevents native crashes) ---
        if (physics != null) {
            try {
                // 1) Detach listener first
                physics.world.setContactListener(null);

                // 2) Destroy ALL bodies in old world safely (world must NOT be locked here)
                com.badlogic.gdx.utils.Array<com.badlogic.gdx.physics.box2d.Body> bodies = new com.badlogic.gdx.utils.Array<>();
                physics.world.getBodies(bodies);
                for (int i = bodies.size - 1; i >= 0; i--) {
                    com.badlogic.gdx.physics.box2d.Body b = bodies.get(i);
                    if (b != null && b.getWorld() != null) {
                        physics.world.destroyBody(b);
                    }
                }
            } catch (Exception ignored) {
                // if something goes wrong, we still dispose below
            } finally {
                // 3) Dispose old world
                physics.dispose();
                physics = null;
            }
        }

        // Also drop old references (important)
        combat = null;
        projectiles = null;
        contactListener = null;

        meleeEnemies.clear();
        rangedEnemies.clear();
        meleeAnims.clear();
        rangedAnims.clear();
        pickups.clear();

        // 4) Build new world + systems
        physics = new PhysicsWorld(new Vector2(0f, PhysicsTuning.GRAVITY_Y));
        combat = new CombatSystem(physics.world);
        projectiles = new ProjectileSystem(physics.world);
        contactListener = new GameContactListener(combat, projectiles);
        physics.world.setContactListener(contactListener);

        platformRects = LevelFactory.createTestLevel(physics.world);
        player = new Player(physics.world, 120f, 140f);
    }

    public void loadCurrentRoom(boolean rebuildPhysics) {
        buildPhysicsIfNeeded(rebuildPhysics);
        destroyTransientBodies();

        meleeEnemies.clear();
        rangedEnemies.clear();
        meleeAnims.clear();
        rangedAnims.clear();
        pickups.clear();

        relicDroppedThisRoom = false;
        inChoiceRoom = false;
        shootCooldown = 0f;

        RoomInstance room = run.current();

        player.body.setTransform(PhysicsConstants.toMeters(room.template.entryXpx), PhysicsConstants.toMeters(room.template.entryYpx), 0f);
        player.body.setLinearVelocity(0f, 0f);

        if (room.type == RoomType.CHOICE) {
            inChoiceRoom = true;
            spawnChoiceRelics(room);
        } else {
            spawnEnemies(room);
        }

        encounter.reset();
    }

    private void spawnEnemies(RoomInstance room) {
        int si = 0;

        for (int i = 0; i < room.meleeCount; i++) {
            Vector2 sp = room.spawnOrder.get(si++ % room.spawnOrder.size);
            meleeEnemies.add(new MeleeEnemy(physics.world, sp.x, sp.y));
            CharacterAnimator a = new CharacterAnimator();
            DebugAnimLibrary.defineMeleeEnemy(a);
            meleeAnims.add(a);
        }

        for (int i = 0; i < room.rangedCount; i++) {
            Vector2 sp = room.spawnOrder.get(si++ % room.spawnOrder.size);
            rangedEnemies.add(new RangedEnemy(physics.world, sp.x, sp.y));
            CharacterAnimator a = new CharacterAnimator();
            DebugAnimLibrary.defineMeleeEnemy(a);
            rangedAnims.add(a);
        }
    }

    private void spawnChoiceRelics(RoomInstance room) {
        Random rr = new Random(room.seed ^ 0x1234ABCD);
        RelicType a = dropRules.rollRelic(rr);
        RelicType b = dropRules.rollRelic(rr);
        if (a == b) {
            b = (a == RelicType.BONUS_PROJECTILE_DAMAGE) ? RelicType.FIRE_RATE_UP : RelicType.BONUS_PROJECTILE_DAMAGE;
        }

        pickups.add(new RelicPickup(physics.world, a, 520f, 220f));
        pickups.add(new RelicPickup(physics.world, b, 820f, 220f));
    }

    public void onEnemyKilledDrop(MeleeEnemy e, int idx) {
        RoomInstance room = run.current();
        if (inChoiceRoom) return;
        if (relicDroppedThisRoom) return;
        if (rng.nextFloat() >= room.relicDropChance) return;

        RelicType t = dropRules.rollRelic(new Random(room.seed ^ (long) idx * 1315423911L));
        pickups.add(new RelicPickup(physics.world, t, e.getXpx(), e.getYpx()));
        relicDroppedThisRoom = true;
    }

    public void processPickupsChoiceAware() {
        for (int i = pickups.size - 1; i >= 0; i--) {
            RelicPickup p = pickups.get(i);
            if (!p.collected) continue;

            if (inChoiceRoom) {
                // remove all pickup bodies, keep chosen type
                RelicType chosen = p.type;
                for (int k = pickups.size - 1; k >= 0; k--) {
                    RelicPickup other = pickups.get(k);
                    if (other.body.getWorld() != null) physics.world.destroyBody(other.body);
                    pickups.removeIndex(k);
                }
                relics.add(chosen);
                inChoiceRoom = false;
                return;
            }

            relics.add(p.type);
            physics.world.destroyBody(p.body);
            pickups.removeIndex(i);
        }
    }

    public boolean canExit() {
        if (run.current().type == RoomType.CHOICE) return !inChoiceRoom;
        return encounter.getState() == EncounterManager.State.CLEAR;
    }

    public void destroyTransientBodies() {
        if (physics == null) return;

        // Destroy melee enemies
        for (int i = meleeEnemies.size - 1; i >= 0; i--) {
            MeleeEnemy e = meleeEnemies.get(i);
            if (e != null && e.body != null && e.body.getWorld() != null) {
                combat.purgeForBody(e.body);
                physics.world.destroyBody(e.body);
            }
        }

        // Destroy ranged enemies
        for (int i = rangedEnemies.size - 1; i >= 0; i--) {
            RangedEnemy e = rangedEnemies.get(i);
            if (e != null && e.body != null && e.body.getWorld() != null) {
                physics.world.destroyBody(e.body);
            }
        }

        // Destroy pickups
        for (int i = pickups.size - 1; i >= 0; i--) {
            RelicPickup p = pickups.get(i);
            if (p != null && p.body != null && p.body.getWorld() != null) {
                physics.world.destroyBody(p.body);
            }
        }

        // Destroy projectiles (alive ones still have bodies)
        if (projectiles != null) {
            for (int i = projectiles.projectiles.size - 1; i >= 0; i--) {
                Projectile pr = projectiles.projectiles.get(i);
                if (pr != null && pr.state == Projectile.State.ALIVE && pr.body != null && pr.body.getWorld() != null) {
                    physics.world.destroyBody(pr.body);
                }
            }
            projectiles.projectiles.clear();
        }

        // Clear combat hitboxes list (safe)
        // purgeForBody already removes per-body hitboxes; but if any remain:
        // easiest: rebuild CombatSystem on full reset; for room swap, this is usually enough.
    }
}
