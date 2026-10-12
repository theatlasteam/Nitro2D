package n2d;

/** 16.16 fixed-point math. No allocation, no floating point. */
public final class Fx {

    public static final int ONE = 1 << 16;

    private Fx() {}

    public static int fromInt(int v) {
        return v << 16;
    }

    public static int toInt(int v) {
        return v >> 16;
    }

    /** Round to nearest int (handles negatives). */
    public static int toIntRound(int v) {
        return (v + (v >= 0 ? ONE >> 1 : -(ONE >> 1))) >> 16;
    }

    public static int mul(int a, int b) {
        return (int) (((long) a * (long) b) >> 16);
    }

    public static int div(int a, int b) {
        return (int) ((((long) a) << 16) / (long) b);
    }

    public static int abs(int v) {
        return v < 0 ? -v : v;
    }

    public static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }

    public static int lerp(int a, int b, int t1024) {
        return a + (int) (((long) (b - a) * t1024) >> 10);
    }

    /** Integer square root (Newton, 8 iterations, exact for 32-bit). */
    public static int sqrt(int v) {
        if (v <= 0) return 0;
        int x = v > ONE ? v >> 9 : ONE >> 4;
        if (x <= 0) x = 1;
        for (int i = 0; i < 8; i++) x = (x + v / x) >> 1;
        return x;
    }

    // --- sine: 0..90 deg table in 0..1024, quadrant-mapped ---

    private static final short[] SIN = {
        0, 18, 36, 54, 71, 89, 107, 125, 143, 160, 178, 195, 213, 230, 248, 265,
        282, 299, 316, 333, 350, 367, 384, 400, 416, 433, 449, 465, 481, 496, 512, 527,
        543, 558, 573, 587, 602, 616, 630, 644, 658, 672, 685, 698, 711, 724, 737, 749,
        761, 773, 784, 796, 807, 818, 828, 839, 849, 859, 868, 878, 887, 896, 904, 912,
        920, 928, 935, 943, 949, 956, 962, 968, 974, 979, 984, 989, 994, 998, 1002, 1005,
        1008, 1011, 1014, 1016, 1018, 1020, 1022, 1023, 1023, 1024, 1024,
    };

    private static int sin90(int deg0to90) {
        if (deg0to90 < 0) deg0to90 = 0;
        if (deg0to90 > 90) deg0to90 = 90;
        return SIN[deg0to90];
    }

    /** sin(degrees) in 16.16. Any integer angle. */
    public static int sin(int deg) {
        int d = deg % 360;
        if (d < 0) d += 360;
        if (d < 90) return (sin90(d) << 16) / 1024;
        if (d < 180) return (sin90(180 - d) << 16) / 1024;
        if (d < 270) return -((sin90(d - 180) << 16) / 1024);
        return -((sin90(360 - d) << 16) / 1024);
    }

    public static int cos(int deg) {
        return sin(deg + 90);
    }
}
