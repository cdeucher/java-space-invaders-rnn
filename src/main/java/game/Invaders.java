package game;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.Transparency;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


import javax.swing.JFrame;
import javax.swing.JPanel;

import actors.*;
import com.cabd.hunting.NeuralNetwork;
import com.cabd.hunting.NeuralNetworkBuilder;
import com.cabd.hunting.SaveLoad;

public class Invaders extends Stage implements KeyListener {

	private static final int MAX_THREADS = 1;
	private static final int DEBUG = 0; // 0 DISABLE, 1 ALL, 2 DEBUG VIEW, 3 RNN VIEW, 4 DISABLE SCREEN
	private static final long serialVersionUID = 1L;
	private static final double LEARNING_RATE = 500;
	private static final double TIME_REFRASH = 1000;
	private static final boolean ONLY_RUN = true;
	private int max_generations = 0;

	private Player player;
	private InputHandler keyPressedHandler;
	private InputHandler keyReleasedHandler;

	public long usedTime;//time taken per game step
	public BufferStrategy strategy;	 //double buffering strategy

	private BufferedImage background, backgroundTile; //background cache
	private int backgroundY; //background cache position

	// Setup neural network
	private final int genomes_per_generation = 3;
	private final int neurons_amount[] = {3, 3, 3};
	private NeuralNetwork nn;
	private SaveLoad saveLoad = new SaveLoad();
	private double playerShotNear = 0.0;
	private double enemyShotNear = 0.0;
	private double nerestInvaderPositions;

	public Invaders() {
		createRNN();

		//init the UI
		setBounds(0,0,Stage.WIDTH,Stage.HEIGHT);
		setBackground(Color.BLACK);

		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(Stage.WIDTH,Stage.HEIGHT));
		panel.setLayout(null);

		panel.add(this);

		JFrame frame = new JFrame("Invaders");
		frame.add(panel);

		frame.setBounds(0,0,Stage.WIDTH,Stage.HEIGHT);
		frame.setResizable(false);
		frame.setVisible(true);

		//cleanup resources on exit
		frame.addWindowListener( new WindowAdapter() {
			          public void windowClosing(WindowEvent e) {
			        	ResourceLoader.getInstance().cleanup();
			            System.exit(0);
			          }
			        });


		addKeyListener(this);

		//create a double buffer
		createBufferStrategy(2);
		strategy = getBufferStrategy();
		requestFocus();
		initWorld();

		keyPressedHandler = new InputHandler(this, player);
		keyPressedHandler.action = InputHandler.Action.PRESS;
		keyReleasedHandler = new InputHandler(this, player);
		keyReleasedHandler.action = InputHandler.Action.RELSEASE;

		game();
	}

	private void createRNN() {
		NeuralNetworkBuilder builder = new NeuralNetworkBuilder();
		nn = builder.setMaxWeight(-1)
			.setMinWeigh(1)
			.setNumberNeurons(neurons_amount)
			.setGenomesPerGeneration(genomes_per_generation)
			.setRandomMutationProbability(0.5)
			.setCrossOverRate(0.5)
			.setSaveLoad(saveLoad)
			.getRNN();
		nn.setOnlyExec(ONLY_RUN);
	}


	/**
	 * add a grid of invaders based on the screen size
	 */
	public void addInvaders() {
		Invader invader = new Invader(this, player);
		//padding between units/rows
		int xPad = invader.getWidth() + 15;
		int yPad = invader.getHeight() + 20;
		//number of units per row
		int unitsPerRow = Stage.WIDTH/(xPad) - 1;
		int rows = (Stage.HEIGHT/yPad) - 3; //number of invader rows

		//create and add invaders for each row
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < unitsPerRow -1; j++) {
				Invader inv = new Invader(this, player);
				inv.setX((j + 1)*xPad);
				inv.setY((i + 1)*yPad);
				inv.setVx(10);
				//set movement boundaries for each invader
				inv.setLeftWall((j + 1)*xPad - 20);
				inv.setRightWall((j + 1)*xPad + 20);
				actors.add(inv);
			}
		}
	}

	public void initWorld() {
		actors = new ArrayList<Actor>();
		gameOver = false;
		gameWon = false;
		//add a player
		player = new Player(this);

		player.setX(Stage.WIDTH/2 - player.getWidth()/2);
		player.setY(Stage.HEIGHT - 50);
		player.setVx(10);

		//load cached background
		backgroundTile = ResourceLoader.getInstance().getSprite("space.gif");
		background = ResourceLoader.createCompatible(
		                    WIDTH, HEIGHT+ backgroundTile.getHeight(),
		                    Transparency.OPAQUE);
		Graphics2D g = (Graphics2D)background.getGraphics();
		g.setPaint( new TexturePaint( backgroundTile,new Rectangle(0,0,backgroundTile.getWidth(),backgroundTile.getHeight())));
		g.fillRect(0,0,background.getWidth(),background.getHeight());
		backgroundY = backgroundTile.getHeight();

		addInvaders();
	}

	public void resetWorld() {
		actors = new ArrayList<Actor>();
		gameOver = false;
		gameWon = false;
		player.resetScore();
		player.setX(Stage.WIDTH/2);
		player.playerActivateShield(false);
		addInvaders();
	}

	public boolean debugRule(int status){
		return status >= DEBUG;
	}

	public void paintWorld() {
		if (DEBUG < 4) {
			//get the graphics from the buffer
			Graphics g = strategy.getDrawGraphics();
			g.fillRect(0, 0, getWidth(), getHeight());
			//init image to background
			g.setColor(getBackground());
			//load subimage from the background
			g.drawImage(background, 0, 0, Stage.WIDTH, Stage.HEIGHT, 0, backgroundY, Stage.WIDTH, backgroundY + Stage.HEIGHT, this);

			if(debugRule(player.getDebugMode()) && DEBUG > 0) {
				if(enemyShotNear <= 0.0 && player.getShield())
					drawBounds(g, player.getBounds(Player.PLAYER_EXTRA_BOUNDS), Color.RED);
				else
					drawBounds(g, player.getBounds(Player.PLAYER_EXTRA_BOUNDS), Color.cyan);
			}
			//paint the actors
			for (int i = 0; i < actors.size(); i++) {
				Actor actor = actors.get(i);

				if(debugRule(actor.getDebugMode()))
					actor.paint(g);

				if (actor instanceof Shot && actor.getParent().equals("invader")) {
					if(debugRule(actor.getDebugMode()) && DEBUG > 0)
						drawBounds(g, actor.getBounds(), Color.ORANGE);
				}
			}

			player.paint(g);
			paintScore(g);
			paintFPS(g);
			//swap buffer
			strategy.show();
		}
	}

	public void drawBounds(Graphics g, Rectangle rec, Color color) {
		g.setColor(color);
		g.fillRect(rec.x, rec.y, rec.width, rec.height);
	}

	public void paintFPS(Graphics g) {
		g.setColor(Color.RED);
		if (usedTime > 0)
			g.drawString(String.valueOf(usedTime*DESIRED_FPS)+" fps",0,Stage.HEIGHT-50);
		else
			g.drawString("--- fps",0,Stage.HEIGHT-50);
	}

	public void paintScore(Graphics g) {
		g.setFont(new Font("Arial",Font.BOLD,20));
		g.setColor(Color.green);
		g.drawString("Score: ",20,20);
		g.setColor(Color.red);
		g.drawString("" + player.getScore(), 100, 20);
	}

	public void paint(Graphics g) {}

	public void updateWorld() {

	    int i = 0;
		int numInvaders = 0;
		double playerShotNearTmp= 0.0;
		Actor enemyShotNearTmp = new Actor(this);;
		Actor nerestInvaderTmp = new Actor(this);
		while (i < actors.size()) {
			Actor actor = actors.get(i);
			if (actor instanceof Shot) {
				if(actor.getParent().equals("invader")) {
					if(checkSensorOfCollisionWithExtra(player, (Shot) actor)) {
						enemyShotNearTmp = actor;
						actor.setDebug(3);
					}
				}
				if(actor.getParent().equals("playerShot")) {
					playerShotNearTmp = actor.getY();
				}
				checkCollision(actor);
			}
			if(actor instanceof Shield){
				if(player.getShield()) {
					((PlayerShield) actor).act(player);
				}
			}

			if (actor.isMarkedForRemoval()) {
				player.updateScore(actor.getPointValue());
				actors.remove(i);
			} else {
				//check how many invaders are remaining
				//0 means player won the match
				if(actor instanceof Invader) {
					numInvaders++;
					nerestInvaderTmp = actor;
				}
				actor.act();
				i++;
			}
		}
		enemyShotNear = enemyShotNearTmp.getY();
		playerShotNear = playerShotNearTmp;
		if(Objects.nonNull(nerestInvaderTmp.sprites))
			setNearestInvaderPosition((Invader) nerestInvaderTmp);

		if (numInvaders == 0)
			super.gameWon = true;

		checkCollision(player);
		player.act();
	}

	private void setNearestInvaderPosition(Invader invader) {
		invader.setDebug(3);
		invader.targetLock = 1;
		nerestInvaderPositions = (getWidth()-invader.getX())-(getWidth()-player.getX());
	}

	private void checkCollision(Actor actor) {

		Rectangle actorBounds = actor.getBounds();
		for (int i = 0; i < actors.size(); i ++) {
			Actor otherActor = actors.get(i);
			if (null == otherActor || actor.equals(otherActor)) continue;
			if (actorBounds.intersects(otherActor.getBounds())) {
				actor.collision(otherActor);
				otherActor.collision(actor);
			}
		}
	}

	private boolean checkSensorOfCollisionWithExtra(Player player, Shot shot) {
		Rectangle playerBounds = player.getBounds(Player.PLAYER_EXTRA_BOUNDS);
		if (playerBounds.intersects(shot.getBounds())) {
			return true;
		}
		return false;
	}

	public void game() {
		usedTime= 0;
		double learnRate = 0;
		super.startGame();

		while(isVisible()) {
			long startTime = System.currentTimeMillis();

			backgroundY--;
			if (backgroundY < 0)
				backgroundY = backgroundTile.getHeight();

			if (super.gameOver || player.getX() > 800 || player.getX() < -200 || player.getScore() < 0) {
				startNewGenome();
				resetWorld();
			}
			else if (super.gameWon) {
				startNewGenome();
				resetWorld();
			}

			updateWorld();
			paintWorld();

			learnRate = getLearnRate(learnRate);

			usedTime = System.currentTimeMillis() - startTime;

			//calculate sleep time
			if (usedTime == 0) usedTime = 1;
			int timeDiff = (int) ((TIME_REFRASH/DESIRED_FPS) - usedTime);
			if (timeDiff > 0) {
				sleep(timeDiff);
			}
		}
	}

	private double getLearnRate(double learnRate) {
		if((System.currentTimeMillis() - learnRate) > LEARNING_RATE) {
			learnRate = System.currentTimeMillis();

			//double[] inputs = { nerestInvaderPositions, playerShotNear, enemyShotNear};
			double[] inputs = { nerestInvaderPositions, nerestInvaderPositions, enemyShotNear};
			double[] outputs = nn.learn(inputs);

			setRnnActions(outputs);

//			if(player.getShield() && enemyShotNear <= 0.0)
//				player.decreaseScore(5);

			nerestInvaderPositions= 0.0;
			enemyShotNear = 0.0;

			System.out.println(String.format("inputs %s %s %s", inputs[0], inputs[1], inputs[2]));
			System.out.println(String.format("outputs %s %s %s", outputs[0], outputs[1], outputs[2]));
		}
		return learnRate;
	}

	private void startNewGenome() {
		max_generations++;
		System.out.println("New generation:" + max_generations);
		nn.newGenome( player.getScore() );
		resetWorld();
	}

	private void setRnnActions(double[] outputs) {
		if(outputs[0] > 0.5)
			player.setX(player.getX() + 20);
		else
			player.setX(player.getX() - 20);

		if(outputs[1] > 0.5)
			player.setFire(true);
		else
			player.setFire(false);

		if(outputs[2] > 0.5) {
			System.out.println("Enable shield");
			player.playerActivateShield(true);
		}else {
			System.out.println("Disable shield");
			player.playerActivateShield(false);
		}
		player.fire();
	}


	public void sleep(int time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public void keyPressed(KeyEvent e) {
		keyPressedHandler.handleInput(e);
	}

	public void keyReleased(KeyEvent e) {
		keyReleasedHandler.handleInput(e);
	}

	public void keyTyped(KeyEvent e) {
	}

	public static void main(String[] args) {
//		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
//		for(int i=0; i<MAX_THREADS; i++) {
//			executor.submit(() -> {
//				new Invaders().game();
//			});
//		}
//		new Invaders().game();

		List<Thread> treads = new ArrayList<>();
		for(int i=0; i<MAX_THREADS; i++) {
			treads.add(new Thread(new Runnable() {
				 public void run() {
						Invaders inv = new Invaders();
						inv.game();
				 }
			}));
		}
		for(Thread thread : treads){
			thread.start();
		}
	}
}
