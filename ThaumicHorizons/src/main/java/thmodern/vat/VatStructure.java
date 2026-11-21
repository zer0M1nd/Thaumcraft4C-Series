package thmodern.vat;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.TCAspects;
import tc4revival.content.*;
import thmodern.Horizons;

@Mod.EventBusSubscriber(modid=Horizons.ID)
public final class VatStructure {
    private static final Set<VatEntity> DISMANTLING=Collections.newSetFromMap(new IdentityHashMap<>());
    public static BlockPos offset(int part){return new BlockPos(part%3-1,part/9-3,part/3%3-1);}
    public static BlockPos center(BlockPos pos,BlockState state){return pos.subtract(offset(state.getValue(VatBlock.PART)));}
    public static boolean port(int part){return part==1||part==3||part==5||part==7;}
    public static BlockState original(int part){
        if(part<9||part>=27)return ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse(part%9==4?"thaumcraft:alchemical_construct":"thaumcraft:greatwood_planks")).defaultBlockState();
        return part%9==4?Blocks.WATER.defaultBlockState():Blocks.GLASS.defaultBlockState();
    }
    public static VatEntity controller(Level level,BlockPos pos,BlockState state){
        if(!state.is(Horizons.VAT.get()))return null;var center=center(pos,state);
        return level.hasChunkAt(center)&&level.getBlockEntity(center) instanceof VatEntity vat?vat:null;
    }
    @SubscribeEvent public static void form(PlayerInteractEvent.RightClickBlock event){
        if(!(event.getEntity() instanceof ServerPlayer player)||!(event.getItemStack().getItem() instanceof CastingWandItem wand)||!thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(player,"healingVat"))return;
        if(!player.mayBuild())return;var level=player.serverLevel();
        for(int y=0;y<4;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var center=event.getPos().offset(x,y,z);if(!matches(level,center))continue;
            for(int part=0;part<36;part++){
                var pos=center.offset(offset(part));
                if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,event.getFace()==null?Direction.UP:event.getFace(),event.getItemStack())||ForgeHooks.onBlockBreakEvent(level,player.gameMode.getGameModeForPlayer(),player,pos)<0)return;
            }
            if(!matches(level,center))return;
            var costs=new LinkedHashMap<tc4revival.aspect.Aspect,Integer>();for(var key:List.of("aqua","terra","ordo"))costs.put(TCAspects.catalog().require(key),50);
            if(!wand.consumeForPlayer(event.getItemStack(),player,costs,true,true,true))return;
            // All positions were checked before changing any block. Centre is installed last.
            for(int part=0;part<36;part++)if(part!=31)level.setBlock(center.offset(offset(part)),Horizons.VAT.get().defaultBlockState().setValue(VatBlock.PART,part),2);
            level.setBlock(center,Horizons.VAT.get().defaultBlockState().setValue(VatBlock.PART,31),3);
            for(int part=0;part<36;part++){var pos=center.offset(offset(part));level.updateNeighborsAt(pos,Horizons.VAT.get());}
            event.setCanceled(true);event.setCancellationResult(InteractionResult.CONSUME);return;
        }
    }
    public static boolean matches(Level level,BlockPos center){
        for(int part=0;part<36;part++){
            var pos=center.offset(offset(part));if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.getBlockState(pos).equals(original(part)))return false;
            // Never delete inventories or other tile data while assembling.
            if(level.getBlockEntity(pos)!=null)return false;
        }
        return true;
    }
    public static void pull(VatEntity vat){
        var level=vat.getLevel();String key=vat.essentia().wanted();if(level==null||key==null)return;
        for(int part:new int[]{1,3,5,7}){
            var port=vat.getBlockPos().offset(offset(part));if(!level.hasChunkAt(port)||!level.getBlockState(port).is(Horizons.VAT.get())||level.getBlockState(port).getValue(VatBlock.PART)!=part)continue;
            for(var side:Direction.values()){
                var pos=port.relative(side);var from=side.getOpposite();
                if(!level.hasChunkAt(pos)||!(level.getBlockEntity(pos) instanceof EssentiaTransport source)||!source.connectable(from)||!source.canOutput(from)||source.suction(from)>=128||source.minimumSuction()>128)continue;
                // Notify demand-driven providers, but do not gate all requested aspects on
                // the amount of one advertised type (healing can also use weaker essentia).
                source.essentiaType(from);
                for(var entry:vat.essentia().required().entrySet()){
                    if(vat.essentia().amount(entry.getKey())>=entry.getValue())continue;
                    var aspect=TCAspects.catalog().require(entry.getKey());
                    if(source.takeEssentia(aspect,1,from)>0){vat.accept(entry.getKey(),1);return;}
                }
            }
        }
    }
    public static void check(VatEntity vat){
        var level=vat.getLevel();if(level==null||level.isClientSide||DISMANTLING.contains(vat))return;
        for(int part=0;part<36;part++)if(!level.hasChunkAt(vat.getBlockPos().offset(offset(part))))return;
        for(int part=0;part<36;part++){
            var pos=vat.getBlockPos().offset(offset(part));var state=level.getBlockState(pos);
            if(!state.is(Horizons.VAT.get())||state.getValue(VatBlock.PART)!=part){dismantle(vat,null);return;}
        }
    }
    public static void dismantle(VatEntity vat,BlockPos broken){
        var level=vat.getLevel();if(level==null||level.isClientSide||!DISMANTLING.add(vat))return;
        try {
            // Break contents once; a removed part is never restored over a player's replacement.
            vat.recoverOnDismantle();
            for(int part=0;part<36;part++){
                var pos=vat.getBlockPos().offset(offset(part));if(pos.equals(broken)||!level.hasChunkAt(pos))continue;
                var state=level.getBlockState(pos);
                if(state.is(Horizons.VAT.get())&&state.getValue(VatBlock.PART)==part)level.setBlock(pos,original(part),3);
            }
        }finally{DISMANTLING.remove(vat);}
    }
}
