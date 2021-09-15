package actors;

import game.Stage;

public class Shield extends Actor{

    public Shield(Stage stage) {
        super(stage);
        width = 60;
        height = 15;
        sprites = new String[]{"shield2.gif"};
    }

    public void collision(Actor a) {


    }


}
