package actors;

import game.Invaders;
import game.Stage;

import java.awt.*;

public class Invader extends Actor {
	
	private static final int POINT_VALUE = 10;
	protected static final double FIRING_FREQUENCY = 0.02;
	 
	private int leftWall = 0;
	private int rightWall = 0;
	private int step = 0;
	private int advanceTime = 1000;
	public int targetLock = 0;
	private Player player;
	
	public Invader(Stage stage, Player player) {
		super(stage);
		
		if (((int)(Math.random()*10))%2 == 0) 
			sprites = new String[]{"invader1.gif", "invader2.gif"};
		else 
			sprites = new String[]{"invader3.gif", "invader4.gif"};
		
		frame = 0;
		frameSpeed = 50;
		actorSpeed = 100;
		width = 20;
		height = 20;
		posX = Stage.WIDTH/2;
		posY = Stage.HEIGHT/2;
		this.player = player;
	}
	
	public void invaderFire() {
		//Rectangle playerBounds = player.getBounds(Player.PLAYER_EXTRA_BOUNDS);
		//if( !playerBounds.intersects(getBounds()) ) {
			InvaderShot shot = new InvaderShot(stage);
			shot.setX(posX + width / 2);
			shot.setY(posY + shot.getHeight());
			shot.setParent("invader");
			stage.actors.add(shot);
		//}
	}

	public void act() {

		super.act();
		if (Math.random() < FIRING_FREQUENCY) {
			if (Math.random() < FIRING_FREQUENCY)
				invaderFire();
		}

		updateXSpeed();
		updateYSpeed();
	}
		
	public void setLeftWall(int leftWall) {
		this.leftWall = leftWall;
	}
	
	public void setRightWall(int rightWall) {
		this.rightWall = rightWall;
	}
	
	private void updateXSpeed() {
		if (time % actorSpeed == 0) {
			posX += getVx();
			if (posX < leftWall || posX > rightWall) setVx(-getVx());
		}
	}
	
	private void updateYSpeed() {
		step++;
		if (step == advanceTime) {
			posY += height;
			step = 0;
		}	

		if (posY == stage.getHeight()) 
			stage.endGame();
	}

	public void collision(Actor a) {
		if (a instanceof InvaderShot)
			return;
		
		playSound("explosion.wav");
		if (a instanceof Shot)// && targetLock > 0)
			setMarkedForRemoval(true);
	}
	
	public int getPointValue() {
		return Invader.POINT_VALUE;
	}	
}
