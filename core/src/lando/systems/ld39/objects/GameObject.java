package lando.systems.ld39.objects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import lando.systems.ld39.utils.Assets;

public abstract class GameObject {

    public Vector2 position;
    public Rectangle bounds;
    public TextureRegion keyframe;

    public GameObject() {
        this.position = new Vector2();
        this.bounds = new Rectangle();
        this.keyframe = new TextureRegion(Assets.testTexture);
    }

    public GameObject(Rectangle bounds) {
        this();
        this.bounds = bounds;
    }

    public abstract void update(float dt);

    public void render(SpriteBatch batch) {
        batch.draw(keyframe, position.x, position.y);
    }

}
