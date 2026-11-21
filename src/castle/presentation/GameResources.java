package castle;

import android.graphics.Bitmap;
import android.graphics.Rect;
import java.util.List;

/** Presentation resource port; views consume ready assets and typed layout values. */
interface GameResources {
    Bitmap image(String path);
    SpriteFrame sprite(String name);
    Rect digit(char value);
    Bitmap numberFont();
    int numberHeight();
    WorldLayout world(int stage);
    int worldCount();
    List<MapMarker> markers(Campaign campaign);
    void releaseTerrain();

    final class SpriteFrame {
        final Bitmap image;
        final Rect frame;

        SpriteFrame(Bitmap image, Rect frame) {
            this.image = image;
            this.frame = new Rect(frame);
        }
    }
}
