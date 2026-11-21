package castle;

/** Save boundary: presentation works with profiles rather than storage keys. */
interface ProfileRepository {
    PlayerProfile load();
    void save(PlayerProfile profile);
}
