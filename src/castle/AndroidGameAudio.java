package castle;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import java.io.IOException;
import java.util.*;

/** Android adapter for the audio port; it never reaches into the activity or view. */
final class AndroidGameAudio implements GameAudio {
    private static final int MAX_PENDING = 64;
    private static final float VOLUME = 1f;
    private static final String[] EFFECTS = {
        "click", "move1", "move2", "attack1", "attack2", "arrow",
        "fireball", "twister", "hypnoses", "support", "explosion"
    };
    private final AssetManager assets;
    private final SoundPool sounds;
    private final Map<String, Integer> soundIds = new HashMap<>();
    private final Map<String, Integer> soundStarts = new HashMap<>();
    private final Set<Integer> readySounds = new HashSet<>();
    private final IdentityHashMap<Object, Integer> effectStreams = new IdentityHashMap<>();
    private final ArrayList<SoundRequest> pendingSounds = new ArrayList<>();
    private final Random audioRandom = new Random();
    private Engine engine;
    private boolean foreground, paused, resultReady, closed, effectsPaused;
    private boolean effectsEnabled = true, musicEnabled = true;
    private String battleTrack, musicName;
    private MediaPlayer music;

    private static final class SoundRequest {
        final String file;
        final Object owner;
        final boolean loop;

        SoundRequest(String file, Object owner, boolean loop) {
            this.file = file;
            this.owner = owner;
            this.loop = loop;
        }
    }

    AndroidGameAudio(Context context) {
        assets = context.getAssets();
        sounds = new SoundPool.Builder().setMaxStreams(24).setAudioAttributes(
            new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()).build();
        sounds.setOnLoadCompleteListener((pool, id, status) -> {
            if (status == 0) {
                readySounds.add(id);
                flushSounds();
            } else android.util.Log.w("CastleWars", "Sound load failed: " + id + " status " + status);
        });
        for (String name : EFFECTS) loadEffect("effects/" + name + ".wav");
    }

    private void loadEffect(String file) {
        try (AssetFileDescriptor descriptor = assets.openFd("audio/" + file)) {
            soundIds.put(file, sounds.load(descriptor, 1));
        } catch (IOException failure) {
            android.util.Log.w("CastleWars", "Sound unavailable", failure);
        }
    }

    @Override public void update(Engine engine, boolean foreground, boolean paused,
            boolean effectsEnabled, boolean musicEnabled, String battleTrack, boolean resultReady) {
        if (closed) return;
        this.engine = engine;
        this.foreground = foreground;
        this.paused = paused;
        this.effectsEnabled = effectsEnabled;
        this.musicEnabled = musicEnabled;
        this.battleTrack = battleTrack;
        this.resultReady = resultReady;
        syncSoundState();
        updateMusic();
    }

    @Override public String chooseBattleTrack() {
        return "music/battle" + (1 + audioRandom.nextInt(3)) + ".wav";
    }

    @Override public void playMovement() {
        play("effects/move" + (1 + audioRandom.nextInt(2)) + ".wav");
    }

    @Override public void playBattleStart() {
        play("effects/attack" + (1 + audioRandom.nextInt(2)) + ".wav");
    }

    @Override public void play(String file) {
        requestSound(new SoundRequest(file, null, false));
    }

    @Override public void play(Engine.AudioCue cue) {
        if (cue.stop) stopEffect(cue.owner);
        else requestSound(new SoundRequest("effects/" + cue.name + ".wav", cue.owner, cue.loop));
    }

    private boolean liveEffect(Object owner) {
        if (engine == null || engine.result != 0) return false;
        if (owner instanceof Engine.Storm) return engine.storms.contains(owner);
        return owner instanceof Engine.Meteor && engine.meteors.contains(owner)
            && !((Engine.Meteor) owner).exploded;
    }

    // Queue unloaded samples so the first spell cannot lose its launch sound.
    private void requestSound(SoundRequest request) {
        if (closed || !effectsEnabled || !foreground) return;
        if (request.owner != null && !liveEffect(request.owner)) return;
        Integer id = soundIds.get(request.file);
        if (id == null) return;
        if ((request.owner != null && paused) || !readySounds.contains(id)) {
            if (pendingSounds.size() < MAX_PENDING) pendingSounds.add(request);
            return;
        }
        int priority = request.file.endsWith("arrow.wav") ? 1 : 2;
        int stream = sounds.play(id, VOLUME, VOLUME, priority, request.loop ? -1 : 0, 1);
        if (stream != 0) {
            soundStarts.put(request.file, soundStarts.getOrDefault(request.file, 0) + 1);
            if (request.owner != null) effectStreams.put(request.owner, stream);
        }
    }

    private void flushSounds() {
        ArrayList<SoundRequest> ready = new ArrayList<>();
        for (Iterator<SoundRequest> it = pendingSounds.iterator(); it.hasNext();) {
            SoundRequest request = it.next();
            if (readySounds.contains(soundIds.get(request.file))) {
                it.remove();
                ready.add(request);
            }
        }
        for (SoundRequest request : ready) requestSound(request);
    }

    // Effect ownership keeps one tornado loop alive and stops it with its spell.
    private void stopEffect(Object owner) {
        Integer stream = effectStreams.remove(owner);
        if (stream != null) sounds.stop(stream);
        pendingSounds.removeIf(request -> request.owner == owner);
    }

    private void syncSoundState() {
        for (Object owner : new ArrayList<>(effectStreams.keySet()))
            if (!liveEffect(owner) || !effectsEnabled) stopEffect(owner);
        pendingSounds.removeIf(request -> request.owner != null && !liveEffect(request.owner));
        boolean pause = !foreground || !effectsEnabled || (engine != null && paused);
        if (pause != effectsPaused) {
            if (pause) sounds.autoPause();
            else sounds.autoResume();
            effectsPaused = pause;
        }
        if (!pause && !pendingSounds.isEmpty()) flushSounds();
    }

    @Override public void restartMusic() {
        if (music != null) music.release();
        music = null;
        musicName = null;
    }

    private void updateMusic() {
        String requested = engine == null ? "music/menu.wav" :
            engine.result == 1 && resultReady ? "effects/won.wav" :
            engine.result == 2 && resultReady ? "effects/lost.wav" : battleTrack;
        if (!musicEnabled) requested = null;
        if (!Objects.equals(requested, musicName)) {
            restartMusic();
            musicName = requested;
            if (requested != null) prepareMusic(requested);
        }
        if (music == null) return;
        boolean playing = foreground && !(engine != null && paused);
        if (playing && !music.isPlaying()) music.start();
        if (!playing && music.isPlaying()) music.pause();
    }

    private void prepareMusic(String file) {
        try (AssetFileDescriptor descriptor = assets.openFd("audio/" + file)) {
            music = new MediaPlayer();
            music.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            music.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            music.setLooping(!file.startsWith("effects/"));
            music.setVolume(VOLUME, VOLUME);
            music.setOnCompletionListener(player -> {
                if (music == player) {
                    music.release();
                    music = null;
                }
            });
            music.prepare();
        } catch (IOException | RuntimeException failure) {
            android.util.Log.w("CastleWars", "Audio unavailable", failure);
            if (music != null) music.release();
            music = null;
        }
    }

    @Override public void pause() {
        if (closed) return;
        foreground = false;
        sounds.autoPause();
        effectsPaused = true;
        if (music != null && music.isPlaying()) music.pause();
    }

    @Override public void close() {
        if (closed) return;
        foreground = false;
        effectsEnabled = false;
        engine = null;
        restartMusic();
        pendingSounds.clear();
        effectStreams.clear();
        sounds.setOnLoadCompleteListener(null);
        sounds.release();
        closed = true;
    }
}
