package tc4revival.content;

import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.entity.living.LivingEvent;

/** Claim generated workplaces only after chunks and their POI sections have reached the server. */
public class VillageWorkstations {
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Villager villager) || !(villager.level() instanceof ServerLevel level)) return;
        var data=villager.getPersistentData();
        if (data.contains("TC4VillageWorkstation") && (villager.tickCount<=1 || villager.tickCount%20==0)) {
            var pos=NbtUtils.readBlockPos(data.getCompound("TC4VillageWorkstation"));
            if (level.hasChunkAt(pos)) {
                var state=level.getBlockState(pos);
                var profession=state.is(TCArcane.BLOCK.get())?TCVillagers.WIZARD_PROFESSION.get():
                        state.is(TCArcane.WAND_PEDESTAL.get())?TCVillagers.BANKER_PROFESSION.get():null;
                if (profession==null || villager.isBaby() || villager.getVillagerXp()>0
                        || villager.getBrain().hasMemoryValue(MemoryModuleType.JOB_SITE)) data.remove("TC4VillageWorkstation");
                else {
                    level.getPoiManager().ensureLoadedAndValid(level,pos,1);
                    var claimed=level.getPoiManager().take(profession.heldJobSite(),(type,p)->p.equals(pos),pos,1);
                    if (claimed.isPresent()) {
                        villager.setVillagerData(villager.getVillagerData().setProfession(profession));
                        villager.getBrain().eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
                        villager.getBrain().setMemory(MemoryModuleType.JOB_SITE,GlobalPos.of(level.dimension(),pos));
                        villager.refreshBrain(level);
                        data.remove("TC4VillageWorkstation");
                    }
                }
            }
        }
        // Old releases saved zero-XP, zero-price-multiplier offers. Without migration those
        // residents can never earn a level, even with the new vanilla trading logic installed.
        if (TCVillagers.role(villager)!=null && !data.getBoolean("TC4ModernTrades")) {
            var offers=villager.getOffers();
            if (!offers.isEmpty() && offers.stream().allMatch(o->o.getXp()==0 && o.getPriceMultiplier()==0)) {
                villager.setOffers(null);
                villager.overrideXp(0);
                villager.setVillagerData(villager.getVillagerData().setLevel(1));
                villager.getOffers();
            }
            data.putBoolean("TC4ModernTrades",true);
        }
    }
}
