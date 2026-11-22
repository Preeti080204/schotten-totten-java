import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Random;

/**
 * Asset pipeline helper: synthesises every sound effect and the ambient music loop of the game
 * (plucked-string harp by Karplus-Strong synthesis, drones and filtered noise), so all audio is
 * original. Run with {@code java scripts/tools/SoundSynth.java <output-directory>}.
 */
public final class SoundSynth {

    private static final int RATE = 44100;

    private SoundSynth() {
    }

    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        dir.mkdirs();
        write(dir, "click", click());
        write(dir, "card-pick", pick());
        write(dir, "card-place", place());
        write(dir, "card-draw", draw());
        write(dir, "claim", claim());
        write(dir, "turn", turn());
        write(dir, "error", error());
        write(dir, "victory", victory());
        write(dir, "defeat", defeat());
        write(dir, "effect", effect());
        write(dir, "music-highlands", music());
    }

    private static double[] buffer(double seconds) {
        return new double[(int) (seconds * RATE)];
    }

    private static double freq(int midi) {
        return 440.0 * Math.pow(2, (midi - 69) / 12.0);
    }

    private static void pluck(double[] out, double start, int midi, double gain, double decay, long seed) {
        int period = (int) Math.round(RATE / freq(midi));
        double[] ring = new double[period];
        Random r = new Random(seed);
        for (int i = 0; i < period; i++) {
            ring[i] = r.nextDouble() * 2 - 1;
        }
        int offset = (int) (start * RATE);
        int length = Math.min(out.length - offset, (int) (RATE * 3.5));
        int idx = 0;
        double previous = 0;
        for (int n = 0; n < length; n++) {
            double v = ring[idx];
            int next = (idx + 1) % period;
            double avg = decay * 0.5 * (ring[idx] + ring[next]);
            ring[idx] = avg;
            idx = next;
            double smooth = 0.6 * v + 0.4 * previous;
            previous = v;
            double attack = Math.min(1.0, n / (RATE * 0.004));
            out[offset + n] += gain * smooth * attack;
        }
    }

    private static void tone(double[] out, double start, double seconds, double f, double gain,
                             double attack, double release, double[] harmonics) {
        int offset = (int) (start * RATE);
        int length = Math.min(out.length - offset, (int) (seconds * RATE));
        for (int n = 0; n < length; n++) {
            double t = n / (double) RATE;
            double env = Math.min(1.0, t / attack) * Math.min(1.0, (seconds - t) / release);
            double v = 0;
            for (int h = 0; h < harmonics.length; h++) {
                v += harmonics[h] * Math.sin(2 * Math.PI * f * (h + 1) * t);
            }
            out[offset + n] += gain * env * v;
        }
    }

    private static void noise(double[] out, double start, double seconds, double gain, double lowpass,
                              double decayRate, long seed) {
        Random r = new Random(seed);
        int offset = (int) (start * RATE);
        int length = Math.min(out.length - offset, (int) (seconds * RATE));
        double y = 0;
        double alpha = lowpass;
        for (int n = 0; n < length; n++) {
            double t = n / (double) RATE;
            y += alpha * ((r.nextDouble() * 2 - 1) - y);
            out[offset + n] += gain * y * Math.exp(-decayRate * t);
        }
    }

    private static double[] click() {
        double[] o = buffer(0.08);
        noise(o, 0, 0.03, 0.5, 0.5, 120, 1);
        tone(o, 0, 0.06, 1250, 0.25, 0.001, 0.05, new double[]{1, 0.3});
        return o;
    }

    private static double[] pick() {
        double[] o = buffer(0.12);
        int len = o.length;
        for (int n = 0; n < len; n++) {
            double t = n / (double) RATE;
            double f = 500 + 2500 * t / 0.12;
            o[n] += 0.18 * Math.sin(2 * Math.PI * f * t) * Math.exp(-28 * t);
        }
        noise(o, 0, 0.08, 0.25, 0.3, 40, 2);
        return o;
    }

    private static double[] place() {
        double[] o = buffer(0.25);
        noise(o, 0, 0.2, 0.9, 0.08, 30, 3);
        tone(o, 0, 0.18, 150, 0.45, 0.002, 0.15, new double[]{1, 0.4, 0.1});
        return o;
    }

    private static double[] draw() {
        double[] o = buffer(0.3);
        Random r = new Random(4);
        double y = 0;
        for (int n = 0; n < o.length; n++) {
            double t = n / (double) RATE;
            double alpha = 0.05 + 0.4 * t / 0.3;
            y += alpha * ((r.nextDouble() * 2 - 1) - y);
            double env = Math.sin(Math.PI * t / 0.3);
            o[n] += 0.45 * y * env;
        }
        return o;
    }

    private static double[] claim() {
        double[] o = buffer(1.6);
        noise(o, 0, 0.35, 1.0, 0.04, 12, 5);
        tone(o, 0, 0.4, 82, 0.6, 0.003, 0.35, new double[]{1, 0.5, 0.2});
        pluck(o, 0.12, 74, 0.5, 0.996, 6);
        pluck(o, 0.24, 81, 0.45, 0.996, 7);
        pluck(o, 0.36, 86, 0.4, 0.996, 8);
        return o;
    }

    private static double[] turn() {
        double[] o = buffer(1.2);
        pluck(o, 0, 74, 0.45, 0.995, 9);
        pluck(o, 0.11, 81, 0.35, 0.995, 10);
        return o;
    }

    private static double[] error() {
        double[] o = buffer(0.32);
        double[] square = {1, 0, 0.33, 0, 0.2};
        tone(o, 0, 0.13, 233, 0.22, 0.004, 0.04, square);
        tone(o, 0.16, 0.14, 196, 0.22, 0.004, 0.06, square);
        return o;
    }

    private static double[] effect() {
        double[] o = buffer(0.9);
        int[] notes = {74, 78, 81, 86, 90};
        for (int i = 0; i < notes.length; i++) {
            pluck(o, i * 0.05, notes[i], 0.3, 0.994, 20 + i);
        }
        return o;
    }

    private static double[] victory() {
        double[] o = buffer(3.6);
        tone(o, 0, 3.5, freq(50), 0.12, 0.4, 1.2, new double[]{1, 0.6, 0.4, 0.25, 0.15});
        tone(o, 0, 3.5, freq(57), 0.08, 0.4, 1.2, new double[]{1, 0.5, 0.3});
        int[] melody = {62, 66, 69, 74, 69, 74, 78, 81};
        double[] times = {0, 0.18, 0.36, 0.54, 0.9, 1.08, 1.26, 1.5};
        double[] brass = {1, 0.7, 0.5, 0.35, 0.2, 0.1};
        for (int i = 0; i < melody.length; i++) {
            double len = i == melody.length - 1 ? 1.8 : 0.32;
            tone(o, times[i], len, freq(melody[i]), 0.16, 0.02, 0.15, brass);
            pluck(o, times[i], melody[i] + 12, 0.25, 0.996, 30 + i);
        }
        return o;
    }

    private static double[] defeat() {
        double[] o = buffer(2.6);
        int[] melody = {69, 67, 65, 62};
        for (int i = 0; i < melody.length; i++) {
            pluck(o, i * 0.38, melody[i], 0.4, 0.996, 40 + i);
            tone(o, i * 0.38, 0.6, freq(melody[i] - 12), 0.08, 0.05, 0.3, new double[]{1, 0.3});
        }
        return o;
    }

    private static double[] music() {
        double bpm = 84;
        double beat = 60.0 / bpm;
        int bars = 16;
        double seconds = bars * 4 * beat;
        double[] o = buffer(seconds + 0.01);
        tone(o, 0, seconds, freq(38), 0.06, 2.0, 2.0, new double[]{1, 0.7, 0.5, 0.3, 0.2, 0.1});
        tone(o, 0, seconds, freq(45), 0.035, 2.0, 2.0, new double[]{1, 0.5, 0.3});
        int[] scale = {62, 64, 66, 69, 71, 74, 76, 78, 81};
        int[][] phrases = {
                {3, 4, 5, 4, 3, 2, 3, -1},
                {5, 6, 7, 6, 5, 4, 3, -1},
                {3, 2, 1, 2, 3, 4, 5, 5},
                {6, 5, 4, 3, 2, 1, 0, -1}};
        int[] order = {0, 1, 0, 2, 0, 1, 3, 3};
        int[] chords = {50, 50, 48, 50, 55, 50, 48, 50};
        long seed = 100;
        for (int bar = 0; bar < bars; bar += 2) {
            int[] phrase = phrases[order[bar / 2 % order.length]];
            double barStart = bar * 4 * beat;
            for (int i = 0; i < phrase.length; i++) {
                if (phrase[i] >= 0) {
                    pluck(o, barStart + i * beat, scale[phrase[i]], 0.22, 0.997, seed++);
                }
            }
            int root = chords[bar / 2 % chords.length];
            for (int b = 0; b < 2; b++) {
                double s = barStart + b * 4 * beat;
                pluck(o, s, root, 0.18, 0.998, seed++);
                pluck(o, s + beat * 1.5, root + 7, 0.12, 0.998, seed++);
                pluck(o, s + beat * 3, root + 12, 0.1, 0.998, seed++);
            }
        }
        int fade = (int) (RATE * 0.05);
        for (int n = 0; n < fade; n++) {
            o[n] *= n / (double) fade;
            o[o.length - 1 - n] *= n / (double) fade;
        }
        return o;
    }

    private static void write(File dir, String name, double[] samples) throws Exception {
        double peak = 1e-9;
        for (double s : samples) {
            peak = Math.max(peak, Math.abs(s));
        }
        double scale = 0.85 / peak;
        byte[] data = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            int v = (int) Math.round(Math.max(-1, Math.min(1, samples[i] * scale)) * 32767);
            data[2 * i] = (byte) (v & 0xFF);
            data[2 * i + 1] = (byte) ((v >> 8) & 0xFF);
        }
        AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
        try (AudioInputStream in = new AudioInputStream(new ByteArrayInputStream(data), format, samples.length)) {
            AudioSystem.write(in, AudioFileFormat.Type.WAVE, new File(dir, name + ".wav"));
        }
    }
}
