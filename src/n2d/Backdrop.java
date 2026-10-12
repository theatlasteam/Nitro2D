package n2d;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Two pre-rendered full-screen parallax layers. Static art costs one
 * native blit per layer per frame — far cheaper than drawing vector
 * scenery every frame, and PNG stays compressed in the JAR.
 */
public final class Backdrop {

    private Image far;
    private Image near;
    private int nearFactor = 2;

    private Backdrop() {}

    public static Backdrop load(String farPath, String nearPath) throws Exception {
        Backdrop b = new Backdrop();
        b.far = Image.createImage(farPath);
        if (nearPath != null) b.near = Image.createImage(nearPath);
        return b;
    }

    /** nearFactor 2 = near layer scrolls at half camera speed. */
    public void setNearFactor(int f) {
        nearFactor = f < 1 ? 1 : f;
    }

    public void paint(Graphics g, int camX, int screenW, int screenH, int quality) {
        g.drawImage(far, 0, 0, Graphics.TOP | Graphics.LEFT);
        if (near == null || quality < 1) return;
        int nw = near.getWidth();
        int off = (camX / nearFactor) % nw;
        if (off < 0) off += nw;
        // tile the near layer horizontally for wrap-around
        for (int x = -off; x < screenW; x += nw) {
            g.drawImage(near, x, 0, Graphics.TOP | Graphics.LEFT);
        }
    }
}
