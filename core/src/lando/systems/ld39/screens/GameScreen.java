package lando.systems.ld39.screens;

import aurelienribon.tweenengine.Tween;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import lando.systems.ld39.objects.GameObject;
import lando.systems.ld39.objects.PlayerCar;
import lando.systems.ld39.particles.ParticleSystem;
import lando.systems.ld39.road.Road;
import lando.systems.ld39.utils.Assets;
import lando.systems.ld39.utils.Config;
import lando.systems.ld39.utils.accessors.RectangleAccessor;

/**
 * Created by Brian on 7/25/2017
 */
public class GameScreen extends BaseScreen {

    public static float zoomScale = 0.15f;
    public static float maxZoom = 1.6f;
    public static float minZoom = 0.2f;
    public static float DRAG_DELTA = 10f;

    public Road road;

    public Array<GameObject> gameObjects = new Array<GameObject>();
    public ParticleSystem particleSystem = new ParticleSystem();

    public PlayerCar playerCar;

    public Rectangle constraintBounds;
    private Vector2 constraintOffset;

    private Vector3 touchStart;
    private Vector3 cameraTouchStart;
    private boolean cancelTouchUp;

    public GameScreen() {
        road = new Road();
        touchStart = new Vector3();
        cameraTouchStart = new Vector3();
        cancelTouchUp = false;

        constraintBounds = new Rectangle(0, 10, camera.viewportWidth, camera.viewportHeight * 0.7f);
        constraintOffset = new Vector2((camera.viewportWidth /2) - 10, camera.viewportHeight /2);

        createCar();
    }

    private void createCar() {

        playerCar = new PlayerCar(this);
        playerCar.constraintBounds = constraintBounds;
        gameObjects.add(playerCar);
    }

    @Override
    public void update(float dt) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
        }

        updateWorld(dt);
        updateObjects(dt);
        updateCamera(dt);
        particleSystem.update(dt);
    }

    private void updateWorld(float dt) {
        road.update(dt);
    }

    private void updateObjects(float dt) {
        playerCar.constraintBounds = constraintBounds;
        for(GameObject gameObject : gameObjects) {
            gameObject.update(dt);
        }
    }

    private void updateCamera(float dt) {
        camera.zoom = MathUtils.clamp(camera.zoom, minZoom, maxZoom);

        float deltaY = playerCar.speed;
        // move camera based on car speed - update position of car in so it doesn't drop
        playerCar.position.y += deltaY;
        camera.position.y += deltaY;
        constraintBounds.y += deltaY;

        // Keep camera within world bounds
//        float minY = world.bounds.y + camera.viewportHeight / 2 * camera.zoom;
//        float maxY = world.bounds.height - camera.viewportHeight / 2 * camera.zoom;
//        float minX = world.bounds.x + camera.viewportWidth / 2 * camera.zoom;
//        float maxX = world.bounds.x + world.bounds.width - camera.viewportWidth / 2 * camera.zoom;
//        if (camera.viewportHeight * camera.zoom > world.bounds.height) {
//            camera.position.y = world.bounds.height / 2;
//        } else {
//            camera.position.y = MathUtils.clamp(camera.position.y, minY, maxY);
//        }
//        if (camera.viewportWidth * camera.zoom > world.bounds.width) {
//            camera.position.x = world.bounds.x + world.bounds.width / 2;
//        } else {
//            camera.position.x = MathUtils.clamp(camera.position.x, minX, maxX);
//        }

        camera.update();
    }

    @Override
    public void render(SpriteBatch batch) {
        Gdx.gl.glClearColor(Config.bgColor.r, Config.bgColor.g, Config.bgColor.b, Config.bgColor.a);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw world
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        {
            road.renderFrameBuffer(batch, camera);
            renderWorld(batch);
            particleSystem.render(batch);
            renderObjects(batch);
        }
        batch.end();

        // Draw hud
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();
        {
            renderHud(batch);
        }
        batch.end();
    }

    private void renderWorld(SpriteBatch batch) {
        road.render(batch, camera);
    }

    private void renderObjects(SpriteBatch batch) {
//        batch.setColor(Color.RED);
//        batch.draw(Assets.whitePixel, constraintBounds.x, constraintBounds.y, constraintBounds.width, constraintBounds.height);
//        batch.setColor(Color.WHITE);

        for (GameObject gameObject : gameObjects) {
            gameObject.render(batch);
        }
    }

    private void renderHud(SpriteBatch batch) {
//            hud.render(batch);
    }

    // ------------------------------------------------------------------------
    // InputAdapter Overrides
    // ------------------------------------------------------------------------

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        cameraTouchStart.set(camera.position);
        touchStart.set(screenX, screenY, 0);

        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        camera.position.x = cameraTouchStart.x + (touchStart.x - screenX) * camera.zoom;
        camera.position.y = cameraTouchStart.y + (screenY - touchStart.y) * camera.zoom;
        if (cameraTouchStart.dst(camera.position) > DRAG_DELTA) {
            cancelTouchUp = true;
        }
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (cancelTouchUp) {
            cancelTouchUp = false;
            return false;
        }

        return false;
    }

    private Vector3 currentUnprojectedTouch = new Vector3();
    @Override
    public boolean scrolled(int change) {
        camera.unproject(currentUnprojectedTouch.set(Gdx.input.getX(), Gdx.input.getY(), 0));
        camera.zoom += change * camera.zoom * zoomScale;

        updateCamera(Gdx.graphics.getDeltaTime());

        float prevUnprojectedTouchX = currentUnprojectedTouch.x;
        float prevUnprojectedTouchY = currentUnprojectedTouch.y;
        camera.unproject(currentUnprojectedTouch.set(Gdx.input.getX(), Gdx.input.getY(), 0));
        camera.position.add(prevUnprojectedTouchX - currentUnprojectedTouch.x, prevUnprojectedTouchY - currentUnprojectedTouch.y, 0);
        camera.update();

        return true;
    }
}
