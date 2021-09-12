package actors;

import game.Stage;

public class VisionPoint extends Vision {

    public String side;

    public VisionPoint(Stage stage , String side) {
        super(stage);
        this.side = side;
    }

    public void collision(Actor a) {
        if (a instanceof Invader)
            return;
    }
}
