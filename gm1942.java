package wingman;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.net.URL;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import javax.sound.sampled.*;
import javax.swing.*;

// Main Game engine
public class gm1942 extends JApplet implements Runnable {

    private Thread thread; 
   
    private int x = 0, move = 0;
    private BufferedImage bimg;
    int speed = 1, score = 0, frame = 0;
    Random generator = new Random(1234567);
    Island I1, I2, I3;
    Plane1 m1;
    Plane2 m2;
    Boss b1;
    Items item;
    gameHUD hud;
    GameEvents gameEvents;
    Clip bgm;
    ArrayList pBullet = new ArrayList();
    ArrayList eBullet = new ArrayList();
    ArrayList eList = new ArrayList();
    ArrayList exList = new ArrayList();
    ArrayList iList = new ArrayList();
    ArrayList names = new ArrayList();
    ArrayList scores = new ArrayList();

    boolean gameOver, boss, startTimer, waveStart, gameScore;
    boolean p1dead, p2dead;
    long start, end, time, waveTimeStart, waveTimeEnd;
    ImageObserver observer;

    // initialize variables, files, sprites, level, before main game loop
    public void init() {
        setBackground(Color.white);
        Image island1, island2, island3;
        
        island1 = getSprite("Resources/island1.png");
        island2 = getSprite("Resources/island2.png");
        island3 = getSprite("Resources/island3.png");
        
        gameOver = false;
        observer = this;
        startTimer = true;
        waveStart = true;
        gameScore = false;
        p1dead = false;
        p2dead = false;

        I1 = new Island(island1, 100, 100, speed, generator);
        I2 = new Island(island2, 200, 400, speed, generator);
        I3 = new Island(island3, 300, 200, speed, generator);
        //item = new Items(400, -10);
        hud = new gameHUD();
        
        KeyControl key = new KeyControl();
        setFocusable(true);
        addKeyListener(key);
        gameEvents = new GameEvents();
        m1 = new Plane1(170, 360, 5);
        m2 = new Plane2(400, 360, 5);
        gameEvents.addObserver(m1);
        gameEvents.addObserver(m2);
        playSound("Resources/background.mid", 1);
        fillScores();
        
    }

    // island background objects
    public class Island {

        Image img;
        int x, y, speed;
        Random gen;

        Island(Image img, int x, int y, int speed, Random gen) {
            this.img = img;
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.gen = gen;
        }

        public void update(int w, int h) {
            y += speed;
            if (y >= h) {
                y = -100;
                x = Math.abs(gen.nextInt() % (w - 30));
            }
        }

        public void draw(Graphics g, ImageObserver obs) {
            g.drawImage(img, x, y, obs);
        }
    }
    
    // events listener
    public class GameEvents extends Observable {
       int type;
       int plane;
       Object event;
       
       public void setValue(KeyEvent e) {
              type = 1; // let's assume this mean key input. Should use CONSTANT value for this
              event = e;
              setChanged();
             // trigger notification
             notifyObservers(this);  
       }

       public void setValue(String msg, int plane) {
          type = 2; // let's assume this mean key input. Should use CONSTANT value for this
          event = msg;
          this.plane = plane;
          setChanged();
         // trigger notification
         notifyObservers(this);  
        }
       
       public void setValue(KeyEvent e, int n) {
          type = x; // let's assume this mean key input. Should use CONSTANT value for this
          event = e;
          setChanged();
         // trigger notification
         notifyObservers(this);  
        }
    }

    // key listener
    public class KeyControl extends KeyAdapter {

        public void keyPressed(KeyEvent e) {
            gameEvents.setValue(e);
        }
        public void keyReleased(KeyEvent e) {
            int n = 0;
            gameEvents.setValue(e, n);
        }
    }

    // player 1 plane
    public class Plane1 implements Observer {
        Image img1, img2, img3;
        int x, y, speed;
        int boom;
        int sizeX;
        int sizeY;
        int health, life;
        long start, end, time, deadTime, spawnTime;
        boolean left, right, up, down;
        boolean fireReleased;
        boolean startTimer, deadTimer;
        boolean powerUp, invincible;

        Plane1(int x, int y, int speed) {
            this.img1 = getSprite("Resources/myplane_1.png");
            this.img2 = getSprite("Resources/myplane_2.png");
            this.img3 = getSprite("Resources/myplane_3.png");
            this.x = x;
            this.y = y;
            this.sizeX = img1.getWidth(null);
            this.sizeY = img1.getHeight(null);
            this.speed = speed;
            health = 4;
            life = 2;
            boom = 0;
            left = right = up = down = false;
            fireReleased = true;
            startTimer = true;
            deadTimer = true;
            powerUp = false;
            invincible = false;
        }

        // draw each frame of sprite after a certain amount of time
        public void draw(Graphics g, ImageObserver obs) {
            if(boom == 0) {
                if (startTimer) {
                    start = System.currentTimeMillis();
                    startTimer = false;
                }
                end = System.currentTimeMillis();
                time = end - start;
                if (time <= 50) {
                    g.drawImage(img1, x, y, obs);
                }
                if (time > 50 && time <= 100) {
                    g.drawImage(img2, x, y, obs);
                }
                if (time > 100) {
                    g.drawImage(img3, x, y, obs);
                }
                if (time > 150) {
                    startTimer = true;
                }
            }
        }
        
        // collision algorithm checking
        public boolean collision(int x, int y, int w, int h, int offset) {
            if(this.x+sizeX >= x+offset && this.x <= x+w-offset 
                    && this.y+sizeY >= y+offset && this.y <= y+h-offset && boom == 0) {
                if (!invincible) {
                    health--;
                }
                if (health <= 0) {
                    gameEvents.setValue("Explosion", 1);
                }
                playSound("Resources/snd_explosion1.wav", 0);
                return true;
            }
            return false;
        }
      
        // check for key events and observer events
        public void update(Observable obj, Object arg) {
            GameEvents ge = (GameEvents) arg;
            if(ge.type == 1) {
                KeyEvent e = (KeyEvent) ge.event;
                if (boom == 0) {
                    switch (e.getKeyCode()) {    
                        case KeyEvent.VK_A:
                            System.out.println("Left Pressed");
                            left = true;
                            break; 
                        case KeyEvent.VK_D:
                            System.out.println("Right Pressed");
                            right = true;
                            break;
                        case KeyEvent.VK_W:
                            System.out.println("Up Pressed");
                            up = true;
                            break;
                        case KeyEvent.VK_S:
                            System.out.println("Down Pressed");
                            down = true;
                            break;
                        default:
                      if(e.getKeyChar() == ' ') {
                            if (fireReleased) {
                                System.out.println("Fire Press");
                                PlayerBullet bullet = new PlayerBullet(x, y, 1);
                                pBullet.add(bullet);
                                if (powerUp) {
                                    PlayerBullet bullet1 = new PlayerBullet(x, y, 2);
                                    PlayerBullet bullet2 = new PlayerBullet(x, y, 3);
                                    pBullet.add(bullet1);
                                    pBullet.add(bullet2);
                                }
                                fireReleased = false;
                            }
                      }
                    }
                }
            }
            else if(ge.type == 0) {
                KeyEvent e = (KeyEvent) ge.event;
                if (boom == 0) {
                    switch (e.getKeyCode()) {    
                        case KeyEvent.VK_A:
                            System.out.println("Left Released");
                            left = false;
                            break; 
                        case KeyEvent.VK_D:
                            System.out.println("Right Released");
                            right = false;
                            break;
                        case KeyEvent.VK_W:
                            System.out.println("Up Released");
                            up = false;
                            break;
                        case KeyEvent.VK_S:
                            System.out.println("Down Released");
                            down = false;
                            break;
                        default:
                            if(e.getKeyChar() == ' ') {
                                fireReleased = true;
                                System.out.println("Fire released");
                            }
                    }
                }
            }
            else if(ge.type == 2 && ge.plane == 1) {
                String msg = (String)ge.event;
                if(msg.equals("Explosion")) {
                    boom = 1;
                    life--;
                    invincible = true;
                    playSound("Resources/snd_explosion2.wav", 0);
                    exList.add(new Explosion(this.x, this.y, 2));
                }
            }
        }
        
        // update plane position, lives, deaths, etc 
        public void update() {
            if (boom == 0 && invincible && ((System.currentTimeMillis() / 1000) - spawnTime) > 0) {
                invincible = false;
            }
            
            // check for user keypresses and move plane accordingly
            if (left) { x -= speed; }
            if (right) { x += speed; }
            if (up) { y -= speed; }
            if (down) { y += speed; }
            
            // set plane boundaries
            if (x > 573) { x = 573; }
            if (x < -5) { x = -5; }
            if (y > 398) { y = 398; }
            if (y < -13) { y = -13; }
            
            // check for player death and respawn time
            if (boom == 1) {
                if (deadTimer) {
                    deadTime = System.currentTimeMillis() / 1000;
                    deadTimer = false;
                }
                if ( ((System.currentTimeMillis() / 1000) - deadTime) > 2
                        && life >= 0) {
                    boom = 0;
                    x = 170;
                    y = 360;
                    health = 4;
                    left = right = up = down = false;
                    powerUp = false;
                    deadTimer = true;
                    spawnTime = System.currentTimeMillis() / 1000;
                } else if (life < 0) {
                    p1dead = true;
                }
            }
            
            // check for player and enemy bullet collision
            for (int i=0; i<eBullet.size(); i++) {
                Bullet eneBullet = (Bullet) eBullet.get(i);
                if (health > 0) {
                    if (eneBullet.collision(x, y, sizeX, sizeY, 17)) {
                        if (!invincible) {
                            health--;
                        }
                        if (health <= 0) {
                            gameEvents.setValue("Explosion", 1);
                        }
                    }
                }
            }
            
            // check for player and item collision
            if (boom == 0) {
                for (int i=0; i<iList.size(); i++) {
                    Items i1 = (Items) iList.get(i);
                    if (i1.collision(x, y, sizeX, sizeY, 10)) {
                        powerUp = true;
                    }
                }
            }
        }
    }
    
    // player 2 plane
    public class Plane2 implements Observer {
        Image img1, img2, img3;
        int x, y, speed;
        int boom;
        int sizeX;
        int sizeY;
        int health, life;
        long start, end, time, deadTime, spawnTime;
        boolean left, right, up, down;
        boolean fireReleased;
        boolean startTimer, deadTimer;
        boolean powerUp, invincible;

        Plane2(int x, int y, int speed) {
            this.img1 = getSprite("Resources/plane2_1.png");
            this.img2 = getSprite("Resources/plane2_2.png");
            this.img3 = getSprite("Resources/plane2_3.png");
            this.x = x;
            this.y = y;
            this.sizeX = img1.getWidth(null);
            this.sizeY = img1.getHeight(null);
            this.speed = speed;
            health = 4;
            life = 2;
            boom = 0;
            left = right = up = down = false;
            fireReleased = true;
            startTimer = true;
            deadTimer = true;
            powerUp = false;
            invincible = false;
        }

        // draw sprite animation
        public void draw(Graphics g, ImageObserver obs) {
            if(boom == 0) {
                if (startTimer) {
                    start = System.currentTimeMillis();
                    startTimer = false;
                }
                end = System.currentTimeMillis();
                time = end - start;
                if (time <= 50) {
                    g.drawImage(img1, x, y, obs);
                }
                if (time > 50 && time <= 100) {
                    g.drawImage(img2, x, y, obs);
                }
                if (time > 100) {
                    g.drawImage(img3, x, y, obs);
                }
                if (time > 150) {
                    startTimer = true;
                }
            }
        }
        
        // check for collision of plane 2
        public boolean collision(int x, int y, int w, int h, int offset) {
            if(this.x+sizeX >= x+offset && this.x <= x+w-offset 
                    && this.y+sizeY >= y+offset && this.y <= y+h-offset && boom == 0) {
                if(!invincible) {
                    health--;
                }
                if (health <= 0) {
                    gameEvents.setValue("Explosion", 2);
                }
                playSound("Resources/snd_explosion1.wav", 0);
                return true;
            }
            return false;
        }
      
        // check for keyevents and game events
        public void update(Observable obj, Object arg) {
            GameEvents ge = (GameEvents) arg;
            if(ge.type == 1) {
                KeyEvent e = (KeyEvent) ge.event;
                if (boom == 0) {
                    switch (e.getKeyCode()) {    
                        case KeyEvent.VK_LEFT:
                            System.out.println("Left Pressed");
                            left = true;
                            break; 
                        case KeyEvent.VK_RIGHT:
                            System.out.println("Right Pressed");
                            right = true;
                            break;
                        case KeyEvent.VK_UP:
                            System.out.println("Up Pressed");
                            up = true;
                            break;
                        case KeyEvent.VK_DOWN:
                            System.out.println("Down Pressed");
                            down = true;
                            break;
                        default:
                      if(e.getKeyChar() == '\n') {
                            if (fireReleased) {
                                System.out.println("Fire Press");
                                PlayerBullet bullet = new PlayerBullet(x, y, 1);
                                pBullet.add(bullet);
                                if (powerUp) {
                                    PlayerBullet bullet1 = new PlayerBullet(x, y, 2);
                                    PlayerBullet bullet2 = new PlayerBullet(x, y, 3);
                                    pBullet.add(bullet1);
                                    pBullet.add(bullet2);
                                }
                                fireReleased = false;
                            }
                      }
                    }
                }
            }
            else if(ge.type == 0) {
                KeyEvent e = (KeyEvent) ge.event;
                if (boom == 0) {
                    switch (e.getKeyCode()) {    
                        case KeyEvent.VK_LEFT:
                            System.out.println("Left Released");
                            left = false;
                            break; 
                        case KeyEvent.VK_RIGHT:
                            System.out.println("Right Released");
                            right = false;
                            break;
                        case KeyEvent.VK_UP:
                            System.out.println("Up Released");
                            up = false;
                            break;
                        case KeyEvent.VK_DOWN:
                            System.out.println("Down Released");
                            down = false;
                            break;
                        default:
                            if(e.getKeyChar() == '\n') {
                                fireReleased = true;
                                System.out.println("Fire released");
                            }
                    }
                }
            }
            else if(ge.type == 2 && ge.plane == 2) {
                String msg = (String)ge.event;
                if(msg.equals("Explosion")) {
                    boom = 1;
                    life--;
                    invincible = true;
                    playSound("Resources/snd_explosion2.wav", 0);
                    exList.add(new Explosion(this.x, this.y, 2));
                }
            }
        }
        
        // update player position, lives, health, etc
        public void update() {
            if (boom == 0 && invincible && ((System.currentTimeMillis() / 1000) - spawnTime) > 0) {
                invincible = false;
            }
            
            // check for user key press to move plane
            if (left) { x -= speed; }
            if (right) { x += speed; }
            if (up) { y -= speed; }
            if (down) { y += speed; }
            
            // set plane boundary
            if (x > 573) { x = 573; }
            if (x < -5) { x = -5; }
            if (y > 398) { y = 398; }
            if (y < -13) { y = -13; }
            
            // check if plane is dead and respawn plane after time has passed
            if (boom == 1) {
                if (deadTimer) {
                    deadTime = System.currentTimeMillis() / 1000;
                    deadTimer = false;
                }
                if ( ((System.currentTimeMillis() / 1000) - deadTime) > 2
                        && life >= 0) {
                    boom = 0;
                    x = 400;
                    y = 360;
                    health = 4;
                    powerUp = false;
                    left = right = up = down = false;
                    deadTimer = true;
                    spawnTime = System.currentTimeMillis() / 1000; 
                } else if (life < 0) {
                    p2dead = true;
                }
            }
            
            // check for plane collision with bullets
            for (int i=0; i<eBullet.size(); i++) {
                Bullet eneBullet = (Bullet) eBullet.get(i);
                if (health > 0) {
                    if (eneBullet.collision(x, y, sizeX, sizeY, 17)) {
                        if(!invincible) {
                            health--;
                        }
                        if (health <= 0) {
                            gameEvents.setValue("Explosion", 2);
                        }
                    }
                }
            }
            
            // check for plane collision with items
            if (boom == 0) {
                for (int i=0; i<iList.size(); i++) {
                    Items i1 = (Items) iList.get(i);
                    if (i1.collision(x, y, sizeX, sizeY, 10)) {
                        powerUp = true;
                    }
                }
            }
        }
    }
    
    public interface Enemy {

        public void update();

        public void draw(Graphics g, ImageObserver obs);
        
        public boolean isShow();
    }

    // basic enemy flies from top to bottom
    public class Enemy1 implements Enemy{

        Image img1, img2, img3;
        int x, y, sizeX, sizeY, speed;
        boolean show;
        long start, end, time;
        boolean startTimer = true;
   
        Enemy1(int speed, int x, int y) {
            this.img1 = getSprite("Resources/enemy1_1.png");
            this.img2 = getSprite("Resources/enemy1_2.png");
            this.img3 = getSprite("Resources/enemy1_3.png");
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.show = true;
            sizeX = img1.getWidth(null);
            sizeY = img1.getHeight(null);
            System.out.println("w:" + sizeX + " y:" + sizeY);
       }

        public void update() {
            if (y > 500) {
                show = false;
            }
            y += speed;
            if(m1.collision(x, y, sizeX, sizeY, 10)) {
                show = false;
            }
            if(m2.collision(x, y, sizeX, sizeY, 10)) {
                show = false;
            }
            for (int i=0; i<pBullet.size(); i++) {
                PlayerBullet p1Bullet = (PlayerBullet) pBullet.get(i);
                if (p1Bullet.collision(x, y, sizeX, sizeY, 10)) {
                    show = false;
                    score += 25;
                }
            }
            if (!show) {
                exList.add(new Explosion(this.x, this.y, 1));
            }
        }

        public void draw(Graphics g, ImageObserver obs) {
            if (show) {
                if (startTimer) {
                    start = System.currentTimeMillis();
                    startTimer = false;
                }
                end = System.currentTimeMillis();
                time = end - start;
                if (time <= 50) {
                    g.drawImage(img1, x, y, obs);
                }
                if (time > 50 && time <= 100) {
                    g.drawImage(img2, x, y, obs);
                }
                if (time > 100) {
                    g.drawImage(img3, x, y, obs);
                }
                if (time > 150) {
                    startTimer = true;
                }
            }
        }
        
        public boolean isShow() {
            return this.show;
        }
    }
    
    public class Enemy2 implements Enemy{

        Image img1, img2, img3, img4, img5, img6, img7, img8, img9;
        int type;
        int x, y, sizeX, sizeY, speed;
        boolean show;
        boolean startTimer = true;
        long start, end, time;
   
        Enemy2(int speed, int x, int y, int type) {
            this.img1 = getSprite("Resources/enemy2_1.png");
            this.img2 = getSprite("Resources/enemy2_2.png");
            this.img3 = getSprite("Resources/enemy2_3.png");
            this.img4 = getSprite("Resources/enemy2_4.png");
            this.img5 = getSprite("Resources/enemy2_5.png");
            this.img6 = getSprite("Resources/enemy2_6.png");
            this.img7 = getSprite("Resources/enemy2_7.png");
            this.img8 = getSprite("Resources/enemy2_8.png");
            this.img9 = getSprite("Resources/enemy2_9.png");
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.type = type;
            this.show = true;
            sizeX = img1.getWidth(null);
            sizeY = img1.getHeight(null);
            System.out.println("w:" + sizeX + " y:" + sizeY);
       }

        public void update() {
            if (y > 500 || (type == 2 && x < -50) || (type == 3 && x > 640) ) {
                show = false;
            }
            if (type == 1) {
                y += speed;
                if (generator.nextInt(100) == 1) {
                    EnemyBullet eB = new EnemyBullet(x, y, 3, 3, 2);
                    eBullet.add(eB);
                }
            } else if (type == 2) {
                x -= speed;
            } else {
                x += speed;
            }
            if(m1.collision(x, y, sizeX, sizeY, 10)) {
                show = false;
            }
            if(m2.collision(x, y, sizeX, sizeY, 10)) {
                show = false;
            }
            for (int i=0; i<pBullet.size(); i++) {
                PlayerBullet p1Bullet = (PlayerBullet) pBullet.get(i);
                if (p1Bullet.collision(x, y, sizeX, sizeY, 10)) {
                    show = false;
                    score += 25;
                }
            }
            if (!show) {
                exList.add(new Explosion(this.x, this.y, 1));
            }
        }

        public void draw(Graphics g, ImageObserver obs) {
            if (show) {
                if (type == 1) {
                    if (startTimer) {
                        start = System.currentTimeMillis();
                        startTimer = false;
                    }
                    end = System.currentTimeMillis();
                    time = end - start;
                    if (time <= 50) {
                        g.drawImage(img1, x, y, obs);
                    }
                    if (time > 50 && time <= 100) {
                        g.drawImage(img2, x, y, obs);
                    }
                    if (time > 100) {
                        g.drawImage(img3, x, y, obs);
                    }
                    if (time > 150) {
                        startTimer = true;
                    }
                } else if (type == 2) {
                    if (startTimer) {
                        start = System.currentTimeMillis();
                        startTimer = false;
                    }
                    end = System.currentTimeMillis();
                    time = end - start;
                    if (time <= 50) {
                        g.drawImage(img4, x, y, obs);
                    }
                    if (time > 50 && time <= 100) {
                        g.drawImage(img5, x, y, obs);
                    }
                    if (time > 100) {
                        g.drawImage(img6, x, y, obs);
                    }
                    if (time > 150) {
                        startTimer = true;
                    }
                } else {
                    if (startTimer) {
                        start = System.currentTimeMillis();
                        startTimer = false;
                    }
                    end = System.currentTimeMillis();
                    time = end - start;
                    if (time <= 50) {
                        g.drawImage(img7, x, y, obs);
                    }
                    if (time > 50 && time <= 100) {
                        g.drawImage(img8, x, y, obs);
                    }
                    if (time > 100) {
                        g.drawImage(img9, x, y, obs);
                    }
                    if (time > 150) {
                        startTimer = true;
                    }
                }
            }
        }
        
        public boolean isShow() {
            return this.show;
        }
    }
    
    public class Enemy3 implements Enemy {

        Image img1, img2, img3;
        int x, y, sizeX, sizeY, speed;
        boolean show;
        boolean startTimer = true;
        long start, end, time;
   
        Enemy3(int speed, int x, int y) {
            this.img1 = getSprite("Resources/enemy3_1.png");
            this.img2 = getSprite("Resources/enemy3_2.png");
            this.img3 = getSprite("Resources/enemy3_3.png");
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.show = true;
            sizeX = img1.getWidth(null);
            sizeY = img1.getHeight(null);
            System.out.println("w:" + sizeX + " y:" + sizeY);
       }

        public void update() {
            if (y > 500) {
                show = false;
            }
            y += speed;
            if (generator.nextInt(100) == 1) {
                EnemyBullet eB = new EnemyBullet(x, y, 3, 3, 1);
                eBullet.add(eB);
            }
            if(m1.collision(x, y, sizeX, sizeY, 10)) {
                show = false;
            }
            if(m2.collision(x, y, sizeX, sizeY, 10)) {
                show = false;
            }
            for (int i=0; i<pBullet.size(); i++) {
                PlayerBullet p1Bullet = (PlayerBullet) pBullet.get(i);
                if (p1Bullet.collision(x, y, sizeX, sizeY, 10)) {
                    show = false;
                    score += 50;
                }
            }
            if (!show) {
                exList.add(new Explosion(this.x, this.y, 1));
            }
        }

        public void draw(Graphics g, ImageObserver obs) {
            if (show) {
                if (startTimer) {
                    start = System.currentTimeMillis();
                    startTimer = false;
                }
                end = System.currentTimeMillis();
                time = end - start;
                if (time <= 50) {
                    g.drawImage(img1, x, y, obs);
                }
                if (time > 50 && time <= 100) {
                    g.drawImage(img2, x, y, obs);
                }
                if (time > 100) {
                    g.drawImage(img3, x, y, obs);
                }
                if (time > 150) {
                    startTimer = true;
                }
            }
        }
        
        public boolean isShow() {
            return this.show;
        }
    }
    
    public class Enemy4 implements Enemy {

        Image img1, img2, img3;
        int x, y, sizeX, sizeY, speed;
        boolean show;
        boolean startTimer = true;
        long start, end, time;
   
        Enemy4(int speed, int x, int y) {
            this.img1 = getSprite("Resources/enemy4_1.png");
            this.img2 = getSprite("Resources/enemy4_2.png");
            this.img3 = getSprite("Resources/enemy4_3.png");
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.show = true;
            sizeX = img1.getWidth(null);
            sizeY = img1.getHeight(null);
            System.out.println("w:" + sizeX + " y:" + sizeY);
       }

        public void update() {
            if (y < -100) {
                show = false;
            }
            y -= speed;
            if(m1.collision(x, y, sizeX, sizeY, 10)) {
                show = false;
            }
            if(m2.collision(x, y, sizeX, sizeY, 10)) {
                show = false;
            }
            for (int i=0; i<pBullet.size(); i++) {
                PlayerBullet p1Bullet = (PlayerBullet) pBullet.get(i);
                if (p1Bullet.collision(x, y, sizeX, sizeY, 10)) {
                    show = false;
                    score += 75;
                }
            }
            if (!show) {
                exList.add(new Explosion(this.x, this.y, 1));
            }
        }

        public void draw(Graphics g, ImageObserver obs) {
            if (show) {
                if (startTimer) {
                    start = System.currentTimeMillis();
                    startTimer = false;
                }
                end = System.currentTimeMillis();
                time = end - start;
                if (time <= 50) {
                    g.drawImage(img1, x, y, obs);
                }
                if (time > 50 && time <= 100) {
                    g.drawImage(img2, x, y, obs);
                }
                if (time > 100) {
                    g.drawImage(img3, x, y, obs);
                }
                if (time > 150) {
                    startTimer = true;
                }
            }
        }
        
        public boolean isShow() {
            return this.show;
        }
    }
    
    public class Boss implements Enemy {

        Image img, hit;
        int x, y, sizeX, sizeY, speed;
        int health;
        boolean show, isHit = false;
   
        Boss(int speed, int x, int y) {
            this.img = getSprite("Resources/bossBig1.png");
            this.hit = getSprite("Resources/bossPlanehit.png");
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.show = true;
            this.health = 100;
            sizeX = img.getWidth(null);
            sizeY = img.getHeight(null);
            System.out.println("w:" + sizeX + " y:" + sizeY);
       }

        public void update() {
            if (y > 500) {
                show = false;
            }
            if (y < 20) {
                y += speed;
            }
            if (generator.nextInt(20) == 1) {
                EnemyBullet eB1 = new EnemyBullet(x+83, y+(sizeY/2), -3, 0, 3);
                EnemyBullet eB2 = new EnemyBullet(x+83, y+(sizeY/2), -3, 3, 3);
                EnemyBullet eB3 = new EnemyBullet(x+83, y+(sizeY/2), 0, 3, 3);
                EnemyBullet eB4 = new EnemyBullet(x+83, y+(sizeY/2), 3, 3, 3);
                EnemyBullet eB5 = new EnemyBullet(x+83, y+(sizeY/2), 3, 0, 3);
                eBullet.add(eB1);
                eBullet.add(eB2);
                eBullet.add(eB3);
                eBullet.add(eB4);
                eBullet.add(eB5);
            }
            if (generator.nextInt(100) == 1) {
                EnemyBullet eB = new EnemyBullet(x+83, y+(sizeY/2), 3, 3, 2);
                eBullet.add(eB);
            }
            if(m1.collision(x, y, sizeX, sizeY-110, 10) ||
                    m1.collision(x+70, y, sizeX-140, sizeY-20, 10)) {
                health--;
                isHit = true;
            }
            if(m2.collision(x, y, sizeX, sizeY-110, 10) ||
                    m2.collision(x+70, y, sizeX-140, sizeY-20, 10)) {
                health--;
                isHit = true;
            }
            for (int i=0; i<pBullet.size(); i++) {
                PlayerBullet p1Bullet = (PlayerBullet) pBullet.get(i);
                if ( p1Bullet.collision(x, y, sizeX, sizeY-110, 10) ||
                     p1Bullet.collision(x+70, y, sizeX-140, sizeY-20, 10) ) {
                    health--;
                    isHit = true;
                    exList.add(new Explosion(p1Bullet.x, p1Bullet.y, 1));
                }
            }
            if (health < 0) {
                show = false;
                score += 300;
            }
            if (!show) {
                playSound("Resources/snd_explosion2.wav", 0);
                exList.add(new Explosion(this.x, this.y, 3));
                gameOver = true;
            }
        }

        public void draw(Graphics g, ImageObserver obs) {
            if (show  && !isHit) {
                g.drawImage(img, x, y, obs);
            }
            if (isHit) {
                g.drawImage(hit, x, y, obs);
                isHit = false;
            }
        }
        
        public boolean isShow() {
            return this.show;
        }
    }
    
    interface Bullet {
        public void update();
        public void draw(Graphics g, ImageObserver obs);
        public boolean show();
        public boolean collision(int x, int y, int w, int h, int offset);
    }
    
    public class PlayerBullet implements Bullet {

        Image img1, img2, img3;
        int x, y, sizeX, sizeY, speed;
        int type;
        Random gen;
        boolean show;
   
        PlayerBullet(int x, int y, int type) {
            this.img1 = getSprite("Resources/bullet.png");
            this.img2 = getSprite("Resources/bulletLeft.png");
            this.img3 = getSprite("Resources/bulletRight.png");
            this.x = x + 17;
            this.y = y;
            this.speed = 8;
            this.show = true;
            this.type = type;
            sizeX = img1.getWidth(null);
            sizeY = img1.getHeight(null);
            System.out.println("PlayerBullet at x: " + x + " y: " + y);
       }

        public void update() {
            if (type == 1) {
                if (y < -2) { show = false; }
                y -= speed;
            } else if (type == 2) {
                if (y < -2 || x < -2) {show = false; }
                y -= speed;
                x -= speed;
            } else if (type == 3) {
                if (y < -2 || x > 642) {show = false; }
                y -= speed;
                x += speed;
            }
        }

        public void draw(Graphics g, ImageObserver obs) {
            if (show) {
                if (type == 1) {
                    g.drawImage(img1, x, y, obs);
                } else if (type == 2) {
                    g.drawImage(img2, x-5, y, obs);
                } else if (type == 3) {
                    g.drawImage(img3, x-14, y-5, obs);
                }
            }
        }
        
        public boolean show() {
            return show;
        }
        
        public boolean collision(int x, int y, int w, int h, int offset) {
            if(this.x+sizeX >= x+offset && this.x <= x+w-offset 
                    && this.y <= y+h-offset && this.y+sizeY >= y+offset) {
                this.show = false;
                playSound("Resources/snd_explosion1.wav", 0);
                return true;
            }
            return false;
        }
    }
    
    public class EnemyBullet implements Bullet {

        Image img1, img2, img3;
        int type;
        double x, y, sizeX, sizeY, speedX, speedY;
        Random gen;
        boolean show, fired;
   
        EnemyBullet(int x, int y, int speedX, int speedY, int type) {
            this.img1 = getSprite("Resources/enemybullet1.png");
            this.img2 = getSprite("Resources/enemybullet2.png");
            this.img3 = getSprite("Resources/bigBullet1.png");
            this.x = x;
            this.y = y + 10;
            this.speedX = speedX;
            this.speedY = speedY;
            this.show = true;
            this.type = type;
            fired = false;
            sizeX = img1.getWidth(null);
            sizeY = img1.getHeight(null);
            System.out.println("PlayerBullet at x: " + x + " y: " + y);
       }

        public void update() {
            if (type == 1) {
                if (y > 480) { show = false; }
                y += speedY;
            } else if (type == 2) {
                if (y > 480 || x > 640 || x < 0) { show = false; }
                if (!fired && generator.nextInt(10)%2 == 0) {
                    fired = true;
                    speedX = (m1.x - x);
                    speedY = (m1.y - y);
                    System.out.println("speedX: " + speedX + " speedY: " + speedY);
                } else if (!fired) {
                    fired = true;
                    speedX = (m2.x - x);
                    speedY = (m2.y - y); 
                }
                x += speedX/90;
                y += speedY/90;
            } else if (type == 3) {
                x += speedX;
                y += speedY;
            }
        }

        public void draw(Graphics g, ImageObserver obs) {
            if (show) {
                if (type == 1) {
                    g.drawImage(img1, (int)x, (int)y, (int)sizeX, (int)sizeY, obs);
                } else if (type == 2) {
                    g.drawImage(img2, (int)x, (int)y, (int)sizeX, (int)sizeY, obs);
                } else {
                    g.drawImage(img3, (int)x, (int)y, (int)sizeX, (int)sizeY, obs);
                }
            }
        }
        
        public boolean show() {
            return show;
        }
        
        public boolean collision(int x, int y, int w, int h, int offset) {
            if(this.x+sizeX >= x+offset && this.x <= x+w-offset 
                    && this.y <= y+h-offset && this.y+sizeY >= y+offset) {
                this.show = false;
                playSound("Resources/snd_explosion1.wav", 0);
                return true;
            }
            return false;
        }
    }
    
    public class Explosion {

        Image img1, img2, img3, img4, img5, img6;
        Image imgE1, imgE2, imgE3, imgE4, imgE5, imgE6, imgE7;
        int x, y, sizeX, sizeY;
        int type;
        long start, end, time;
        boolean show, startTimer = true;
   
        Explosion(int x, int y, int type) {
            this.img1 = getSprite("Resources/explosion1_1.png");
            this.img2 = getSprite("Resources/explosion1_2.png");
            this.img3 = getSprite("Resources/explosion1_3.png");
            this.img4 = getSprite("Resources/explosion1_4.png");
            this.img5 = getSprite("Resources/explosion1_5.png");
            this.img6 = getSprite("Resources/explosion1_6.png");
            this.imgE1 = getSprite("Resources/explosion2_1.png");
            this.imgE2 = getSprite("Resources/explosion2_2.png");
            this.imgE3 = getSprite("Resources/explosion2_3.png");
            this.imgE4 = getSprite("Resources/explosion2_4.png");
            this.imgE5 = getSprite("Resources/explosion2_5.png");
            this.imgE6 = getSprite("Resources/explosion2_6.png");
            this.imgE7 = getSprite("Resources/explosion2_7.png");
            this.x = x;
            this.y = y;
            sizeX = imgE1.getWidth(null);
            sizeY = imgE1.getHeight(null);
            this.type = type;
            this.show = true;
            System.out.println("Explosion at x: " + x + " y: " + y);
       }

        public void update() {
            if (y < -10) { show = false; }
            y -= speed;
        }

        public void draw(Graphics g, ImageObserver obs) {
            if (show) {
                if (type == 1) {
                    if (startTimer) {
                        start = System.currentTimeMillis();
                        startTimer = false;
                    }
                    end = System.currentTimeMillis();
                    time = end - start;
                    if (time <= 50) {
                        g.drawImage(img1, x, y, obs);
                    }
                    if (time > 50 && time <= 100) {
                        g.drawImage(img2, x, y, obs);
                    }
                    if (time > 100 && time <= 150) {
                        g.drawImage(img3, x, y, obs);
                    }
                    if (time > 150 && time <= 200) {
                        g.drawImage(img4, x, y, obs);
                    }
                    if (time > 200 && time <= 250) {
                        g.drawImage(img5, x, y, obs);
                    }
                    if (time > 250 && time <= 300) {
                        g.drawImage(img6, x, y, obs);
                    }
                    if (time > 300 && time <= 350) {
                        show = false;
                        startTimer = true;
                    }
                } else if (type==2) {
                    if (startTimer) {
                        start = System.currentTimeMillis();
                        startTimer = false;
                    }
                    end = System.currentTimeMillis();
                    time = end - start;
                    if (time <= 50) {
                        g.drawImage(imgE1, x, y, obs);
                    }
                    if (time > 50 && time <= 100) {
                        g.drawImage(imgE2, x, y, obs);
                    }
                    if (time > 100 && time <= 150) {
                        g.drawImage(imgE3, x, y, obs);
                    }
                    if (time > 150 && time <= 200) {
                        g.drawImage(imgE4, x, y, obs);
                    }
                    if (time > 200 && time <= 250) {
                        g.drawImage(imgE5, x, y, obs);
                    }
                    if (time > 250 && time <= 300) {
                        g.drawImage(imgE6, x, y, obs);
                    }
                    if (time > 300 && time <= 350) {
                        g.drawImage(imgE7, x, y, obs);
                        show = false;
                        startTimer = true;
                    }
                    if (time > 350 && time <= 400) {
                        show = false;
                        startTimer = true;
                    }
                } else {
                    if (startTimer) {
                        start = System.currentTimeMillis();
                        startTimer = false;
                    }
                    end = System.currentTimeMillis();
                    time = end - start;
                    if (time <= 50) {
                        g.drawImage(imgE1, x-10, y, sizeX*4, sizeY*4, obs);
                    }
                    if (time > 50 && time <= 100) {
                        g.drawImage(imgE2, x-10, y, sizeX*4, sizeY*4, obs);
                    }
                    if (time > 100 && time <= 150) {
                        g.drawImage(imgE3, x-10, y, sizeX*4, sizeY*4, obs);
                    }
                    if (time > 150 && time <= 200) {
                        g.drawImage(imgE4, x-10, y, sizeX*4, sizeY*4, obs);
                    }
                    if (time > 200 && time <= 250) {
                        g.drawImage(imgE5, x-10, y, sizeX*4, sizeY*4, obs);
                    }
                    if (time > 250 && time <= 300) {
                        g.drawImage(imgE6, x-10, y, sizeX*4, sizeY*4, obs);
                    }
                    if (time > 300 && time <= 350) {
                        g.drawImage(imgE7, x-10, y, sizeX*4, sizeY*4, obs);
                        show = false;
                        startTimer = true;
                    }
                    if (time > 350 && time <= 400) {
                        show = false;
                        startTimer = true;
                    }
                }
            }
        }
    }
    
    public class Items {

        Image img;
        int x, y, sizeX, sizeY, speedX, speedY;
        Random gen;
        boolean show;
   
        Items(int x, int y) {
            this.img = getSprite("Resources/powerup.png");
            this.x = x;
            this.y = y;
            this.show = true;
            this.speedX = 2;
            this.speedY = 2;
            sizeX = img.getWidth(null);
            sizeY = img.getHeight(null);
            System.out.println("Item at x: " + x + " y: " + y);
       }

        public void update() {
            if (x < 0) {
                if (speedX < 0) {speedX = 2;}
            }
            if (x > 625) {
                if (speedX > 0) {speedX = -2;}
            }
            if (y < 0) {
                if (speedY < 0) {speedY = 2;}
            }
            if (y > 440) {
                if (speedY > 0) {speedY = -2;}
            }
            y+=speedY;
            x+=speedX;         
        }

        public void draw(Graphics g, ImageObserver obs) {
            if (show) {
                g.drawImage(img, x, y, sizeX, sizeY, obs);
            }
        }
        
        public boolean collision(int x, int y, int w, int h, int offset) {
            if(this.x+sizeX >= x+offset && this.x <= x+w-offset 
                    && this.y <= y+h-offset && this.y+sizeY >= y+offset) {
                this.show = false;
                //playSound("Resources/snd_explosion1.wav", 0);
                return true;
            }
            return false;
        }
    }
    
    public double slope(double x, double x2, double y, double y2) {
        double deltaX = x2 - x;
        double deltaY = y2 - y;
        double slope = deltaY / deltaX;
        return Math.abs(slope);
    }
    
    public class gameHUD {

        Image healthBar, healthTick, life;
        int x, y;
        int type;
        long start, end, time;
        boolean show, startTimer = true;
   
        gameHUD() {
           healthBar = getSprite("Resources/healthbar.png");
           healthTick = getSprite("Resources/tick.png");
           life = getSprite("Resources/life.png"); 
        }

        public void draw(Graphics g, ImageObserver obs, int player) {
            g.setColor(Color.white);
            g.setFont(new Font ("Monospace", Font.BOLD, 12));
            g.drawString("Player 1", 20, 405);
            g.drawImage(healthBar, 20, 410, obs);
            for (int i=0; i<m1.health*4+2 && m1.health>0; i++) {
                g.drawImage(healthTick, 22+(i*7), 412, obs);
            }
            for (int i=0; i<m1.life; i++) {
                g.drawImage(life, 20+(i*25), 420, obs);
            }
            if (player == 2) {
                g.drawString("Player 2", 480, 405);
                g.drawImage(healthBar, 480, 410, obs);
                for (int i=0; i<m2.health*4+2 && m2.health>0; i++) {
                    g.drawImage(healthTick, 482+(i*7), 412, obs);
                }
                for (int i=0; i<m2.life; i++) {
                    g.drawImage(life, 480+(i*25), 420, obs);
                }
            }
            if (boss && b1.health>0) {
                g.drawString("Boss", 20, 45);
                g.drawImage(healthBar, 20, 50, obs);
                for (int i=0; i<(b1.health/10 * 2)-2; i++) {
                    g.drawImage(healthTick, 22+(i*7), 52, obs);
                }
                g.drawImage(healthTick, 22, 52, obs);
            }
        }
    }
    
    public void timeline() {
        if (startTimer) {
            start = System.currentTimeMillis() / 1000;
            startTimer = false;
        }
        end = System.currentTimeMillis() / 1000;
        time = end - start;
        waveTimeEnd = System.currentTimeMillis() / 1000;
        if ( (waveTimeEnd - waveTimeStart) > 0) {
            waveStart = true;
        }
        if (time == 2 && waveStart || time == 27 && waveStart 
                || time == 33 && waveStart) {
            Enemy1 e1 = new Enemy1(speed+1, 200, -100);
            Enemy1 e2 = new Enemy1(speed+1, 200, -200);
            Enemy1 e3 = new Enemy1(speed+1, 200, -300);
            Enemy1 e4 = new Enemy1(speed+1, 400, -100);
            Enemy1 e5 = new Enemy1(speed+1, 400, -200);
            Enemy1 e6 = new Enemy1(speed+1, 400, -300);
            eList.add(e1);
            eList.add(e2);
            eList.add(e3);
            eList.add(e4);
            eList.add(e5);
            eList.add(e6);
            waveTimeStart = System.currentTimeMillis() / 1000;
            waveStart = false;
        }
        if (time == 7 && waveStart || time == 32 && waveStart 
                || time == 45 && waveStart) {
            Enemy2 e1 = new Enemy2(speed+1, 640, 50, 2);
            Enemy2 e2 = new Enemy2(speed+1, 640, 150, 2);
            Enemy2 e3 = new Enemy2(speed+1, 640, 250, 2);
            Enemy2 e4 = new Enemy2(speed+1, -32, 100, 3);
            Enemy2 e5 = new Enemy2(speed+1, -32, 200, 3);
            Enemy2 e6 = new Enemy2(speed+1, -32, 300, 3);
            eList.add(e1);
            eList.add(e2);
            eList.add(e3);
            eList.add(e4);
            eList.add(e5);
            eList.add(e6);
            waveTimeStart = System.currentTimeMillis() / 1000;
            waveStart = false;
        }
        if (time == 12 && waveStart || time == 37 && waveStart 
                || time == 39 && waveStart) {
            Enemy3 e1 = new Enemy3(speed+1, 50, -300);
            Enemy3 e2 = new Enemy3(speed+1, 150, -200);
            Enemy3 e3 = new Enemy3(speed+1, 250, -100);
            Enemy3 e4 = new Enemy3(speed+1, 350, -100);
            Enemy3 e5 = new Enemy3(speed+1, 450, -200);
            Enemy3 e6 = new Enemy3(speed+1, 550, -300);
            eList.add(e1);
            eList.add(e2);
            eList.add(e3);
            eList.add(e4);
            eList.add(e5);
            eList.add(e6);
            waveTimeStart = System.currentTimeMillis() / 1000;
            waveStart = false;
        }
        if (time == 17 && waveStart || time == 42 && waveStart 
                || time == 44 && waveStart) {
            Enemy4 e1 = new Enemy4(speed+1, 100, 700);
            Enemy4 e2 = new Enemy4(speed+1, 100, 800);
            Enemy4 e3 = new Enemy4(speed+1, 300, 700);
            Enemy4 e4 = new Enemy4(speed+1, 300, 800);
            Enemy4 e5 = new Enemy4(speed+1, 500, 700);
            Enemy4 e6 = new Enemy4(speed+1, 500, 800);
            eList.add(e1);
            eList.add(e2);
            eList.add(e3);
            eList.add(e4);
            eList.add(e5);
            eList.add(e6);
            item = new Items(generator.nextInt(640), -10);
            iList.add(item);
            waveTimeStart = System.currentTimeMillis() / 1000;
            waveStart = false;
        }
        if (time == 22 && waveStart || time == 47 && waveStart 
                || time == 39 && waveStart) {
            Enemy2 e1 = new Enemy2(speed+1, 100, -300, 1);
            Enemy2 e2 = new Enemy2(speed+1, 200, -200, 1);
            Enemy2 e3 = new Enemy2(speed+1, 300, -100, 1);
            Enemy2 e4 = new Enemy2(speed+1, 400, -200, 1);
            Enemy2 e5 = new Enemy2(speed+1, 500, -300, 1);
            eList.add(e1);
            eList.add(e2);
            eList.add(e3);
            eList.add(e4);
            eList.add(e5);
            waveTimeStart = System.currentTimeMillis() / 1000;
            waveStart = false;
        }
        if (time == 55 && waveStart) {
            b1 = new Boss(speed+1, 220, -200);
            eList.add(b1);
            boss = true;
            waveTimeStart = System.currentTimeMillis() / 1000;
            waveStart = false;
        }
    }
    
    public Image getSprite(String name) {
        URL url = gm1942.class.getResource(name);
        Image img = getToolkit().getImage(url);
        try {
            MediaTracker tracker = new MediaTracker(this);
            tracker.addImage(img, 0);
            tracker.waitForID(0);
        } catch (Exception e) {
        }
        return img;
    }

    // generates a new color with the specified hue
    public void drawBackGroundWithTileImage(int w, int h, Graphics2D g2) {
        Image sea;
        sea = getSprite("Resources/water.png");
        int TileWidth = sea.getWidth(this);
        int TileHeight = sea.getHeight(this);

        int NumberX = (int) (w / TileWidth);
        int NumberY = (int) (h / TileHeight);

        Image Buffer = createImage(NumberX * TileWidth, NumberY * TileHeight);
        //Graphics BufferG = Buffer.getGraphics();


        for (int i = -1; i <= NumberY; i++) {
            for (int j = 0; j <= NumberX; j++) {
                g2.drawImage(sea, j * TileWidth, i * TileHeight + (move % TileHeight), TileWidth, TileHeight, this);
            }
        }
        move += speed;
    }

    public void drawDemo(int w, int h, Graphics2D g2) {
            if (gameOver) {
                frame++;
            }
            drawBackGroundWithTileImage(w, h, g2);
            I1.update(w, h);
            I1.draw(g2, this);

            I2.update(w, h);
            I2.draw(g2, this);

            I3.update(w, h);
            I3.draw(g2, this);
            
            timeline();
            
            if (p1dead && p2dead) {
                gameOver = true;
            }
            
            if (!gameOver) {
                for (int i=0; i<pBullet.size(); i++) {
                    Bullet p1Bullet = (Bullet) pBullet.get(i);
                    if (p1Bullet.show()) {
                        p1Bullet.update();
                        p1Bullet.draw(g2, this);
                    } else {
                        pBullet.remove(i);
                    }
                }

                for (int i=0; i<eBullet.size(); i++) {
                    Bullet eneBullet = (Bullet) eBullet.get(i);
                    if (eneBullet.show()) {
                        eneBullet.update();
                        eneBullet.draw(g2, this);
                    } else {
                        eBullet.remove(i);
                    }
                }

                for (int i=0; i<eList.size(); i++) {
                    Enemy enemy1 = (Enemy) eList.get(i);
                    if (enemy1.isShow()) {
                        enemy1.update();
                        enemy1.draw(g2, this);
                    } else {
                        eList.remove(i);
                    }
                }

                for (int i=0; i<iList.size(); i++) {
                    Items i1 = (Items) iList.get(i);
                    if (i1.show) {
                        i1.update();
                        i1.draw(g2, this);
                    } else {
                        iList.remove(i);
                    }
                }

                m1.update();
                m1.draw(g2, this);

                m2.update();
                m2.draw(g2, this);
                
                hud.draw(g2, this, 2);
           }
            
           for (int i=0; i<exList.size(); i++) {
                Explosion ex1 = (Explosion) exList.get(i);
                if (ex1.show) {
                    //ex1.update();
                    ex1.draw(g2, this);
                } else {
                    exList.remove(i);
                }
            }
           
           if (gameOver && frame > 120) {
               scoreBoard(g2, this);
           } else if (gameOver && frame <= 120) {
               m1.update();
               m1.draw(g2, this);

               m2.update();
               m2.draw(g2, this);
           }
    }
    
    public void fillScores() {
        // adding placement holder scores
        names.add("Scott");names.add("Scott");names.add("Scott");
        names.add("Scott");names.add("Scott");names.add("Scott");
        names.add("Scott");names.add("Scott");names.add("Scott");
        names.add("Scott");
        scores.add("999999");scores.add("999999");scores.add("999999");
        scores.add("999999");scores.add("999999");scores.add("999999");
        scores.add("999999");scores.add("999999");scores.add("999999");
        scores.add("999999");
    }
    
    public void scoreBoard(Graphics g, ImageObserver obs) {
        g.setColor(Color.white);
        g.setFont(new Font ("Courier New", Font.BOLD, 18));
        g.drawString("High Scores", 250, 30);
        g.drawString("Name", 20, 50);
        g.drawString("Score", 500, 50);
        g.drawString("Your Score: " + score, 200, 440);
        for (int i=1; i<=10; i++) {
            if (i < 10) {
                g.drawString(" " + i + ".", 10, 50 + (i*33));
            } else {
                g.drawString(i + ".", 10, 50 + (i*33));
            }
            if (i <= names.size()) {
                g.drawString((String)names.get(i-1), 50, 50 + (i*33));
                g.drawString((String)scores.get(i-1), 500, 50 + (i*33));
            }
        }   
    }

    public Graphics2D createGraphics2D(int w, int h) {
        Graphics2D g2 = null;
        if (bimg == null || bimg.getWidth() != w || bimg.getHeight() != h) {
            bimg = (BufferedImage) createImage(w, h);
        }
        g2 = bimg.createGraphics();
        g2.setBackground(getBackground());
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g2.clearRect(0, 0, w, h);
        return g2;
    }

    public void paint(Graphics g) {
        Dimension d = getSize();
        Graphics2D g2 = createGraphics2D(d.width, d.height);
        drawDemo(d.width, d.height, g2);
        g2.dispose();
        g.drawImage(bimg, 0, 0, this);
    }

    public void start() {
        thread = new Thread(this);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }
    
    public void destroy() {
        bgm.close();
        System.exit(0);
    }

    public void run() {
    	
        Thread me = Thread.currentThread();
        while (thread == me) {
            repaint();
          if (!hasFocus()) {
              //stop();
          }
          try {
                thread.sleep(17);
            } catch (InterruptedException e) {
                break;
            }
            
        }
    	    	
       // thread = null;
    }

    private void playSound(String filename, int back) {
        URL url = gm1942.class.getResource(filename);
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            FloatControl volume = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            volume.setValue(-20);
            clip.loop(back);
            if (back == 1) {
                bgm = clip;
            }
        } catch (UnsupportedAudioFileException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public static void main(String argv[]) {
        final gm1942 demo = new gm1942();
        demo.init();
        JFrame f = new JFrame("Scrolling Shooter");
        f.addWindowListener(new WindowAdapter() {});
        f.getContentPane().add("Center", demo);
        f.pack();
        f.setSize(new Dimension(640, 480));
        f.setVisible(true);
        f.setResizable(false);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        demo.start();
    }

    /**
     * This method is called from within the init() method to initialize the
     * form. WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
