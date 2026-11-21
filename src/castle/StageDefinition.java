package castle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable stage data, parsed once; every match receives independent mutable objects. */
final class StageDefinition {
    private static final List<StageDefinition> ALL = createCatalog();
    final int number;
    final Campaign campaign;
    final String name, brief;
    final List<Building> buildings;
    final List<Region> regions;
    final List<Altar> altars;

    private StageDefinition(Campaign campaign, int index) {
        this.campaign = campaign;
        number = campaign.stageAt(index);
        name = campaign.names()[index];
        brief = campaign.briefs()[index];
        ArrayList<Building> buildings = new ArrayList<>();
        for (String record : records(campaign.buildings()[index])) buildings.add(new Building(record));
        this.buildings = Collections.unmodifiableList(buildings);
        ArrayList<Region> regions = new ArrayList<>();
        for (String record : records(campaign.regions()[index])) regions.add(new Region(record));
        this.regions = Collections.unmodifiableList(regions);
        ArrayList<Altar> altars = new ArrayList<>();
        for (String record : records(campaign.altars()[index])) altars.add(new Altar(record));
        this.altars = Collections.unmodifiableList(altars);
    }

    private static List<StageDefinition> createCatalog() {
        ArrayList<StageDefinition> stages = new ArrayList<>();
        for (Campaign campaign : Campaign.values()) {
            for (String[] values : new String[][]{campaign.buildings(), campaign.regions(), campaign.altars(), campaign.names(), campaign.briefs()})
                if (values.length != Campaign.STAGES_PER_CAMPAIGN) throw new IllegalStateException("Incomplete campaign: " + campaign);
            for (int index = 0; index < Campaign.STAGES_PER_CAMPAIGN; index++) stages.add(new StageDefinition(campaign, index));
        }
        return Collections.unmodifiableList(stages);
    }

    static StageDefinition get(int stage) {
        Campaign.forStage(stage);
        return ALL.get(stage - 1);
    }

    private static String[] records(String value) { return value.isEmpty() ? new String[0] : value.split(";"); }
    private static float number(String value) {
        float parsed = Float.parseFloat(value);
        if (!Float.isFinite(parsed)) throw new IllegalArgumentException("Non-finite stage coordinate");
        return parsed;
    }

    void populate(Engine engine) {
        for (Building building : buildings) {
            Engine.Fort fort = new Engine.Fort(building.x, building.y, 500, building.side, building.kind);
            fort.visualX = building.visualX; fort.visualY = building.visualY; fort.size = building.size;
            engine.forts.add(fort);
        }
        for (Region region : regions) engine.marshes.add(new Engine.Marsh(region.points.clone(), region.slow));
        for (Altar altar : altars) engine.altars.add(new Engine.Altar(altar.x, altar.y, altar.buff));
    }

    static final class Building {
        final float x, y, visualX, visualY;
        final int side, kind;
        final char size;
        Building(String record) {
            String[] fields = record.split(",");
            if (fields.length != 5 || !fields[0].matches("[grby]_(house_[bms]|tower_[bms]|mine)"))
                throw new IllegalArgumentException("Invalid building: " + record);
            String name = fields[0]; side = "grby".indexOf(name.charAt(0));
            kind = name.contains("mine") ? Engine.CRYSTAL : name.contains("tower") ? Engine.ARCHERY : Engine.GENERATOR;
            size = name.charAt(name.length() - 1);
            x = number(fields[1]); y = number(fields[2]); visualX = number(fields[3]); visualY = number(fields[4]);
        }
    }

    static final class Region {
        final boolean slow;
        private final float[] points;
        Region(String record) {
            String[] fields = record.split(",");
            if (fields.length < 7 || fields.length % 2 != 1 || !(fields[0].equals("50") || fields[0].equals("51")))
                throw new IllegalArgumentException("Invalid terrain region");
            slow = fields[0].equals("51"); points = new float[fields.length - 1];
            for (int i = 1; i < fields.length; i++) points[i - 1] = number(fields[i]);
        }
    }

    static final class Altar {
        final float x, y;
        final int buff;
        Altar(String record) {
            String[] fields = record.split(",");
            if (fields.length != 3 || !(fields[2].equals("100") || fields[2].equals("101")))
                throw new IllegalArgumentException("Invalid altar: " + record);
            x = number(fields[0]); y = number(fields[1]); buff = fields[2].equals("100") ? 1 : 2;
        }
    }
}
