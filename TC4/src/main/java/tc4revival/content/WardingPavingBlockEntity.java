package tc4revival.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
public  class WardingPavingBlockEntity extends BlockEntity {
    private int count;
    public WardingPavingBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.WARDING_PAVING_ENTITY.get(),pos,state); }
    public void tick() {
        if(level==null||level.isClientSide)return;
        if(count==0)count=level.random.nextInt(100);
        if(count%5==0&&!level.hasNeighborSignal(worldPosition)) {
            var area=new AABB(worldPosition.getX(),worldPosition.getY(),worldPosition.getZ(),worldPosition.getX()+1,worldPosition.getY()+3,worldPosition.getZ()+1).inflate(.1);
            for(var entity:level.getEntitiesOfClass(LivingEntity.class,area)) if(!entity.onGround()&&!(entity instanceof Player)) {
                float radians=(entity.getYRot()+180)*(float)Math.PI/180;
                entity.push(-net.minecraft.util.Mth.sin(radians)*.2f,-.1,net.minecraft.util.Mth.cos(radians)*.2f);
            }
        }
        if(++count%100==0)for(int height=1;height<=2;height++) {
            var pos=worldPosition.above(height);var state=level.getBlockState(pos);
            if(!state.is(TCArcane.LAMP_AIR.get())&&state.canBeReplaced())level.setBlock(pos,TCArcane.WARDING_BARRIER.get().defaultBlockState(),3);
        }
    }
}
