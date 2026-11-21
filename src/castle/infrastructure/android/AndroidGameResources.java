package castle;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import org.json.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/** Decodes Android assets once and exposes typed, read-only layout data to presentation. */
final class AndroidGameResources implements GameResources {
    private final AssetManager assets;
    private final Map<String, Bitmap> images = new HashMap<>();
    private final Map<String, SpriteFrame> sprites = new HashMap<>();
    private final Map<Character, Rect> digits = new HashMap<>();
    private final List<WorldLayout> worlds = new ArrayList<>();
    private final Map<Campaign, List<MapMarker>> navigation = new EnumMap<>(Campaign.class);
    private final String fontImage;
    private final int fontHeight;

    AndroidGameResources(AssetManager assets) throws IOException, JSONException {
        this.assets = assets;
        JSONObject manifest = AssetMetadata.read(assets, "manifest.json");
        JSONArray atlases = manifest.getJSONArray("atlases");
        for (int index = 0; index < atlases.length(); index++) loadAtlas(atlases.getString(index));
        JSONObject campaigns = AssetMetadata.read(assets, manifest.getString("campaigns")).getJSONObject("campaigns");
        JSONObject terrainPaths = manifest.getJSONObject("terrain");
        for (Campaign campaign : Campaign.values()) {
            loadWorlds(campaign, terrainPaths.getJSONObject(campaign.id).getString("worlds"));
            JSONArray points = campaigns.getJSONObject(campaign.id).getJSONArray("markers");
            if (points.length() != Campaign.STAGES_PER_CAMPAIGN) throw new JSONException("Invalid campaign markers");
            ArrayList<MapMarker> markers = new ArrayList<>();
            for (int index = 0; index < points.length(); index++) {
                JSONArray point = points.getJSONArray(index);
                markers.add(new MapMarker((float) point.getDouble(0), (float) point.getDouble(1)));
            }
            navigation.put(campaign, Collections.unmodifiableList(markers));
        }
        JSONObject font = AssetMetadata.read(assets, manifest.getString("number_font"));
        fontImage = font.getString("image");
        fontHeight = font.getInt("line_height");
        JSONObject glyphs = font.getJSONObject("glyphs");
        for (Iterator<String> keys = glyphs.keys(); keys.hasNext();) {
            String code = keys.next();
            digits.put((char) Integer.parseInt(code), AssetMetadata.frame(glyphs.getJSONObject(code)));
        }
    }

    private void loadWorlds(Campaign campaign, String path) throws IOException, JSONException {
        JSONObject terrain = AssetMetadata.read(assets, path);
        JSONArray levels = terrain.getJSONArray("worlds");
        if (!terrain.getString("campaign").equals(campaign.id)
                || terrain.getInt("first_stage") != campaign.firstStage
                || levels.length() != Campaign.STAGES_PER_CAMPAIGN)
            throw new JSONException("Invalid terrain world group: " + campaign.id);
        for (int index = 0; index < levels.length(); index++) {
            JSONObject level = levels.getJSONObject(index);
            JSONArray overlay = level.optJSONArray("overlay");
            JSONArray objects = level.optJSONArray("objects");
            ArrayList<WorldLayout.Decoration> scenery = new ArrayList<>();
            if (objects != null) for (int item = 0; item < objects.length(); item++)
                scenery.add(decoration(objects.getJSONArray(item)));
            worlds.add(new WorldLayout((float) level.optDouble("width", 480),
                (float) level.optDouble("height", 320), overlay == null ? null : decoration(overlay), scenery,
                level.optString("background", "map/backgrounds/map1.png")));
        }
    }

    private WorldLayout.Decoration decoration(JSONArray values) throws JSONException {
        return new WorldLayout.Decoration(values.getString(0), (float) values.getDouble(1),
            (float) values.getDouble(2), (float) values.getDouble(3), (float) values.getDouble(4));
    }

    private void loadAtlas(String path) throws IOException, JSONException {
        JSONObject atlas = AssetMetadata.read(assets, path);
        Bitmap image = image(atlas.getString("image"));
        if (image == null) throw new IOException("Missing atlas image: " + path);
        JSONObject frames = atlas.getJSONObject("frames");
        for (Iterator<String> keys = frames.keys(); keys.hasNext();) {
            String name = keys.next();
            Rect frame = AssetMetadata.frame(frames.getJSONObject(name));
            if (frame.right > image.getWidth() || frame.bottom > image.getHeight())
                throw new IOException("Atlas frame outside image: " + name);
            sprites.put(name, new SpriteFrame(image, frame));
        }
    }

    @Override public Bitmap image(String path) {
        if (images.containsKey(path)) return images.get(path);
        Bitmap bitmap;
        try (InputStream input = assets.open(path)) {
            bitmap = BitmapFactory.decodeStream(input);
        } catch (IOException unavailable) {
            bitmap = null;
        }
        images.put(path, bitmap);
        return bitmap;
    }

    @Override public SpriteFrame sprite(String name) { return sprites.get(name); }
    @Override public Rect digit(char value) { return digits.containsKey(value) ? new Rect(digits.get(value)) : null; }
    @Override public Bitmap numberFont() { return image(fontImage); }
    @Override public int numberHeight() { return fontHeight; }
    @Override public WorldLayout world(int stage) { Campaign.forStage(stage); return worlds.get(stage - 1); }
    @Override public int worldCount() { return worlds.size(); }
    @Override public List<MapMarker> markers(Campaign campaign) { return navigation.get(campaign); }

    // Terrain bitmaps are disposable between stages; shared atlases stay cached.
    @Override public void releaseTerrain() {
        images.keySet().removeIf(path -> path.startsWith("terrain/"));
    }
}
