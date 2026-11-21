package castle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable visual layout values; renderers never need to parse world JSON. */
final class WorldLayout {
    final float width, height;
    final Decoration overlay;
    final List<Decoration> scenery;

    WorldLayout(float width, float height, Decoration overlay, List<Decoration> scenery) {
        this.width = width;
        this.height = height;
        this.overlay = overlay;
        this.scenery = Collections.unmodifiableList(new ArrayList<>(scenery));
    }

    static final class Decoration {
        final String image;
        final float x, y, width, height;

        Decoration(String image, float x, float y, float width, float height) {
            this.image = image;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
