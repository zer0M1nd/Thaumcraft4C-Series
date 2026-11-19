package fmmodern.compat;

import fmmodern.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.RegistryObject;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.wands.WandRod;
import tc4revival.aspect.TCAspects;
import tc4revival.content.CastingWandItem;
import tc4revival.content.TCItems;
import tc4revival.content.WandParts;
import vazkii.botania.api.block_entity.*;
import vazkii.botania.api.mana.ManaItemHandler;
import vazkii.botania.forge.block.ForgeSpecialFlowerBlock;

/** Optional class: never loaded unless Botania is installed. */
public final class Botany {
    private static final Map<String,RegistryObject<Block>> FLOWERS=new LinkedHashMap<>();
    private static final Map<String,RegistryObject<BlockEntityType<?>>> TYPES=new LinkedHashMap<>();
    public static void register(){
        for(String name:new String[]{"euclidaisy","whisperweed","tainthistle","bloodthorn"}){
            FLOWERS.put(name,Content.block(name,()->new ForgeSpecialFlowerBlock(MobEffects.BLINDNESS,8,BlockBehaviour.Properties.copy(Blocks.DANDELION),()->type(name))));
            TYPES.put(name,Content.ENTITIES.register(name,()->BlockEntityType.Builder.of((pos,state)->name.equals("tainthistle")?new Tainthistle(pos,state):new Functional(name,pos,state),FLOWERS.get(name).get()).build(null)));
        }
    }
    @SuppressWarnings("unchecked") public static BlockEntityType<? extends SpecialFlowerBlockEntity> type(String name){return (BlockEntityType<? extends SpecialFlowerBlockEntity>)TYPES.get(name).get();}
    public static void setup(){
        for(String name:new String[]{"livingwood","dreamwood","dreamwood_staff"})WandRod.registerUpdate("forbidden_magic:"+name,Botany::recharge);

    }
    private static void recharge(ItemStack stack,Player player){
        if(player.level().isClientSide||!IntegrationContent.botanyEnabled()||!fmmodern.ForbiddenConfig.value(ForbiddenConfig.CROSS_WAND)||player.tickCount%40!=0||!(stack.getItem() instanceof CastingWandItem wand))return;
        boolean hotbar=false;for(int slot=0;slot<9;slot++)if(player.getInventory().getItem(slot)==stack){hotbar=true;break;}if(!hotbar)return;
        String cap=WandParts.cap(stack.getTag()).tag;int cost=Math.max(0,fmmodern.ForbiddenConfig.value(ForbiddenConfig.MANA_VIS)-(cap.equals("forbidden_magic:manasteel")||cap.equals("forbidden_magic:elementium")?2:0));
        for(var aspect:TCAspects.catalog().primals()){
            int amount=Math.min(100,Math.max(0,wand.capacity(stack)-wand.vis(stack,aspect)));
            if(amount>0&&ManaItemHandler.instance().requestManaExact(stack,player,amount*cost,true))wand.addVis(stack,aspect,amount,false,true);
        }
    }
    public static final class Functional extends FunctionalFlowerBlockEntity {
        private final String kind;
        Functional(String kind,BlockPos pos,BlockState state){super(type(kind),pos,state);this.kind=kind;}
        @Override public int getColor(){return switch(kind){case "euclidaisy"->0xff8cff;case "whisperweed"->0x745380;default->0x7d0600;};}
        @Override public int getMaxMana(){return switch(kind){case "euclidaisy"->9000;case "whisperweed"->6000;default->360;};}
        @Override public boolean acceptsRedstone(){return true;}
        @Override public RadiusDescriptor getRadius(){return RadiusDescriptor.Rectangle.square(getEffectivePos(),kind.equals("bloodthorn")?6:kind.equals("whisperweed")?3:0);}
        @Override public void tickFlower(){
            super.tickFlower();if(!(getLevel() instanceof ServerLevel level)||redstoneSignal>0||!IntegrationContent.botanyEnabled())return;
            var pos=getEffectivePos();
            if(kind.equals("euclidaisy")&&ticksExisted%400==0&&getMana()>=9000){
                var all=TCAspects.catalog().all().stream().filter(a->!ForbiddenConfig.value(ForbiddenConfig.NO_LUST)||!a.tag().equals("forbidden_magic:luxuria")).toList();var aspect=level.random.nextInt(10)<4?TCAspects.catalog().require("auram"):all.get(level.random.nextInt(all.size()));
                var entity=new ItemEntity(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,TCItems.WISP_ESSENCE.get().of(aspect));entity.setPickUpDelay(10);
                if(level.addFreshEntity(entity)){addMana(-9000);sync();tc4revival.network.FocusEffectPacket.burst(level,pos.getCenter(),1);}
            }else if(kind.equals("whisperweed")&&ticksExisted%300==0&&getMana()>=6000){
                var players=level.getEntitiesOfClass(ServerPlayer.class,new AABB(pos).inflate(3),p->!p.isSpectator());if(players.isEmpty())return;
                var player=players.get(level.random.nextInt(players.size()));int amount=1+level.random.nextInt(3);
                if(level.random.nextInt(10)<2){amount+=1+level.random.nextInt(3);ThaumcraftApiHelper.addWarpToPlayer(player,1+level.random.nextInt(5),true);}
                var primals=TCAspects.catalog().primals();var compounds=TCAspects.catalog().all().stream().filter(a->!a.isPrimal()&&(!ForbiddenConfig.value(ForbiddenConfig.NO_LUST)||!a.tag().equals("forbidden_magic:luxuria"))).toList();
                for(int i=0;i<amount;i++){var choices=level.random.nextInt(25)>2?primals:compounds;ThaumcraftApiHelper.addResearchPoints(player,thaumcraft.api.aspects.Aspect.wrap(choices.get(level.random.nextInt(choices.size()))),(short)1);}
                addMana(-6000);sync();
            }else if(kind.equals("bloodthorn")&&IntegrationContent.bloodMagic()&&fmmodern.ForbiddenConfig.value(ForbiddenConfig.BLOOD_MAGIC)){
                for(var entity:level.getEntitiesOfClass(LivingEntity.class,new AABB(pos.offset(-6,-6,-6),pos.offset(6,6,6)),e->!(e instanceof Player)&&!e.hasEffect(MobEffects.WEAKNESS))){
                    if(getMana()<40)break;if(entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,60,2)))addMana(-40);
                }
            }
        }
    }
    public static final class Tainthistle extends GeneratingFlowerBlockEntity {
        private int burnTime;
        Tainthistle(BlockPos pos,BlockState state){super(type("tainthistle"),pos,state);}
        @Override public int getColor(){return 0x4d00ff;}
        @Override public int getMaxMana(){return 150;}
        @Override public RadiusDescriptor getRadius(){return RadiusDescriptor.Rectangle.square(getEffectivePos(),4);}
        @Override public void tickFlower(){
            super.tickFlower();if(!(getLevel() instanceof ServerLevel level)||!IntegrationContent.botanyEnabled())return;
            if(ticksExisted%80==0){
                var origin=getEffectivePos();boolean changed=false;
                for(var pos:BlockPos.betweenClosed(origin.offset(-4,-4,-4),origin.offset(4,4,4))){
                    if(!level.hasChunkAt(pos))continue;var state=level.getBlockState(pos);var id=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());
                    if(id==null||!id.getNamespace().equals("thaumcraft")||!(id.getPath().equals("flux_gas")||id.getPath().equals("flux_goo")))continue;
                    int depth=state.hasProperty(tc4revival.content.FiniteFluidBlock.QUANTA)?state.getValue(tc4revival.content.FiniteFluidBlock.QUANTA)-1:0;
                    if(level.setBlock(pos,Blocks.AIR.defaultBlockState(),3)){burnTime=Math.min(4000,burnTime+depth*20);changed=true;}
                }
                if(changed){level.playSound(null,origin,net.minecraft.sounds.SoundEvents.GENERIC_DRINK,net.minecraft.sounds.SoundSource.BLOCKS,.05f,.5f+level.random.nextFloat()*.5f);sync();}
            }
            if(burnTime>0){if(ticksExisted%2==0)addMana(3);burnTime--;setChanged();}
        }
        @Override public void writeToPacketNBT(CompoundTag tag){super.writeToPacketNBT(tag);tag.putInt("burnTime",burnTime);}
        @Override public void readFromPacketNBT(CompoundTag tag){super.readFromPacketNBT(tag);burnTime=Math.max(0,Math.min(4000,tag.getInt("burnTime")));}
    }
    private Botany(){}
}
