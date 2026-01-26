package com.analiticasoft.hitraider.gameplay;

import com.analiticasoft.hitraider.assets.PlayerSprites;
import com.analiticasoft.hitraider.assets.SpriteManager;
import com.analiticasoft.hitraider.controllers.*;
import com.analiticasoft.hitraider.diagnostics.FrameStats;
import com.analiticasoft.hitraider.physics.PhysicsDestroyQueue;
import com.analiticasoft.hitraider.render.BackgroundParallax;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.Array;

/**
 * GameplayContext: shared runtime state for GameplayScreen subsystems.
 * Keep this as the single place where "global" gameplay refs live.
 */
public class GameplayContext {

    // Core controllers
    public final RunController run = new RunController();
    public final CameraController camera = new CameraController();
    public final ShakeController shake = new ShakeController();
    public final TransitionController transition = new TransitionController();

    // Fortification
    public final PhysicsDestroyQueue destroyQueue = new PhysicsDestroyQueue();
    public final FrameStats frameStats = new FrameStats();

    // Cameras
    public OrthographicCamera worldCamera;
    public OrthographicCamera uiCamera;

    // Assets
    public final SpriteManager sprites = new SpriteManager();
    public BackgroundParallax background;

    // Visual state timers
    public float playerStateTime = 0f;
    public PlayerSprites.State playerVisualState = PlayerSprites.State.IDLE;

    // Enemy timers (parallel arrays)
    public final Array<Float> meleeAnimTimes = new Array<>();
    public final Array<Float> rangedAnimTimes = new Array<>();

    // Deferred flags
    public boolean restartRequested = false;
    public boolean reloadRequested = false;

    // Strict debug
    public boolean strictModeOn = false;
    public boolean strictFreezeOnFail = true;
    public boolean frozenByStrict = false;
    public String lastStrictError = null;

    // Feel
    public float hitstopTimer = 0f;
    public int meleeHitCounter = 0;

    // Door state stays in context (simple, stable)
    public com.badlogic.gdx.physics.box2d.Body doorBody;
    public boolean doorClosed = false;
}
