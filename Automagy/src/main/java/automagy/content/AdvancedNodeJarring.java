package automagy.content;

import automagy.AutomagyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import tc4revival.content.*;
import tc4revival.research.KnowledgeSavedData;

/** The original Automagy jar: nine warded silverwood logs, 26 owned warded glass blocks, 125 of each primal. */
public final class AdvancedNodeJarring {
    public static void register() {
        thaumcraft.api.wands.WandTriggerRegistry.register(ResourceLocation.parse("automagy:advanced_node_jar"),
                state->state.is(TCArcane.WARDED.get())||state.is(TCArcane.WARDED_GLASS.get()),AdvancedNodeJarring::form);
    }
    public static boolean matches(Level level,BlockPos origin,ServerPlayer player) {
        for(int row=0;row<4;row++)for(int x=0;x<3;x++)for(int z=0;z<3;z++) {
            var pos=origin.offset(x,2-row,z);
            if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return false;
            var tile=level.getBlockEntity(pos);
            if(row==0) {
                if(!(tile instanceof WardedBlockEntity ward)||!ward.ownedBy(player)||!ward.original().is(TCArcane.SILVERWOOD_LOG.get()))return false;
            } else if(row==2&&x==1&&z==1) {
                if(NodeJarCapture.payload(level,pos)==null)return false;
            } else if(!level.getBlockState(pos).is(TCArcane.WARDED_GLASS.get())
                    ||!(tile instanceof OwnedDeviceBlockEntity owned)||!owned.access().ownedBy(player.getGameProfile().getName()))return false;
        }
        return true;
    }
    public static InteractionResult form(UseOnContext context) {
        var stack=context.getItemInHand();
        if(!AutomagyConfig.ADVANCED_JARRING.get()||!(context.getPlayer() instanceof ServerPlayer player)
                ||!(stack.getItem() instanceof CastingWandItem wand)||!WandFocusItem.attached(stack).isEmpty())return InteractionResult.PASS;
        if(!KnowledgeSavedData.get(player.getServer()).snapshot(player.getUUID())
                .map(k->k.researchMarkers().contains("automagy:ADVNODEJAR")).orElse(false))return InteractionResult.PASS;
        var level=context.getLevel();
        for(int x=-2;x<=0;x++)for(int y=-2;y<=1;y++)for(int z=-2;z<=0;z++) {
            var origin=context.getClickedPos().offset(x,y,z);
            if(!matches(level,origin,player))continue;
            for(int row=0;row<4;row++)for(int col=0;col<3;col++)for(int depth=0;depth<3;depth++) {
                var pos=origin.offset(col,2-row,depth);
                if(!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,context.getClickedFace(),stack))return InteractionResult.FAIL;
            }
            var cost=new java.util.LinkedHashMap<tc4revival.aspect.Aspect,Integer>();
            for(var aspect:thaumcraft.api.aspects.AspectSelection.select(tc4revival.aspect.TCAspects.catalog(),thaumcraft.api.aspects.AspectSelection.STRUCTURE_COST))cost.put(aspect,125);
            if(!wand.consumeForPlayer(stack,player,cost,true,true,false))return InteractionResult.FAIL;
            var payload=NodeJarCapture.payload(level,origin.offset(1,0,1));
            boolean success=NodeJarCapture.convertValidated(level,origin,payload,
                    ()->wand.consumeForPlayer(stack,player,cost,true,true,true),(pos,state)->level.setBlock(pos,state,18),
                    ()->matches(level,origin,player));
            if(!success)return InteractionResult.FAIL;
            level.blockEvent(origin.offset(1,0,1),TCArcane.NODE_JAR.get(),9,0);
            level.playSound(null,origin,TCSounds.WAND.get(),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
