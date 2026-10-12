package n2d;

import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.PlayerListener;
import javax.microedition.media.control.ToneControl;
import javax.microedition.media.control.VolumeControl;

/**
 * Sound: zero-asset ToneControl SFX (jump/coin/death/thud/click),
 * fire-and-forget WAV playback, and looped MIDI music. Everything is
 * guarded: phones without mixers just stay silent. Master switch via
 * `enabled`; key 0 in the demo toggles it.
 */
public final class Sfx {

    public static boolean enabled = true;

    private Sfx() {}

    // MIDI note numbers
    private static final int[] JUMP = {64, 5, 72, 7};
    private static final int[] COIN = {83, 4, 88, 10};
    private static final int[] DEATH = {67, 5, 63, 5, 58, 12};
    private static final int[] THUD = {45, 5};
    private static final int[] CLICK = {76, 3};

    private static Player musicPlayer;

    private static final class Closer implements PlayerListener {
        public void playerUpdate(Player p, String ev, Object data) {
            if (PlayerListener.END_OF_MEDIA.equals(ev)
                    || PlayerListener.ERROR.equals(ev)) {
                try {
                    p.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private static final Closer CLOSER = new Closer();

    /** Play a tone sequence. notes = pairs of (midiNote, duration64ths). */
    public static void tone(int[] notes, int tempo) {
        if (!enabled) return;
        try {
            byte[] seq = new byte[6 + notes.length / 2 * 3 + 4];
            int o = 0;
            seq[o++] = ToneControl.VERSION;
            seq[o++] = 1;
            seq[o++] = ToneControl.TEMPO;
            seq[o++] = (byte) tempo;
            seq[o++] = ToneControl.BLOCK_START;
            seq[o++] = 0;
            for (int i = 0; i + 1 < notes.length; i += 2) {
                seq[o++] = (byte) notes[i];
                seq[o++] = (byte) notes[i + 1];
                seq[o++] = (byte) 90;
            }
            seq[o++] = ToneControl.BLOCK_END;
            seq[o++] = 0;
            seq[o++] = ToneControl.PLAY_BLOCK;
            seq[o++] = 0;
            Player p = Manager.createPlayer(Manager.TONE_DEVICE_LOCATOR);
            p.realize();
            ToneControl tc = (ToneControl) p.getControl("ToneControl");
            if (tc == null) {
                p.close();
                return;
            }
            tc.setSequence(seq);
            setVolume(p, 80);
            p.addPlayerListener(CLOSER);
            p.start();
        } catch (Exception e) {
        }
    }

    public static void jump() {
        tone(JUMP, 40);
    }

    public static void coin() {
        tone(COIN, 36);
    }

    public static void death() {
        tone(DEATH, 30);
    }

    public static void thud() {
        tone(THUD, 30);
    }

    public static void click() {
        tone(CLICK, 40);
    }

    /** Fire-and-forget WAV from a resource path (e.g. "/boom.wav"). */
    public static void wav(String resPath, int volume) {
        if (!enabled) return;
        try {
            java.io.InputStream in = "".getClass().getResourceAsStream(resPath);
            if (in == null) return;
            Player p = Manager.createPlayer(in, "audio/x-wav");
            p.realize();
            setVolume(p, volume);
            p.addPlayerListener(CLOSER);
            p.prefetch();
            p.start();
        } catch (Exception e) {
        }
    }

    /** Looped MIDI music. Pass loops < 0 for infinite. No asset shipped. */
    public static void music(String resPath, int loops) {
        stopMusic();
        if (!enabled) return;
        try {
            java.io.InputStream in = "".getClass().getResourceAsStream(resPath);
            if (in == null) return;
            musicPlayer = Manager.createPlayer(in, "audio/midi");
            musicPlayer.realize();
            setVolume(musicPlayer, 60);
            musicPlayer.setLoopCount(loops);
            musicPlayer.prefetch();
            musicPlayer.start();
        } catch (Exception e) {
            musicPlayer = null;
        }
    }

    public static void stopMusic() {
        if (musicPlayer != null) {
            try {
                musicPlayer.stop();
            } catch (Exception e) {
            }
            try {
                musicPlayer.close();
            } catch (Exception e) {
            }
            musicPlayer = null;
        }
    }

    private static void setVolume(Player p, int level) {
        try {
            VolumeControl vc = (VolumeControl) p.getControl("VolumeControl");
            if (vc != null) vc.setLevel(level);
        } catch (Exception e) {
        }
    }
}
