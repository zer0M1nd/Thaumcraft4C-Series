package tc4revival.content;

/** Old ItemShard metadata, texture multipliers and names; order is the legacy creative order. */
public enum ShardType {
    AIR(0, "air", 0xFFFF7E, "Air Shard", "风之碎片"),
    FIRE(1, "fire", 16727041, "Fire Shard", "火之碎片"),
    WATER(2, "water", 37119, "Water Shard", "水之碎片"),
    EARTH(3, "earth", 40960, "Earth Shard", "地之碎片"),
    ORDER(4, "order", 0xEECCFF, "Order Shard", "秩序碎片"),
    ENTROPY(5, "entropy", 0x555577, "Entropy Shard", "混沌碎片"),
    BALANCED(6, "balanced", 0xFFFFFF, "Balanced Shard", "平衡碎片");

    private final int metadata;
    private final String material;
    private final int color;
    private final String english;
    private final String chinese;
    ShardType(int metadata, String material, int color, String english, String chinese) {
        this.metadata = metadata;
        this.material = material;
        this.color = color;
        this.english = english;
        this.chinese = chinese;
    }
    public int metadata() { return metadata; }
    public String material() { return material; }
    public String path() { return material + "_shard"; }
    public String crystalClusterPath() { return (this == BALANCED ? "mixed" : material) + "_crystal_cluster"; }
    public String id() { return "thaumcraft:" + path(); }
    public String texture() { return this == BALANCED ? "shard_balanced" : "shard"; }
    public int tint() { return 0xFF000000 | color; }
    public String name(boolean chinese) { return chinese ? this.chinese : english; }
}
