package castle;

import java.util.Locale;

/** Formats current/next upgrade values from the same domain formulas used by the simulation. */
final class UpgradeDescription {
    final String title, current, next;
    private static final String[] TITLES = {
        "Attack · 500 castle troops", "Defense · 50 incoming damage", "Movement speed · clear ground",
        "Castle troop generation", "Starting crystals", "Crystal income · every 6 seconds",
        "Meteor · 5 impacts in a 100×100 area", "Tornado · moving 50×50 area",
        "Mutiny · 100×100 area", "Reinforcements", "Spell cooldowns · seconds"
    };

    private UpgradeDescription(int upgrade, int level) {
        title = TITLES[upgrade];
        current = value(upgrade, level);
        next = level < UpgradeRules.MAX_LEVEL ? value(upgrade, level + 1) : null;
    }

    static UpgradeDescription forLevel(int upgrade, int level) {
        if (upgrade < 0 || upgrade >= TITLES.length || level < 0 || level > UpgradeRules.MAX_LEVEL)
            throw new IllegalArgumentException("Unknown upgrade or tier");
        return new UpgradeDescription(upgrade, level);
    }

    private static String number(float value) {
        return String.format(Locale.US, "%.2f", value).replaceAll("\\.?0+$", "");
    }

    private static String value(int upgrade, int level) {
        switch (upgrade) {
            case 0:
                return number(Engine.siegeAttack(500, Engine.GENERATOR, level)) + " damage/tick before enemy defense";
            case 1:
                return number(50 / UpgradeRules.defenseDivisor(level)) + " castle troops lost/tick; no shield";
            case 2:
                return "Castle/crystal " + number(UpgradeRules.movementSpeed(Engine.GENERATOR, level))
                    + "; archer " + number(UpgradeRules.movementSpeed(Engine.ARCHERY, level)) + " units/s";
            case 3:
                return UpgradeRules.castleGeneration(level) + " troops / 1.25s";
            case 4:
                return number(UpgradeRules.initialCrystals(level)) + " crystals at stage start";
            case 5:
                return number(UpgradeRules.crystalIncome(level, 0)) + " base + "
                    + number(UpgradeRules.crystalIncome(level, 1) - UpgradeRules.crystalIncome(level, 0)) + " per owned crystal";
            case 6:
                float impact = UpgradeRules.meteorDamage(level);
                return "5 × " + number(impact) + " damage = " + number(impact * 5) + " total";
            case 7:
                return number(UpgradeRules.stormDamage(level)) + " damage / 0.4s for "
                    + number(UpgradeRules.stormDuration(level)) + "s";
            case 8:
                return "Enemy troops stopped for " + number(UpgradeRules.mutinyDuration(level)) + "s";
            case 9:
                int groups = Engine.reinforcementGroups(level);
                return groups + (groups == 1 ? " group × " : " groups × ") + Engine.reinforcementPerGroup(level)
                    + " troops = " + Engine.reinforcementAmount(level) + " total";
            default:
                return "Meteor " + number(UpgradeRules.spellCooldown(0, level))
                    + " · Tornado " + number(UpgradeRules.spellCooldown(1, level))
                    + " · Mutiny " + number(UpgradeRules.spellCooldown(2, level))
                    + " · Reinforce " + number(UpgradeRules.spellCooldown(3, level));
        }
    }
}
