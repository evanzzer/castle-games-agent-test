package castle;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;

/** Verifies decoded stage backgrounds and captures each original texture through the real renderer. */
public final class BackgroundUiCheck extends Instrumentation {
    private void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
        Bundle result = new Bundle(); int code = -1;
        GameView view = null; PlayerProfile saved = null;
        try {
            Main main = (Main) startActivitySync(new Intent(Intent.ACTION_MAIN)
                .setClassName("game.castlewars.rebuilt", "castle.Main").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync(); Field field = Main.class.getDeclaredField("board"); field.setAccessible(true);
            view = (GameView) field.get(main); saved = view.profile;
            final GameView board = view;
            int[] original = {1,1,1,1,2,1,1,1,1,2,2,2,3,2,3,3,4,4,3,3,4,4,4,2,1,2,2,2,1,1};
            runOnMainSync(() -> {
                ProfileFixtures.completed(board, 89);
                for (int stage = 1; stage <= 90; stage++) {
                    WorldLayout world = board.resources().world(stage);
                    check(world.background.matches("map/backgrounds/map[1-4]\\.png"), "valid stage background");
                    check(board.resources().image(world.background) != null, "background image decodes");
                    if (stage <= 30) check(world.background.equals("map/backgrounds/map" + original[stage - 1] + ".png"), "original stage mapping");
                }
            });
            for (int stage : new int[]{1, 5, 13, 17, 31, 61}) {
                Bitmap bitmap = Bitmap.createBitmap(board.getWidth(), board.getHeight(), Bitmap.Config.ARGB_8888);
                runOnMainSync(() -> { board.mission = stage; board.startMission(); board.engine.think = 100000; board.draw(new Canvas(bitmap)); });
                try (FileOutputStream output = new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null), "background-stage-" + stage + ".png"))) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
                }
                bitmap.recycle();
            }
            result.putString("stream", "Background UI passed: all 90 stage paths decode, recovered Original assignments and six rendered previews.\n");
        } catch (Throwable failure) {
            code = 0; result.putString("stream", android.util.Log.getStackTraceString(failure));
        } finally {
            if (view != null && saved != null) {
                final GameView board = view; final PlayerProfile original = saved;
                runOnMainSync(() -> { board.profile = original; board.engine = null; board.screen = "menu"; board.save(); });
            }
        }
        finish(code, result);
    }
}
