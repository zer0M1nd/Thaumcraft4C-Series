package tc4revival.data;

import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import tc4revival.aspect.entity.EntityAspectJson;

public  class EntityAspectProvider implements DataProvider {
    private final PackOutput output;
    public EntityAspectProvider(PackOutput output) { this.output = output; }
    @Override public CompletableFuture<?> run(CachedOutput cache) {
        var root=output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("thaumcraft/entity_aspects");
        var taint=new java.util.ArrayList<>(java.util.List.of(
                new tc4revival.aspect.entity.EntityAspectRule("thaumcraft:taint_spider","Thaumcraft.TaintSpider",
                        tc4revival.aspect.entity.EntityAspectRule.Variant.ANY,"",java.util.List.of(
                        new tc4revival.aspect.entity.EntityAspectRule.Quantity("vitium",1),new tc4revival.aspect.entity.EntityAspectRule.Quantity("terra",1))),
                new tc4revival.aspect.entity.EntityAspectRule("thaumcraft:taint_spore","Thaumcraft.TaintSpore",
                        tc4revival.aspect.entity.EntityAspectRule.Variant.ANY,"",java.util.List.of(
                        new tc4revival.aspect.entity.EntityAspectRule.Quantity("vitium",2),new tc4revival.aspect.entity.EntityAspectRule.Quantity("aer",2)))));
        for (int i=0;i<3;i++) {
            var quantities=new java.util.ArrayList<tc4revival.aspect.entity.EntityAspectRule.Quantity>();
            quantities.add(new tc4revival.aspect.entity.EntityAspectRule.Quantity("humanus",2)); quantities.add(new tc4revival.aspect.entity.EntityAspectRule.Quantity("praecantatio",i==2 ? 4 : 2)); quantities.add(new tc4revival.aspect.entity.EntityAspectRule.Quantity("permutatio",2));
            if (i!=2) quantities.add(new tc4revival.aspect.entity.EntityAspectRule.Quantity(i==0 ? "lucrum" : "telum",2));
            taint.add(new tc4revival.aspect.entity.EntityAspectRule("thaumcraft:pech","Thaumcraft.Pech",new tc4revival.aspect.entity.EntityAspectRule.Variant[]{tc4revival.aspect.entity.EntityAspectRule.Variant.PECH_FORAGER,tc4revival.aspect.entity.EntityAspectRule.Variant.PECH_MAGE,tc4revival.aspect.entity.EntityAspectRule.Variant.PECH_ARCHER}[i],"PechType"+i,quantities));
        }
        for (var row:new String[][]{
                {"primal_orb","PrimalOrb","aer=5 perditio=10 praecantatio=10 potentia=10"},
                {"thaumic_slime","ThaumSlime","limus=2 praecantatio=1 aqua=1"},
                {"taint_hive","TaintSwarmer","vitium=2 aer=2"},
                {"taint_swarm","TaintSwarm","vitium=3 aer=3"},
                {"tainted_chicken","TaintedChicken","vitium=2 volatus=2 aer=1"},
                {"tainted_cow","TaintedCow","vitium=3 terra=3"},
                {"tainted_pig","TaintedPig","vitium=2 terra=2"},
                {"tainted_sheep","TaintedSheep","vitium=2 terra=2"},
                {"tainted_villager","TaintedVillager","vitium=3 aer=2"},
                {"tainted_creeper","TaintedCreeper","vitium=2 ignis=2"},
                {"brainy_zombie","BrainyZombie","exanimis=3 humanus=1 cognitio=1 terra=1"},
                {"giant_brainy_zombie","GiantBrainyZombie","exanimis=4 humanus=2 cognitio=1 terra=2"},
                {"golem","Golem","aer=2 terra=2 praecantatio=2"},
                {"taintacle","Taintacle","vitium=3 aqua=2"},{"taintacle_small","TaintacleTiny","vitium=1 aqua=1"},
                {"fire_bat","Firebat","bestia=2 volatus=1 ignis=2"},
                {"mind_spider","MindSpider","vitium=2 ignis=2"},
                {"eldritch_guardian","EldritchGuardian","alienis=4 mortuus=2 exanimis=4"},{"eldritch_orb","EldritchOrb","alienis=2 mortuus=2"},
                {"cultist_knight","CultistKnight","alienis=1 humanus=2 perditio=1"},{"cultist_cleric","CultistCleric","alienis=1 humanus=2 perditio=1"}}) {
            var quantities=java.util.Arrays.stream(row[2].split(" ")).map(value->{ var pair=value.split("="); return new tc4revival.aspect.entity.EntityAspectRule.Quantity(pair[0],Integer.parseInt(pair[1])); }).toList();
            taint.add(new tc4revival.aspect.entity.EntityAspectRule("thaumcraft:"+row[0],"Thaumcraft."+row[1],tc4revival.aspect.entity.EntityAspectRule.Variant.ANY,"",quantities));
        }
        return CompletableFuture.allOf(DataProvider.saveStable(cache,EntityAspectJson.encode(LegacyEntityDefinitions.rules()),root.resolve("vanilla.json")),
                DataProvider.saveStable(cache,EntityAspectJson.encode(taint),root.resolve("taint.json")));
    }
    @Override public String getName() { return "TC4 vanilla entity aspect mappings"; }
}
