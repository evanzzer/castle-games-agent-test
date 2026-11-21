package castle;

import android.content.res.AssetManager;
import android.graphics.Rect;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** JSON manifest, atlas and bitmap-font metadata shared by the current renderer. */
final class AssetMetadata {
    private AssetMetadata() {}

    static JSONObject read(AssetManager assets, String path) throws IOException, JSONException {
        try (InputStream input = assets.open(path);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            for (int size; (size = input.read(buffer)) != -1;) output.write(buffer, 0, size);
            JSONObject value = new JSONObject(output.toString("UTF-8"));
            if (value.has("schema_version") && value.getInt("schema_version") != 1)
                throw new IOException("Unsupported asset metadata version: " + path);
            return value;
        }
    }

    static Rect frame(JSONObject entry) throws JSONException {
        JSONArray frame = entry.getJSONArray("frame");
        if (frame.length() != 4) throw new JSONException("Frame needs x, y, width, height");
        int x = frame.getInt(0), y = frame.getInt(1);
        int width = frame.getInt(2), height = frame.getInt(3);
        if (x < 0 || y < 0 || width <= 0 || height <= 0)
            throw new JSONException("Invalid frame bounds");
        return new Rect(x, y, x + width, y + height);
    }
}
