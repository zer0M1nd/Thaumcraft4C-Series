package thmodern.creature;

import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import thmodern.Horizons;

@Mod.EventBusSubscriber(modid=Horizons.ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class CreatureAttributes {
    @SubscribeEvent public static void register(EntityAttributeCreationEvent event){
        for(var type:PlanarSlimes.TYPES.values())event.put(type.get(),net.minecraft.world.entity.Mob.createMobAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).build());
        event.put(Horizons.SOUL.get(),thmodern.containment.SoulEntity.attributes().build());
        event.put(Horizons.GUARDIAN_PANTHER.get(),net.minecraft.world.entity.animal.Cat.createAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,24).add(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,.5).add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,6).build());
        for(var type:java.util.List.of(Horizons.GRAVEKEEPER,Horizons.FAMILIAR))event.put(type.get(),net.minecraft.world.entity.animal.Cat.createAttributes().build());
        event.put(Horizons.NIGHTMARE.get(),net.minecraft.world.entity.animal.horse.Horse.createBaseHorseAttributes().build());
        for(var type:java.util.List.of(Horizons.ENDERSTEED,Horizons.UNDEAD_HORSE,Horizons.SKELETAL_HORSE))event.put(type.get(),net.minecraft.world.entity.animal.horse.Horse.createBaseHorseAttributes().build());
        for(var type:java.util.List.of(Horizons.SEAWOLF,Horizons.LUNAR_WOLF,Horizons.NETHER_HOUND))event.put(type.get(),net.minecraft.world.entity.animal.Wolf.createAttributes().build());
        event.put(Horizons.SHEEDER.get(),net.minecraft.world.entity.monster.Spider.createAttributes().build());
        event.put(Horizons.TAINT_EATER_PIG.get(),net.minecraft.world.entity.animal.Pig.createAttributes().build());
        event.put(Horizons.ORE_PIG.get(),net.minecraft.world.entity.animal.Pig.createAttributes().build());
        event.put(Horizons.CHOCOLATE_COW.get(),net.minecraft.world.entity.animal.Cow.createAttributes().build());
        event.put(Horizons.CHROMATIC_SHEEP.get(),net.minecraft.world.entity.animal.Sheep.createAttributes().build());
        event.put(Horizons.SELF_SHEARING_SHEEP.get(),net.minecraft.world.entity.animal.Sheep.createAttributes().build());
        event.put(Horizons.SCHOLAR_CHICKEN.get(),net.minecraft.world.entity.animal.Chicken.createAttributes().build());
        event.put(Horizons.GOLD_CHICKEN.get(),net.minecraft.world.entity.animal.Chicken.createAttributes().build());
    }
}
