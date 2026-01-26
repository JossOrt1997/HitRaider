package com.analiticasoft.hitraider.controllers;

import com.analiticasoft.hitraider.combat.CombatSystem;
import com.analiticasoft.hitraider.combat.Projectile;
import com.analiticasoft.hitraider.combat.ProjectileSystem;
import com.analiticasoft.hitraider.config.PhysicsTuning;
import com.analiticasoft.hitraider.entities.MeleeEnemy;
import com.analiticasoft.hitraider.entities.Player;
import com.analiticasoft.hitraider.entities.RangedEnemy;
import com.analiticasoft.hitraider.physics.GameContactListener;
import com.analiticasoft.hitraider.physics.PhysicsConstants;
import com.analiticasoft.hitraider.physics.PhysicsDestroyQueue;
import com.analiticasoft.hitraider.physics.PhysicsWorld;
import com.analiticasoft.hitraider.render.CharacterAnimator;
import com.analiticasoft.hitraider.render.DebugAnimLibrary;
import com.analiticasoft.hitraider.relics.RelicManager;
import com.analiticasoft.hitraider.relics.RelicPickup;
import com.analiticasoft.hitraider.relics.RelicType;
import com.analiticasoft.hitraider.world.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;

import java.util.Random;

public class RunController {

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

    public final RunManager run = new RunManager();
    public final RoomTemplateRegistry templates = new RoomTemplateRegistry();
    public final RoomInstanceGenerator generator = new RoomInstanceGenerator();
    public final DropRules dropRules = new DropRules();
    public Array<RoomInstance> runRooms;

    public boolean relicDroppedThisRoom = false;
    public boolean inChoiceRoom = false;

    public float shootCooldown = 0f;

    private final Random rng = new Random();

    // Fortification
    private PhysicsDestroyQueue destroyQueue;

    public void setDestroyQueue(PhysicsDestroyQueue q) {
        this.destroyQueue = q;
        if (projectiles != null) projectiles.setDestroyQueue(q);
    }

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

    /**
     * HARD RESET allowed: destroy bodies directly ONLY here.
     */
    public void buildPhysicsIfNeeded(boolean rebuildPhysics) {
        if (!rebuildPhysics) return;

        if (physics != null) {
            try {
                physics.world.setContactListener(null);
                Array<Body> bodies = new Array<>();
                physics.world.getBodies(bodies);
                for (int i = bodies.size - 1; i >= 0; i--) {
                    Body b = bodies.get(i);
                    if (b != null && b.getWorld() != null) physics.world.destroyBody(b);
                }
            } catch (Throwable ignored) {
            } finally {
                physics.dispose();
                physics = null;
            }
        }

        combat = null;
        projectiles = null;
        contactListener = null;

        meleeEnemies.clear();
        rangedEnemies.clear();
        meleeAnims.clear();
        rangedAnims.clear();
        pickups.clear();

        physics = new PhysicsWorld(new Vector2(0f, PhysicsTuning.GRAVITY_Y));
        combat = new CombatSystem(physics.world);
        projectiles = new ProjectileSystem(physics.world);

        if (destroyQueue != null) projectiles.setDestroyQueue(destroyQueue);

        contactListener = new GameContactListener(combat, projectiles);
        physics.world.setContactListener(contactListener);

        platformRects = LevelFactory.createTestLevel(physics.world);
        player = new Player(physics.world, 120f, 140f);
    }

    /**
     * Queue-destroy transient bodies from previous room (enemies, pickups, alive projectiles).
     * Never calls destroyBody directly here.
     */
    private void queueDestroyTransients() {
        if (destroyQueue == null || physics == null) return;

        for (int i = meleeEnemies.size - 1; i >= 0; i--) {
            MeleeEnemy e = meleeEnemies.get(i);
            if (e != null && e.body != null && e.body.getWorld() == physics.world) destroyQueue.queueBody(e.body);
        }
        for (int i = rangedEnemies.size - 1; i >= 0; i--) {
            RangedEnemy e = rangedEnemies.get(i);
            if (e != null && e.body != null && e.body.getWorld() == physics.world) destroyQueue.queueBody(e.body);
        }
        for (int i = pickups.size - 1; i >= 0; i--) {
            RelicPickup p = pickups.get(i);
            if (p != null && p.body != null && p.body.getWorld() == physics.world) destroyQueue.queueBody(p.body);
        }

        if (projectiles != null) {
            for (int i = projectiles.projectiles.size - 1; i >= 0; i--) {
                Projectile pr = projectiles.projectiles.get(i);
                if (pr != null && pr.state == Projectile.State.ALIVE && pr.body != null && pr.body.getWorld() == physics.world) {
                    destroyQueue.queueBody(pr.body);
                }
            }
            projectiles.projectiles.clear();
        }
    }

    public void loadCurrentRoom(boolean rebuildPhysics) {
        buildPhysicsIfNeeded(rebuildPhysics);

        // Clean previous transients safely
        queueDestroyTransients();
        if (destroyQueue != null && physics != null && !physics.world.isLocked()) {
            destroyQueue.flush(physics.world, combat);
        }

        meleeEnemies.clear();
        rangedEnemies.clear();
        meleeAnims.clear();
        rangedAnims.clear();
        pickups.clear();

        relicDroppedThisRoom = false;
        inChoiceRoom = false;
        shootCooldown = 0f;

        RoomInstance room = run.current();

        player.body.setTransform(
            PhysicsConstants.toMeters(room.template.entryXpx),
            PhysicsConstants.toMeters(room.template.entryYpx),
            0f
        );
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
        if (a == b) b = (a == RelicType.BONUS_PROJECTILE_DAMAGE) ? RelicType.FIRE_RATE_UP : RelicType.BONUS_PROJECTILE_DAMAGE;

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
                RelicType chosen = p.type;

                for (int k = pickups.size - 1; k >= 0; k--) {
                    RelicPickup other = pickups.get(k);
                    if (destroyQueue != null) destroyQueue.queueBody(other.body);
                    pickups.removeIndex(k);
                }

                relics.add(chosen);
                inChoiceRoom = false;
                return;
            }

            relics.add(p.type);
            if (destroyQueue != null) destroyQueue.queueBody(p.body);
            pickups.removeIndex(i);
        }
    }

    public boolean canExit() {
        if (run.current().type == RoomType.CHOICE) return !inChoiceRoom;
        return encounter.getState() == EncounterManager.State.CLEAR;
    }
}
