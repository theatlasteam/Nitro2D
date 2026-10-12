package n2d;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.game.GameCanvas;

/**
 * Fixed-timestep game loop on GameCanvas. Simulation runs at SIM_DT
 * (20ms) independent of render rate; at most MAX_STEPS sim steps per
 * frame so stalls slow time instead of spiral-of-death. Rendering uses
 * the offscreen buffer + flushGraphics (double-buffered, no tearing).
 * Keys: keys() is the live bitmask, hit() latches presses across every
 * sim step so no tap is lost at low frame rates. FPS is an EMA over
 * 500ms windows; sustained low FPS auto-degrades quality (games read
 * `quality` to shed layers/particles). Zero allocation in the loop.
 */
public abstract class Game extends GameCanvas implements Runnable {

    public static final int SIM_DT = 20;
    private static final int MAX_STEPS = 3;

    private Thread thread;
    private boolean running;
    private long lastMs;
    private long acc;
    private long fpsWindowStart;
    private int fpsFrames;
    /** FPS x10 exponential moving average. */
    public int fpsX10;
    /** 2 = full, 1 = reduced, 0 = minimal. Engine auto-degrades. */
    public int quality = 2;
    private int lowSince;
    private int latch;
    private int lastStep;

    protected Game() {
        super(true);
        setFullScreenMode(true);
    }

    public void start() {
        if (running) return;
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    public void stop() {
        running = false;
    }

    /** One fixed 20ms simulation step. Poll keys()/hit() here. */
    protected abstract void update();

    /** Draw the current frame. */
    protected abstract void render(Graphics g);

    /** Live key bitmask (held state). */
    protected int keys() {
        return getKeyStates();
    }

    /** True if the mask was pressed during any sim step this frame. */
    protected boolean hit(int mask) {
        return (latch & mask) != 0;
    }

    public void run() {
        lastMs = System.currentTimeMillis();
        acc = 0;
        fpsWindowStart = lastMs;
        fpsFrames = 0;
        latch = 0;
        lastStep = getKeyStates();
        while (running) {
            long now = System.currentTimeMillis();
            long elapsed = now - lastMs;
            if (elapsed < 0) elapsed = 0;
            if (elapsed > 250) elapsed = 250;
            lastMs = now;
            acc += elapsed;
            int steps = 0;
            while (acc >= SIM_DT && steps < MAX_STEPS) {
                int sk = getKeyStates();
                latch |= sk & ~lastStep;
                lastStep = sk;
                update();
                acc -= SIM_DT;
                steps++;
            }
            if (steps == MAX_STEPS) acc = 0;
            Graphics g = getGraphics();
            render(g);
            flushGraphics();
            latch = 0;
            fpsFrames++;
            if (now - fpsWindowStart >= 500) {
                int inst = fpsFrames * 10000 / (int) (now - fpsWindowStart);
                fpsX10 = fpsX10 == 0 ? inst : (fpsX10 * 3 + inst) / 4;
                fpsFrames = 0;
                fpsWindowStart = now;
                if (fpsX10 < 250 && fpsX10 > 0) {
                    lowSince++;
                    if (lowSince >= 4 && quality > 0) {
                        quality--;
                        lowSince = 0;
                    }
                } else {
                    lowSince = 0;
                }
            }
            long spare = SIM_DT - (System.currentTimeMillis() - now);
            if (spare > 2) {
                try {
                    Thread.sleep(spare > 8 ? 8 : spare);
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
