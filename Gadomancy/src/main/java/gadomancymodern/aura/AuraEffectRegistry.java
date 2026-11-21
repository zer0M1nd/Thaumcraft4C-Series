package gadomancymodern.aura;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

public class AuraEffectRegistry {
    protected static final java.util.Map<String,AuraEffect> EFFECTS=new java.util.LinkedHashMap<>();
    public static void register(String aspect,AuraEffect effect){java.util.Objects.requireNonNull(effect);if(effect.interval()<1||!Double.isFinite(effect.range())||effect.range()<1||effect.range()>128)throw new IllegalArgumentException("Invalid aura interval/range");EFFECTS.put(java.util.Objects.requireNonNull(aspect),effect);}
    public static java.util.Map<String,AuraEffect> effects(){return java.util.Collections.unmodifiableMap(EFFECTS);}
    public static void distribute(String aspect,ServerLevel level,BlockPos origin,int ticks){
        var effect=EFFECTS.get(aspect);if(effect==null||ticks%effect.interval()!=0||AuraEffectsConfig.BLACKLIST.get().contains(aspect))return;
        for(var entity:level.getEntities((net.minecraft.world.entity.Entity)null,new AABB(origin).inflate(effect.range()),entity->entity.isAlive()&&effect.accepts(entity)))effect.entity(level,origin,entity);
        int count=effect.blockCount(level.random),range=(int)effect.range();var visited=new java.util.HashSet<BlockPos>();
        // Legacy retries duplicate samples. Bound retries so an addon cannot hang a server with an impossible count.
        int tries=0,maximumAttempts=Math.max(64,Math.min(100000,count*16));
        while(visited.size()<count&&tries++<maximumAttempts){var pos=origin.offset(level.random.nextInt(range)-level.random.nextInt(range),level.random.nextInt(range)-level.random.nextInt(range),level.random.nextInt(range)-level.random.nextInt(range));if(visited.add(pos)&&level.hasChunkAt(pos))effect.block(level,origin,pos);}
        var catalog=tc4revival.aspect.TCAspects.catalog();var knownAspect=catalog.find(aspect);if(knownAspect.isEmpty())return;
        for(var player:level.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class,new AABB(origin).inflate(effect.range()))){var data=tc4revival.research.KnowledgeSavedData.get(level.getServer());var knowledge=data.snapshot(player.getUUID());var key="gadomancy:aura/"+aspect.replace(':','/');if(knowledge.isPresent()&&knowledge.get().researchMarkers().contains("gadomancy:aura_effects")&&knowledge.get().knows(knownAspect.get())&&!knowledge.get().researchMarkers().contains(key)){data.update(player.getUUID(),player.getRandom()::nextInt,state->state.rememberResearch(key));player.displayClientMessage(net.minecraft.network.chat.Component.translatable("gadomancy.aura.research.unlock",aspect),true);}}
    }
}
