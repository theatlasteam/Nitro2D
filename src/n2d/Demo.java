package n2d;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;

/** Nitro2D demo runner. Boots art, shows the game, stops on pause. */
public final class Demo extends MIDlet {

    private DemoGame game;

    protected void startApp() {
        game = new DemoGame();
        Display.getDisplay(this).setCurrent(game);
        if (!game.boot()) return;
        game.start();
    }

    protected void pauseApp() {
        if (game != null) game.stop();
    }

    protected void destroyApp(boolean unconditional) {
        if (game != null) game.stop();
    }
}
