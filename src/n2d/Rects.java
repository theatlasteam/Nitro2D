package n2d;

/** Axis-aligned bounding-box helpers. Pure integer, no LCDUI. */
public final class Rects {

    private Rects() {}

    public static boolean overlap(int ax, int ay, int aw, int ah,
            int bx, int by, int bw, int bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    /** Visible tile range [c0, r0, c1, r1] (inclusive) for a camera view. */
    public static int[] visibleRange(int camX, int camY, int viewW, int viewH,
            int tileW, int tileH, int cols, int rows) {
        int c0 = camX / tileW;
        int r0 = camY / tileH;
        int c1 = (camX + viewW) / tileW;
        int r1 = (camY + viewH) / tileH;
        if (c0 < 0) c0 = 0;
        if (r0 < 0) r0 = 0;
        if (c1 >= cols) c1 = cols - 1;
        if (r1 >= rows) r1 = rows - 1;
        return new int[]{c0, r0, c1, r1};
    }
}
