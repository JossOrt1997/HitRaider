package com.analiticasoft.hitraider.entities;

import com.analiticasoft.hitraider.combat.Damageable;
import com.analiticasoft.hitraider.combat.Faction;
import com.analiticasoft.hitraider.combat.HealthComponent;
import com.analiticasoft.hitraider.physics.CollisionBits;
import com.analiticasoft.hitraider.physics.PhysicsConstants;
import com.analiticasoft.hitraider.world.MeleeEnemyProfile;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.analiticasoft.hitraider.combat.EnemyProfiles;

public class MeleeEnemy implements Damageable {

    public enum State { IDLE, CHASE, TELEGRAPH, ATTACK, COOLDOWN, STAGGER, DEAD }

    private EnemyProfiles.MeleeAIProfile profile;
    public final Body body;
    private final HealthComponent health = new HealthComponent(4);

    private State state = State.IDLE;

    private float telegraphTimer = 0f;
    private float attackTimer = 0f;
    private float cooldownTimer = 0f;

    private boolean attackStartedThisFrame = false;
    private int facingDir = 1;

    // AI tuning
    private static final float CHASE_SPEED = 1.8f;
    private static final float AGGRO_RANGE_PX = 240f;
    private static final float ATTACK_RANGE_PX = 48f;

    private static final float TELEGRAPH_TIME = 0.18f;
    private static final float ATTACK_TIME = 0.06f;
    private static final float COOLDOWN_TIME = 0.35f;

    private static final int DAMAGE = 1;

    private static final float INVULN_TIME = 0.12f;
    private static final float FLASH_TIME = 0.08f;
    private static final float STUN_TIME = 0.10f;

    public MeleeEnemy(World world, float xPx, float yPx) {
        this(world, xPx, yPx, true);
    }

    public MeleeEnemy(World world, float xPx, float yPx, boolean facingRight) {
        this.facingDir = facingRight ? 1 : -1;
        this.profile = EnemyProfiles.ELDER_BLADE;

        float x = PhysicsConstants.toMeters(xPx);
        float y = PhysicsConstants.toMeters(yPx);

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        bd.position.set(x, y);
        bd.fixedRotation = true;

        body = world.createBody(bd);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(PhysicsConstants.toMeters(10f), PhysicsConstants.toMeters(14f));

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = 1.0f;
        fd.friction = 0.4f;
        fd.restitution = 0.0f;

        // ✅ Enemy collisions
        fd.filter.categoryBits = CollisionBits.ENEMY;
        fd.filter.maskBits = CollisionBits.MASK_ENEMY_BODY;

        Fixture fx = body.createFixture(fd);
        fx.setUserData(this);

        shape.dispose();
    }

    public MeleeEnemy(World world, float xPx, float yPx, MeleeEnemyProfile profile) {
        this(world, xPx, yPx, profile.facingRight);
    }

    public MeleeEnemy(World world, float xPx, float yPx, EnemyProfiles.MeleeAIProfile profile) {
        this(world, xPx, yPx, true);
        this.profile = profile;
    }

    public void update(float delta, Player player) {
        attackStartedThisFrame = false;

        health.update(delta);
        if (!health.isAlive()) {
            state = State.DEAD;
            Vector2 v = body.getLinearVelocity();
            body.setLinearVelocity(0f, v.y);
            return;
        }

        if (health.isStunned()) {
            state = State.STAGGER;
            Vector2 v = body.getLinearVelocity();
            body.setLinearVelocity(0f, v.y);
            return;
        } else if (state == State.STAGGER) {
            state = State.CHASE;
        }

        float dxPx = player.getXpx() - getXpx();
        float distPx = Math.abs(dxPx);

        if (dxPx < -1f) facingDir = -1;
        else if (dxPx > 1f) facingDir = 1;

        switch (state) {
            case IDLE -> {
                body.setLinearVelocity(0f, body.getLinearVelocity().y);
                if (distPx <= AGGRO_RANGE_PX) state = State.CHASE;
            }
            case CHASE -> {
                if (distPx > AGGRO_RANGE_PX) { state = State.IDLE; break; }
                if (distPx <= ATTACK_RANGE_PX) {
                    state = State.TELEGRAPH;
                    telegraphTimer = TELEGRAPH_TIME;
                    body.setLinearVelocity(0f, body.getLinearVelocity().y);
                    break;
                }
                Vector2 v = body.getLinearVelocity();
                body.setLinearVelocity(facingDir * CHASE_SPEED, v.y);
            }
            case TELEGRAPH -> {
                telegraphTimer -= delta;
                body.setLinearVelocity(0f, body.getLinearVelocity().y);
                if (telegraphTimer <= 0f) {
                    state = State.ATTACK;
                    attackTimer = ATTACK_TIME;
                    attackStartedThisFrame = true;
                }
            }
            case ATTACK -> {
                attackTimer -= delta;
                body.setLinearVelocity(0f, body.getLinearVelocity().y);
                if (attackTimer <= 0f) {
                    state = State.COOLDOWN;
                    cooldownTimer = COOLDOWN_TIME;
                }
            }
            case COOLDOWN -> {
                cooldownTimer -= delta;
                body.setLinearVelocity(0f, body.getLinearVelocity().y);
                if (cooldownTimer <= 0f) state = State.CHASE;
            }
            case STAGGER, DEAD -> {}
        }
    }

    public boolean didStartAttackThisFrame() { return attackStartedThisFrame; }
    public int getFacingDir() { return facingDir; }
    public int getDamage() { return DAMAGE; }
    public State getState() { return state; }

    public float getTelegraphAlpha() {
        if (state != State.TELEGRAPH) return 0f;
        float t = Math.max(0f, telegraphTimer) / TELEGRAPH_TIME;
        return 1f - t;
    }

    public float getXpx() { return PhysicsConstants.toPixels(body.getPosition().x); }
    public float getYpx() { return PhysicsConstants.toPixels(body.getPosition().y); }

    @Override public Faction getFaction() { return Faction.ENEMY; }
    @Override public boolean isAlive() { return health.isAlive(); }
    @Override public HealthComponent getHealth() { return health; }

    @Override
    public void applyDamage(int amount, Vector2 knockback) {
        boolean applied = health.tryDamage(amount, INVULN_TIME, FLASH_TIME, STUN_TIME);
        if (!applied) return;

        Vector2 v = body.getLinearVelocity();
        body.setLinearVelocity(v.x + knockback.x, v.y + knockback.y);
    }

    public boolean isFlashing() { return health.isFlashing(); }
}
