package castle;

import android.content.Context;
import android.content.SharedPreferences;

/** Maps Android preferences and legacy save fields to the platform-free profile model. */
final class AndroidProfileRepository implements ProfileRepository {
    private final SharedPreferences preferences;

    AndroidProfileRepository(Context context) {
        preferences = context.getSharedPreferences("original-campaign", Context.MODE_PRIVATE);
    }

    @Override public PlayerProfile load() {
        int[] levels = new int[PlayerProfile.UPGRADE_COUNT];
        int points = preferences.getInt("points", 0);
        if (preferences.getBoolean("five-upgrades", false)) {
            for (int index = 0; index < 3; index++) levels[index] = preferences.getInt("up" + index, 0);
            levels[5] = preferences.getInt("up3", 0);
            int sharedSpellLevel = preferences.getInt("up4", 0);
            for (int index = 6; index <= 9; index++) levels[index] = Math.min(UpgradeRules.MAX_LEVEL, sharedSpellLevel);
            points += Math.max(0, sharedSpellLevel - UpgradeRules.MAX_LEVEL);
        } else {
            for (int index = 0; index < levels.length; index++) levels[index] = preferences.getInt("up" + index, 0);
        }
        return PlayerProfile.restore(preferences.getInt("completed", 0), points, levels,
            preferences.getBoolean("music", true), preferences.getBoolean("sound", true));
    }

    @Override public void save(PlayerProfile profile) {
        SharedPreferences.Editor editor = preferences.edit()
            .putInt("completed", profile.completedStages()).putInt("points", profile.upgradePoints())
            .putBoolean("five-upgrades", false).putBoolean("music", profile.musicEnabled())
            .putBoolean("sound", profile.soundEnabled());
        for (int index = 0; index < profile.upgradeCount(); index++)
            editor.putInt("up" + index, profile.upgradeLevel(index));
        editor.apply();
    }
}
