package castle;
public final class StageCatalogCheck {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    public static void main(String[] args) {
        for (int stage = 1; stage <= Campaign.TOTAL_STAGES; stage++) {
            StageDefinition definition = StageDefinition.get(stage);
            Campaign campaign = Campaign.forStage(stage);
            check(definition == StageDefinition.get(stage), "definitions are cached");
            check(definition.campaign == campaign && campaign.stageAt(campaign.localIndex(stage)) == stage, "campaign stage mapping");
            Engine first = new Engine(stage,0), second = new Engine(stage,0);
            check(first.forts.size() == definition.buildings.size() && first.marshes.size() == definition.regions.size(), "catalog populates full stage");
            check(!definition.name.isEmpty() && !definition.brief.isEmpty(), "all stages have current copy");
            float count = second.forts.get(0).n;
            first.forts.get(0).n = 1;
            check(second.forts.get(0).n == count, "match garrisons are independent");
            if (!first.marshes.isEmpty()) {
                float point = second.marshes.get(0).points[0];
                first.marshes.get(0).points[0] += 10;
                check(second.marshes.get(0).points[0] == point, "match polygon arrays are independent");
                check(new Engine(stage,0).marshes.get(0).points[0] == point, "catalog geometry stays immutable");
            }
            try { definition.buildings.clear(); throw new AssertionError("mutable definition"); }
            catch (UnsupportedOperationException expected) {}
        }
        for (int stage : new int[]{0,91}) {
            try { StageDefinition.get(stage); throw new AssertionError("invalid stage accepted"); }
            catch (IllegalArgumentException expected) {}
        }
        System.out.println("Stage catalog passed: all 90 mappings, cached immutable definitions and isolated match state.");
    }
}
