package tc4revival.debug;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import tc4revival.content.LegacyOreBlock;

/** Bounded work per tick; never retains the surveyed chunks or touches existing generation markers. */
@Mod.EventBusSubscriber(modid="thaumcraft")
public class OreSurveyCommand {
    private static Survey active;
    static com.mojang.brigadier.builder.ArgumentBuilder<CommandSourceStack,?> tree(){
        return Commands.literal("ore_survey")
            .then(Commands.literal("cancel").executes(c->{active=null;return 1;}))
            .then(Commands.literal("status").executes(c->{if(active!=null)c.getSource().sendSuccess(()->Component.literal(active.report()),false);return active==null?0:1;}))
            .then(Commands.literal("start").then(Commands.argument("blocks",IntegerArgumentType.integer(16,4096)).executes(c->{
                if(active!=null){c.getSource().sendFailure(Component.literal("An ore survey is already running."));return 0;}
                active=new Survey(c.getSource(),IntegerArgumentType.getInteger(c,"blocks"));
                c.getSource().sendSuccess(()->Component.literal("Ore survey started at your current chunk. Generates and saves chunks; use a test world. "+active.report()),false);return 1;
            })));
    }
    private static class Survey {
        final CommandSourceStack source;final ServerLevel level;final int startX,startZ,width;
        int generated,counted;long stone,deepslate,solid;final long[] ores=new long[6];
        Survey(CommandSourceStack source,int blocks){this.source=source;level=source.getLevel();var p=new net.minecraft.world.level.ChunkPos(BlockPos.containing(source.getPosition()));startX=p.x;startZ=p.z;width=(blocks+15)/16;}
        String report(){return "Ore survey "+width*16+"x"+width*16+": generated="+generated+"/"+(width*width)+", counted="+counted+", ores="+java.util.Arrays.toString(ores)+", stone="+stone+", deepslate="+deepslate+", solid="+solid+", ores/chunk="+(counted==0?0:java.util.Arrays.stream(ores).sum()/(double)counted)+", ores/(stone+ores)="+(stone==0?0:java.util.Arrays.stream(ores).sum()/(double)(stone+java.util.Arrays.stream(ores).sum()));}
        boolean tick(){
            if(generated<width*width){int i=generated++;level.getChunk(startX+i%width,startZ+i/width);return false;}
            // A separate counting pass allows queued legacy decorators to finish first.
            if(counted<width*width){int i=counted++;var chunk=level.getChunk(startX+i%width,startZ+i/width);
                for(var section:chunk.getSections())if(!section.hasOnlyAir())for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<16;y++){
                    var state=section.getBlockState(x,y,z);if(state.is(Blocks.STONE))stone++;if(state.is(Blocks.DEEPSLATE))deepslate++;if(state.isSolid())solid++;
                    if(state.getBlock() instanceof LegacyOreBlock ore&&ore.metadata()>=1&&ore.metadata()<=6)ores[ore.metadata()-1]++;
                }
                return false;
            }
            String report=report();source.sendSuccess(()->Component.literal(report),false);com.mojang.logging.LogUtils.getLogger().info(report);
            try{var path=level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("ore-survey.txt");java.nio.file.Files.writeString(path,report+System.lineSeparator(),java.nio.file.StandardOpenOption.CREATE,java.nio.file.StandardOpenOption.APPEND);}catch(java.io.IOException e){com.mojang.logging.LogUtils.getLogger().error("Could not save ore survey",e);}
            return true;
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){if(event.phase==TickEvent.Phase.END&&active!=null)try{if(active.tick())active=null;}catch(Exception e){com.mojang.logging.LogUtils.getLogger().error("Ore survey failed",e);active=null;}}
    @SubscribeEvent public static void stop(ServerStoppingEvent event){active=null;}
}
