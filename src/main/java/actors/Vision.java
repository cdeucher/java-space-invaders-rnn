package actors;

import game.Stage;

public class Vision extends Actor{

    private Player player;

    public Vision(Stage stage) {
        super(stage);
        width = 60;
        height = 15;
        sprites = new String[]{"player.gif"};
    }

    public void setPlayer(Player player){
        this.player = player;
    }

    public void collision(Actor a) {


    }


}
