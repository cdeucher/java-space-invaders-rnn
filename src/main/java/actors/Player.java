package actors;

import game.Stage;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Objects;

public class Player extends Actor {

	public static final int PLAYER_EXTRA_BOUNDS = 150;

	private boolean up,down,left,right;
	private int score = 0;
	private boolean fire = false;
	private boolean shield = false;
	private PlayerShield playerShield;
	private int applyBounds;
	public double seconds = 0.0;

	public Player(Stage stage) {
		super(stage);
		sprites = new String[]{"player.gif"};
		frame = 0;
		frameSpeed = 35;
		actorSpeed = 10;
		width = 32;
		height = 20;
		posX = Stage.WIDTH/2;
		posY = Stage.HEIGHT/2;
		super.setDebug(2);
	}

	public void act() {
		super.act();		
	}

	protected void updateSpeed() {
		vx = 0;
		vy = 0;
		if (down)
			vy = actorSpeed;
		if (up)
			vy = -actorSpeed;
		if (left)
			vx = -actorSpeed;
		if (right)
			vx = actorSpeed;
		
		//don't allow scrolling off the edge of the screen		
		if (posX - width/2 > 0 && vx < 0)
			posX += vx;
		else if (posX + width  + (width/2)< Stage.WIDTH && vx > 0)
			posX += vx;
		else if (posY - height/2 > 0 && vy < 0)
			posY += vy;
		else if (posY + height + (height/2) < Stage.HEIGHT && vy > 0)
			posY += vy;
	}

	public void keyReleased(KeyEvent e) {
		switch (e.getKeyCode()) {
		case KeyEvent.VK_DOWN:
			down = false;
			break;
		case KeyEvent.VK_UP:
			up = false;
			break;
		case KeyEvent.VK_LEFT:
			left = false;
			break;
		case KeyEvent.VK_RIGHT:
			right = false;
			break;
		}
		updateSpeed();
	}

	public void keyPressed(KeyEvent e) {
		switch (e.getKeyCode()) {
		///*
		case KeyEvent.VK_UP:
			up = true;
			break;
		//*/
		case KeyEvent.VK_LEFT:
			left = true;
			break;
		case KeyEvent.VK_RIGHT:
			right = true;
			break;
		///*
		case KeyEvent.VK_DOWN:
			down = true;
			break;
	    //*/
		case KeyEvent.VK_SPACE:
			setFire(true);
			fire();
			setFire(false);
			break;

		}
		updateSpeed();
	}

	public void collision(Actor a) {
		if( a instanceof Shot && shield == true)
			return;
		else
			stage.endGame();
	}

	public void fire() {
		if(fire) { // && seconds > 10
			Actor shot = new PlayerShot(stage);
			shot.setX(posX);
			shot.setY(posY - shot.getHeight());
			shot.setParent("playerShot");
			shot.setDebug(3);
			stage.actors.add(shot);
			playSound("photon.wav");
		}
	}

	public void updateScore(int score) {
		this.score += score;
	}
	public void decreaseScore(int score) {
		this.score -= score;
	}

	public void resetScore() {
		this.score = 0;
	}

	public int getScore() {
		return score;
	}

	public void setFire(boolean b) {
		fire = b;
	}

	public double getFire() {
		return fire ? 1 : 0;
	}

	public void playerActivateShield(boolean activate) {

		if(activate == true && Objects.isNull(this.playerShield)) {
			PlayerShield newShield = new PlayerShield(stage, this);
			newShield.setX(getX());
			newShield.setY(getY());
			newShield.setParent("player");
			newShield.setDebug(3);
			playerShield = newShield;
			stage.actors.add(newShield);
		}else
		if(activate == false && Objects.nonNull(playerShield)) {
			playerDisableShield();
		}
		shield = activate;
	}

	public Rectangle getBounds(int applyExtraSize) {
		applyBounds = applyExtraSize;
		return new Rectangle(posX-(applyExtraSize/2),posY-(applyExtraSize/2),width+applyExtraSize, height+(applyExtraSize));
	}

	public void playerDisableShield() {
		playerShield.setMarkedForRemoval(true);
		playerShield = null;
	}

	public boolean getShield(){
		return shield;
	}

}
