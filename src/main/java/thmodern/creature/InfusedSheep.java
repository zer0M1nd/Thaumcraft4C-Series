package thmodern.creature;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundSource;
import thmodern.Horizons;

public final class InfusedSheep extends Sheep {
    public InfusedSheep(EntityType<? extends Sheep> type,Level level){super(type,level);}
    @Override public void aiStep(){
        if(!level().isClientSide&&isAlive()){
            if(getType()==Horizons.CHROMATIC_SHEEP.get()&&tickCount%30==0)setColor(DyeColor.byId((getColor().getId()+1)%16));
            else if(getType()==Horizons.SELF_SHEARING_SHEEP.get()&&tickCount%100==0&&readyForShearing())shear(SoundSource.NEUTRAL);
        }
        super.aiStep();
    }
}
