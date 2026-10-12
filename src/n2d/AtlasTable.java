package n2d;

/**
 * Atlas metadata table. Text format (one sprite per line):
 * <pre>
 * N2ATLAS 1
 * sheet 256 256
 * s hero0 4 4 28 30 14 30
 * </pre>
 * Fields: id srcX srcY w h anchorX anchorY (anchor = draw origin,
 * bottom-center for actors standing on ground). Pure Java, no LCDUI:
 * desktop tests parse the same bytes the device loads.
 */
public final class AtlasTable {

    public int sheetW;
    public int sheetH;
    public String[] ids = new String[0];
    public int[] sx = new int[0];
    public int[] sy = new int[0];
    public int[] sw = new int[0];
    public int[] sh = new int[0];
    public int[] ax = new int[0];
    public int[] ay = new int[0];
    public int count;

    private AtlasTable() {}

    public int find(String id) {
        for (int i = 0; i < count; i++) {
            if (ids[i].equals(id)) return i;
        }
        return -1;
    }

    public static AtlasTable parse(byte[] data, int len) {
        AtlasTable t = new AtlasTable();
        String[] ids = new String[128];
        int[] v = new int[128 * 6];
        int count = 0;
        int lineStart = 0;
        for (int i = 0; i <= len; i++) {
            if (i == len || data[i] == '\n') {
                int lineEnd = i;
                if (lineEnd > lineStart && data[lineEnd - 1] == '\r') lineEnd--;
                count = parseLine(data, lineStart, lineEnd, t, ids, v, count);
                lineStart = i + 1;
            }
        }
        t.ids = new String[count];
        t.sx = new int[count];
        t.sy = new int[count];
        t.sw = new int[count];
        t.sh = new int[count];
        t.ax = new int[count];
        t.ay = new int[count];
        for (int i = 0; i < count; i++) {
            t.ids[i] = ids[i];
            t.sx[i] = v[i * 6];
            t.sy[i] = v[i * 6 + 1];
            t.sw[i] = v[i * 6 + 2];
            t.sh[i] = v[i * 6 + 3];
            t.ax[i] = v[i * 6 + 4];
            t.ay[i] = v[i * 6 + 5];
        }
        t.count = count;
        return t;
    }

    // Returns updated count. Fields: s id x y w h ax ay | sheet w h
    private static int parseLine(byte[] d, int s, int e, AtlasTable t,
            String[] ids, int[] v, int count) {
        // skip leading spaces
        while (s < e && d[s] == ' ') s++;
        if (s >= e || d[s] == '#') return count;
        if (d[s] == 's' && s + 5 < e && d[s + 1] == 'h'
                && d[s + 2] == 'e' && d[s + 3] == 'e' && d[s + 4] == 't'
                && d[s + 5] == ' ') {
            int[] p = new int[2];
            if (parseInts(d, s + 6, e, p, 2) == 2) {
                t.sheetW = p[0];
                t.sheetH = p[1];
            }
            return count;
        }
        if (d[s] != 's' || s + 1 >= e || d[s + 1] != ' ' || count >= 128) return count;
        int idStart = s + 2;
        int idEnd = idStart;
        while (idEnd < e && d[idEnd] != ' ') idEnd++;
        if (idEnd >= e) return count;
        int[] p = new int[6];
        if (parseInts(d, idEnd + 1, e, p, 6) != 6) return count;
        if (p[2] <= 0 || p[3] <= 0) return count;
        StringBuffer sb = new StringBuffer();
        for (int i = idStart; i < idEnd; i++) sb.append((char) d[i]);
        ids[count] = sb.toString();
        for (int i = 0; i < 6; i++) v[count * 6 + i] = p[i];
        return count + 1;
    }

    /** Parse up to want ints from ASCII bytes. Returns ints found. */
    static int parseInts(byte[] d, int s, int e, int[] out, int want) {
        int n = 0;
        int i = s;
        while (i < e && n < want) {
            while (i < e && (d[i] == ' ' || d[i] == '\t')) i++;
            if (i >= e) break;
            boolean neg = false;
            if (d[i] == '-') {
                neg = true;
                i++;
            }
            if (i >= e || d[i] < '0' || d[i] > '9') break;
            int val = 0;
            while (i < e && d[i] >= '0' && d[i] <= '9') {
                val = val * 10 + (d[i] - '0');
                i++;
            }
            out[n++] = neg ? -val : val;
        }
        return n;
    }
}
