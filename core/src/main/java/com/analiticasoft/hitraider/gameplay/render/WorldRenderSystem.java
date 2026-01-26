package com.analiticasoft.hitraider.gameplay.render;

import com.analiticasoft.hitraider.assets.*;
import com.analiticasoft.hitraider.combat.Faction;
import com.analiticasoft.hitraider.combat.Projectile;
import com.analiticasoft.hitraider.config.GameConfig;
import com.analiticasoft.hitraider.entities.MeleeEnemy;
import com.analiticasoft.hitraider.entities.RangedEnemy;
import com.analiticasoft.hitraider.gameplay.GameplayContext;
import com.analiticasoft.hitraider.physics.PhysicsConstants;
import com.analiticasoft.hitraider.relics.RelicPickup;
import com.analiticasoft.hitraider.render.DebugPhysicsRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.physics.box2d.Fixture;

public class WorldRenderSystem {

    private final DebugPhysicsRenderer debugPhysics = new DebugPhysicsRenderer();

    public void render(GameplayContext ctx, ShapeRenderer shapes, SpriteBatch batch,
                       boolean debugHitboxes, boolean debugHurtboxes) {

        // Background
        if (ctx.background != null) {
            batch.setProjectionMatrix(ctx.worldCamera.combined);
            batch.begin();
            ctx.background.render(batch, ctx.worldCamera.position.x, ctx.worldCamera.position.y,
                GameConfig.VIRTUAL_W, GameConfig.VIRTUAL_H);
            batch.end();
        }

        // Shapes: world + projectiles + pickups + door
        shapes.setProjectionMatrix(ctx.worldCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);

        if (ctx.run.platformRects != null) {
            for (var p : ctx.run.platformRects) {
                if ("oneway".equals(p.type)) shapes.setColor(0.35f, 0.35f, 0.38f, 1f);
                else shapes.setColor(0.20f, 0.20f, 0.22f, 1f);
                shapes.rect(p.cx - p.w / 2f, p.cy - p.h / 2f, p.w, p.h);
            }
        }

        if (ctx.doorClosed && ctx.doorBody != null) {
            float dx = PhysicsConstants.toPixels(ctx.doorBody.getPosition().x);
            float dy = PhysicsConstants.toPixels(ctx.doorBody.getPosition().y);
            shapes.setColor(0.08f, 0.08f, 0.09f, 1f);
            shapes.rect(dx - 14f, dy - 110f, 28f, 220f);
        }

        shapes.setColor(0.10f, 0.45f, 0.10f, 1f);
        for (RelicPickup p : ctx.run.pickups) shapes.circle(p.getXpx(), p.getYpx(), 6f, 16);

        for (var pr : ctx.run.projectiles.projectiles) {
            if (pr.state == Projectile.State.ALIVE) {
                if (pr.faction == Faction.PLAYER) shapes.setColor(0.05f, 0.05f, 0.05f, 1f);
                else shapes.setColor(0.10f, 0.10f, 0.25f, 1f);
                shapes.rect(pr.lastXpx - 3f, pr.lastYpx - 3f, 6f, 6f);
            }
        }

        shapes.end();

        // Batch: sprites (enemies + player)
        batch.setProjectionMatrix(ctx.worldCamera.combined);
        batch.begin();

        EnemySprites meleeS = ctx.sprites.eldarMelee();
        for (int i = 0; i < ctx.run.meleeEnemies.size; i++) {
            MeleeEnemy e = ctx.run.meleeEnemies.get(i);
            var st = EnemyStateMapper.map(String.valueOf(e.getState()));
            float t = (i < ctx.meleeAnimTimes.size) ? ctx.meleeAnimTimes.get(i) : 0f;
            TextureRegion frame = EnemySprites.isOneShot(st) ? meleeS.getOnce(st, t) : meleeS.get(st, t);

            if (frame != null) {
                float ex = e.getXpx();
                float footY = e.getYpx() - meleeS.feetOffsetPx;
                float w = frame.getRegionWidth() * meleeS.scale;
                float h = frame.getRegionHeight() * meleeS.scale;

                boolean flip = e.getFacingDir() < 0;
                if (frame.isFlipX() != flip) frame.flip(true, false);

                batch.draw(frame, ex - w / 2f, footY, w, h);
            }
        }

        EnemySprites rangedS = ctx.sprites.eldarRanged();
        for (int i = 0; i < ctx.run.rangedEnemies.size; i++) {
            RangedEnemy e = ctx.run.rangedEnemies.get(i);
            var st = EnemyStateMapper.map(String.valueOf(e.getState()));
            float t = (i < ctx.rangedAnimTimes.size) ? ctx.rangedAnimTimes.get(i) : 0f;
            TextureRegion frame = EnemySprites.isOneShot(st) ? rangedS.getOnce(st, t) : rangedS.get(st, t);

            if (frame != null) {
                float ex = e.getXpx();
                float footY = e.getYpx() - rangedS.feetOffsetPx;
                float w = frame.getRegionWidth() * rangedS.scale;
                float h = frame.getRegionHeight() * rangedS.scale;

                boolean flip = e.getFacingDir() < 0;
                float drawX = flip ? ex + w / 2f : ex - w / 2f;
                float drawW = flip ? -w : w;

                batch.draw(frame, drawX, footY, drawW, h);
            }
        }

        PlayerSprites ps = ctx.sprites.player();
        TextureRegion pFrame = PlayerSprites.isOneShot(ctx.playerVisualState)
            ? ps.getOnce(ctx.playerVisualState, ctx.playerStateTime)
            : ps.get(ctx.playerVisualState, ctx.playerStateTime);

        if (pFrame != null) {
            float px = ctx.run.player.getXpx();
            float footY = ctx.run.player.getYpx() - ps.feetOffsetPx;
            float w = pFrame.getRegionWidth() * ps.scale;
            float h = pFrame.getRegionHeight() * ps.scale;

            boolean flip = ctx.run.player.getFacingDir() < 0;
            float drawX = flip ? px + w / 2f : px - w / 2f;
            float drawW = flip ? -w : w;

            batch.draw(pFrame, drawX, footY, drawW, h);
        }

        batch.end();

        // Debug lines
        if (debugHitboxes || debugHurtboxes) {
            shapes.setProjectionMatrix(ctx.worldCamera.combined);
            shapes.begin(ShapeRenderer.ShapeType.Line);

            if (debugHurtboxes) {
                shapes.setColor(0.2f, 0.4f, 0.9f, 1f);

                for (Fixture fx : ctx.run.player.body.getFixtureList()) {
                    if ("player_ground_sensor".equals(fx.getUserData())) continue;
                    debugPhysics.drawFixtureOutline(shapes, fx);
                }
                for (MeleeEnemy e : ctx.run.meleeEnemies)
                    for (Fixture fx : e.body.getFixtureList()) debugPhysics.drawFixtureOutline(shapes, fx);

                for (RangedEnemy e : ctx.run.rangedEnemies)
                    for (Fixture fx : e.body.getFixtureList()) debugPhysics.drawFixtureOutline(shapes, fx);
            }

            if (debugHitboxes) {
                shapes.setColor(0.9f, 0.2f, 0.2f, 1f);
                for (Fixture fx : ctx.run.combat.getActiveHitboxFixtures()) debugPhysics.drawFixtureOutline(shapes, fx);
            }

            shapes.end();
        }
    }
}
