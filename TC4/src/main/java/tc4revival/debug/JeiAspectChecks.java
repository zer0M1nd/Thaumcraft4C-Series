package tc4revival.debug;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import tc4revival.aspect.TCAspects;
import tc4revival.aspect.item.ItemStackAspects;
import tc4revival.research.*;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="thaumcraft")
public class JeiAspectChecks {
    private static BrowserRecipes.Catalog survival,creative;
    private static final java.util.Map<String,Integer> survivalRows=new java.util.HashMap<>(),creativeRows=new java.util.HashMap<>();
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if(event.phase!=net.minecraftforge.event.TickEvent.Phase.END||survival==null)return;
        try {
            collect(survival,survivalRows);collect(creative,creativeRows);
            if(!survival.done()||!creative.done())return;
            if(!survivalRows.equals(creativeRows))throw new IllegalStateException("Creative/survival catalogs differ");
            for(String type:java.util.List.of("arcane","crucible","infusion","aspect_combine"))
                if(survivalRows.keySet().stream().noneMatch(k->k.startsWith(type+"|")))throw new IllegalStateException("Missing catalog type: "+type);
            tc4revival.TC4Revival.LOGGER.info("JEI public catalog passed: {} identical recipes across fresh survival and creative; all four recipe categories present.",survivalRows.size());
            clear();
        }catch(RuntimeException failure){tc4revival.TC4Revival.LOGGER.error("JEI public catalog FAILED",failure);clear();}
    }
    private static void collect(BrowserRecipes.Catalog catalog,java.util.Map<String,Integer> rows) {
        for(var row:catalog.drain(4096,2_000_000L)) {
            String key=row.kind()+"|"+row.id()+"|"+row.research()+"|"+row.aspectInputs()+"|"+row.aspectOutputs()+"|"+row.output().save(new net.minecraft.nbt.CompoundTag());
            rows.merge(key,1,Integer::sum);
        }
    }
    private static void clear(){survival=null;creative=null;survivalRows.clear();creativeRows.clear();}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent event){clear();}
    public static int verify(CommandSourceStack source) {
        var blank=new PlayerKnowledge(TCAspects.catalog());
        var scanned=new PlayerKnowledge(TCAspects.catalog());
        var learned=new PlayerKnowledge(TCAspects.catalog());TCAspects.catalog().all().forEach(learned::discover);
        var sword=new ItemStack(Items.IRON_SWORD);sword.enchant(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS,3);
        var stacks=java.util.List.of(new ItemStack(Items.ENDER_PEARL),new ItemStack(Items.IRON_SWORD),sword,new ItemStack(Items.ECHO_SHARD));
        for(var stack:stacks) {
            scanned.rememberScan(PlayerKnowledge.ScanKind.OBJECT,"@"+ScanIdentity.item(stack));
            var expected=BrowserRecipe.amounts(ItemStackAspects.resolve(stack).orElseGet(tc4revival.aspect.AspectAmounts::new));
            if(stack.is(Items.ENDER_PEARL)&&expected.isEmpty())throw new IllegalStateException("Pearl aspect fixture missing");
            for(var knowledge:new PlayerKnowledge[]{null,blank,scanned,learned})for(boolean creative:new boolean[]{true,false,true,false}) {
                var row=BrowserRecipes.item(stack,knowledge,creative).orElseThrow();
                if(!row.aspectOutputs().equals(expected)||row.dynamic()||!row.research().isEmpty()
                        ||!ItemStack.isSameItemSameTags(row.inputs().get(0).get(0),stack))throw new IllegalStateException("JEI aspect visibility depends on player state or loses NBT");
                var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                try {
                    new tc4revival.network.BrowserRecipesPacket(19,false,true,java.util.List.of(row)).encode(buffer);
                    var copy=tc4revival.network.BrowserRecipesPacket.decode(buffer).recipes().get(0);
                    if(!copy.aspectOutputs().equals(expected))throw new IllegalStateException("Aspect packet lost values");
                }finally {buffer.release();}
            }
        }
        if(BrowserRecipes.item(ItemStack.EMPTY).isPresent())throw new IllegalStateException("Empty stack should have no page");
        clear();
        var player=new net.minecraftforge.common.util.FakePlayer(source.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"JeiCatalogAudit"));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);survival=BrowserRecipes.catalog(player);
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);creative=BrowserRecipes.catalog(player);
        source.sendSuccess(()->Component.literal("JEI aspects passed: pearl/swords/modern item, absent/blank/scanned/learned knowledge, repeated creative-survival changes, full amounts, NBT and packet round-trip."),false);
        source.sendSuccess(()->Component.literal("Full public catalog parity audit started; completion is written to the server log."),false);
        return 1;
    }
}
