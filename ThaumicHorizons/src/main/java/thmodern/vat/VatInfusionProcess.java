package thmodern.vat;

import java.util.*;
import com.google.gson.JsonParser;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import tc4revival.aspect.TCAspects;
import tc4revival.content.ArcanePedestalBlockEntity;
import thaumcraft.api.crafting.IInfusionStabiliser;

/** In-progress recipe is snapshotted so /reload cannot substitute a cheaper output mid-craft. */
public final class VatInfusionProcess {
    private VatInfusionRecipe recipe;
    private UUID caster;
    private int ticks,component,itemTicks,instability;
    private BlockPos drawingFrom;
    public boolean active(){return recipe!=null;}
    public int instability(){return instability;}
    private List<BlockPos> pedestals(Level level,BlockPos center){
        var result=new ArrayList<BlockPos>();
        for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)if(x!=0||z!=0){
            for(int down=1;down<=10;down++){
                var pos=center.offset(x,-down,z);if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof ArcanePedestalBlockEntity){result.add(pos);break;}
            }
        }
        return result;
    }
    private int symmetry(Level level,BlockPos center,List<BlockPos> pedestals){
        int value=0;
        for(var pos:pedestals){
            boolean occupied=!((ArcanePedestalBlockEntity)level.getBlockEntity(pos)).isEmpty();value+=2+(occupied?1:0);
            var opposite=new BlockPos(2*center.getX()-pos.getX(),pos.getY(),2*center.getZ()-pos.getZ());
            if(level.hasChunkAt(opposite)&&level.getBlockEntity(opposite) instanceof ArcanePedestalBlockEntity other){value-=2;if(occupied&&!other.isEmpty())value--;}
        }
        float decorations=0;
        for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)if(x!=0||z!=0)for(int y=-10;y<=5;y++){
            var pos=center.offset(x,y,z);if(!stabilizer(level,pos))continue;
            decorations+=.1f;if(stabilizer(level,center.offset(-x,y,-z)))decorations-=.2f;
        }
        return value+(int)decorations;
    }
    private boolean stabilizer(Level level,BlockPos pos){
        if(!level.hasChunkAt(pos))return false;var state=level.getBlockState(pos);
        return state.getBlock() instanceof net.minecraft.world.level.block.AbstractSkullBlock
            ||state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK,ResourceLocation.parse("thaumcraft:infusion_stabilizers")))
            ||state.getBlock() instanceof IInfusionStabiliser block&&block.canStabiliseInfusion(level,pos)
            ||level.getBlockEntity(pos) instanceof IInfusionStabiliser tile&&tile.canStabiliseInfusion(level,pos);
    }
    public boolean begin(VatEntity vat,ServerPlayer player){
        if(active()||(vat.mode()!=VatEntity.IDLE&&vat.mode()!=VatEntity.EFFIGY)||(vat.subject()==null&&!vat.hasEffigy()))return false;
        var positions=pedestals(vat.getLevel(),vat.getBlockPos());var stacks=new ArrayList<ItemStack>();for(var pos:positions){var tile=(ArcanePedestalBlockEntity)vat.getLevel().getBlockEntity(pos);if(!tile.isEmpty())stacks.add(tile.getItem(0).copyWithCount(1));}
        for(var candidate:VatInfusionRecipes.all()){
            boolean applicable=vat.hasEffigy()?vat.canInfuseSelf(candidate):CreatureInfusions.canApply(vat.subject(),candidate.output())||CreatureTransformation.canApply(vat.subject(),candidate.output());
            if(!applicable||candidate.match(player,vat.subject(),vat.hasEffigy(),stacks).isEmpty())continue;
            for(var aspect:candidate.aspects.keySet())TCAspects.catalog().require(aspect);
            recipe=candidate;caster=player.getUUID();ticks=component=itemTicks=0;drawingFrom=null;instability=Math.min(25,recipe.instability+symmetry(vat.getLevel(),vat.getBlockPos(),positions));
            vat.essentia().clear();vat.essentia().demand(recipe.aspects);vat.startInfusion();return true;
        }
        return false;
    }
    public void tick(VatEntity vat){
        if(!(vat.getLevel() instanceof ServerLevel level)||recipe==null)return;
        if(vat.subject()==null&&!vat.hasEffigy()){vat.destroySubject();clear();return;}
        if(!(level.getBlockEntity(vat.getBlockPos().above()) instanceof VatMatrixEntity))return; // Resume after the matrix is replaced.
        if(++ticks%10!=0){vat.setChanged();return;}
        var positions=pedestals(level,vat.getBlockPos());
        if(instability>0&&level.random.nextInt(500)<=instability)instabilityEvent(vat,positions,level.random.nextInt(21));
        if(!active()||vat.isRemoved())return;
        if(instability>0){
            var matrix=(VatMatrixEntity)level.getBlockEntity(vat.getBlockPos().above());int mitigation=100;
            for(var key:List.of("terra","aqua","ordo"))mitigation=Math.min(mitigation,matrix.consumeVis(TCAspects.catalog().require(key),100));
            if(!vat.damageInfusionSubject(instability/10f/(5+mitigation))){vat.destroySubject();clear();return;}
        }
        if(vat.essentia().wanted()!=null){
            int before=vat.essentia().stored().values().stream().mapToInt(Integer::intValue).sum();VatStructure.pull(vat);
            int after=vat.essentia().stored().values().stream().mapToInt(Integer::intValue).sum();
            if(after==before&&level.random.nextInt(Math.max(1,100-recipe.instability*3))==0)instability=Math.min(25,instability+1);
            vat.syncInfusion();return;
        }
        if(component<recipe.ingredients.size()){
            var ingredient=recipe.ingredients.get(component);
            if(drawingFrom!=null&&(!positions.contains(drawingFrom)||!(level.getBlockEntity(drawingFrom) instanceof ArcanePedestalBlockEntity tile)||!ingredient.test(tile.getItem(0)))){drawingFrom=null;itemTicks=0;}
            var stacks=positions.stream().map(pos->((ArcanePedestalBlockEntity)level.getBlockEntity(pos)).getItem(0)).toList();
            var assignment=IngredientAssignment.matchAvailable(recipe.ingredients.subList(component,recipe.ingredients.size()),stacks);
            if(assignment.isEmpty()){drawingFrom=null;itemTicks=0;vat.syncInfusion();return;}
            var selected=positions.get(assignment.get()[0]);
            if(drawingFrom==null||!drawingFrom.equals(selected)){drawingFrom=selected;itemTicks=5;}
            else if(--itemTicks<=0){
                var tile=(ArcanePedestalBlockEntity)level.getBlockEntity(drawingFrom);var consumed=tile.removeItem(0,1);var remainder=consumed.getCraftingRemainingItem();
                if(!remainder.isEmpty()){if(tile.isEmpty())tile.setFromInfusion(remainder);else net.minecraft.world.Containers.dropItemStack(level,drawingFrom.getX()+.5,drawingFrom.getY()+1,drawingFrom.getZ()+.5,remainder);}
                component++;drawingFrom=null;
            }
            if(drawingFrom!=null)thaumcraft.api.fx.LegacyEffects.emit(level,"infusion_item",drawingFrom.getCenter().add(0,.75,0),vat.getBlockPos().getCenter().add(0,-1,0),0xffffff,1,20,1,0,0);
            vat.syncInfusion();return;
        }
        if(vat.hasEffigy()?vat.infuseSelf(recipe):CreatureInfusions.apply(vat.subject(),recipe,caster)||vat.transformSubject(recipe)){
            tc4revival.network.FocusEffectPacket.blockSparkle(level,vat.getBlockPos().below(),-9999,16);tc4revival.network.FocusEffectPacket.blockSparkle(level,vat.getBlockPos().below(2),-9999,16);
            vat.finishInfusion();clear();
        }
    }
    private void instabilityEvent(VatEntity vat,List<BlockPos> pedestals,int event){
        var level=(ServerLevel)vat.getLevel();var center=vat.getBlockPos();
        int ejection=switch(event){case 0,2,10,13->0;case 6,17->1;case 1,11->2;case 19->3;case 7->4;case 4,15->5;default->-1;};
        if(ejection>=0){
            var occupied=pedestals.stream().filter(pos->level.getBlockEntity(pos) instanceof ArcanePedestalBlockEntity tile&&!tile.isEmpty()).toList();if(occupied.isEmpty())return;
            var pos=occupied.get(level.random.nextInt(occupied.size()));var tile=(ArcanePedestalBlockEntity)level.getBlockEntity(pos);var removed=tile.removeItem(0,1);
            if(ejection<3||ejection==5)net.minecraft.world.Containers.dropItemStack(level,pos.getX()+.5,pos.getY()+1,pos.getZ()+.5,removed);
            if((ejection==1||ejection==3||ejection==2||ejection==4)&&level.isEmptyBlock(pos.above())){
                var block=ejection==1||ejection==3?tc4revival.content.TCArcane.FLUX_GOO.get():tc4revival.content.TCArcane.FLUX_GAS.get();level.setBlock(pos.above(),block.defaultBlockState().setValue(tc4revival.content.FiniteFluidBlock.QUANTA,8),3);
            }
            if(ejection==5)level.explode(null,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,1,Level.ExplosionInteraction.NONE);
            level.blockEvent(pos,tile.getBlockState().getBlock(),11,0);return;
        }
        if(event==9){level.explode(null,center.getX()+.5,center.getY()+.5,center.getZ()+.5,1.5f+level.random.nextFloat(),Level.ExplosionInteraction.NONE);return;}
        if(event==20){
            var players=level.getEntitiesOfClass(ServerPlayer.class,new AABB(center).inflate(10));if(players.isEmpty())return;var player=players.get(level.random.nextInt(players.size()));
            if(level.random.nextFloat()<.25f)thaumcraft.api.ThaumcraftApiHelper.addStickyWarpToPlayer(player,1);else thaumcraft.api.ThaumcraftApiHelper.addWarpToPlayer(player,1+level.random.nextInt(5),true);return;
        }
        boolean harm=event==5||event==16||event==18,all=event==12||event==18;
        for(var entity:level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new AABB(center).inflate(10))){
            if(harm){
                var key=level.random.nextBoolean()?"flux_taint":"vis_exhaust";var effect=net.minecraftforge.registries.ForgeRegistries.MOB_EFFECTS.getValue(ResourceLocation.parse("thaumcraft:"+key));
                if(effect!=null){var instance=new net.minecraft.world.effect.MobEffectInstance(effect,key.equals("flux_taint")?120:2400,0,true,false);if(key.equals("vis_exhaust"))instance.setCurativeItems(List.of());entity.addEffect(instance);}
            }else{entity.hurt(level.damageSources().magic(),4+level.random.nextInt(4));tc4revival.network.FocusEffectPacket.lightning(level,vat.getBlockPos().getCenter(),entity.getBoundingBox().getCenter());}
            if(!all)break;
        }
    }
    public void clear(){recipe=null;caster=null;drawingFrom=null;ticks=component=itemTicks=instability=0;}
    public CompoundTag save(){
        var tag=new CompoundTag();if(recipe==null)return tag;
        tag.putString("Id",recipe.id.toString());tag.putString("Recipe",recipe.json().toString());if(caster!=null)tag.putUUID("Caster",caster);
        tag.putInt("Ticks",ticks);tag.putInt("Component",component);tag.putInt("ItemTicks",itemTicks);tag.putInt("Instability",instability);if(drawingFrom!=null)tag.putLong("DrawingFrom",drawingFrom.asLong());return tag;
    }
    public void load(CompoundTag tag){
        clear();if(!tag.contains("Recipe"))return;
        try{
            String json=tag.getString("Recipe");if(json.length()>65536)return;
            recipe=new VatInfusionRecipe(ResourceLocation.parse(tag.getString("Id")),JsonParser.parseString(json).getAsJsonObject());caster=tag.hasUUID("Caster")?tag.getUUID("Caster"):null;
            ticks=Math.max(0,tag.getInt("Ticks"))%10;component=Math.max(0,Math.min(recipe.ingredients.size(),tag.getInt("Component")));itemTicks=Math.max(0,Math.min(5,tag.getInt("ItemTicks")));instability=Math.max(-100,Math.min(25,tag.getInt("Instability")));
            if(tag.contains("DrawingFrom"))drawingFrom=BlockPos.of(tag.getLong("DrawingFrom"));
        }catch(RuntimeException malformed){clear();}
    }
}
