package castle;

import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;

/** Checks half-point balances, fixed-cost purchases and once-only rewards through real handlers. */
public final class UpgradeUiCheck extends Instrumentation {
    private void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private void capture(GameView board, String name) throws Exception {
        Bitmap shot = Bitmap.createBitmap(board.getWidth(), board.getHeight(), Bitmap.Config.ARGB_8888);
        runOnMainSync(() -> board.draw(new Canvas(shot)));
        try (FileOutputStream output = new FileOutputStream(new File(getTargetContext().getExternalFilesDir(null), name))) {
            shot.compress(Bitmap.CompressFormat.PNG, 100, output);
        }
        shot.recycle();
    }

    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }

    @Override public void onStart() {
        Bundle result = new Bundle(); int code = -1;
        GameView view = null; PlayerProfile original = null;
        try {
            Main main = (Main) startActivitySync(new Intent(Intent.ACTION_MAIN)
                .setClassName("game.castlewars.rebuilt", "castle.Main").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            waitForIdleSync();
            Field field = Main.class.getDeclaredField("board"); field.setAccessible(true);
            view = (GameView) field.get(main); original = view.profile;
            final GameView board = view;
            runOnMainSync(() -> {
                board.profile = PlayerProfile.restore(0, 0, new int[11], true, true);
                board.mission = 1; board.startMission(); board.engine.result = 1;
                board.recordVictory(); board.recordVictory();
                check(board.profile.upgradePoints() == .5f, "one victory awards half a point once");
                board.engine = null; board.screen = "upgrade"; board.upgradeSelection = 0;
                board.click(350, 300);
                check(board.profile.upgradePoints() == .5f && board.profile.upgradeLevel(0) == 0,
                    "half point leaves purchase disabled");
                board.invalidate();
            });
            waitForIdleSync();
            capture(board, "half-point-upgrades.png");
            runOnMainSync(() -> {
                ProfileFixtures.points(board, 1.5f); board.click(350, 300);
                check(board.profile.upgradePoints() == .5f && board.profile.upgradeLevel(0) == 1,
                    "upgrade costs one point and retains fractional remainder");
                check(new AndroidProfileRepository(getTargetContext()).load().upgradePoints() == .5f,
                    "purchase persists the fractional balance");
                board.upgradeSelection = PlayerProfile.TROOP_GENERATION_UPGRADE;
                ProfileFixtures.points(board, 3); board.click(350, 300);
                check(board.profile.upgradePoints() == 2 && board.profile.upgradeLevel(3) == 1,
                    "generation upgrade costs one point");
            });
            capture(board, "castle-generation-upgrades.png");
            runOnMainSync(() -> {
                Paint metrics = new Paint(); metrics.setTypeface(Typeface.create("serif", Typeface.BOLD));
                for (int index = 0; index < 11; index++) for (int level = 0; level <= 5; level++) {
                    UpgradeDescription description = UpgradeDescription.forLevel(index, level);
                    metrics.setTextSize(11);
                    check(metrics.measureText(description.title) <= 300, "upgrade title fits");
                    metrics.setTextSize(8);
                    check(metrics.measureText("Current: " + description.current) <= 300, "current values fit");
                    check(description.next == null || metrics.measureText("Next: " + description.next) <= 300, "next values fit");
                }
                board.profile = PlayerProfile.restore(0, .5f, new int[11], true, true);
            });
            for (int index = 0; index < 11; index++) {
                final int selected = index;
                runOnMainSync(() -> board.upgradeSelection = selected);
                capture(board, "upgrade-description-" + index + ".png");
            }
            result.putString("stream", "Upgrade UI passed: once-only half-point reward, disabled purchase, one-point cost, fractional save, generation purchase and all 66 upgrade descriptions fitting their panels.\n");
        } catch (Throwable failure) {
            code = 0; result.putString("stream", android.util.Log.getStackTraceString(failure));
        } finally {
            if (view != null && original != null) {
                final GameView board = view; final PlayerProfile saved = original;
                runOnMainSync(() -> { board.profile = saved; board.engine = null; board.screen = "menu"; board.save(); });
            }
        }
        finish(code, result);
    }
}
