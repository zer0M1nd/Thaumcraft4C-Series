package thaumicomniverse.compat;

import dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry;
import dev.shadowsoffire.apotheosis.adventure.socket.gem.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import tc4revival.content.*;

/** Random pool eligibility is in the gem JSON; every outer-lands boss also guarantees one. */
public final class WarpedGemDrops {
    private static final ResourceLocation GEM=ResourceLocation.parse("thaumicomniverse:warped");
    private static final ResourceLocation OUTER=ResourceLocation.parse("thaumcraft:outer_lands");
    public static void setup(){MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST,WarpedGemDrops::drops);}
    private static void drops(LivingDropsEvent event) {
        var mob=event.getEntity();
        if(mob.level().isClientSide||!mob.level().dimension().location().equals(OUTER))return;
        if(!(mob instanceof ThaumcraftBossEntity || mob instanceof GiantTaintacleEntity || mob instanceof CultistPortalEntity))return;
        var gem=GemRegistry.INSTANCE.getValue(GEM);if(gem==null)return;
        event.getDrops().removeIf(drop->drop.getItem().getItem() instanceof GemItem && GEM.equals(GemItem.getGem(drop.getItem()).getId()));
        var rarity=RarityRegistry.byLegacyId(mob.getRandom().nextBoolean()?"epic":"mythic").get();
        event.getDrops().add(new ItemEntity(mob.level(),mob.getX(),mob.getY(),mob.getZ(),GemRegistry.createGemStack(gem,rarity)));
    }
    private WarpedGemDrops() {}
}
