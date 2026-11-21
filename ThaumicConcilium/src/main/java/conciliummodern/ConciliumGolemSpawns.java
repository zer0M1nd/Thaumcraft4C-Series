package conciliummodern;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.biome.*;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.world.*;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.registries.*;

public final class ConciliumGolemSpawns implements BiomeModifier {
    public static final ForgeConfigSpec SPEC;public static final ForgeConfigSpec.IntValue WEIGHT;
    public static final ForgeConfigSpec.ConfigValue<java.util.List<? extends String>> BLACKLIST;
    static{var builder=new ForgeConfigSpec.Builder();builder.push("mobs");WEIGHT=builder.comment("复仇傀儡自然生成权重，默认5；0禁用，仅 Automagy 存在时启用。", "Vengeful golem spawn weight; default 5, zero disables; requires Automagy.").defineInRange("vengefulGolemSpawnChance",5,0,1000000);BLACKLIST=builder.comment("禁止自然生成的群系注册名。", "Biome registry names excluded from natural spawning.").defineListAllowEmpty("vengefulGolemBiomeBlacklist",java.util.List.of(),v->v instanceof String text&&ResourceLocation.tryParse(text)!=null);builder.pop();SPEC=builder.build();}
    public static final DeferredRegister<Codec<? extends BiomeModifier>> CODECS=DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,ThaumicConcilium.ID);
    public static final RegistryObject<Codec<ConciliumGolemSpawns>> CODEC=CODECS.register("vengeful_golem",()->Codec.unit(ConciliumGolemSpawns::new));
    @Override public void modify(Holder<Biome> biome,Phase phase,ModifiableBiomeInfo.BiomeInfo.Builder builder){
        if(phase!=Phase.ADD||!net.minecraftforge.fml.ModList.get().isLoaded("automagy")||WEIGHT.get()<=0||biome.unwrapKey().map(k->BLACKLIST.get().contains(k.location().toString())).orElse(false))return;
        builder.getMobSpawnSettings().addSpawn(MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(ConciliumGolems.VENGEFUL.get(),WEIGHT.get(),1,2));
    }
    @Override public Codec<? extends BiomeModifier> codec(){return CODEC.get();}
    public static void placements(SpawnPlacementRegisterEvent event){event.register(ConciliumGolems.VENGEFUL.get(),SpawnPlacements.Type.ON_GROUND,Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,Monster::checkMonsterSpawnRules,SpawnPlacementRegisterEvent.Operation.REPLACE);}
}
