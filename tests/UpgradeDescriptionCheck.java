package castle;

public final class UpgradeDescriptionCheck {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        String[] base = {"50 damage/tick", "50 castle troops", "Castle/crystal 50; archer 60", "50 troops / 1.25s",
            "200 crystals", "25 base + 40", "5 × 40 damage = 200", "50 damage / 0.4s for 10s",
            "Enemy troops stopped for 5s", "1 group × 100 troops = 100", "Meteor 10 · Tornado 8 · Mutiny 15 · Reinforce 8"};
        String[] maximum = {"100 damage/tick", "25 castle troops", "Castle/crystal 75; archer 90", "75 troops / 1.25s",
            "450 crystals", "50 base + 80", "5 × 70 damage = 350", "87.5 damage / 0.4s for 17.5s",
            "Enemy troops stopped for 8.75s", "4 groups × 200 troops = 800", "Meteor 6.67 · Tornado 5.33 · Mutiny 10 · Reinforce 5.33"};
        for (int index = 0; index < base.length; index++) {
            check(UpgradeDescription.forLevel(index, 0).current.startsWith(base[index]), "base upgrade " + index);
            UpgradeDescription last = UpgradeDescription.forLevel(index, 5);
            check(last.current.startsWith(maximum[index]) && last.next == null, "maximum upgrade " + index);
            for (int level = 0; level < 5; level++) {
                UpgradeDescription description = UpgradeDescription.forLevel(index, level);
                check(description.next.equals(UpgradeDescription.forLevel(index, level + 1).current), "next/current tier continuity");
                check(!description.current.contains("%"), "upgrade descriptions show actual quantities");
            }
        }
        check(UpgradeDescription.forLevel(7, 1).current.contains("57.5"), "fractional tornado damage is not rounded to 58");
        System.out.println("Upgrade descriptions passed: all eleven upgrades, six tiers, exact examples, fractional values and max-tier display.");
    }
}
