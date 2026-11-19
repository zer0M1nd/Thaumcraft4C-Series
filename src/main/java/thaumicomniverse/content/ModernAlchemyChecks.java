package thaumicomniverse.content;

import tc4revival.content.*;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import tc4revival.aspect.TCAspects;

/** Opt-in checks for a reserved empty test area; never runs in a player's world automatically. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumicomniverse")
public final class ModernAlchemyChecks {
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void commands(net.minecraftforge.event.RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("tc_inputs_verify").requires(s->s.hasPermission(2)).executes(c->{
            try{verify(c.getSource().getLevel());c.getSource().sendSuccess(()->Component.literal("Modern alchemy: nearest/full/restricted/filter/closed/multiple-servo/save checks PASS"),false);return 1;}
            catch(Exception e){c.getSource().sendFailure(Component.literal("Modern alchemy checks FAILED: "+e));throw new RuntimeException(e);}
        }));
    }
    private static void check(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
    public static void verify(ServerLevel world){
        var origin=new BlockPos(128,100,128);var positions=new java.util.ArrayList<BlockPos>();
        for(int x=0;x<7;x++)for(int y=0;y<3;y++)for(int z=-1;z<3;z++){var pos=origin.offset(x,y,z);world.getChunkAt(pos);if(!world.isEmptyBlock(pos))throw new IllegalStateException("Test area 128,100,128 must be empty");positions.add(pos);}
        try{
            var air=TCAspects.catalog().require("aer");var fire=TCAspects.catalog().require("ignis");
            for(int x=0;x<6;x++)world.setBlockAndUpdate(origin.offset(x,1,0),ModernAlchemy.CONVENIENT_TUBE.get().defaultBlockState());
            for(int x:new int[]{0,2,5})world.setBlockAndUpdate(origin.offset(x,0,0),TCArcane.ESSENTIA_JAR.get().defaultBlockState());
            var source=(EssentiaJarBlockEntity)world.getBlockEntity(origin);var near=(EssentiaJarBlockEntity)world.getBlockEntity(origin.offset(2,0,0));var far=(EssentiaJarBlockEntity)world.getBlockEntity(origin.offset(5,0,0));
            var start=(ConvenientTubeBlockEntity)world.getBlockEntity(origin.above());start.setServo(Direction.DOWN,true);source.addToContainer(air,32);
            start.transferCycle();check(source.amount()==31&&near.amount()==1&&far.amount()==0,"nearest receiver");
            near.addToContainer(air,63);start.transferCycle();check(far.amount()==1&&source.amount()==30,"full receiver must yield");
            far.addToContainer(air,63);start.transferCycle();check(source.amount()==30,"all full must stop extraction");
            near.empty();far.empty();
            var middle=(ConvenientTubeBlockEntity)world.getBlockEntity(origin.offset(1,1,0));middle.flow().setOpen(Direction.EAST,false);start.transferCycle();check(source.amount()==30,"closed port");middle.flow().setOpen(Direction.EAST,true);
            world.setBlockAndUpdate(origin.offset(1,1,0),ModernAlchemy.CONVENIENT_FILTER.get().defaultBlockState());middle=(ConvenientTubeBlockEntity)world.getBlockEntity(origin.offset(1,1,0));middle.flow().filter(fire);start.transferCycle();check(source.amount()==30,"filter rejection");middle.flow().filter(air);start.transferCycle();check(source.amount()==29&&near.amount()==1,"filter acceptance");
            // A short restricted spur loses to a longer ordinary route.
            world.setBlockAndUpdate(origin.offset(0,1,1),ModernAlchemy.CONVENIENT_RESTRICTOR.get().defaultBlockState());world.setBlockAndUpdate(origin.offset(0,0,1),TCArcane.ESSENTIA_JAR.get().defaultBlockState());
            var restricted=(EssentiaJarBlockEntity)world.getBlockEntity(origin.offset(0,0,1));start.transferCycle();check(restricted.amount()==0&&near.amount()==2,"restricted path weight");
            // A second independently installed servo extracts an additional unit per cycle.
            world.setBlockAndUpdate(origin.offset(0,1,-1),TCArcane.ALEMBIC.get().defaultBlockState());var upper=(AlembicBlockEntity)world.getBlockEntity(origin.offset(0,1,-1));upper.addToContainer(air,8);start.setServo(Direction.NORTH,true);
            int total=near.amount();start.transferCycle();check(near.amount()==total+2&&upper.amount()==7,"multiple servos");
            var saved=start.saveWithoutMetadata();var restored=new ConvenientTubeBlockEntity(start.getBlockPos(),start.getBlockState());restored.load(saved);check(restored.servo(Direction.NORTH)&&restored.servo(Direction.DOWN),"servo persistence");
            check(!start.canInput(Direction.EAST)&&!start.canOutput(Direction.EAST)&&start.suction(Direction.EAST)==0,"independent of suction");
        }finally{for(var pos:positions)world.removeBlock(pos,false);}
    }
    private ModernAlchemyChecks(){}
}
