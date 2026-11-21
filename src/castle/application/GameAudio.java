package castle;

/** Application audio port; callers do not depend on Android playback classes. */
interface GameAudio {
    /** A null engine selects the menu; all playback context is provided explicitly. */
    void update(Engine engine, boolean foreground, boolean paused,
                boolean effectsEnabled, boolean musicEnabled, String battleTrack, boolean resultReady);
    void play(String file);
    void play(Engine.AudioCue cue);
    void playMovement();
    void playBattleStart();
    String chooseBattleTrack();
    void pause();
    void restartMusic();
    void close();
}
