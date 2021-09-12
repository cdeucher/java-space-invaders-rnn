package actors;

import game.Stage;

public class PlayerShot extends Shot {

	public PlayerShot(Stage stage) {
		super(stage);
		width = 10;
		height = 15;
		sprites = new String[]{"shot1.gif","shot2.gif"};
	}

	public void collision(Actor a) {
		if (a instanceof Shield || a instanceof Shot)
			return;
		setMarkedForRemoval(true);
	}
	
}
