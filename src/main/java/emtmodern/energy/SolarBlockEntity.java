package emtmodern.energy;

import emtmodern.ElectroMagicTools;
import ic2.api.energy.prefab.BasicSource;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SolarBlockEntity extends BlockEntity {
    public final BasicSource energy=new BasicSource(this,10000,4);
    private int checkTicks;
    private boolean sun;
    public SolarBlockEntity(BlockPos pos,BlockState state){super(ElectroMagicTools.SOLAR_BE.get(),pos,state);}
    public static void tick(Level world,BlockPos pos,BlockState state,SolarBlockEntity machine){
        if(world.isClientSide)return;
        machine.energy.update();
        SolarBlock block=(SolarBlock)state.getBlock();
        boolean raining=world.isRaining()||world.isThundering();
        boolean hasRain=world.getBiome(pos).value().hasPrecipitation();
        boolean ceiling=world.dimensionType().hasCeiling();
        if(machine.checkTicks--<=0){machine.sun=world.canSeeSky(pos.above())&&world.isDay()&&!(raining&&hasRain);machine.checkTicks=64;}
        double base=EmtEnergyConfig.output(block.compression), produced;
        if(block.kind.equals("water")||block.kind.equals("dark")||block.kind.equals("fire"))
            produced=SolarRule.production(block.kind,base,world.canSeeSky(pos.above()),world.isDay(),raining,hasRain,ceiling,world.dimension().equals(Level.NETHER),pos.getY());
        else produced=SolarRule.production(block.kind,base,machine.sun,true,false,false,ceiling,false,pos.getY());
        if(produced>0&&machine.energy.addEnergy(produced)>0)machine.setChanged();
    }
    @Override public void onLoad(){super.onLoad();if(level!=null&&!level.isClientSide)energy.onLoad();}
    @Override public void onChunkUnloaded(){energy.onChunkUnload();super.onChunkUnloaded();}
    @Override public void setRemoved(){energy.invalidate();super.setRemoved();}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);energy.writeToNBT(tag);}
    @Override public void load(CompoundTag tag){super.load(tag);energy.readFromNBT(tag);energy.setEnergyStored(Math.max(0,Math.min(10000,energy.getEnergyStored())));}
}
