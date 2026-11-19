package tc4revival.content;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.Aspect;
import tc4revival.aspect.TCAspects;

/** Core processing and the adjacent, visually flush fire-essentia inlets. No inventory automation GUI existed in TC4. */
public  class InfernalFurnaceBlockEntity extends BlockEntity implements InfernalFurnaceProcess.Host,EssentiaTransport {
    private final InfernalFurnaceProcess process=new InfernalFurnaceProcess();
    private boolean nozzleChecked;
    private Direction nozzleFacing;
    private BlockPos corePosition;
    private int drawDelay;
    public InfernalFurnaceBlockEntity(BlockPos pos,BlockState state) { super(TCArcane.INFERNAL_FURNACE_ENTITY.get(),pos,state); }
    public boolean core() { return getBlockState().getValue(InfernalFurnaceBlock.PART)==0; }
    public InfernalFurnaceProcess process() { return process; }
    public Direction outlet() { return getBlockState().getValue(InfernalFurnaceBlock.FACING); }
    public void tick() {
        if (level==null) return;
        if (core()) { if (!level.isClientSide) process.tick(this); return; }
        if (!nozzleChecked) {
            nozzleChecked=true;
            for (var side:Direction.values()) {
                var target=worldPosition.relative(side);
                if (level.hasChunkAt(target) && level.getBlockEntity(target) instanceof InfernalFurnaceBlockEntity furnace && furnace.core()) {
                    corePosition=target; nozzleFacing=side.getOpposite(); break;
                }
            }
        }
        var furnace=linkedCore();
        if (level.isClientSide || furnace==null || furnace.process.speedyTime()>=60 || ++drawDelay%5!=0) return;
        var sourcePos=worldPosition.relative(nozzleFacing);
        if (!level.hasChunkAt(sourcePos) || !(level.getBlockEntity(sourcePos) instanceof EssentiaTransport source)
                || !source.connectable(nozzleFacing.getOpposite()) || !source.canOutput(nozzleFacing.getOpposite())) return;
        if (source.suction(nozzleFacing.getOpposite())<suction(nozzleFacing)
                && source.takeEssentia(TCAspects.catalog().require("ignis"),1,nozzleFacing.getOpposite())==1)
            furnace.process.addFireEssentia(furnace);
    }
    private InfernalFurnaceBlockEntity linkedCore() {
        if (level==null || corePosition==null || !level.hasChunkAt(corePosition)) return null;
        return level.getBlockEntity(corePosition) instanceof InfernalFurnaceBlockEntity furnace && furnace.core() ? furnace : null;
    }
    public boolean insert(ItemStack stack) { return core() && level!=null && !level.isClientSide && process.insert(stack,this); }
    @Override public Optional<InfernalFurnaceProcess.Smelting> recipe(ItemStack input) {
        if (level==null) return Optional.empty();
        var inventory=new SimpleContainer(input);
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,inventory,level)
                .map(recipe->new InfernalFurnaceProcess.Smelting(recipe.assemble(inventory,level.registryAccess()),recipe.getExperience()))
                .filter(recipe->!recipe.output().isEmpty());
    }
    @Override public int bellows() {
        if (level==null) return 0;
        int count=0;
        for (var side:Direction.values()) if (side!=Direction.UP) {
            var pos=worldPosition.relative(side,2);
            if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof BellowsBlockEntity bellows
                    && bellows.facing()==side.getOpposite() && !level.hasNeighborSignal(pos)) count++;
        }
        return Math.min(3,count);
    }
    @Override public int drainFireVis(int requested) { return VisNetwork.drain(level,worldPosition,TCAspects.catalog().require("ignis"),requested); }
    private void ejectStack(ItemStack stack) {
        if (stack.isEmpty()) return;
        var direction=outlet();
        var entity=new ItemEntity(level,worldPosition.getX()+.5+direction.getStepX()*1.2,worldPosition.getY()+.4,
                worldPosition.getZ()+.5+direction.getStepZ()*1.2,stack);
        entity.setDeltaMovement(motion(direction.getStepX(),.03f),0,motion(direction.getStepZ(),.03f)); level.addFreshEntity(entity);
    }
    private double motion(int step,float spread) { return step==0 ? (level.random.nextFloat()-level.random.nextFloat())*spread : step*.13f; }
    @Override public void eject(InfernalFurnaceProcess.Smelting recipe,ItemStack input) {
        ejectStack(recipe.output().copy());
        InfernalBonusRecipe.find(level,input).ifPresent(bonus->ejectStack(bonus.roll(bellows(),level.random)));
        float exact=Math.max(0,recipe.experience());
        int experience=net.minecraft.util.Mth.floor(exact);
        if(level.random.nextFloat()<exact-experience)experience++;
        var direction=outlet();
        while (experience>0) {
            int split=ExperienceOrb.getExperienceValue(experience); experience-=split;
            var orb=new ExperienceOrb(level,worldPosition.getX()+.5+direction.getStepX()*1.2,worldPosition.getY()+.4,
                    worldPosition.getZ()+.5+direction.getStepZ()*1.2,split);
            orb.setDeltaMovement(motion(direction.getStepX(),.025f),0,motion(direction.getStepZ(),.025f)); level.addFreshEntity(orb);
        }
    }
    @Override public void destroyedInput() {
        level.playSound(null,worldPosition,SoundEvents.FIRE_EXTINGUISH,SoundSource.BLOCKS,.3f,2.6f+(level.random.nextFloat()-level.random.nextFloat())*.8f);
        if (level instanceof ServerLevel server) server.sendParticles(ParticleTypes.LAVA,worldPosition.getX()+level.random.nextFloat(),worldPosition.getY()+1,
                worldPosition.getZ()+level.random.nextFloat(),1,0,0,0,0);
    }
    @Override public void smelted() { level.blockEvent(worldPosition,getBlockState().getBlock(),3,0); }
    @Override public void changed() { setChanged(); }
    @Override protected void saveAdditional(CompoundTag tag) { super.saveAdditional(tag); if (core()) process.save(tag); }
    @Override public void load(CompoundTag tag) { super.load(tag); process.load(tag); nozzleChecked=false; nozzleFacing=null; corePosition=null; }
    @Override public boolean connectable(Direction face) { return !core() && nozzleFacing!=null; }
    @Override public boolean canInput(Direction face) { return connectable(face); }
    @Override public boolean canOutput(Direction face) { return false; }
    @Override public Aspect suctionType(Direction face) { return TCAspects.catalog().require("ignis"); }
    @Override public int suction(Direction face) { var furnace=linkedCore(); return furnace!=null && furnace.process.speedyTime()<40 ? 128 : 0; }
    @Override public int minimumSuction() { return 0; }
    @Override public Aspect essentiaType(Direction face) { return null; }
    @Override public int essentiaAmount(Direction face) { return 0; }
    @Override public int takeEssentia(Aspect aspect,int amount,Direction face) { return 0; }
    @Override public int addEssentia(Aspect aspect,int amount,Direction face) { return 0; }
}
