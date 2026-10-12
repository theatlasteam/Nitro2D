package n2d;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.Sprite;

/**
 * Sprite atlas: ONE decoded PNG shared by every actor. Hot-loop drawing
 * is a single native drawRegion blit per sprite — no per-frame scaling,
 * no setTransform churn (mirrored variants are baked into the sheet by
 * tools/pack_atlas.py --flip), no allocation after load.
 */
public final class Atlas {

    private Image sheet;
    public AtlasTable table;

    private Atlas() {}

    public static Atlas load(String pngPath, String tblPath) throws Exception {
        Atlas a = new Atlas();
        a.sheet = Image.createImage(pngPath);
        java.io.InputStream in = "".getClass().getResourceAsStream(tblPath);
        if (in == null) throw new Exception("missing " + tblPath);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[512];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        byte[] data = out.toByteArray();
        a.table = AtlasTable.parse(data, data.length);
        return a;
    }

    public Image sheet() {
        return sheet;
    }

    /** Heap bytes of the decoded sheet (RGB565 ~2B/px on S40). */
    public int sheetBytes() {
        return sheet.getWidth() * sheet.getHeight() * 2;
    }

    public int find(String id) {
        return table.find(id);
    }

    /** Blit sprite idx so its anchor lands at (x, y). Native, no garbage. */
    public void draw(Graphics g, int idx, int x, int y) {
        g.drawRegion(sheet, table.sx[idx], table.sy[idx],
                table.sw[idx], table.sh[idx], Sprite.TRANS_NONE,
                x - table.ax[idx], y - table.ay[idx],
                Graphics.TOP | Graphics.LEFT);
    }

    public int width(int idx) {
        return table.sw[idx];
    }

    public int height(int idx) {
        return table.sh[idx];
    }
}
