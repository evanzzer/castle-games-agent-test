package castle;

import android.app.Activity;
import android.os.Bundle;
import android.media.AudioManager;

/** Composition root: creates adapters and connects them to the presentation controller. */
public final class Main extends Activity {
    private GameView board;
    private GameAudio audio;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        immersive();
        audio = new AndroidGameAudio(this);
        try {
            GameResources resources = new AndroidGameResources(getAssets());
            ProfileRepository profiles = new AndroidProfileRepository(this);
            board = new GameView(this, audio, profiles, resources, new MatchFactory(), this::finish);
            setContentView(board);
        } catch (java.io.IOException | org.json.JSONException failure) {
            throw new IllegalStateException("Game assets could not load", failure);
        }
    }

    private void immersive() { getWindow().getDecorView().setSystemUiVisibility(5894); }

    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (focused) immersive();
    }

    @Override protected void onResume() {
        super.onResume();
        if (board != null) board.resumeForeground();
    }

    @Override protected void onPause() {
        if (audio != null) audio.pause();
        if (board != null) board.pauseForBackground();
        super.onPause();
    }

    @Override protected void onDestroy() {
        if (audio != null) audio.close();
        super.onDestroy();
    }

    @Override public void onBackPressed() { board.back(); }
}
