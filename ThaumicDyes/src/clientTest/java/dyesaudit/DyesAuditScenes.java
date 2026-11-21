package dyesaudit;

import com.mojang.brigadier.arguments.StringArgumentType;
import dyesmodern.*;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.RegisterCommandsEvent;
import tc4revival.content.*;
import tc4revival.research.*;

/** Server-authoritative fixtures and snapshots, never an alternate implementation of a recipe. */
public final class DyesAuditScenes {
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    private static final class Session {
        final ServerLevel level;final BlockPos anchor;final ListTag inventory;final PlayerKnowledge knowledge;
        final LinkedHashMap<BlockPos,BlockSnapshot> blocks=new LinkedHashMap<>();final List<UUID> entities=new ArrayList<>();
        Session(ServerPlayer player){level=player.serverLevel();anchor=player.blockPosition().offset(4,0,4);inventory=player.getInventory().save(new ListTag());knowledge=KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).orElseThrow();}
        void block(BlockPos pos,BlockState state){
            if(!level.hasChunkAt(pos))throw new IllegalStateException("Fixture chunk is not loaded: "+pos);
            if(Math.abs(pos.getX()-anchor.getX())>64||Math.abs(pos.getZ()-anchor.getZ())>64||Math.abs(pos.getY()-anchor.getY())>8)throw new IllegalArgumentException("Fixture outside bounded scene");
            blocks.computeIfAbsent(pos.immutable(),p->BlockSnapshot.create(level.dimension(),level,p));level.setBlock(pos,state,3);
        }
        void floor(BlockPos pos){block(pos.below(),Blocks.SMOOTH_STONE.defaultBlockState());}
        void entity(Entity entity){if(!level.addFreshEntity(entity))throw new IllegalStateException("Fixture entity rejected: "+entity.getType());entities.add(entity.getUUID());}
    }
    public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("dyesaudit_scene").requires(source->source.hasPermission(2))
                .executes(ctx->run(ctx.getSource(),"help"))
                .then(Commands.argument("arguments",StringArgumentType.greedyString()).executes(ctx->run(ctx.getSource(),StringArgumentType.getString(ctx,"arguments")))));
    }
    private static int run(CommandSourceStack source,String arguments){
        String id=DyesAuditHarness.requestId();String operation=arguments;
        try{
            if(!Boolean.getBoolean("tc4.validationHarness"))throw new IllegalStateException("Test helper disabled");
            var tokens=new ArrayList<>(List.of(arguments.trim().split("\\s+")));
            for(var token:List.copyOf(tokens))if(token.startsWith("request=")){String requested=token.substring(8);if(!requested.matches("[A-Za-z0-9_-]{1,80}"))throw new IllegalArgumentException("Invalid request ID");id=requested;tokens.remove(token);}
            var player=source.getPlayerOrException();String action=tokens.remove(0);var before=snapshot(player);Map<String,Object> details;
            if(action.equals("help"))details=Map.of("commands",List.of("catalog","gallery [dye_layers|poses]","equipment <family|fortress1|fortress2|fortress3|primal3|primal4> [color=RRGGBB] [mask=0..2] [goggles] [upgrades=5,2,1] [eye] [pearl]","craft","infusion <recipe-id> (hold the actual center item)","mask","research <key|all|book>","probe","reset"));
            else if(action.equals("probe"))details=snapshot(player);
            else if(action.equals("reset"))details=reset(player);
            else{
                var session=SESSIONS.computeIfAbsent(player.getUUID(),ignored->new Session(player));
                if(session.level!=player.serverLevel())throw new IllegalStateException("Return to fixture dimension or reset before preparing another scene");
                details=switch(action){
                    case "catalog"->catalog(session);
                    case "gallery"->gallery(session,tokens);
                    case "equipment"->equipment(player,tokens);
                    case "craft"->craft(player,session);
                    case "infusion"->infusion(player,session,tokens);
                    case "mask"->mask(player,session);
                    case "research"->research(player,tokens);
                    default->throw new IllegalArgumentException("Unknown scene action: "+action);
                };
            }
            player.inventoryMenu.broadcastChanges();player.containerMenu.broadcastChanges();
            var result=new LinkedHashMap<String,Object>();result.put("before",before);result.put("setup",details);result.put("after",snapshot(player));
            DyesAuditHarness.record("server",id,operation,true,result);String finished=id;source.sendSuccess(()->Component.literal("DYES_AUDIT setup/observation recorded: "+finished),false);return 1;
        }catch(Exception error){DyesAuditHarness.LOG.error("DYES_AUDIT scene failed {}",operation,error);DyesAuditHarness.record("server",id,operation,false,DyesAuditHarness.error(error));source.sendFailure(Component.literal(error.toString()));return 0;}
    }
    private static Map<String,Object> snapshot(ServerPlayer player){
        var out=new LinkedHashMap<String,Object>(DyesAuditHarness.entity(player));out.put("worldTick",player.serverLevel().getGameTime());out.put("dimension",player.level().dimension().location().toString());
        out.put("gameMode",player.gameMode.getGameModeForPlayer().getName());out.put("selectedSlot",player.getInventory().selected);
        out.put("inventory",player.getInventory().save(new ListTag()).toString());out.put("menu",player.containerMenu.getClass().getName());
        var slots=new ArrayList<Object>();for(var slot:player.containerMenu.slots)slots.add(Map.of("index",slot.index,"stack",DyesAuditHarness.stack(slot.getItem())));out.put("menuSlots",slots);
        out.put("research",KnowledgeSavedData.get(player.server).snapshot(player.getUUID()).map(k->k.researchMarkers().stream().sorted().toList()).orElse(List.of()));
        var session=SESSIONS.get(player.getUUID());if(session!=null){out.put("anchor",position(session.anchor));var blocks=new ArrayList<Object>();for(var pos:session.blocks.keySet()){var tile=session.level.getBlockEntity(pos);var row=new LinkedHashMap<String,Object>();row.put("pos",position(pos));row.put("state",session.level.getBlockState(pos).toString());if(tile!=null)row.put("blockEntity",tile.saveWithFullMetadata().toString());blocks.add(row);}out.put("fixtureBlocks",blocks);}
        return out;
    }
    private static Map<String,Object> reset(ServerPlayer player){
        var session=SESSIONS.get(player.getUUID());if(session==null)throw new IllegalStateException("No in-memory scene snapshot for this player");
        player.closeContainer();for(var uuid:session.entities){var entity=session.level.getEntity(uuid);if(entity!=null)entity.discard();}
        var saved=new ArrayList<>(session.blocks.values());Collections.reverse(saved);for(var block:saved)if(!block.restore(true,false))throw new IllegalStateException("Failed to restore "+block.getPos());
        player.getInventory().load(session.inventory.copy());var knowledge=KnowledgeSavedData.get(player.server);
        if(!knowledge.replace(player.getUUID(),knowledge.revision(player.getUUID()),session.knowledge))throw new IllegalStateException("Knowledge restore revision conflict");
        KnowledgeSync.flushBeforeScreen(player);SESSIONS.remove(player.getUUID());return Map.of("restoredBlocks",saved.size(),"removedFixtureEntities",session.entities.size(),"restoredInventoryAndKnowledge",true);
    }
    private static List<Integer> position(BlockPos pos){return List.of(pos.getX(),pos.getY(),pos.getZ());}
    private static Map<String,List<ItemStack>> families(){
        var out=new LinkedHashMap<String,List<ItemStack>>();for(var entry:DyesItems.ALL.values())if(entry.get() instanceof DyesArmorItem item)out.computeIfAbsent(item.family,ignored->new ArrayList<>()).add(new ItemStack(item));return out;
    }
    private static void chest(Session session,BlockPos pos,List<ItemStack> items){
        if(items.size()>27)throw new IllegalArgumentException("Too many chest fixture items");session.floor(pos);session.block(pos,Blocks.CHEST.defaultBlockState());
        var chest=(ChestBlockEntity)session.level.getBlockEntity(pos);chest.clearContent();for(int i=0;i<items.size();i++)chest.setItem(i,items.get(i).copy());chest.setChanged();
    }
    private static Map<String,Object> catalog(Session session){
        var items=DyesItems.ALL.values().stream().map(v->new ItemStack(v.get())).toList();var locations=new ArrayList<Object>();
        for(int i=0;i<items.size();i+=27){var pos=session.anchor.offset((i/27)*2,0,0);chest(session,pos,items.subList(i,Math.min(i+27,items.size())));locations.add(Map.of("position",position(pos),"from",i,"to",Math.min(i+27,items.size())-1));}
        return Map.of("items",items.size(),"chests",locations,"instruction","Inspect actual chest slots and tooltips with MC Pilot; no recipe was executed.");
    }
    private static Map<String,Object> gallery(Session session,List<String> args){
        String mode=args.isEmpty()?"armor_gallery":args.get(0);if(!Set.of("armor_gallery","dye_layers","poses").contains(mode))throw new IllegalArgumentException("Unknown gallery mode "+mode);var rows=new ArrayList<Object>();int index=0;
        for(var family:families().entrySet()){
            if(mode.equals("dye_layers")&&!((DyesArmorItem)family.getValue().get(0).getItem()).dyeable())continue;
            int variants=mode.equals("dye_layers")?4:mode.equals("poses")?2:1;
            for(int variant=0;variant<variants;variant++){
                var pos=session.anchor.offset(index%8*3,0,4+index/8*4);session.floor(pos);LivingEntity wearer;
                if(mode.equals("poses")){var zombie=EntityType.HUSK.create(session.level);zombie.setNoAi(true);zombie.setBaby(variant==1);zombie.setPersistenceRequired();wearer=zombie;}
                else{var stand=new ArmorStand(session.level,pos.getX()+.5,pos.getY(),pos.getZ()+.5);stand.setNoGravity(true);wearer=stand;}
                wearer.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,180,0);wearer.setInvulnerable(true);wearer.setCustomNameVisible(true);wearer.setCustomName(Component.literal(index+" "+family.getKey()+" / "+variant));
                for(var original:family.getValue()){var item=original.copy();if(mode.equals("dye_layers")&&variant>0)((DyedArmorItem)item.getItem()).setColor(item,new int[]{0,0xffffff,0,0xff00ff}[variant]);wearer.setItemSlot(((ArmorItem)item.getItem()).getEquipmentSlot(),item);}
                session.entity(wearer);rows.add(DyesAuditHarness.entity(wearer));index++;
            }
        }
        return Map.of("mode",mode,"entities",rows,"instruction","Gallery covers real armor layers. Walking, crouching and bow poses require actual player input using the equipment command.");
    }
    private static Map<String,Object> equipment(ServerPlayer player,List<String> args){
        if(args.isEmpty())throw new IllegalArgumentException("equipment requires family or preset");String family=args.get(0);int pieces=4;
        if(family.startsWith("fortress")&&family.length()==9){pieces=Integer.parseInt(family.substring(8));family="ThaumiumFortressDyed";}
        else if(family.equals("primal3")||family.equals("primal4")){pieces=Integer.parseInt(family.substring(6));family="ItemRunicArmorPrimal";}
        var items=new ArrayList<>(families().getOrDefault(family,List.of()));if(family.equals("ItemRunicArmorPrimal")&&pieces==4)items.addAll(families().get("ItemRunicArmorPrimalBoots"));
        if(items.isEmpty())throw new IllegalArgumentException("Unknown family: "+family);
        for(var slot:DyesAuditHarness.ARMOR)player.setItemSlot(slot,ItemStack.EMPTY);int equipped=0;
        for(var stack:items){if(equipped++>=pieces)break;for(String option:args.subList(1,args.size())){
            if(option.startsWith("color=")){if(!(stack.getItem() instanceof DyedArmorItem dyed))throw new IllegalArgumentException("Family cannot be dyed");dyed.setColor(stack,Integer.parseInt(option.substring(6),16));}
            else if(option.startsWith("mask="))stack.getOrCreateTag().putInt("mask",Integer.parseInt(option.substring(5)));
            else if(option.equals("goggles"))stack.getOrCreateTag().putBoolean("goggles",true);
            else if(option.startsWith("upgrades=")){String[] upgrades=option.substring(9).split(",");if(upgrades.length>3)throw new IllegalArgumentException("At most three upgrade slots");for(int n=0;n<upgrades.length;n++)stack.getOrCreateTag().putByte(n==0?"upgrade":"upgrade"+(n+1),Byte.parseByte(upgrades[n]));}
            else if(!option.equals("eye")&&!option.equals("pearl"))throw new IllegalArgumentException("Unknown equipment option "+option);
        }player.setItemSlot(((ArmorItem)stack.getItem()).getEquipmentSlot(),stack);}
        for(String key:List.of("eldritch_eye","primordial_pearl")){var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(ResourceLocation.parse("thaumcraft:"+key));if(item==null)throw new IllegalStateException("Missing "+key);for(int i=0;i<player.getInventory().items.size();i++)if(player.getInventory().getItem(i).is(item))player.getInventory().setItem(i,ItemStack.EMPTY);if(args.contains(key.equals("eldritch_eye")?"eye":"pearl"))give(player,new ItemStack(item));}
        give(player,new ItemStack(Items.BOW));give(player,new ItemStack(Items.ARROW,32));return Map.of("family",family,"pieces",pieces,"options",args,"fixtureOnly",true,"instruction","NBT variants are visual fixtures, not evidence of dyeing or infusion gameplay.");
    }
    private static void give(ServerPlayer player,ItemStack stack){if(!player.getInventory().add(stack))throw new IllegalStateException("Fixture inventory full; reset or free slots before requesting materials");}
    private static Map<String,Object> craft(ServerPlayer player,Session session){
        var table=session.anchor.offset(0,0,6);session.floor(table);session.block(table,Blocks.CRAFTING_TABLE.defaultBlockState());
        var water=table.east(2);session.floor(water);session.block(water,Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL,3));
        var arcane=table.east(4);session.floor(arcane);session.block(arcane,TCArcane.BLOCK.get().defaultBlockState());
        var materials=new ArrayList<ItemStack>();for(var entry:DyesItems.ALL.values())if(entry.get() instanceof DyedArmorItem)materials.add(new ItemStack(entry.get()));chest(session,table.south(3),materials);
        var dyes=List.of(new ItemStack(Items.WHITE_DYE,32),new ItemStack(Items.BLACK_DYE,32),new ItemStack(Items.MAGENTA_DYE,32),new ItemStack(Items.RED_DYE,32),new ItemStack(Items.BLUE_DYE,32),new ItemStack(Items.WATER_BUCKET));chest(session,table.south(3).east(2),dyes);
        give(player,TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true));give(player,new ItemStack(TCItems.THAUMONOMICON.get()));
        return Map.of("craftingTable",position(table),"washCauldron",position(water),"arcaneWorkbench",position(arcane),"armorInputs",position(table.south(3)),"dyes",position(table.south(3).east(2)),"instruction","Use the actual crafting screen and cauldron interaction. Call probe before and after; helper has not dyed or washed any input.");
    }
    private static Map<String,Object> infusion(ServerPlayer player,Session session,List<String> args){
        if(args.size()!=1)throw new IllegalArgumentException("infusion requires a recipe ID and the actual center held in main hand");var id=ResourceLocation.parse(args.get(0));
        var recipe=player.level().getRecipeManager().byKey(id).orElseThrow();if(!(recipe instanceof InfusionRecipe infusion))throw new IllegalArgumentException("Not an infusion recipe: "+id);
        var center=player.getMainHandItem().copy();if(!infusion.acceptsCenter(center))throw new IllegalArgumentException("Held center does not satisfy recipe");var plan=infusion.planForCraft(center,player.level());
        var base=session.anchor.offset(12,0,10);session.floor(base);session.block(base,TCArcane.ARCANE_PEDESTAL.get().defaultBlockState());session.block(base.above(),Blocks.AIR.defaultBlockState());session.block(base.above(2),TCArcane.INFUSION_MATRIX.get().defaultBlockState());
        for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){var pos=base.offset(x,0,z);session.floor(pos);session.block(pos,TCArcane.INFUSION_PILLAR.get().defaultBlockState());session.block(pos.above(),TCArcane.INFUSION_PILLAR_TOP.get().defaultBlockState());}
        if(plan.components().size()>24)throw new IllegalArgumentException("Scene supports up to 24 components");
        var ring=new ArrayList<BlockPos>();for(int x=-3;x<=3;x++)ring.add(base.offset(x,0,-3));for(int z=-2;z<=3;z++)ring.add(base.offset(3,0,z));for(int x=2;x>=-3;x--)ring.add(base.offset(x,0,3));for(int z=2;z>=-2;z--)ring.add(base.offset(-3,0,z));
        var components=new ArrayList<ItemStack>();var pedestals=new ArrayList<Object>();int n=0;for(var ingredient:plan.components()){
            var choices=ingredient.displayStacks();if(choices.isEmpty())throw new IllegalStateException("Recipe component has no real display stack");components.add(choices.get(0).copy());
            var pos=ring.get(n++);session.floor(pos);session.block(pos,TCArcane.ARCANE_PEDESTAL.get().defaultBlockState());pedestals.add(position(pos));
        }
        chest(session,base.west(5),components);
        var jars=new ArrayList<Object>();n=0;for(var amount:plan.aspects().view().entrySet()){int left=amount.getValue();while(left>0){if(n>=32)throw new IllegalArgumentException("Scene supports 32 jars");var pos=base.offset(n%8-4,0,5+n/8);session.floor(pos);session.block(pos,TCArcane.ESSENTIA_JAR.get().defaultBlockState());var jar=(EssentiaJarBlockEntity)session.level.getBlockEntity(pos);jar.empty();int count=Math.min(64,left);if(jar.addToContainer(amount.getKey(),count)!=0)throw new IllegalStateException("Fixture jar rejected essentia");jars.add(Map.of("position",position(pos),"aspect",amount.getKey().tag(),"amount",count));left-=count;n++;}}
        give(player,TCItems.CASTING_WAND.get().create(WandParts.Rod.SILVERWOOD,WandParts.Cap.GOLD,false,true));
        return Map.of("recipe",id.toString(),"researchRequired",infusion.research(),"center",DyesAuditHarness.stack(center),"altarCenter",position(base),"matrix",position(base.above(2)),"componentChest",position(base.west(5)),"pedestals",pedestals,"jars",jars,"instruction","Place held center and supplied raw components yourself, then use the real wand interaction. Research is not auto-granted; no product recipe method was used to generate an output.");
    }
    private static Map<String,Object> mask(ServerPlayer player,Session session){
        player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(DyesItems.get("itemHelmetMaskTX")));var rows=new ArrayList<Object>();
        for(int i=0;i<3;i++){var zombie=EntityType.HUSK.create(session.level);zombie.setNoAi(true);zombie.setPersistenceRequired();zombie.setPos(player.getX()+(i-1)*3,player.getY(),player.getZ()+5);zombie.setHealth(zombie.getMaxHealth());session.entity(zombie);rows.add(DyesAuditHarness.entity(zombie));}
        return Map.of("targets",rows,"instruction","Aim, lose line of sight, and observe five-tick gaze plus twenty-tick cone effects through actual server/client ticks. Targets are not invulnerable.");
    }
    private static Map<String,Object> research(ServerPlayer player,List<String> args){
        if(args.size()!=1)throw new IllegalArgumentException("research requires key, all, or book");String key=args.get(0);var granted=new ArrayList<String>();
        if(key.equals("all")){for(var entry:ResearchCatalogs.catalog().all())if(entry.key().startsWith("thaumicdyes:")){thaumcraft.api.ThaumcraftApiHelper.completeResearch(player,entry.key());granted.add(entry.key());}}
        else if(!key.equals("book")){if(!key.startsWith("thaumicdyes:"))throw new IllegalArgumentException("Only Dyes research fixtures are permitted");ResearchCatalogs.catalog().require(key);thaumcraft.api.ThaumcraftApiHelper.completeResearch(player,key);granted.add(key);}
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(TCItems.THAUMONOMICON.get()));
        player.getMainHandItem().getItem().use(player.level(),player,InteractionHand.MAIN_HAND);
        return Map.of("grantedFixtureResearch",granted,"openedThrough","ThaumonomiconItem.use -> ResearchBookOpening -> BookOpenPacket","instruction","This grants fixture knowledge for page inspection, not evidence of research acquisition.");
    }
    private DyesAuditScenes(){}
}
