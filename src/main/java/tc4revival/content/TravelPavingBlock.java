package tc4revival.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
public  class TravelPavingBlock extends Block {
    public TravelPavingBlock() { super(Properties.of().strength(2,10).sound(SoundType.STONE).lightLevel(state->9).isValidSpawn((state,world,pos,type)->false)); }
    @Override public void stepOn(Level level,BlockPos pos,BlockState state,Entity entity) {
        if(entity instanceof LivingEntity living) {
            if(level.isClientSide) tc4revival.client.ClientFocusEffects.blockSparkle(level,pos,32768,5);
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,40,1));living.addEffect(new MobEffectInstance(MobEffects.JUMP,40,0));
        }
        super.stepOn(level,pos,state,entity);
    }
}
