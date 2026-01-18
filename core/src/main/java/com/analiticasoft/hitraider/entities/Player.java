package com.analiticasoft.hitraider.entities;

import com.analiticasoft.hitraider.combat.Damageable;
import com.analiticasoft.hitraider.combat.Faction;
import com.analiticasoft.hitraider.combat.HealthComponent;
import com.analiticasoft.hitraider.input.Action;
import com.analiticasoft.hitraider.input.InputState;
import com.analiticasoft.hitraider.physics.GameContactListener;
import com.analiticasoft.hitraider.physics.PhysicsConstants;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

public class Player implements Damageable {

    public enum State {
        IDLE, RUN, JUMP, FALL, DASH, ATTACK, HURT, DEAD
    }

    public final Body body;
    private final GameContactListener.GroundContactCounter groundCounter;

    private final HealthComponent health = new HealthComponent(10);

    private State state = State.IDLE;

    // Movement
    private static final float MOVE_SPEED_MPS = 2.6f;

    // Jump
    private static final float JUMP_VELOCITY = 9.0f;
    private static final float COYOTE_TIME = 0.10f;
    private static final float JUMP_BUFFER_TIME = 0.12f;
    private static final float JUMP_CUT_MULT = 0.45f;
    private static final float MIN_CUT_VY = 1.5f;
    private static final float MAX_FALL_SPEED = -12f;

    // Dash
    private static final float DASH_SPEED_MPS = 7.5f;
    private static final float DASH_TIME = 0.12f;
    private static final float DASH_COOLDOWN = 0.25f;

    // ATTACK TIMING (feel)
    private static final float ATK_STARTUP = 0.06f;
    private static final float ATK_ACTIVE  = 0.10f;
    private static final float ATK_RECOVER = 0.14f;
    private static final float ATK_COOLDOWN = 0.10f;

    // Cancel windows
    // - Dash can cancel during RECOVERY (y también HURT si quieres)
    private static final boolean DASH_CANCEL_RECOVERY = true;

    // Hurt
    private static final float HURT_LOCK_TIME = 0.10f;

    // Damage feel
    private static final float INVULN_TIME = 0.25f;
    private static final float FLASH_TIME = 0.10f;
    private static final float STUN_TIME = 0.10f;

    // Timers
    private float coyoteTimer = 0f;
    private float jumpBufferTimer = 0f;
    private boolean wasJumpDown = false;

    private float dashTimer = 0f;
    private float dashCooldownTimer = 0f;
    private int facingDir = 1;

    // Attack phase
    private enum AttackPhase { NONE, STARTUP, ACTIVE, RECOVERY }
    private AttackPhase attackPhase = AttackPhase.NONE;
    private float attackPhaseTimer = 0f;
    private float attackCooldownTimer = 0f;
    private boolean attackHitboxSpawnThisFrame = false;

    // Hurt timer
    private float hurtTimer = 0f;

    public Player(World world, float startXpx, float startYpx) {
        float x = PhysicsConstants.toMeters(startXpx);
        float y = PhysicsConstants.toMeters(startYpx);

        BodyDef bd = new BodyDef();
        bd.type = BodyDef.BodyType.DynamicBody;
        bd.position.set(x, y);
        bd.fixedRotation = true;

        body = world.createBody(bd);

        // Capsule collider (box + 2 circles)
        float halfW = PhysicsConstants.toMeters(10f);
        float halfH = PhysicsConstants.toMeters(16f);
        float radius = PhysicsConstants.toMeters(7f);

        PolygonShape box = new PolygonShape();
        float boxHalfH = Math.max(halfH - radius, PhysicsConstants.toMeters(4f));
        box.setAsBox(halfW, boxHalfH);

        FixtureDef boxFd = new FixtureDef();
        boxFd.shape = box;
        boxFd.density = 1.0f;
        boxFd.friction = 0.0f;
        boxFd.restitution = 0.0f;

        Fixture main = body.createFixture(boxFd);
        main.setUserData(this);
        box.dispose();

        CircleShape top = new CircleShape();
        top.setRadius(radius);
        top.setPosition(new Vector2(0, boxHalfH));
        FixtureDef topFd = new FixtureDef();
        topFd.shape = top;
        topFd.density = 1.0f;
        topFd.friction = 0.0f;
        topFd.restitution = 0.0f;
        Fixture topFix = body.createFixture(topFd);
        topFix.setUserData(this);
        top.dispose();

        CircleShape bottom = new CircleShape();
        bottom.setRadius(radius);
        bottom.setPosition(new Vector2(0, -boxHalfH));
        FixtureDef bottomFd = new FixtureDef();
        bottomFd.shape = bottom;
        bottomFd.density = 1.0f;
        bottomFd.friction = 0.0f;
        bottomFd.restitution = 0.0f;
        Fixture bottomFix = body.createFixture(bottomFd);
        bottomFix.setUserData(this);
        bottom.dispose();

        // Ground sensor
        PolygonShape sensorShape = new PolygonShape();
        sensorShape.setAsBox(
            PhysicsConstants.toMeters(8f),
            PhysicsConstants.toMeters(2f),
            new Vector2(0, -halfH),
            0f
        );
        FixtureDef sfd = new FixtureDef();
        sfd.shape = sensorShape;
        sfd.isSensor = true;
        Fixture sensor = body.createFixture(sfd);
        sensor.setUserData("player_ground_sensor");
        sensorShape.dispose();

        groundCounter = new GameContactListener.GroundContactCounter();
        body.setUserData(groundCounter);

        body.setLinearDamping(0.0f);
        body.setAngularDamping(10.0f);
    }

    public void update(float delta, InputState input) {
        attackHitboxSpawnThisFrame = false;

        health.update(delta);
        if (!health.isAlive()) {
            state = State.DEAD;
            Vector2 v = body.getLinearVelocity();
            body.setLinearVelocity(0f, v.y);
            return;
        }

        // Timers
        if (isGrounded()) coyoteTimer = COYOTE_TIME;
        else coyoteTimer = Math.max(0f, coyoteTimer - delta);

        if (input.isJustPressed(Action.JUMP)) jumpBufferTimer = JUMP_BUFFER_TIME;
        else jumpBufferTimer = Math.max(0f, jumpBufferTimer - delta);

        if (dashCooldownTimer > 0f) dashCooldownTimer = Math.max(0f, dashCooldownTimer - delta);
        if (dashTimer > 0f) dashTimer = Math.max(0f, dashTimer - delta);

        if (attackCooldownTimer > 0f) attackCooldownTimer = Math.max(0f, attackCooldownTimer - delta);

        if (hurtTimer > 0f) hurtTimer = Math.max(0f, hurtTimer - delta);

        // Facing dir
        float mx = input.getMoveX();
        if (mx < -0.01f) facingDir = -1;
        else if (mx > 0.01f) facingDir = 1;

        // ---- HURT lock ----
        if (state == State.HURT) {
            // durante hurt, no control, pero sí gravedad/velocidad
            Vector2 v = body.getLinearVelocity();
            body.setLinearVelocity(0f, v.y);

            if (hurtTimer <= 0f && !health.isStunned()) {
                state = State.IDLE;
            }
            // aun así dejamos caer/saltar? no.
            return;
        }

        // ---- Attack phase update ----
        if (attackPhase != AttackPhase.NONE) {
            attackPhaseTimer = Math.max(0f, attackPhaseTimer - delta);

            if (attackPhase == AttackPhase.STARTUP && attackPhaseTimer <= 0f) {
                attackPhase = AttackPhase.ACTIVE;
                attackPhaseTimer = ATK_ACTIVE;
                attackHitboxSpawnThisFrame = true; // Spawn aquí: empieza active
            } else if (attackPhase == AttackPhase.ACTIVE && attackPhaseTimer <= 0f) {
                attackPhase = AttackPhase.RECOVERY;
                attackPhaseTimer = ATK_RECOVER;
            } else if (attackPhase == AttackPhase.RECOVERY && attackPhaseTimer <= 0f) {
                attackPhase = AttackPhase.NONE;
                state = State.IDLE;
            }
        }

        // ---- Input priority: DASH (cuando es posible) ----
        boolean wantsDash = input.isJustPressed(Action.DASH);

        boolean canDash = dashCooldownTimer <= 0f && dashTimer <= 0f;
        boolean canDashCancelAttack = DASH_CANCEL_RECOVERY && attackPhase == AttackPhase.RECOVERY;

        if (wantsDash && canDash && (attackPhase == AttackPhase.NONE || canDashCancelAttack)) {
            // cancel recovery si aplica
            attackPhase = AttackPhase.NONE;
            state = State.DASH;

            dashTimer = DASH_TIME;
            dashCooldownTimer = DASH_COOLDOWN;

            Vector2 v = body.getLinearVelocity();
            body.setLinearVelocity(facingDir * DASH_SPEED_MPS, v.y);
            return;
        }

        // ---- Start ATTACK? ----
        boolean wantsAttack = input.isJustPressed(Action.ATTACK);
        if (wantsAttack && attackPhase == AttackPhase.NONE && attackCooldownTimer <= 0f && dashTimer <= 0f) {
            state = State.ATTACK;
            attackPhase = AttackPhase.STARTUP;
            attackPhaseTimer = ATK_STARTUP;
            attackCooldownTimer = ATK_COOLDOWN;
        }

        // ---- Dash in progress ----
        if (state == State.DASH && dashTimer > 0f) {
            Vector2 v = body.getLinearVelocity();
            body.setLinearVelocity(facingDir * DASH_SPEED_MPS, v.y);
            return;
        } else if (state == State.DASH && dashTimer <= 0f) {
            state = State.IDLE;
        }

        // ---- Movement lock during ATTACK startup/active/recovery ----
        boolean movementLocked = (attackPhase != AttackPhase.NONE);

        Vector2 v = body.getLinearVelocity();
        if (movementLocked) {
            body.setLinearVelocity(0f, v.y);
        } else {
            body.setLinearVelocity(mx * MOVE_SPEED_MPS, v.y);
        }

        // ---- Jump execute (buffer + coyote) ----
        if (!movementLocked) {
            if (jumpBufferTimer > 0f && coyoteTimer > 0f) {
                Vector2 now = body.getLinearVelocity();
                body.setLinearVelocity(now.x, JUMP_VELOCITY);
                jumpBufferTimer = 0f;
                coyoteTimer = 0f;
                wasJumpDown = true;
            }

            // Variable jump cut
            boolean jumpDown = input.isDown(Action.JUMP);
            boolean releasedThisFrame = wasJumpDown && !jumpDown;
            wasJumpDown = jumpDown;

            if (releasedThisFrame) {
                Vector2 cur = body.getLinearVelocity();
                if (cur.y > MIN_CUT_VY) body.setLinearVelocity(cur.x, cur.y * JUMP_CUT_MULT);
            }
        }

        // Clamp fall
        Vector2 cur = body.getLinearVelocity();
        if (cur.y < MAX_FALL_SPEED) body.setLinearVelocity(cur.x, MAX_FALL_SPEED);

        updateState(body.getLinearVelocity());
    }

    private void updateState(Vector2 v) {
        if (state == State.DASH && dashTimer > 0f) return;
        if (attackPhase != AttackPhase.NONE) return;
        if (state == State.HURT) return;
        if (state == State.DEAD) return;

        if (!isGrounded()) {
            state = (v.y > 0.2f) ? State.JUMP : State.FALL;
            return;
        }

        state = (Math.abs(v.x) > 0.05f) ? State.RUN : State.IDLE;
    }

    public boolean isGrounded() { return groundCounter.grounded(); }
    public State getState() { return state; }

    public int getFacingDir() { return facingDir; }

    /** -1 down, 0 neutral, +1 up */
    public int getAimY(InputState input) {
        if (input.isDown(Action.MOVE_UP)) return 1;
        if (input.isDown(Action.MOVE_DOWN)) return -1;
        return 0;
    }

    /** true solo el frame en que entra a ACTIVE */
    public boolean shouldSpawnAttackHitboxThisFrame() {
        return attackHitboxSpawnThisFrame;
    }

    public float getXpx() { return PhysicsConstants.toPixels(body.getPosition().x); }
    public float getYpx() { return PhysicsConstants.toPixels(body.getPosition().y); }

    public boolean isFlashing() { return health.isFlashing(); }

    // Damageable
    @Override public Faction getFaction() { return Faction.PLAYER; }
    @Override public boolean isAlive() { return health.isAlive(); }
    @Override public HealthComponent getHealth() { return health; }

    @Override
    public void applyDamage(int amount, Vector2 knockback) {
        boolean applied = health.tryDamage(amount, INVULN_TIME, FLASH_TIME, STUN_TIME);
        if (!applied) return;

        // Enter HURT (interrumpe ataque/dash)
        state = State.HURT;
        hurtTimer = HURT_LOCK_TIME;

        Vector2 v = body.getLinearVelocity();
        body.setLinearVelocity(v.x + knockback.x, v.y + knockback.y);
    }
}
