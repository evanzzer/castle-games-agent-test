package castle;

/** Application event delivery stays outside rendering, so drawing cannot replay gameplay audio. */
final class MatchAudioEvents {
    private Engine current;
    private int announcedRaid;

    void deliver(Engine match, GameAudio audio) {
        if (match != current) {
            current = match;
            announcedRaid = 0;
        }
        if (match == null) return;
        while (announcedRaid < match.raidSerial) {
            announcedRaid++;
            audio.playBattleStart();
        }
        for (Engine.AudioCue cue : match.audioCues) audio.play(cue);
        match.audioCues.clear();
    }
}
