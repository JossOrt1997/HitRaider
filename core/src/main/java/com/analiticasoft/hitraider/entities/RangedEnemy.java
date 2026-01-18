package com.analiticasoft.hitraider.entities;

import com.analiticasoft.hitraider.combat.Damageable;
import com.analiticasoft.hitraider.combat.Faction;
import com.analiticasoft.hitraider.combat.HealthComponent;
import com.analiticasoft.hitraider.physics.PhysicsConstants;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class RangedEnemy implements Damageable {

    public enum State { IDLE, KITE, TELEGRAPH, SHOOT, COOLDOWN, STAGGER, DEAD }

    public final Body body;
    private final HealthComponent health = new HealthComponent(3);

    private State state = State.IDLE;

    private float telegraphTimer = 0f;
    private float cooldownTimer = 0f;

    private boolean shotThisFrame = false;
    private int facingDir = 1;

    // tuning
    private static final float AGGRO_RANGE_PX = 340f;
    private static final float KEEP_DISTANCE_PX = 160f;
    private static final float KITE_SPEED = 1.6f;      // m/s
    private static final float TELEGRAPH_TIME = 0.20f;
    private static final float COOLDOWN_TIME = 0.55f;

    private static final float INVULN_TIME = 0.12f;
    private static final float FLASH_TIME = 0.08f;
    private static final float STUN_TIME = 0.10f;

    public RangedEnemy(World world, float xPx, float yPx) {
        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        bd.position.set(PhysicsConstants.toMeters(xPx), PhysicsConstants.toMeters(yPx));
        bd.fixedRotation = true;

        body = world.createBody(bd);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(PhysicsConstants.toMeters(10f), PhysicsConstants.toMeters(14f));

        FixtureDef fd = new FixtureDef();
        fd.shape = shape;
        fd.density = 1.0f;
        fd.friction = 0.4f;
        fd.restitution = 0.0f;

        Fixture fx = body.createFixture(fd);
        fx.setUserData(this);

        shape.dispose();
    }

    public void update(float delta, Player player) {
        shotThisFrame = false;

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
            state = State.IDLE;
        }

        float dx = player.getXpx() - getXpx();
        float dist = Math.abs(dx);

        if (dx < -1f) facingDir = -1;
        else if (dx > 1f) facingDir = 1;

        switch (state) {
            case IDLE -> {
                body.setLinearVelocity(0f, body.getLinearVelocity().y);
                if (dist <= AGGRO_RANGE_PX) state = State.KITE;
            }
            case KITE -> {
                if (dist > AGGRO_RANGE_PX) {
                    state = State.IDLE;
                    break;
                }

                Vector2 v = body.getLinearVelocity();

                if (dist < KEEP_DISTANCE_PX) {
                    body.setLinearVelocity(-facingDir * KITE_SPEED, v.y);
                } else {
                    body.setLinearVelocity(0f, v.y);
                    state = State.TELEGRAPH;
                    telegraphTimer = TELEGRAPH_TIME;
                }
            }
            case TELEGRAPH -> {
                telegraphTimer -= delta;
                body.setLinearVelocity(0f, body.getLinearVelocity().y);
                if (telegraphTimer <= 0f) {
                    state = State.SHOOT;
                    shotThisFrame = true;
                }
            }
            case SHOOT -> {
                state = State.COOLDOWN;
                cooldownTimer = COOLDOWN_TIME;
            }
            case COOLDOWN -> {
                cooldownTimer -= delta;
                body.setLinearVelocity(0f, body.getLinearVelocity().y);
                if (cooldownTimer <= 0f) state = State.KITE;
            }
            case STAGGER, DEAD -> {}
        }
    }

    public boolean didShootThisFrame() { return shotThisFrame; }
    public int getFacingDir() { return facingDir; }
    public State getState() { return state; }

    public float getTelegraphAlpha() {
        if (state != State.TELEGRAPH) return 0f;
        float t = Math.max(0f, telegraphTimer) / TELEGRAPH_TIME;
        return 1f - t;
    }

    public float getXpx() { return PhysicsConstants.toPixels(body.getPosition().x); }
    public float getYpx() { return PhysicsConstants.toPixels(body.getPosition().y); }

    // Damageable
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
