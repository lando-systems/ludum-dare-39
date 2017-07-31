package lando.systems.ld39.objects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import lando.systems.ld39.ai.StateMachine;
import lando.systems.ld39.ai.Transition;
import lando.systems.ld39.ai.states.CruisingState;
import lando.systems.ld39.ai.states.FollowState;
import lando.systems.ld39.ai.states.LeaderState;
import lando.systems.ld39.ai.states.State;
import lando.systems.ld39.screens.GameScreen;
import lando.systems.ld39.utils.Assets;

/**
 * Created by Brian on 7/30/2017.
 */

public class EnemyCar extends Vehicle {

    // in order - these need to increase
    public static float dropBattery = 0.1f; //10%
    public static float dropWeapon = 0.3f; //20%
    public static float dropHealth = 0.4f; //10%
    public static float dropCash = 0.85f; //45%

    enum Type {cruiser, follower, leader, miniBoss, musk}

    private int chassis;
    public float relSpeed = 3f; // Relative speed
    private float deadTimer;
    private StateMachine stateMachine;
    public Type type;
    private boolean collidedWithPlayer = false;
    private float collidedWithPlayerTimer;
    private static float collidedWithPlayerTimerDefault = 1f;
    private Vector2 collisionDirection;

    public EnemyCar(GameScreen gameScreen) {
        this(gameScreen, Item.EnemyChassis1, Type.cruiser);
    }

    public EnemyCar(GameScreen gameScreen, int enemyChassis, Type type) {
        super(gameScreen, enemyChassis);
        chassis = enemyChassis;
        deadTimer = 1;
        this.type = type;
        collisionDirection = new Vector2();
        collidedWithPlayerTimer = collidedWithPlayerTimerDefault;

        setRandom(Item.Explosions);
        setRandom(Item.Engine);
        setRandom(Item.Booster);
    }

    private void setRandom(int item) {
        int max = Item.getMaxLevel(item);
        if (max != -1) {
            setUpgrade(item, MathUtils.random(max));
        }
    }

    @Override
    protected void updateCollisionBounds(Rectangle bounds) {
        collisionBounds.set(bounds);
        Assets.inflateRect(collisionBounds, -4, -10);
        collision_offset_x = collisionBounds.width / 2;
        collision_offset_y = collisionBounds.height / 2;
    }

    @Override
    public void killCar() {
        super.killCar();

        int id = 0;
        float drop = MathUtils.random.nextFloat();
        if (drop < dropBattery) {
            id = GameItem.Battery;
        } else if (drop < dropWeapon) {
            id = GameItem.Weapon;
        } else if (drop < dropHealth) {
            id = GameItem.Repair;
        } else if (drop < dropCash) {
            id = GameItem.Money;
        }

        if (id > 0) {
            GameItem item = new GameItem(gameScreen, true, id);
            item.setLocation(position.x, position.y);
            gameScreen.drops.add(item);
        }
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        if (dead){
            deadTimer -= dt;
            if (deadTimer < 0){
                remove = true;
            }
            return;
        }
        tiresOffRoad(dt);

        Rectangle playerBounds = gameScreen.playerCar.bounds;
        Vector2 playerPosition = gameScreen.playerCar.position;

        if (collidedWithPlayer) {
            collidedWithPlayerTimer -= dt;
            if (collidedWithPlayerTimer < 0) {
                collidedWithPlayer = false;
                collidedWithPlayerTimer = collidedWithPlayerTimerDefault;
            } else {
                if (collisionDirection.x != 0) {
                    position.add(-(collisionDirection.x * relSpeed), gameScreen.playerCar.speed * dt);
                } else {
                    position.add(0, (gameScreen.playerCar.speed * dt) - (collisionDirection.y * relSpeed));
                }
                setLocation(position.x, position.y);
            }
        } else if (collisionBounds.overlaps(gameScreen.playerCar.collisionBounds)) {
            // TODO: Collide
            if (!collidedWithPlayer) {
                gameScreen.playerCar.health -= 4;
                if (gameScreen.playerCar.upgrades.getLevel(Item.Axes) > 0){
                    health -= 9;
                    gameScreen.playerCar.hitAxe();
                }
                health -= 1;
                Rectangle intersection = new Rectangle();
                Intersector.intersectRectangles(collisionBounds, gameScreen.playerCar.collisionBounds, intersection);
                if (intersection.width > intersection.height) {
                    if (intersection.y > collisionBounds.y) {
                        // Intersects with top
                        collisionDirection.set(0, 1);
                    } else {
                        // Intersects with bottom
                        collisionDirection.set(0, -1);
                    }
                } else {
                    if(intersection.x > collisionBounds.x) {
                        // Intersects with right
                        collisionDirection.set(1, 0);
                    } else {
                        // Intersects with left
                        collisionDirection.set(-1, 0);
                    }
                }

                collidedWithPlayer = true;
            }
        } else {
            // Follow player
//            float distance = position.dst(playerPosition);
//            Vector2 direction = (new Vector2(position)).sub(playerPosition).nor();
//
//            position.add(- (direction.x * relSpeed), (gameScreen.playerCar.speed *dt)- (direction.y * relSpeed));
//            setLocation(position.x, position.y);
            stateMachine.update(dt);
        }
    }

    @Override
    public void render(SpriteBatch batch) {
//        batch.setColor(Color.BLUE);
//        batch.draw(Assets.whitePixel, bounds.x, bounds.y, bounds.width, bounds.height);
//        batch.setColor(Color.WHITE);

        render(batch, chassis, true);
        super.render(batch);
    }

    private static int testlevel = 0;
    public static Vehicle getEnemy(GameScreen gameScreen) {
        //Vector3 cameraPosition = gameScreen.camera.position;

        int chassis = Item.EnemyChassis1;

        //int level = MathUtils.random(Item.getMaxLevel(chassis));
        int newLevel = testlevel++;
        if (testlevel == Item.getMaxLevel(chassis)) {
            testlevel = 0;
        }
        Type type = Type.cruiser;
        float r = MathUtils.random();
        if (r > .9f){
            type = Type.follower;
        } else if (r > .5f){
            type = Type.leader;
        }

        EnemyCar enemyCar = new EnemyCar(gameScreen, chassis, type);
        enemyCar.setUpgrade(chassis, newLevel);

        float positionY = gameScreen.camera.position.y + gameScreen.camera.viewportHeight/2f + enemyCar.bounds_offset_y;
        if (type == Type.follower){
            positionY = gameScreen.camera.position.y - gameScreen.camera.viewportHeight/2f - enemyCar.bounds_offset_y;
        }
        float left = gameScreen.road.getLeftEdge(positionY);
        float right = gameScreen.road.getRightEdge(positionY);
        float positionX = MathUtils.random(left + enemyCar.bounds_offset_x, right - enemyCar.bounds_offset_x);


        enemyCar.setLocation(positionX, positionY);
        enemyCar.initializeStates();

        return enemyCar;
    }



    public void initializeStates(){
        switch (type) {
            case cruiser:
                createCruiser();
                break;
            case follower:
                createFollower();
                break;
            case leader:
                createLeader();
                break;
        }

    }


    public void createLeader() {
        health = 30;
        State initialState = new LeaderState(this);

        Array<Transition> transitions = new Array<Transition>();

        stateMachine = new StateMachine(initialState, transitions);
    }

    public void createFollower() {
        State initialState = new FollowState(this);

        Array<Transition> transitions = new Array<Transition>();

        stateMachine = new StateMachine(initialState, transitions);
    }

    public void createCruiser(){
        State initialState = new CruisingState(this);

        Array<Transition> transitions = new Array<Transition>();

        stateMachine = new StateMachine(initialState, transitions);
    }

    public float avoidGrass(float positionY){
        float topOfCar = positionY + bounds.height/2f;
        float bottomOfCar = positionY - bounds.height/2f;
        float testPosition = topOfCar + 5f;
        float left = gameScreen.road.getLeftEdge(testPosition);
        float right = gameScreen.road.getRightEdge(testPosition);
        float deltaX = 0;
        if (position.x - bounds.width/2 <= left) {deltaX += 4; }
        if (position.x + bounds.width/2 >= right) {deltaX += -4; }

        left = gameScreen.road.getLeftEdge(topOfCar);
        right = gameScreen.road.getRightEdge(topOfCar);

        if (position.x - bounds.width/2 <= left) {deltaX += 2; }
        if (position.x + bounds.width/2 >= right) {deltaX += -2; }

        left = gameScreen.road.getLeftEdge(bottomOfCar);
        right = gameScreen.road.getRightEdge(bottomOfCar);

        if (position.x - bounds.width/2 <= left) {deltaX += 4; }
        if (position.x + bounds.width/2 >= right) {deltaX += -4; }
        return deltaX;
    }
}
