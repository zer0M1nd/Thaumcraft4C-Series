package fmmodern;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.*;
import tc4revival.aspect.TCAspects;

public final class WrathCageEntity extends BlockEntity implements IEssentiaTransport,IAspectContainer {
    private ResourceLocation mob;
    private int special,wrath,sloth,mode,fuel,delay=20;
    private boolean slothful;
    public double rotation,previousRotation;
    public static void clientTick(Level level,BlockPos pos,BlockState state,WrathCageEntity cage){
        cage.previousRotation=cage.rotation;if(cage.mob==null||cage.fuel<=0&&ForbiddenConfig.value(ForbiddenConfig.WRATH_COST)>0)return;
        if(cage.delay>0)cage.delay--;cage.rotation+=(1000d/(cage.delay+200));
        var random=level.random;
        thaumcraft.api.fx.LegacyEffects.sparkle(level,new net.minecraft.world.phys.Vec3(pos.getX()+random.nextDouble(),pos.getY()+random.nextDouble(),pos.getZ()+random.nextDouble()),0x870404,1,1,0);
    }
    public WrathCageEntity(BlockPos pos,BlockState state){super(Content.CAGE_ENTITY.get(),pos,state);}
    public ResourceLocation mob(){return mob;}
    public void cycleMode(){mode=(mode+1)%3;changed();}
    public void setMob(ResourceLocation id){if(!CageMobs.allowed(id))return;if(mob==null||!CageMobs.aspect(mob).equals(CageMobs.aspect(id)))special=0;mob=id;changed();}
    public ItemStack crystal(){if(mob==null)return ItemStack.EMPTY;var stack=new ItemStack(Content.MOB_CRYSTAL.get());stack.getOrCreateTag().putString("mob",mob.toString());return stack;}
    private tc4revival.aspect.Aspect specialAspect(){return CageMobs.aspect(mob);}
    private static tc4revival.aspect.Aspect wrathAspect(){return TCAspects.catalog().require("forbidden_magic:ira");}
    private static tc4revival.aspect.Aspect slothAspect(){return TCAspects.catalog().require("forbidden_magic:desidia");}
    private void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public static void tick(Level level,BlockPos pos,BlockState state,WrathCageEntity cage){
        if(!(level instanceof ServerLevel server)||!CageMobs.allowed(cage.mob))return;
        cage.draw();if(level.hasNeighborSignal(pos))return;
        if(cage.fuel<=0){
            int cost=fmmodern.ForbiddenConfig.value(ForbiddenConfig.WRATH_COST);
            if(cage.special>=cost){cage.special-=cost;cage.slothful=false;}
            else if(cage.wrath>=cost){cage.wrath-=cost;cage.slothful=false;}
            else if(cage.sloth>=cost){cage.sloth-=cost;cage.slothful=true;}
            else return;
            cage.fuel=fmmodern.ForbiddenConfig.value(ForbiddenConfig.WRATH_EFF);cage.changed();
        }
        if(cage.delay>0){cage.delay--;cage.setChanged();return;}
        var type=CageMobs.type(cage.mob);if(type==null)return;
        boolean spawned=false;
        for(int attempt=0;attempt<3&&cage.fuel>0;attempt++){
            if(level.getEntitiesOfClass(Mob.class,new AABB(pos).inflate(8,4,8),e->e.getType()==type).size()>=6){cage.resetDelay();return;}
            var entity=type.create(level);if(!(entity instanceof Mob creature)){if(entity!=null)entity.discard();return;}
            double x=pos.getX()+(level.random.nextDouble()-level.random.nextDouble())*4,z=pos.getZ()+(level.random.nextDouble()-level.random.nextDouble())*4;
            creature.moveTo(x,pos.getY()+level.random.nextInt(3)-1,z,level.random.nextFloat()*360,0);
            var box=creature.getBoundingBox();
            if(level.noCollision(creature,box)&&!level.containsAnyLiquid(box)&&level.getEntities(creature,box).isEmpty()){
                // Deliberately ignore light/biome spawning rules, as the legacy cage does.
                net.minecraftforge.event.ForgeEventFactory.onFinalizeSpawn(creature,server,server.getCurrentDifficultyAt(creature.blockPosition()),MobSpawnType.SPAWNER,null,null);
                if(server.addFreshEntity(creature)){cage.fuel--;spawned=true;level.levelEvent(2004,pos,0);}else creature.discard();
            }else creature.discard();
        }
        if(spawned)cage.resetDelay();else{cage.delay=20;cage.setChanged();}
    }
    private void resetDelay(){delay=200+level.random.nextInt(100)+(slothful?200:0);changed();}
    private void draw(){
        var requested=suctionType(Direction.UP);if(requested==null||amount(requested)>=64)return;
        for(var face:Direction.values()){
            var source=ThaumcraftApiHelper.getConnectableTile(level,worldPosition,face);var other=face.getOpposite();
            if(source!=null&&source.canOutput(other)&&requested.equals(source.essentiaType(other))&&source.essentiaAmount(other)>0&&suction(face)>source.suction(other)&&suction(face)>=source.minimumSuction()){
                int taken=source.takeEssentia(requested,1,other);if(taken>0){addEssentia(requested,taken,face);return;}
            }
        }
    }
    private int amount(tc4revival.aspect.Aspect aspect){return aspect.equals(wrathAspect())?wrath:aspect.equals(slothAspect())?sloth:aspect.equals(specialAspect())?special:0;}
    @Override public boolean accepts(tc4revival.aspect.Aspect aspect){return CageMobs.allowed(mob)&&aspect!=null&&(aspect.equals(specialAspect())||aspect.equals(wrathAspect())||aspect.equals(slothAspect()));}
    @Override public int capacity(tc4revival.aspect.Aspect aspect){return accepts(aspect)?64:0;}
    @Override public AspectList getAspects(){var result=new AspectList();if(mob!=null){result.add(Aspect.wrap(specialAspect()),special);result.add(Aspect.wrap(wrathAspect()),wrath);result.add(Aspect.wrap(slothAspect()),sloth);}return result;}
    @Override public void setAspects(AspectList values){special=clamp(values.unwrap().amount(specialAspect()));wrath=clamp(values.unwrap().amount(wrathAspect()));sloth=clamp(values.unwrap().amount(slothAspect()));changed();}
    @Override public boolean takeFromContainer(tc4revival.aspect.Aspect aspect,int amount){return false;}
    @Override public boolean connectable(Direction face){return true;}
    @Override public boolean canInput(Direction face){return true;}
    @Override public boolean canOutput(Direction face){return false;}
    @Override public tc4revival.aspect.Aspect suctionType(Direction face){return !CageMobs.allowed(mob)?null:mode==0?specialAspect():mode==1?wrathAspect():slothAspect();}
    @Override public int suction(Direction face){var type=suctionType(face);return type!=null&&amount(type)<64?256:0;}
    @Override public int minimumSuction(){return 9000;}
    @Override public tc4revival.aspect.Aspect essentiaType(Direction face){return suctionType(face);}
    @Override public int essentiaAmount(Direction face){var type=essentiaType(face);return type==null?0:amount(type);}
    @Override public int takeEssentia(tc4revival.aspect.Aspect aspect,int amount,Direction face){return 0;}
    @Override public int addEssentia(tc4revival.aspect.Aspect aspect,int amount,Direction face){
        if(amount<=0||!accepts(aspect))return 0;int accepted=Math.min(amount,64-amount(aspect));
        if(accepted>0){if(aspect.equals(wrathAspect()))wrath+=accepted;else if(aspect.equals(slothAspect()))sloth+=accepted;else special+=accepted;changed();}return accepted;
    }
    private static int clamp(int value){return Math.max(0,Math.min(64,value));}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);if(mob!=null)tag.putString("Mob",mob.toString());tag.putInt("Special",special);tag.putInt("Wrath",wrath);tag.putInt("Sloth",sloth);tag.putInt("Mode",mode);tag.putInt("Fuel",fuel);tag.putInt("Delay",delay);tag.putBoolean("Slothful",slothful);}
    @Override public void load(CompoundTag tag){super.load(tag);mob=ResourceLocation.tryParse(tag.getString("Mob"));special=clamp(tag.getInt("Special"));wrath=clamp(tag.getInt("Wrath"));sloth=clamp(tag.getInt("Sloth"));mode=Math.floorMod(tag.getInt("Mode"),3);fuel=Math.max(0,Math.min(100000,tag.getInt("Fuel")));delay=Math.max(0,Math.min(499,tag.getInt("Delay")));slothful=tag.getBoolean("Slothful");}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
