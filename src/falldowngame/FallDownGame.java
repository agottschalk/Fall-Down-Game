/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package falldowngame;

import java.util.ArrayList;
import java.util.Random;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

/**
 *
 * @author agott
 */
public class FallDownGame extends Application {
    final static int SCREEN_WIDTH = 640;
    final static int SCREEN_HEIGHT = 480;
    
    final static double GRAVITY = .7;
    
    //platform constants
    //These are located in the main game class rather than in the platform
    //class since they are referred to frequently in drawing and collision
    //detection.  It's simply more convenient to place them here and not have to
    //refer to the platform class every time one needs to be accessed.
    final static int PLATFORM_START = 40;   //in the code this winds up being 40 pixels below the bottom of the screen
    final static int PLATFORM_STAGGER = 130;
    final static int GAP_WIDTH = 150;
    final static int MAX_PLATFORMS = 5;
    final static double RISING_SPEED = 5;
    
    final Random rand = new Random();
    
    //game objects
    Box player;
    ArrayList<Platform> platforms;
    
    int score;
    
    //true while playing, false if game over
    boolean running;
    
    @Override
    public void start(Stage primaryStage) {
        Canvas canvas = new Canvas(SCREEN_WIDTH, SCREEN_HEIGHT);
        Group root = new Group(canvas);
        Scene scene = new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        //set up text display so that coordinates refer to center of text box
        //helps with centering displays
        gc.setTextBaseline(VPos.CENTER);
        
        //event handlers
        scene.setOnKeyPressed(
        new EventHandler<KeyEvent>(){
            @Override
            public void handle(KeyEvent k){
                switch (k.getCode()){
                        case LEFT: player.left(true);
                            break;
                        case RIGHT: player.right(true);
                            break;
                        case ENTER:
                            if(!running){
                                reset();
                            }
                }
            }
        });
        
        scene.setOnKeyReleased(
        new EventHandler<KeyEvent>(){
            @Override
            public void handle(KeyEvent k){
                switch (k.getCode()){
                        case LEFT: player.left(false);
                            break;
                        case RIGHT: player.right(false);
                            break;
                }     
            }
        });
        
        //set up game
        reset();
        
        //game loop
        new AnimationTimer(){
            //loop goes inside this method
            @Override
            public void handle(long now){
                //if not game over, run game
                if(running){
                    //update positions
                    player.update();
                    for(int i=0; i<platforms.size(); i++){
                        platforms.get(i).update();
                    }

                    //score increases by 1 each frame player is alive
                    score++;

                    //check collisions
                    for(int i=0; i<platforms.size(); i++){
                        Platform p = platforms.get(i);
                        if(p.passed){
                            //if player has already passed platform, don't check
                            continue;
                        }else if(p.Ypos < (player.Ypos + player.height/2)){
                            //if player tries to move past a platform, check to
                            //see if they are in the gap
                            if((player.Xpos - player.width/4) > p.gapPosition
                                    && (player.Xpos + player.width/4) 
                                        < (p.gapPosition + GAP_WIDTH)){
                                //if so, mark the platform as passed
                                p.passed = true;
                                score += 123;
                            }else{
                                //otherwise stop on the platform
                                player.land(p.Ypos);
                            }
                        }
                    }
                    
                    //check for game over
                    if((player.Ypos + player.height/2) < 0){
                        running = false;
                    }
                        
                }
                
                //Drawing
                
                //draw background
                gc.setFill(Color.BLACK);
                gc.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
                
                //draw game objects
                //platforms
                gc.setFill(Color.BLUEVIOLET);
                for(int i=0; i<platforms.size(); i++){
                    Platform p = platforms.get(i);
                    gc.fillRect(0, p.Ypos, p.gapPosition, 10);
                    gc.fillRect(p.gapPosition + GAP_WIDTH, p.Ypos,
                        SCREEN_WIDTH - (GAP_WIDTH + p.gapPosition), 10);
                }
                
                //player
                gc.setFill(Color.CADETBLUE);
                gc.fillRect(
                    player.Xpos-(player.width/2), 
                    player.Ypos-(player.height/2), 
                    player.height, player.width);
                
                //score
                gc.setFill(Color.FLORALWHITE);
                gc.setTextAlign(TextAlignment.LEFT);
                gc.setFont(Font.font(24));
                gc.fillText("Score: " + score, 4, 16);
                
                //game over display
                if(!running){
                    gc.setFill(Color.RED);
                    gc.setTextAlign(TextAlignment.CENTER);
                    gc.setFont(Font.font(72));
                    gc.fillText("Game Over", SCREEN_WIDTH/2, SCREEN_HEIGHT/2);
                    
                    gc.setFill(Color.AQUA);
                    gc.setFont(Font.font(24));
                    gc.fillText("Press 'Enter' to start a new game", 
                            SCREEN_WIDTH/2, SCREEN_HEIGHT * 0.65);
                }
            }
        }.start();
        
        primaryStage.setTitle("Fall Down");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public void reset(){
        //initialize game objects
        player = new Box();
        platforms = new ArrayList<Platform>();
        for(int i=0; i<MAX_PLATFORMS; i++){
            platforms.add(new Platform(rand.nextInt(SCREEN_WIDTH - GAP_WIDTH), 
                SCREEN_HEIGHT + PLATFORM_START + i*PLATFORM_STAGGER));
        }
        
        //set score starting value and game state
        score = 0;
        running = true;
        
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    
    public class Box{
        //dimensions of the box for collision and drawing
        final int height = 30;
        final int width = 30;
        
        //refers to position of CENTER of box
        double Xpos;
        double Ypos;
        
        private double Xvel;
        private double Yvel;
        
        //track state of keyboard
        private boolean left = false;
        private boolean right = false;
        
        //maximum falling speed
        private final double MAX_SPEED = 8;
        
        //left and right movement speed
        private final double MOVE_SPEED = 6;
        
        public Box(){
            Xpos = SCREEN_WIDTH/2;  //starts center top of screen
            Ypos = 0;
            
            Xvel = 0;
            Yvel = 0;
        }
        
        public void update(){
            //adjust velocity based on gravitational acceleration
            Yvel += GRAVITY;
            if(Yvel > MAX_SPEED){
                Yvel = MAX_SPEED;
            }
            
            //determine x velocity based on key inputs
            Xvel = 0;
            if(left){Xvel -= MOVE_SPEED;}
            if(right){Xvel += MOVE_SPEED;}
            
            //adjust position based on velocity
            Xpos += Xvel;
            Ypos += Yvel;
            
            //check edge collision
            //bottom
            if ((Ypos + height/2) > SCREEN_HEIGHT){
                Ypos = SCREEN_HEIGHT - height/2;
                Yvel = 0;
            }
            //right side
            if((Xpos + width/2) > SCREEN_WIDTH){
                Xpos = SCREEN_WIDTH - width/2;
                Xvel = 0;
            }
            //left side
            if(Xpos < width/2){
                Xpos = width/2;
                Xvel = 0;
            }
        }
        
        //for setting keyboard state variables
        public void left(boolean b){left = b;}
        public void right(boolean b){right = b;}
        
        //called when box hits a platform
        //stops falling and sets box on top of platform
        public void land(double Y){
            Yvel = 0;
            Ypos = Y - (height/2);
        }
    }
    
    public class Platform{
        double Ypos;
        
        int gapPosition;
        
        boolean passed;
        
        public Platform(int gapPos, int startingYPosition){
            gapPosition = gapPos;
            Ypos = startingYPosition;
            passed = false;
        }
        
        public void update(){
            Ypos -= RISING_SPEED;   //- is up
            
            //when reaching top, reset
            //the long calculation in the conditional helps keep the platforms
            //evenly spaced
            if(Ypos < (SCREEN_HEIGHT + PLATFORM_START) - 
                    (PLATFORM_STAGGER * MAX_PLATFORMS)){
                Ypos = SCREEN_HEIGHT + PLATFORM_START;
                gapPosition = rand.nextInt(SCREEN_WIDTH - GAP_WIDTH);
                passed = false;
            }
        }
    }
}
