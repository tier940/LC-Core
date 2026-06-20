package buildcraft.silicon.gate;

// Stub for the class removed in flyingperson23's BuildCraft; satisfies AkutoEngine classloading only.
public enum EnumGateModifier {

    NO_MODIFIER(0),
    LAPIS(1),
    QUARTZ(2),
    DIAMOND(3);

    private final int tier;

    EnumGateModifier(int tier) {
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    public String getTag() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
