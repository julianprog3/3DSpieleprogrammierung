package mygame;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.font.BitmapText;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.util.SkyFactory;
import com.jme3.texture.Texture;

/**
 * test
 * @author normenhansen
 */
public class Main extends SimpleApplication{
    boolean isWalking;
    boolean isRunning;
    boolean anyKeyPressed;
    
    long startTime;
    int itemsCollected;
    int pulsefactor = 2;

    final long FADETIME = 5000;
    final int ITEMNUMBER = 8; // Anzahl Prog Themen
    final int MOVEMENTSPEED = 5;
    final int GRAVITY = 10;
    final int JUMPFACTOR = 50;
    final int ITEMSET = 5;
    final float PROGMAN_X = -100.0f;
    final float PROGMAN_Y = 2.5f;
    final float PROGMAN_Z = -10.0f;
    final float PROGMAN_MAX_SPEED = 0.1f;
    final float WORLD_SIZE = 125.0f;
    
    
    Camera camera;
    Vector3f position;
    Vector3f progman_pos;
    ColorRGBA color;
    
    
    // Figures and Textures
    Geometry [] items;
    Geometry progman;
    Spatial flash;
    PointLight light;
    Spatial floor;
    
    // Sounds and Audio
    private AudioNode audio_theme;
    private AudioNode audio_nature;
    private AudioNode audio_foodsteps;
    private AudioNode audio_foodsteps_end;
    
    // Labels & Textfields
    BitmapText textField;
            
    
    // Stuff for Collision detection
    private Vector3f camDir = new Vector3f();
    private Vector3f walkDirection = new Vector3f(0,0,0);
    private Vector3f camLeft = new Vector3f();
    private boolean left = false, right = false, move = false, back = false;
    
    BulletAppState bulletAppState;
    CharacterControl player;
    RigidBodyControl physicsNode;
            
    public static void main(String[] args) {
        Main app = new Main();
        app.start();
    }

        
    @Override
    public void simpleInitApp() {
        isRunning = true;
        isWalking = false;
        anyKeyPressed = false;
        camera = viewPort.getCamera();
        itemsCollected = 0;
        startTime = 0;
        
        // Physics and Collision
        bulletAppState = new BulletAppState();
        stateManager.attach(bulletAppState);
        
         
        
        // Init functionalities
        initListeners();
        initAudio();
        initPlayerPhysics();
        //bulletAppState.getPhysicsSpace().enableDebug(assetManager);

        // Init Geometries
        initForest();
        initSky();
        initHouses();
        initProgman();
        
        items = new Geometry [ITEMSET];
        Node itemNode = new Node();
        
        for (int i = 0; i < items.length; i++){
            float random = (float) Math.random()*50;
            Box box = new Box(0.5f, 0.5f, 0.5f);
            Geometry cube = new Geometry("box", box);
            Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
            mat1.setColor("Color", ColorRGBA.randomColor());
            cube.setMaterial(mat1);
            cube.setLocalTranslation(random, 5f, random);
            items[i] = cube;
                       
            //item = makeCube("Box", random, 0f, 1f);
            itemNode.attachChild(cube);
            
            
        }
        // Textfield
        guiNode.setQueueBucket(Bucket.Gui);
        textField = new BitmapText(guiFont, false);          
        textField.setSize(2*guiFont.getCharSet().getRenderedSize()); 
        color = new ColorRGBA(ColorRGBA.White);
        textField.setColor(color);                             // font color
        textField.setText("");             // the text
        textField.setLocalTranslation(settings.getWidth()/2 - 100, settings.getHeight()/2, 0); // position
 
           
        // Attach to game
        rootNode.attachChild(itemNode);
        
        floor = makeFloor();
        rootNode.attachChild(floor);
        setDisplayStatView(false);
        flyCam.setMoveSpeed(MOVEMENTSPEED);
        camera.setFrustumPerspective(45f, (float)cam.getWidth() / cam.getHeight(), 1f, 100f); // Camera nur bis 100 meter
        
                
    }
    public void updateProgman()
    {
        Vector3f direction = new Vector3f(position.x-progman_pos.x, 0f, position.z-progman_pos.z);
       //System.out.println("pos "+position + " progman: " + progman_pos + " moving to " + direction);
        if(direction.length() > PROGMAN_MAX_SPEED)
            direction = direction.divide(direction.length()).mult(PROGMAN_MAX_SPEED);
        //System.out.println("2pos "+position + " progman: " + progman_pos + " moving to " + direction);
        progman_pos = progman_pos.add(direction);
        progman.setLocalTranslation(progman_pos);
    }
    @Override
    public void simpleUpdate(float tpf) {
        
        updateProgman();
        // no jumps allowed
        System.out.println("pos:" +  position.x + " " + position.y + " " + position.z);
        //Set position of text label           
        foodstepsCheck();
        isWalking = false; // Muss jedes Frame neu gesetzt werden
        fadeHUD(tpf);
        
        // Collision detection
        camDir.set(cam.getDirection()).multLocal(0.1f);
        camLeft.set(cam.getLeft()).multLocal(0.1f);
        
        walkDirection.set(0, 0, 0);
        
        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (move) {
            walkDirection.addLocal(camDir);
        }
        if (back) {
            walkDirection.addLocal(camDir.negate());
        }
        player.setWalkDirection(walkDirection);
        cam.setLocation(player.getPhysicsLocation());
        // Update Flashlight
        light.setPosition(player.getPhysicsLocation());

    
    }
   
    @Override
    public void simpleRender(RenderManager rm) {
        // wird automatisch nach simple Update ausgeführt
    }
     
    // Anonyme Klasse des AnalogListeners
    private AnalogListener analogListener = new AnalogListener(){
        public void onAnalog(String name, float value, float tpf) {
               if (name.equals("Move") && isRunning == true){
                   isWalking = true;
                   audio_foodsteps.play();
               }
               if (name.equals("Left") && isRunning == true){ 
                   isWalking = true;
                   audio_foodsteps.play();
               }
               if (name.equals("Back") && isRunning == true){
                   isWalking = true;
                   audio_foodsteps.play();
               }
               if (name.equals("Right") && isRunning == true){
                    isWalking = true;
                    audio_foodsteps.play();
               }   
        }   
    };
    
    private ActionListener actionListener = new ActionListener(){
        public void onAction(String name, boolean isPressed, float tpf) {
            if(name.equals("Pause") && isPressed){
                isRunning = !isRunning; // Continue or Pause game
               showHUD(tpf);
            }
            if(name.equals("Move") && isPressed == false){
                audio_foodsteps.stop();
            } 
            if(name.equals("Jump") && isPressed == true){
                showHUD(tpf, "That was a jump right there!");
                audio_foodsteps.stop();
                player.jump();
            } 
            
            // Collision detection
             if (name.equals("Left")) {
              left = isPressed;
            } else if (name.equals("Right")) {
              right= isPressed;
            } else if (name.equals("Move")) {
              move = isPressed;
            } else if (name.equals("Back")) {
              back = isPressed;
            } 
        }
        
    };
    
    
    
    // Functional methods
    public void pulseElement(float tpf, Geometry figure){
        figure.setLocalScale(figure.getLocalScale().getX() + tpf*pulsefactor, figure.getLocalScale().getY() + tpf*pulsefactor, figure.getLocalScale().getZ() + tpf*pulsefactor);
       if (figure.getLocalScale().getX() > 3.0f){
           pulsefactor = -pulsefactor;
       }
       if(figure.getLocalScale().getX() <= 1.0f){
           pulsefactor = -pulsefactor;
       }
    }
   
 
    protected Spatial makeFloor() {
        Spatial scenefile = assetManager.loadModel("Models/Scenes/newScene.j3o");
        rootNode.attachChild(scenefile);
        
        CollisionShape groundShape = CollisionShapeFactory.createMeshShape((Node) scenefile);
        
        RigidBodyControl groundControl = new RigidBodyControl(groundShape, 0);
        bulletAppState.getPhysicsSpace().add(groundControl);
     
        
        
   /* Box box = new Box(256, .2f, 256);
    Geometry floor = new Geometry("the Floor", box);
    floor.setLocalTranslation(0, 0, 0);
    Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    mat1.setColor("Color", ColorRGBA.Brown);
    floor.setMaterial(mat1);*/
    return scenefile;
  }
    
    public void foodstepsCheck(){
        if (isWalking == false){
            audio_foodsteps.stop();
        }   
    }

    public void showHUD(float tpf){
        startTime = System.currentTimeMillis();
        textField.setText("You have collected " + itemsCollected + "/" + ITEMNUMBER + " items.");
        guiNode.attachChild(textField);
    }
    public void showHUD(float tpf, String text){
        startTime = System.currentTimeMillis();
        textField.setText("" + text);
        guiNode.attachChild(textField);
    }
    
    public void fadeHUD(float tpf){
         if (startTime == 0)
             return;
         long time = System.currentTimeMillis();
         float t = ((float) (time - startTime))/FADETIME;
         System.out.println(t);
         if(t > 1){
             startTime = 0;
             return;
         }
         float colorValue = 1-t;
         color.a = colorValue;
         textField.setColor(color);
     }         
     
 
       
     // INIT METHODS
     
    public void initAudio(){
        // Background audio
       audio_theme = new AudioNode(assetManager, "Sounds/horror_theme_01.wav", true); 
       audio_theme.setPositional(false);
       audio_theme.setLooping(false);
       audio_theme.setVolume(0.5f);
       
       rootNode.attachChild(audio_theme);
       audio_theme.play();
       
       // Sound FX  
       audio_foodsteps = new AudioNode(assetManager, "Sounds/sound_fx_foodsteps1.wav", false);
       audio_foodsteps.setPositional(false);
       audio_foodsteps.setLooping(true);
       audio_foodsteps.setVolume(0.2f);
       rootNode.attachChild(audio_foodsteps);
       
    }
      
    public void initSky(){
        Texture west = assetManager.loadTexture("Models/sky/purplenebula_bk.jpg");
        Texture east = assetManager.loadTexture("Models/sky/purplenebula_dn.jpg");
        Texture north = assetManager.loadTexture("Models/sky/purplenebula_ft.jpg");
        Texture south = assetManager.loadTexture("Models/sky/purplenebula_lf.jpg");
        Texture up = assetManager.loadTexture("Models/sky/purplenebula_rt.jpg");
        Texture down = assetManager.loadTexture("Models/sky/purplenebula_up.jpg");

        Spatial sky = SkyFactory.createSky(assetManager, west, east, north, south, up, down);
        rootNode.attachChild(sky);  
    }
     
    public void initHouses(){
       Spatial house = assetManager.loadModel("Models/Houses/Tree1.j3o");
        
       CollisionShape houseShape = CollisionShapeFactory.createMeshShape((Node) house);
       physicsNode = new RigidBodyControl(houseShape, 0);
       house.addControl(physicsNode);
       bulletAppState.getPhysicsSpace().add(house);
       physicsNode.setPhysicsLocation(new Vector3f(10, 0, 20));

       rootNode.attachChild(house);

    }
     
    public void initForest()
    {
        final int anzahlBaueme = 50;
        final float MAX_X_RANDOM = 2.0f;
        final float MAX_Z_RANDOM = 2.0f;
        Spatial [][] trees = new Spatial[anzahlBaueme][anzahlBaueme];
        Spatial tree = assetManager.loadModel("Models/Tree/Tree.mesh.j3o");
        tree.scale(1.0f, 5.0f, 1.0f);
        
        
        CollisionShape treeShape = new BoxCollisionShape(new Vector3f (0.3f, 10, 0.3f));

        for( int i = 0; i < trees.length; i++)
        {
            for(int j = 0; j < trees[i].length; j++)
            {
                trees[i][j] = tree.clone();
                rootNode.attachChild(trees[i][j]);
                float xrandom = (float)(Math.random()-0.3)*2.0f*MAX_X_RANDOM;
                
                float zrandom = (float)(Math.random()-0.3)*2.0f*MAX_Z_RANDOM;
                trees[i][j].setLocalTranslation((i-anzahlBaueme/2)*5.0f + xrandom,0f,(j-anzahlBaueme/2)*5.0f+zrandom);
                
                RigidBodyControl treeNode = new RigidBodyControl(treeShape, 0);
                trees[i][j].addControl(treeNode);
                bulletAppState.getPhysicsSpace().add(trees[i][j]);
                treeNode.setPhysicsLocation(trees[i][j].getLocalTranslation());  
            }
        }
    }
    public void initProgman()
    {
        Box box = new Box(0.5f, 0.5f, 0.5f);
        progman = new Geometry("box", box);
        Material mat1 = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat1.setColor("Color", ColorRGBA.White);
        progman.setMaterial(mat1);
        progman_pos = new Vector3f(PROGMAN_X,PROGMAN_Y,PROGMAN_Z);
        progman.setLocalTranslation(progman_pos);
        rootNode.attachChild(progman);
    }
    
    
    public void initListeners(){
        inputManager.addMapping("Move", new KeyTrigger(keyInput.KEY_W));
        inputManager.addMapping("Left", new KeyTrigger(keyInput.KEY_A));
        inputManager.addMapping("Back", new KeyTrigger(keyInput.KEY_S));
        inputManager.addMapping("Right", new KeyTrigger(keyInput.KEY_D));
        inputManager.addMapping("Jump", new KeyTrigger(keyInput.KEY_SPACE));
        inputManager.addMapping("Pause", new KeyTrigger(keyInput.KEY_P));

        inputManager.addListener(analogListener, "Move");
        inputManager.addListener(analogListener, "Left");
        inputManager.addListener(analogListener, "Back");
        inputManager.addListener(analogListener, "Right");

        inputManager.addListener(actionListener, "Pause");
        inputManager.addListener(actionListener, "Move");
        inputManager.addListener(actionListener, "Left");
        inputManager.addListener(actionListener, "Back");
        inputManager.addListener(actionListener, "Right");
        inputManager.addListener(actionListener, "Jump");


    }
    
    public void initPlayerPhysics(){
        CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(1.5f, 2f, 1);
        player = new CharacterControl(capsuleShape, 0.05f);
        player.setPhysicsLocation(new Vector3f(0, 2, 0));
        bulletAppState.getPhysicsSpace().add(player);
        player.setGravity(20);
        position = player.getPhysicsLocation();
        
        
        light = new PointLight();
        light.setPosition(player.getPhysicsLocation());
        rootNode.addLight(light);
        
        flash = assetManager.loadModel("Models/Flashlight/flashlight.j3o");
        flash.scale(1.5f);
        flash.setLocalTranslation(0f, 1.5f, 0f);
        rootNode.attachChild(flash);

    }
    
    public void createFog(){
        // Verwendung von exponentiellem Verhalten:
        // f = e^(-d*b) mit d = distance, b = attenuation
        // final color = (1.0 -  f) * fogColor + f * light Color
        // Rangebased technique -> Vertex to Camera
        // Vertex -> Dreiecke, Fragment -> Pixelweise
        // Vertexshader berechnet position und übergibt sie weiter an fragemnt shader
        /*
         * uniform -> User defined variables (global)
         * attribute -> Per vertex variables (position e.g)
         * varying -> Vertex shader to fragment shader variables
         */
        
        
        
        ColorRGBA fogColor = new ColorRGBA(0.5f, 0.5f, 0.5f, 1f);
        float d = 0; // Distance as range based calculation
        float b_density = 0.05f; // fog density
        float f = (float) Math.exp(-d * b_density);
        
        
        ColorRGBA finalColor = new ColorRGBA();
        float r = (float) (1.0 - f) * fogColor.r + f * light.getColor().r;
        float g = (float) (1.0 - f) * fogColor.g + f * light.getColor().g;
        float b = (float) (1.0 - f) * fogColor.b + f * light.getColor().b;

        finalColor.r = r;
        finalColor.g = g;
        finalColor.b = b;
    }
 
}
