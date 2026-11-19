package tc4revival.content;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

/** The original ConfigEntities egg mappings omitted during the entity port. */
public final class LegacyMobEggs {
    public record Entry(String path,String legacyName,Supplier<? extends EntityType<? extends Mob>> type,int primary,int secondary) {}
    private LegacyMobEggs() {}
    public static List<Entry> entries(){return List.of(
            new Entry("fire_bat","Firebat",()->TCEntities.FIRE_BAT.get(),0xffc0ff,0xc00000),
            new Entry("mind_spider","MindSpider",()->TCEntities.MIND_SPIDER.get(),0xaaaaaa,0x404040),
            new Entry("eldritch_guardian","EldritchGuardian",()->TCEntities.ELDRITCH_GUARDIAN.get(),0x222222,0x404040),
            new Entry("eldritch_warden","EldritchWarden",()->TCEntities.ELDRITCH_WARDEN.get(),0x552222,0x404040),
            new Entry("cultist_knight","CultistKnight",()->TCEntities.CULTIST_KNIGHT.get(),0xff5055,0x000080),
            new Entry("cultist_cleric","CultistCleric",()->TCEntities.CULTIST_CLERIC.get(),0xff5055,0x800000),
            new Entry("cultist_leader","CultistLeader",()->TCEntities.CULTIST_LEADER.get(),0xff5055,0x505050),
            new Entry("cultist_portal","CultistPortal",()->TCEntities.CULTIST_PORTAL.get(),0xff5055,0xff50ff),
            new Entry("eldritch_golem","EldritchGolem",()->TCEntities.ELDRITCH_GOLEM.get(),0x555555,0x404040),
            new Entry("eldritch_crab","EldritchCrab",()->TCEntities.ELDRITCH_CRAB.get(),0x555555,0x550000),
            new Entry("inhabited_zombie","InhabitedZombie",()->TCEntities.INHABITED_ZOMBIE.get(),0x557755,0x550000),
            new Entry("thaumic_slime","ThaumSlime",()->TCEntities.THAUMIC_SLIME.get(),0xffc0ff,0xff80ff),
            new Entry("taint_spider","TaintSpider",()->TCEntities.TAINT_SPIDER.get(),0xffc0ff,0x404040),
            new Entry("taintacle","Taintacle",()->TCEntities.TAINTACLE.get(),0xffc0ff,0x800080),
            new Entry("taintacle_small","TaintacleTiny",()->TCEntities.TAINTACLE_SMALL.get(),0xffc0ff,0x800090),
            new Entry("taint_spore","TaintSpore",()->TCEntities.TAINT_SPORE.get(),0xffc0ff,0x800070));}
}
