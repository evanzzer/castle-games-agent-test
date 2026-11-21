package castle;

import android.graphics.*;

/** Canvas primitives and bitmap-font rendering; game rules and storage stay outside. */
final class CanvasPainter {
    private final GameResources resources;
    private final Paint paint = new Paint(3);

    CanvasPainter(GameResources resources) { this.resources = resources; }
    Paint paint() { return paint; }

    void image(Canvas canvas, String file, float x, float y, float width, float height) {
        Bitmap bitmap = resources.image(file);
        if (bitmap == null) return;
        paint.setAlpha(255);
        canvas.drawBitmap(bitmap, null, new RectF(x, y, x + width, y + height), paint);
    }
    void sprite(Canvas canvas, String name, float x, float y, float width, float height) {
        GameResources.SpriteFrame sprite = resources.sprite(name);
        if (sprite == null) return;
        paint.setAlpha(255);
        canvas.drawBitmap(sprite.image, sprite.frame, new RectF(x, y, x + width, y + height), paint);
    }
    void natural(Canvas canvas, String name, float x, float y, float size) {
        GameResources.SpriteFrame sprite = resources.sprite(name);
        if (sprite != null) sprite(canvas, name, x - sprite.frame.width() * size / 2,
            y - sprite.frame.height() * size / 2, sprite.frame.width() * size, sprite.frame.height() * size);
    }
    void label(Canvas canvas, String text, float x, float y, float size, int color, boolean centered) {
        paint.setColor(color);
        paint.setTypeface(Typeface.create("serif", Typeface.BOLD));
        paint.setTextAlign(centered ? Paint.Align.CENTER : Paint.Align.LEFT);
        paint.setTextSize(size);
        paint.setShadowLayer(2, 0, 1, Color.BLACK);
        canvas.drawText(text, x, y, paint);
        paint.clearShadowLayer();
    }
    void number(Canvas canvas, int value, float x, float y, float height) {
        String text = Integer.toString(Math.max(0, value));
        float width = 0;
        for (char ch : text.toCharArray()) {
            Rect frame = resources.digit(ch);
            if (frame != null) width += frame.width() * height / resources.numberHeight();
        }
        float pen = x - width / 2;
        for (char ch : text.toCharArray()) {
            Rect frame = resources.digit(ch);
            if (frame == null) continue;
            float w = frame.width() * height / resources.numberHeight();
            paint.setAlpha(255);
            canvas.drawBitmap(resources.numberFont(), frame, new RectF(pen, y, pen + w, y + height), paint);
            pen += w;
        }
    }
    void troopNumber(Canvas canvas, int value, float x, float y, float height, int side) {
        paint.setColorFilter(new LightingColorFilter(color(side), 0));
        number(canvas, value, x, y, height);
        paint.setColorFilter(null);
    }
    void panel(Canvas canvas, float x, float y, float w, float h) {
        paint.setColor(0xe6211c19);
        canvas.drawRoundRect(x, y, x + w, y + h, 4, 4, paint);
    }
    int color(int side) { return new int[]{0xffb3cba2, 0xffff7066, 0xff7dabff, 0xffffdf70}[side]; }
}
