package tc4revival.debug;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import tc4revival.content.*;

public class Report0919TwoChecks {
    public static int verify(net.minecraft.commands.CommandSourceStack source) {
        var level=source.getLevel();var pos=new BlockPos(360,200,360);
        if(!level.hasChunkAt(pos)){source.sendFailure(Component.literal("Load the test area first"));return 0;}
        var chunk=level.getChunkAt(pos);var snapshot=new java.util.ArrayList<java.util.List<Holder<net.minecraft.world.level.biome.Biome>>>();
        for(var section:chunk.getSections()){var entries=new java.util.ArrayList<Holder<net.minecraft.world.level.biome.Biome>>();for(int y=0;y<4;y++)for(int z=0;z<4;z++)for(int x=0;x<4;x++)entries.add(section.getNoiseBiome(x,y,z));snapshot.add(entries);}
        var block=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos);
        var player=new net.minecraftforge.common.util.FakePlayer(level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"Crab0919"));
        var crab=TCEntities.ELDRITCH_CRAB.get().create(level);
        try {
            level.setBlock(pos,Blocks.STONE.defaultBlockState(),3);
            for(var target:java.util.List.of(TCBiomes.TAINTED_LAND,TCBiomes.EERIE,TCBiomes.MAGICAL_FOREST)) {
                WorldBiomeChanges.changeColumn(level,pos,net.minecraft.world.level.biome.Biomes.PLAINS);
                require(WorldBiomeChanges.changeNodeBiome(level,pos.above(8),pos.above(8),target),"Surface conversion failed");
                require(level.getBiomeManager().getNoiseBiomeAtPosition(pos.below(40)).is(net.minecraft.world.level.biome.Biomes.PLAINS),"Surface node changed cave");
                require(level.getBiomeManager().getNoiseBiomeAtPosition(pos.above(8)).is(target),"Surface biome missing");
                WorldBiomeChanges.changeColumn(level,pos,net.minecraft.world.level.biome.Biomes.PLAINS);
                require(WorldBiomeChanges.changeNodeBiome(level,pos.below(40),pos.below(40),target),"Cave conversion failed");
                require(level.getBiomeManager().getNoiseBiomeAtPosition(pos.above(8)).is(net.minecraft.world.level.biome.Biomes.PLAINS),"Cave node changed surface");
                require(level.getBiomeManager().getNoiseBiomeAtPosition(pos.below(40)).is(target),"Cave biome missing");
            }
            require(crab!=null,"Cannot create crab");player.setPos(360,220,360);require(crab.startRiding(player,true),"Mount failed");
            player.xOld=360;player.yOld=220;player.zOld=360;player.xo=359;player.yo=219;player.zo=359;player.setPos(370,222,365);
            crab.synchronizePassengerPosition();double offset=player.getPassengersRidingOffset()+crab.getMyRidingOffset();
            require(crab.getX()==370&&crab.getZ()==365&&Math.abs(crab.getY()-222-offset)<.00001,"Crab current position wrong");
            require(crab.xOld==360&&crab.xo==359&&Math.abs(crab.yOld-220-offset)<.00001,"Crab interpolation histories wrong");
            int completions=0;
            for(var entry:tc4revival.research.ResearchCatalogs.catalog().all()) {
                var packet=tc4revival.network.ResearchCompletionPacket.from(new tc4revival.research.ResearchProgression.Completion(entry.key(),0,0));
                require(packet.key().equals(entry.key()),"Research key changed case");completions++;
            }
            var hungry=new HungryChestBlockEntity(pos,TCArcane.HUNGRY_CHEST.get().defaultBlockState());hungry.triggerEvent(2,2);
            require(hungry.lid==.2f,"Eat opening not legacy 0.2");
            HungryChestBlockEntity.tick(level,pos,hungry.getBlockState(),hungry);HungryChestBlockEntity.tick(level,pos,hungry.getBlockState(),hungry);
            require(hungry.lid==0,"Eat did not close in two ticks");
            source.sendSuccess(()->Component.literal("0919_2 PASSED: three biome targets preserve surface/caves, moving passenger histories, all research completion icons, two-tick chest eating."),false);
            tc4revival.TC4Revival.LOGGER.info("0919_2 PASSED; {} research completion packets constructed",completions);return 1;
        }catch(Exception e){source.sendFailure(Component.literal("0919_2 FAILED: "+e));tc4revival.TC4Revival.LOGGER.error("0919_2 FAILED",e);return 0;}
        finally {
            if(crab!=null){crab.stopRiding();crab.discard();}player.discard();block.restore(true,false);
            for(int i=0;i<chunk.getSections().length;i++){var old=snapshot.get(i);chunk.getSections()[i].fillBiomesFromNoise((x,y,z,s)->old.get((y&3)*16+(z&3)*4+(x&3)),level.getChunkSource().randomState().sampler(),QuartPos.fromBlock(chunk.getPos().getMinBlockX()),QuartPos.fromSection(chunk.getSectionYFromSectionIndex(i)),QuartPos.fromBlock(chunk.getPos().getMinBlockZ()));}
            chunk.setUnsaved(true);level.getChunkSource().chunkMap.resendBiomesForChunks(java.util.List.of(chunk));
        }
    }
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
