package actors;

import game.Stage;

public class PlayerShield extends Shield {

    Player player;


    public PlayerShield(Stage stage, Player player) {
        super(stage);
        this.player = player;
    }

    public void act(Actor actor) {
        super.act();
        posY = actor.getY()-20;
        posX = actor.getX();
    }

    public void collision(Actor a) {
        if (a instanceof Shield)
            return;
        if( a.getParent().equals("invader") ) {
            player.updateScore(10);
            a.setMarkedForRemoval(true);
        }
    }
}
