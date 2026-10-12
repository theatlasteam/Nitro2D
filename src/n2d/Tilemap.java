package n2d;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.TiledLayer;

/**
 * Static tile layer over a native TiledLayer (culling done in C).
 * Build from a compact int[] map, row-major, 0 = empty.
 */
public final class Tilemap {

    private TiledLayer layer;
    public final int cols;
    public final int rows;
    public final int tileW;
    public final int tileH;

    public Tilemap(Image tiles, int tileW, int tileH, int[] map, int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        this.tileW = tileW;
        this.tileH = tileH;
        int tilesPerRow = tiles.getWidth() / tileW;
        layer = new TiledLayer(cols, rows, tiles, tileW, tileH);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int v = map[r * cols + c];
                if (v > 0) layer.setCell(c, r, ((v - 1) % tilesPerRow) + 1);
            }
        }
    }

    public void paint(Graphics g, int camX, int camY) {
        layer.setPosition(-camX, -camY);
        layer.paint(g);
    }

    public int pixelWidth() {
        return cols * tileW;
    }

    public int pixelHeight() {
        return rows * tileH;
    }
}
