package technomancy;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Temporary joint validation commands. This source set never enters a product jar. */
@Mod.EventBusSubscriber(modid=Technomancy.ID)
public class TechnomancyScenes {
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("technomancy_test").requires(s->s.hasPermission(4))
            .then(Commands.literal("gallery").executes(c->gallery(c.getSource().getPlayerOrException())))
            .then(Commands.literal("effects").executes(c->effects(c.getSource().getPlayerOrException())))
            .then(Commands.literal("processing").executes(c->processing(c.getSource().getPlayerOrException())))
            .then(Commands.literal("snapshot").then(Commands.argument("x",IntegerArgumentType.integer()).then(Commands.argument("y",IntegerArgumentType.integer()).then(Commands.argument("z",IntegerArgumentType.integer()).executes(c->{
                var p=new BlockPos(IntegerArgumentType.getInteger(c,"x"),IntegerArgumentType.getInteger(c,"y"),IntegerArgumentType.getInteger(c,"z"));var be=c.getSource().getLevel().getBlockEntity(p);
                c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(be==null?"No block entity":be.saveWithoutMetadata().toString()),false);return 1;
            }))))));
    }
    private static int gallery(ServerPlayer player){
        var level=player.serverLevel();level.setDayTime(6000);level.setWeatherParameters(6000,0,false,false);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,level.getServer());
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,level.getServer());
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false,level.getServer());
        for(var p:BlockPos.betweenClosed(-4,79,-10,46,79,42))level.setBlock(p,Blocks.SMOOTH_STONE.defaultBlockState(),3);
        int index=0;
        for(var k:MachineKind.values())if(k.enabled()){
            var p=new BlockPos(index%8*6,80,index/8*6);level.setBlock(p,TechRegistry.MACHINES.get(k).get().defaultBlockState(),3);
            var m=(MachineEntity)level.getBlockEntity(p);m.owner=player.getUUID();m.redstone=0;
            if(k==MachineKind.NODE_FABRICATOR)NodeStructure.create(m);
            if(k==MachineKind.QUANTUM_JAR){m.insert(MachineLogic.aspect("aer"),384,false);m.filter=MachineLogic.aspect("aer");}
            if(k==MachineKind.CREATIVE_JAR)m.filter=MachineLogic.aspect("ignis");
            if(k==MachineKind.IGNIS_INCINERATOR)m.inventory.setStackInSlot(0,new ItemStack(Items.RAW_IRON,3));
            m.changed();index++;
            org.slf4j.LoggerFactory.getLogger("TECHNOMANCY_SCENE").info("{} {}",k.id(),p.toShortString());
        }
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.teleportTo(level,15,83,-7,0,10);
        player.getInventory().clearContent();for(String id:new String[]{"ritual_tome","coil_coupler","potency_gem","pen","energized_wand_core","technoturge_scepter"})player.addItem(TechRegistry.item(id).getDefaultInstance());
        player.addItem(new ItemStack(tc4revival.content.TCItems.THAUMONOMICON.get()));
        return index;
    }
    private static MachineEntity put(ServerPlayer p,MachineKind kind,BlockPos pos){
        var l=p.serverLevel();l.setBlock(pos,TechRegistry.MACHINES.get(kind).get().defaultBlockState(),3);var m=(MachineEntity)l.getBlockEntity(pos);m.owner=p.getUUID();m.redstone=0;return m;
    }
    private static int processing(ServerPlayer p){
        var m=(MachineEntity)p.serverLevel().getBlockEntity(new BlockPos(24,80,6));
        m.inventory.setStackInSlot(0,new ItemStack(Items.RAW_IRON,3));m.inventory.setStackInSlot(1,ItemStack.EMPTY);m.insert(MachineLogic.aspect("ignis"),64,false);m.changed();
        var jar=put(p,MachineKind.CREATIVE_JAR,m.getBlockPos().below());jar.filter=MachineLogic.aspect("ignis");jar.changed();return 1;
    }
    private static int effects(ServerPlayer p){
        var level=p.serverLevel();for(var q:BlockPos.betweenClosed(60,79,-8,90,79,12))level.setBlock(q,Blocks.SMOOTH_STONE.defaultBlockState(),3);
        level.setBlock(new BlockPos(70,80,0),Blocks.AIR.defaultBlockState(),3);
        level.setBlock(new BlockPos(70,80,6),Blocks.AIR.defaultBlockState(),3);
        level.setBlock(new BlockPos(70,81,3),Blocks.AIR.defaultBlockState(),3);
        var one=put(p,MachineKind.NODE_FABRICATOR,new BlockPos(70,80,0));
        var two=put(p,MachineKind.NODE_FABRICATOR,new BlockPos(70,80,6));
        level.setBlock(one.getBlockPos(),one.getBlockState().setValue(MachineBlock.FACING,Direction.SOUTH),3);
        NodeStructure.create(one);NodeStructure.create(two);one.energy=50000000;one.insert(MachineLogic.aspect("auram"),61,false);two.insert(MachineLogic.aspect("vitium"),61,false);one.changed();two.changed();NodeFabrication.begin(one);
        // An extended lifetime permits inspection; actual activation/expiry is covered by GameTest.
        var hole=put(p,MachineKind.DARK_CATALYST,new BlockPos(85,83,6));hole.state.putString("ritual","black_hole_1");hole.state.putInt("ritualAge",-10000);hole.active=true;hole.changed();
        p.teleportTo(level,66,80,3,270,0);return 1;
    }
}
