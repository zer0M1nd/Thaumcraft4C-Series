package thaumicomniverse.compat.create;

import com.simibubi.create.content.processing.burner.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thaumcraft.api.aspects.IEssentiaStorage;

public class ArcaneBurner extends BlazeBurnerBlock {
    public ArcaneBurner(){super(Properties.copy(Blocks.IRON_BLOCK).strength(3).noOcclusion().lightLevel(s->s.getValue(HEAT_LEVEL).isAtLeast(HeatLevel.KINDLED)?15:0));}
    @Override public BlockEntityType<? extends BlazeBurnerBlockEntity> getBlockEntityType(){return ArcaneCreateContent.BURNER_TILE.get();}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Tile(pos,state);}
    public static class BurnerItem extends BlockItem {
        private final boolean filled;
        public BurnerItem(boolean filled){super(ArcaneCreateContent.BURNER.get(),new Properties());this.filled=filled;}
        @Override public void registerBlocks(java.util.Map<Block,Item> map,Item item){if(filled)super.registerBlocks(map,item);}
        @Override public String getDescriptionId(){return filled?super.getDescriptionId():"item.thaumicomniverse.empty_arcane_blaze_burner";}
        @Override protected BlockState getPlacementState(BlockPlaceContext context){var state=super.getPlacementState(context);return state==null?null:state.setValue(HEAT_LEVEL,filled?HeatLevel.SMOULDERING:HeatLevel.NONE);}
        @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){
            if(filled||!(target instanceof Blaze)||!ArcaneContraptions.enabled())return InteractionResult.PASS;
            if(!player.level().isClientSide){
                var captured=new ItemStack(ArcaneCreateContent.FILLED_BURNER.get());
                if(!player.getAbilities().instabuild)stack.shrink(1);
                if(stack.isEmpty())player.setItemInHand(hand,captured);else if(!player.addItem(captured))player.drop(captured,false);
                target.discard();player.level().playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.BLAZE_AMBIENT,net.minecraft.sounds.SoundSource.BLOCKS,1,1);
            }
            // Like Create's burner, stop Player.interactOn's post-success cleanup:
            // the consumed stack is empty, but the hand now holds the captured burner.
            return InteractionResult.FAIL;
        }
    }
    public static class Tile extends BlazeBurnerBlockEntity implements IEssentiaStorage,EssentiaTransport,RevealedAspectContainer {
        private int fire,power;
        public Tile(BlockPos pos,BlockState state){super(ArcaneCreateContent.BURNER_TILE.get(),pos,state);}
        private boolean filled(){return getBlockState().getValue(HEAT_LEVEL)!=HeatLevel.NONE;}
        private Aspect fire(){return TCAspects.catalog().require("ignis");}
        private Aspect power(){return TCAspects.catalog().require("potentia");}
        private void changed(){setChanged();sendData();}
        @Override protected boolean tryUpdateFuel(ItemStack stack,boolean overflow,boolean simulate){return false;}
        @Override public boolean isCreativeFuel(ItemStack stack){return false;}
        @Override public void tick(){
            if(level==null)return;
            if(level.isClientSide){super.tick();return;}
            if(!filled())return;
            if(!ArcaneContraptions.enabled()){setBlockHeat(HeatLevel.SMOULDERING);return;}
            if(level.getGameTime()%5==0)draw();
            if(remainingBurnTime>0){remainingBurnTime--;setChanged();}
            if(activeFuel==FuelType.NORMAL&&power>0&&fire>0){
                fire--;power--;activeFuel=FuelType.SPECIAL;remainingBurnTime=100;changed();
            }else if(remainingBurnTime==0){
                if(fire>0){fire--;if(power>0){power--;activeFuel=FuelType.SPECIAL;remainingBurnTime=100;}else{activeFuel=FuelType.NORMAL;remainingBurnTime=400;}}
                else activeFuel=FuelType.NONE;
                changed();
            }
            setBlockHeat(activeFuel==FuelType.SPECIAL?HeatLevel.SEETHING:activeFuel==FuelType.NORMAL?HeatLevel.KINDLED:HeatLevel.SMOULDERING);
        }
        private void draw(){
            Aspect wanted=suctionType(null);if(wanted==null)return;
            for(var side:Direction.values()){
                var pos=worldPosition.relative(side);if(!level.hasChunkAt(pos))continue;
                if(!(level.getBlockEntity(pos) instanceof EssentiaTransport source)||!source.connectable(side.getOpposite())||!source.canOutput(side.getOpposite())
                    ||source.suction(side.getOpposite())>=suction(side)||source.minimumSuction()>suction(side))continue;
                int taken=source.takeEssentia(wanted,1,side.getOpposite());if(taken>0){insert(wanted,taken,false);break;}
            }
        }
        @Override public AspectAmounts contents(){return new AspectAmounts().add(fire(),fire).add(power(),power);}
        @Override public AspectAmounts revealedAspects(){return contents();}
        @Override public int totalCapacity(){return filled()?16:0;}
        @Override public int capacity(Aspect a){return filled()&&(a.equals(fire())||a.equals(power()))?8:0;}
        @Override public int insert(Aspect a,int n,boolean simulate){
            if(n<0)throw new IllegalArgumentException("Negative essentia");int stored=a.equals(fire())?fire:a.equals(power())?power:0;
            int accepted=Math.min(n,Math.max(0,capacity(a)-stored));
            if(!simulate&&accepted>0){if(a.equals(fire()))fire+=accepted;else power+=accepted;changed();}return accepted;
        }
        @Override public int extract(Aspect a,int n,boolean simulate){
            if(n<0)throw new IllegalArgumentException("Negative essentia");int taken=Math.min(n,a.equals(fire())?fire:a.equals(power())?power:0);
            if(!simulate&&taken>0){if(a.equals(fire()))fire-=taken;else power-=taken;changed();}return taken;
        }
        @Override public boolean connectable(Direction side){return filled();}
        @Override public boolean canInput(Direction side){return filled();}
        @Override public boolean canOutput(Direction side){return false;}
        @Override public Aspect suctionType(Direction side){if(fire>=8&&power>=8)return null;if(fire>=8)return power();if(power>=8)return fire();return level==null||level.getGameTime()/5%2==0?fire():power();}
        @Override public int suction(Direction side){return ArcaneContraptions.enabled()&&filled()&&(fire<8||power<8)?128:0;}
        @Override public int minimumSuction(){return 0;}
        @Override public Aspect essentiaType(Direction side){return null;}
        @Override public int essentiaAmount(Direction side){return fire+power;}
        @Override public int takeEssentia(Aspect a,int n,Direction side){return 0;}
        @Override public int addEssentia(Aspect a,int n,Direction side){return ArcaneContraptions.enabled()?insert(a,n,false):0;}
        @Override public void write(CompoundTag tag,boolean client){super.write(tag,client);tag.putInt("Ignis",fire);tag.putInt("Potentia",power);}
        @Override protected void read(CompoundTag tag,boolean client){
            // Clamp before the upstream enum decoder; malformed data must not crash a world load.
            tag=tag.copy();tag.putInt("fuelLevel",Math.max(0,Math.min(2,tag.getInt("fuelLevel"))));
            super.read(tag,client);isCreative=false;fire=Math.max(0,Math.min(8,tag.getInt("Ignis")));power=Math.max(0,Math.min(8,tag.getInt("Potentia")));
            remainingBurnTime=Math.max(0,Math.min(activeFuel==FuelType.SPECIAL?100:400,remainingBurnTime));
        }
    }
}
