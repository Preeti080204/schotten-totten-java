import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayDeque;

/**
 * Asset pipeline helper: makes the near-white background of an illustration transparent by
 * flood-filling from the image border, then softens the cut edge. Run with
 * {@code java scripts/tools/CutoutBackground.java in.jpg out.png}.
 */
public final class CutoutBackground {

    private CutoutBackground() {
    }

    public static void main(String[] args) throws Exception {
        BufferedImage src = ImageIO.read(new File(args[0]));
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        boolean[] background = new boolean[w * h];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        for (int x = 0; x < w; x++) {
            queue.add(new int[]{x, 0});
            queue.add(new int[]{x, h - 1});
        }
        for (int y = 0; y < h; y++) {
            queue.add(new int[]{0, y});
            queue.add(new int[]{w - 1, y});
        }
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int x = p[0];
            int y = p[1];
            if (x < 0 || y < 0 || x >= w || y >= h || background[y * w + x] || !isLight(src.getRGB(x, y))) {
                continue;
            }
            background[y * w + x] = true;
            queue.add(new int[]{x + 1, y});
            queue.add(new int[]{x - 1, y});
            queue.add(new int[]{x, y + 1});
            queue.add(new int[]{x, y - 1});
        }
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y) & 0xFFFFFF;
                int alpha;
                if (background[y * w + x]) {
                    alpha = 0;
                } else {
                    int near = 0;
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int nx = x + dx;
                            int ny = y + dy;
                            if (nx >= 0 && ny >= 0 && nx < w && ny < h && background[ny * w + nx]) {
                                near++;
                            }
                        }
                    }
                    alpha = near == 0 ? 255 : 255 - near * 22;
                }
                out.setRGB(x, y, (alpha << 24) | rgb);
            }
        }
        ImageIO.write(out, "png", new File(args[1]));
    }

    private static boolean isLight(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        return r > 232 && g > 232 && b > 232;
    }
}
