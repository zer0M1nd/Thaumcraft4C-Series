package temodern.test;

/** Development-only scene setup; excluded from both the published mod and its gameplay API. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumicenergistics")
public class Report0920Visual {
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void keepMounted(net.minecraftforge.event.TickEvent.PlayerTickEvent event){
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END||event.player.level().isClientSide)return;
        for(var crab:event.player.level().getEntitiesOfClass(tc4revival.content.EldritchCrabEntity.class,event.player.getBoundingBox().inflate(8)))
            if(crab.getTags().contains("report0920_visual"))crab.startRiding(event.player,true);
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void register(net.minecraftforge.event.RegisterCommandsEvent event){
        var root=net.minecraft.commands.Commands.literal("tevisual").requires(s->s.hasPermission(2));
        for(String mode:new String[]{"pattern","terminal","crab","inscriber","assembler"})root.then(net.minecraft.commands.Commands.literal(mode).executes(c->{
            var player=c.getSource().getPlayerOrException();var level=player.serverLevel();var pos=new net.minecraft.core.BlockPos(300,70,300);
            player.teleportTo(level,300.5,70,303.5,180,20);
            level.setBlockAndUpdate(pos.west(),appeng.core.definitions.AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
            if(mode.equals("crab")){
                var crab=tc4revival.content.TCEntities.ELDRITCH_CRAB.get().create(level);crab.addTag("report0920_visual");crab.setNoAi(true);crab.setPos(player.position());level.addFreshEntity(crab);crab.startRiding(player,true);return 1;
            }
            level.setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            if(mode.equals("pattern")||mode.equals("terminal")){
                level.setBlockAndUpdate(pos,appeng.core.definitions.AEBlocks.CABLE_BUS.block().defaultBlockState());
                var cable=(appeng.blockentity.networking.CableBusBlockEntity)level.getBlockEntity(pos);
                cable.addPart(appeng.core.definitions.AEParts.GLASS_CABLE.item(appeng.api.util.AEColor.TRANSPARENT),null,player);
                var item=mode.equals("pattern")?appeng.core.definitions.AEParts.PATTERN_ENCODING_TERMINAL.asItem():net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("thaumicenergistics","arcane_terminal"));
                cable.addPart((appeng.api.parts.IPartItem<?>)item,net.minecraft.core.Direction.SOUTH,player);
                player.getInventory().add(appeng.core.definitions.AEItems.BLANK_PATTERN.stack(16));
            }else{
                String id=mode.equals("inscriber")?"knowledge_inscriber":"arcane_assembler";
                level.setBlockAndUpdate(pos,temodern.machine.Machines.ALL.get(id).get().defaultBlockState());
            }
            player.getInventory().selected=0;player.getInventory().setItem(0,net.minecraft.world.item.ItemStack.EMPTY);
            return 1;
        }));event.getDispatcher().register(root);
    }
}
