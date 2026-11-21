package castle;

import android.app.Instrumentation;
import android.content.SharedPreferences;
import android.os.Bundle;
import java.util.*;

/** Exercises real legacy/current save migrations and restores the original test-device preferences. */
public final class ProfileUiCheck extends Instrumentation {
    private void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }

    @Override public void onStart() {
        Bundle result = new Bundle(); int code = -1;
        SharedPreferences preferences = getTargetContext().getSharedPreferences("original-campaign", 0);
        Map<String, ?> original = new HashMap<>(preferences.getAll());
        try {
            preferences.edit().clear().putBoolean("five-upgrades", true).putInt("completed", 7)
                .putInt("points", 2).putInt("up0", 3).putInt("up1", 2).putInt("up2", 1)
                .putInt("up3", 4).putInt("up4", 8).putBoolean("music", false).commit();
            ProfileRepository repository = new AndroidProfileRepository(getTargetContext());
            PlayerProfile legacy = repository.load();
            check(legacy.completedStages() == 7 && legacy.upgradePoints() == 5, "legacy progress and shared-spell refund");
            check(legacy.upgradeLevel(0) == 3 && legacy.upgradeLevel(5) == 4, "legacy attack/crystal mapping");
            for (int spell = 6; spell <= 9; spell++) check(legacy.upgradeLevel(spell) == 5, "legacy spell tier clamp");
            check(!legacy.musicEnabled() && legacy.soundEnabled(), "legacy settings preserved");
            repository.save(legacy);
            PlayerProfile again = repository.load();
            check(again.upgradePoints() == 5 && !preferences.getBoolean("five-upgrades", true), "migration marker consumed and refund not repeated");
            check(Arrays.equals(again.upgradeLevels(), legacy.upgradeLevels()), "legacy profile round trip");
            preferences.edit().clear().putInt("completed", 160).putInt("points", 4)
                .putInt("up9", 8).putInt("up0", -2).commit();
            PlayerProfile current = repository.load();
            check(current.completedStages() == 150 && current.upgradePoints() == 7, "current stage clamp and refund");
            check(current.upgradeLevel(9) == 5 && current.upgradeLevel(0) == 0, "current upgrade normalization");
            repository.save(current);
            check(repository.load().upgradePoints() == 7, "current save does not refund twice");
            preferences.edit().clear().putInt("points", 2).putInt("up3", 4).commit();
            PlayerProfile experience = repository.load();
            check(experience.upgradePoints() == 2 && experience.upgradeLevel(3) == 4, "Experience tiers become generation");
            repository.save(experience);
            check(repository.load().upgradePoints() == 2 && repository.load().upgradeLevel(3) == 4, "generation migration round trip");
            PlayerProfile half = PlayerProfile.restore(0, 0, new int[11], true, true);
            half.completeStage(1); repository.save(half);
            PlayerProfile fraction = repository.load();
            check(fraction.upgradePoints() == .5f && !fraction.purchaseUpgrade(0), "saved half point cannot purchase");
            fraction.completeStage(2); repository.save(fraction);
            PlayerProfile whole = repository.load();
            check(whole.upgradePoints() == 1 && whole.purchaseUpgrade(0), "two saved clears buy one upgrade");
            repository.save(whole);
            check(repository.load().upgradePoints() == 0 && repository.load().upgradeLevel(0) == 1, "purchase round trip");
            result.putString("stream", "Profile Android checks passed: legacy/current preference mapping, settings, tier refunds and save round trips.\n");
        } catch (Throwable failure) {
            code = 0; result.putString("stream", android.util.Log.getStackTraceString(failure));
        } finally {
            SharedPreferences.Editor editor = preferences.edit().clear();
            for (Map.Entry<String, ?> entry : original.entrySet()) {
                Object value = entry.getValue(); String key = entry.getKey();
                if (value instanceof Integer) editor.putInt(key, (Integer) value);
                else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
                else if (value instanceof Long) editor.putLong(key, (Long) value);
                else if (value instanceof Float) editor.putFloat(key, (Float) value);
                else if (value instanceof String) editor.putString(key, (String) value);
                else if (value instanceof Set) editor.putStringSet(key, new HashSet<>((Set<String>) value));
            }
            editor.commit();
        }
        finish(code, result);
    }
}
