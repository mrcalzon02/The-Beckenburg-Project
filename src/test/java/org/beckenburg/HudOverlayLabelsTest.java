package org.beckenburg;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.image.BufferedImage;

/** Responsive ghost HUD labels must fit the same pixel budget as their panel. */
public final class HudOverlayLabelsTest {
    private static void check(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
    }
    public static void main(String[] args) {
        var image = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            var labels = new String[] {
                "SPECTRAL / THE TWINS / VERY LONG BEHAVIOR NAME",
                "MOVE TO MUTE / HOLD STILL TO LISTEN",
                "Space  Drift to Master Bedroom   ·   Z / Q  Cycle exit   ·   E  Haunt fixture    Next event 30s",
                "Frenzy WARNING   120s    Awaiting host validation of spectral power",
                "SPECTRAL TERRITORY: FARMHOUSE"
            };
            for (var font : new Font[] {new Font(Font.MONOSPACED, Font.BOLD, 13),
                    new Font(Font.SANS_SERIF, Font.BOLD, 12)}) {
                graphics.setFont(font);
                FontMetrics fm = graphics.getFontMetrics();
                for (int width : new int[] {0, 12, 28, 40, 64, 120, 200, 320, 640, 850}) {
                    int budget = Math.max(0, width - 32);
                    for (var label : labels) {
                        String fitted = HudTextFit.ellipsize(fm, label, budget);
                        check(fm.stringWidth(fitted) <= budget, "HUD label overflow for width " + width);
                        if (fm.stringWidth(label) <= budget)
                            check(fitted.equals(label), "HUD label truncated unnecessarily");
                    }
                }
            }
            System.out.println("PASS Ghost HUD labels fit narrow and normal viewports");
        } finally {
            graphics.dispose();
        }
    }
}
