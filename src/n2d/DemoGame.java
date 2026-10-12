package n2d;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.game.GameCanvas;

/**
 * Demo runner: parallax backdrop, tiled ground, animated hero, coins,
 * crates, pooled particles, camera follow + shake, FPS HUD.
 * D-pad UP / FIRE / key 2 jumps (hold to bunny-hop).
 *
 * Units: hero position in milli-pixels (int), velocity in px/s.
 * No allocation per frame: HUD strings rebuild only when values change.
 */
public final class DemoGame extends Game {

    private static final int SCREEN_W = 240;
    private static final int GROUND_Y = 258;
    private static final int TILES_H = 72;
    private static final int RUN_SPEED = 140;
    private static final int GRAV_PX_S2 = 2600;
    private static final int JUMP_V = -560;
    private static final int JUMP_MASK = GameCanvas.UP_PRESSED | GameCanvas.FIRE_PRESSED;

    private Atlas atlas;
    private Backdrop back;
    private Tilemap tiles;
    private Camera cam;
    private Particles parts;
    private Font hud;

    private int heroX;
    private int heroYm; // milli-pixels
    private int heroVy; // px/s
    private boolean onGround = true;
    private int invulnMs;
    private int score;
    private int coinsTaken;
    private long simMs;
    private boolean jumpQueued;
    private boolean numHeld;

    private int[] heroFrames = new int[4];
    private int[] coinFrames = new int[4];
    private int dotRegion;
    private int sparkRegion;
    private int crateRegion;

    private int[] coinX;
    private int[] coinY;
    private boolean[] coinGone;
    private int[] crateX;

    private String hudLeft = "";
    private String hudRight = "";
    private int lastScore = -1;
    private int lastCoins = -1;
    private int lastFps = -1;
    private int lastQ = -1;

    private boolean ready;

    public boolean boot() {
        try {
            atlas = Atlas.load("/atlas.png", "/atlas.tbl");
            back = Backdrop.load("/bg_far.png", "/bg_near.png");
            Image tileImg = Image.createImage("/tiles.png");
            int cols = 240;
            int[] map = new int[cols * 3];
            for (int c = 0; c < cols; c++) {
                map[c] = 1;
                map[cols + c] = 1;
                map[2 * cols + c] = 1;
            }
            tiles = new Tilemap(tileImg, 24, 24, map, cols, 3);
            for (int i = 0; i < 4; i++) {
                heroFrames[i] = atlas.find("hero" + i);
                coinFrames[i] = atlas.find("coin" + i);
            }
            dotRegion = atlas.find("dot");
            sparkRegion = atlas.find("spark");
            crateRegion = atlas.find("crate0");
            hud = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            cam = new Camera(0, 0, cols * 24 - SCREEN_W, 0);
            parts = new Particles(64);
            parts.gravity = 900;
            int arcs = 12;
            coinX = new int[arcs * 5];
            coinY = new int[arcs * 5];
            coinGone = new boolean[arcs * 5];
            for (int a = 0; a < arcs; a++) {
                int baseX = 420 + a * 430;
                for (int k = 0; k < 5; k++) {
                    int lift = k == 2 ? 74 : (k == 1 || k == 3 ? 52 : 26);
                    coinX[a * 5 + k] = baseX + k * 30;
                    coinY[a * 5 + k] = GROUND_Y - 30 - lift;
                }
            }
            crateX = new int[]{640, 1290, 2160, 3030, 3900, 4770};
            heroX = 0;
            heroYm = GROUND_Y * 1000;
            ready = true;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected void keyPressed(int keyCode) {
        if (keyCode == KEY_NUM2 || keyCode == KEY_NUM5 || gameAction(keyCode) == FIRE) {
            jumpQueued = true;
            numHeld = true;
        }
    }

    protected void keyReleased(int keyCode) {
        if (keyCode == KEY_NUM2 || keyCode == KEY_NUM5 || gameAction(keyCode) == FIRE) {
            numHeld = false;
        }
    }

    private int gameAction(int keyCode) {
        try {
            return getGameAction(keyCode);
        } catch (Exception e) {
            return 0;
        }
    }

    protected void update() {
        if (!ready) return;
        simMs += SIM_DT;
        boolean upFire = (keys() & JUMP_MASK) != 0;
        boolean grounded = heroYm == GROUND_Y * 1000;
        if (jumpQueued) {
            jumpQueued = false;
            if (onGround) {
                heroVy = JUMP_V;
                onGround = false;
            }
        } else if ((upFire || numHeld) && onGround && grounded) {
            heroVy = JUMP_V;
            onGround = false;
        }
        if (hit(JUMP_MASK) && onGround) {
            heroVy = JUMP_V;
            onGround = false;
        }
        heroX += RUN_SPEED * SIM_DT / 1000;
        heroVy += GRAV_PX_S2 * SIM_DT / 1000;
        heroYm += heroVy * SIM_DT;
        if (heroYm >= GROUND_Y * 1000) {
            if (!onGround) {
                for (int i = 0; i < 4; i++) {
                    parts.spawn(heroX + 14, GROUND_Y - 2,
                            -40 + i * 24, -60 - i * 10, 350, dotRegion);
                }
            }
            heroYm = GROUND_Y * 1000;
            heroVy = 0;
            onGround = true;
        }
        if (invulnMs > 0) invulnMs -= SIM_DT;

        int heroTop = heroYm / 1000 - 26;
        if (invulnMs <= 0) {
            for (int i = 0; i < crateX.length; i++) {
                if (Rects.overlap(heroX + 6, heroTop, 16, 26,
                        crateX[i] + 3, GROUND_Y - 24, 18, 24)) {
                    parts.burst(heroX + 14, heroYm / 1000 - 14, 14, 160, 600,
                            sparkRegion, simMs);
                    cam.shake(14);
                    heroX -= 120;
                    if (heroX < 0) heroX = 0;
                    invulnMs = 1200;
                    score -= 25;
                    if (score < 0) score = 0;
                    break;
                }
            }
        }

        for (int i = 0; i < coinX.length; i++) {
            if (coinGone[i]) continue;
            if (coinX[i] < cam.x - 40 || coinX[i] > cam.x + SCREEN_W + 40) continue;
            if (Rects.overlap(heroX, heroTop - 2, 28, 28,
                    coinX[i] - 8, coinY[i] - 8, 16, 16)) {
                coinGone[i] = true;
                coinsTaken++;
                score += 10;
                parts.burst(coinX[i], coinY[i], 6, 110, 400, dotRegion, simMs + i);
            }
        }

        cam.follow(heroX - 60, 0, 4);
        cam.tick();
        parts.update(SIM_DT);
    }

    protected void render(Graphics g) {
        if (!ready) {
            g.setColor(0x101828);
            g.fillRect(0, 0, getWidth(), getHeight());
            return;
        }
        int cx = cam.x + cam.shakeX(simMs);
        int cy = cam.shakeY(simMs);
        back.paint(g, cx, SCREEN_W, getHeight(), quality);
        tiles.paint(g, cx, TILES_H - GROUND_Y - cy);
        int spin = Anim.frameAt(simMs, 120, 4);
        for (int i = 0; i < coinX.length; i++) {
            if (coinGone[i]) continue;
            int sx = coinX[i] - cx;
            if (sx < -24 || sx > SCREEN_W + 24) continue;
            atlas.draw(g, coinFrames[(spin + i) % 4], sx, coinY[i] + cy);
        }
        for (int i = 0; i < crateX.length; i++) {
            int sx = crateX[i] - cx;
            if (sx < -32 || sx > SCREEN_W + 32) continue;
            atlas.draw(g, crateRegion, sx + 12, GROUND_Y + cy);
        }
        if (invulnMs <= 0 || (simMs / 90) % 2 == 0) {
            int f = onGround ? Anim.frameAt(simMs, 110, 4) : 1;
            atlas.draw(g, heroFrames[f], heroX - cx + 14, heroYm / 1000 + cy);
        }
        int cap = quality == 2 ? 64 : quality == 1 ? 32 : 12;
        parts.renderCapped(g, atlas, cx, -cy, cap);
        // HUD strings rebuild only when values change (no per-frame garbage)
        if (score != lastScore || coinsTaken != lastCoins) {
            lastScore = score;
            lastCoins = coinsTaken;
            hudLeft = "S:" + score + " C:" + coinsTaken;
        }
        int fps = fpsX10 / 10;
        if (fps != lastFps || quality != lastQ) {
            lastFps = fps;
            lastQ = quality;
            hudRight = "FPS:" + fps + " Q:" + quality;
        }
        g.setFont(hud);
        g.setColor(0xffffff);
        g.drawString(hudLeft, 4, 2, Graphics.TOP | Graphics.LEFT);
        g.drawString(hudRight, SCREEN_W - 4, 2, Graphics.TOP | Graphics.RIGHT);
    }
}
