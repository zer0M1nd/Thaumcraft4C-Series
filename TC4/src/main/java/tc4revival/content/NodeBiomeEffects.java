package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import tc4revival.config.TCConfig;

/** Node-driven conversion uses actual quart cells, matching the cells written by WorldBiomeChanges. */
 class NodeBiomeEffects {
    private NodeBiomeEffects() {}
    static void tick(ServerLevel level,BlockPos origin,AuraNodeType type) {
        var random=level.random;
        if(type==AuraNodeType.TAINTED) {
            WorldBiomeChanges.changeNodeBiome(level,origin,origin.offset(random.nextInt(8)-random.nextInt(8),0,random.nextInt(8)-random.nextInt(8)),TCBiomes.TAINTED_LAND);
            if(TCConfig.SERVER.hardNodes.get() && random.nextBoolean()) {
                int x=random.nextInt(5)-random.nextInt(5),z=random.nextInt(5)-random.nextInt(5),y=random.nextInt(5)-random.nextInt(5);
                TaintFibreBlock.spread(level,origin.offset(x,y,z));
            }
        } else if(type==AuraNodeType.DARK && darkDimensionAllowed(level)) {
            WorldBiomeChanges.changeNodeBiome(level,origin,origin.offset(random.nextInt(12)-random.nextInt(12),0,random.nextInt(12)-random.nextInt(12)),TCBiomes.EERIE);
            if(TCConfig.SERVER.hardNodes.get() && random.nextBoolean()) spawnGiant(level,origin);
        }
    }
    static boolean darkDimensionAllowed(ServerLevel level) { return level.dimension()!=Level.NETHER && level.dimension()!=Level.END; }
    static boolean spawnGiant(ServerLevel level,BlockPos origin) {
        if(level.getNearestPlayer(origin.getX()+.5,origin.getY()+.5,origin.getZ()+.5,24,false)==null
                || level.getEntitiesOfClass(GiantBrainyZombieEntity.class,new AABB(origin).inflate(10,6,10)).size()>3) return false;
        var random=level.random;var mob=TCEntities.GIANT_BRAINY_ZOMBIE.get().create(level);if(mob==null)return false;
        mob.moveTo(origin.getX()+(random.nextDouble()-random.nextDouble())*5,origin.getY()+random.nextInt(3)-1,
                origin.getZ()+(random.nextDouble()-random.nextDouble())*5,random.nextFloat()*360,0);
        if(!level.hasChunkAt(mob.blockPosition()) || !level.getWorldBorder().isWithinBounds(mob.blockPosition())
                || !EerieSpawning.legacyDarkness(level,mob.blockPosition(),random) || !mob.checkSpawnRules(level,MobSpawnType.NATURAL)
                || !mob.checkSpawnObstruction(level)) return false;
        // TileNode did not call onSpawnWithEgg: no equipment or leader initialization here.
        if(!level.addFreshEntity(mob))return false;
        level.levelEvent(2004,origin,0);mob.spawnAnim();return true;
    }
}
