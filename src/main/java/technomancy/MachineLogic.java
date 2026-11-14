package technomancy;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thaumcraft.api.aspects.EssentiaStorageApi;
import thaumcraft.api.nodes.NodeApi;

public final class MachineLogic {
    public static Aspect aspect(String name){return name.isEmpty()?null:TCAspects.catalog().find(name).orElse(null);}
    public static BlockEntity loaded(MachineEntity m,BlockPos p){return m.getLevel().hasChunkAt(p)?m.getLevel().getBlockEntity(p):null;}
    public static void tick(MachineEntity m){
        if(m.kind==MachineKind.ESSENTIA_FUSOR){fusion(m);return;}
        if(m.kind.essentiaCapacity>0&&m.kind!=MachineKind.ELDRITCH_CONSUMER&&m.kind!=MachineKind.ENERGY_CONDENSER&&m.kind!=MachineKind.CREATIVE_JAR)pullEssentia(m);
        if(m.kind.dynamo()){dynamo(m);return;}
        if(m.kind.processor()){OreProcessing.tick(m);return;}
        switch(m.kind){
            case ENERGY_CONDENSER -> {if(m.runs()&&m.amount()<64&&m.pay(TechConfig.CONDENSER.get())){m.aspects.add(aspect("potentia"),1);m.changed();}}
            case NODE_FABRICATOR -> nodes(m);
            case ORDERLY_LAMP -> lamp(m);
            case ECOLOGIC_TRANSMUTER -> {if(m.runs()&&m.energy>=TechConfig.BIOME.get()){biome(m);biome(m);m.pay(TechConfig.BIOME.get());}}
            case GALVANIZED_BELLOWS -> BellowsLogic.tick(m);
            case ADVANCED_DECONSTRUCTION_TABLE -> deconstruct(m);
            case ESSENTIA_COIL,ITEM_COIL -> CoilLogic.tick(m);
            case ELDRITCH_CONSUMER -> ConsumerLogic.tick(m);
            case BLOOD_FABRICATOR,MANA_FABRICATOR,MANA_EXCHANGER -> technomancy.compat.MagicIntegration.tick(m);
            default -> { }
        }
    }
    public static void pullEssentia(MachineEntity m){
        for(var side:Direction.values()){
            if(!m.canInput(side)||m.amount()>=m.capacity())continue;
            if(loaded(m,m.getBlockPos().relative(side)) instanceof EssentiaTransport source&&source.canOutput(side.getOpposite())){
                var a=source.essentiaType(side.getOpposite());
                if(a==null||!m.accepts(a)||source.suction(side.getOpposite())>=m.suction(side)||source.minimumSuction()>m.suction(side)||m.insert(a,1,true)<1)continue;
                int taken=source.takeEssentia(a,1,side.getOpposite());
                if(taken>0)m.insert(a,taken,false);
            }
        }
    }
    private static void dynamo(MachineEntity m){
        if(m.kind==MachineKind.NODE_DYNAMO&&m.ticks%21==0&&m.state.getInt("nodeFuel")<16){
            var candidates=new ArrayList<AuraNodeBlockEntity>();
            for(var pos:BlockPos.betweenClosed(m.getBlockPos().offset(-4,-4,-4),m.getBlockPos().offset(4,4,4)))if(loaded(m,pos) instanceof AuraNodeBlockEntity n)candidates.add(n);
            Collections.shuffle(candidates,new Random(m.server().random.nextLong()));
            outer:for(var node:candidates)for(var entry:NodeApi.snapshot(node).charge().unwrap().view().entrySet())if(entry.getValue()>1){
                NodeApi.extract(node,thaumcraft.api.aspects.Aspect.wrap(entry.getKey()),1,false);
                m.state.putInt("nodeFuel",m.state.getInt("nodeFuel")+1);m.changed();break outer;
            }
        }
        if(m.kind==MachineKind.HIPPIE_DYNAMO)technomancy.compat.MagicIntegration.pullMana(m,100);
        int rate=m.boost?320:80;
        if(m.runs()&&m.energy<=40000-rate){
            if(m.fuel<32){
                int multiplier=m.boost?4:1;
                if(m.kind==MachineKind.ESSENTIA_DYNAMO&&m.amount()>=multiplier){
                    var a=m.aspect();m.extract(a,multiplier,false);m.fuel+=dynamoFuel(a,m);
                }else if(m.kind==MachineKind.NODE_DYNAMO&&m.state.getInt("nodeFuel")>=multiplier){
                    m.state.putInt("nodeFuel",m.state.getInt("nodeFuel")-multiplier);m.fuel+=30;
                }else if(m.kind==MachineKind.HIPPIE_DYNAMO&&m.mana>=20*multiplier){m.mana-=20*multiplier;m.fuel+=160;}
                else if(m.kind==MachineKind.BLOOD_DYNAMO&&m.fluid.getFluidAmount()>=100*multiplier){m.fluid.drain(100*multiplier,FluidAction.EXECUTE);m.fuel+=400;}
            }
            if(m.fuel>0){m.energy+=rate;m.fuel--;m.active=true;m.changed();}else m.active=false;
        }else m.active=false;
        var target=loaded(m,m.getBlockPos().relative(m.facing()));
        if(target!=null&&m.energy>0)target.getCapability(ForgeCapabilities.ENERGY,m.facing().getOpposite()).ifPresent(e->{int moved=e.receiveEnergy(Math.min(320,m.energy),false);if(moved>0){m.energy-=moved;m.changed();}});
    }
    public static int dynamoFuel(Aspect a,MachineEntity m){
        var level=m.server();var biome=level.getBiome(m.getBlockPos());String id=biome.unwrapKey().map(k->k.location().toString()).orElse("");
        return switch(a.tag()){
            case "ignis","potentia"->800;
            case "aqua"->biome.value().getModifiedClimateSettings().downfall()>.85f?200:50;
            case "praecantatio","alienis"->id.equals("minecraft:the_end")||id.equals("thaumcraft:magical_forest")?300:75;
            case "aer","ordo","machina","metallum","motus","instrumentum","vinculum","perfodio","fabrico","iter","vitreus","perditio"->75;
            case "humanus","sensus","sano","messis","fames","mortuus","bestia","venenum","cognitio","spiritus","telum","tempestas"->30;
            case "arbor","herba","meto","pannus","corpus"->40;
            case "vacuos"->m.getBlockPos().getY()<60?200:50;
            case "exanimis"->id.equals("minecraft:the_end")?100:25;
            case "permutatio"->level.random.nextInt(1200);
            case "volatus"->m.getBlockPos().getY()>150?200:50;
            case "limus"->net.minecraft.world.level.levelgen.WorldgenRandom.seedSlimeChunk(m.getBlockPos().getX()>>4,m.getBlockPos().getZ()>>4,level.getSeed(),987234911L).nextInt(10)==0?200:100;
            case "lux"->level.isDay()?300:50;
            case "tenebrae"->!level.isDay()?300:50;
            case "auram"->id.equals("thaumcraft:magical_forest")?600:100;
            case "vitium"->id.equals("thaumcraft:tainted_land")?600:100;
            default->25;
        };
    }
    public static boolean validFluid(MachineEntity m,FluidStack f){
        String id=String.valueOf(ForgeRegistries.FLUIDS.getKey(f.getFluid()));
        return switch(m.kind){
            case BLOOD_DYNAMO,BLOOD_FABRICATOR->id.equals("bloodmagic:life_essence_fluid");
            case ORDERLY_LAMP->id.equals("thaumcraft:flux_goo");
            case MANA_EXCHANGER->id.equals("technomancy:mana_condensate");
            default->false;
        };
    }
    private static void lamp(MachineEntity m){
        if(m.ticks%10!=0)return;
        var previous=m.state.contains("matrix")?BlockPos.of(m.state.getLong("matrix")):null;
        InfusionMatrixBlockEntity matrix=previous==null?null:loaded(m,previous) instanceof InfusionMatrixBlockEntity i?i:null;
        if(matrix==null)for(var p:BlockPos.betweenClosed(m.getBlockPos().offset(-10,-5,-10),m.getBlockPos().offset(9,4,9)))if(loaded(m,p) instanceof InfusionMatrixBlockEntity i){matrix=i;m.state.putLong("matrix",p.asLong());break;}
        if(matrix==null)return;
        if(!matrix.crafting()){if(m.state.getBoolean("stabilized")){m.state.putBoolean("stabilized",false);m.changed();}return;}
        if(m.state.getBoolean("stabilized"))return;
        int n=Math.min(matrix.instability(),Math.min(5,Math.min(m.aspects.amount(aspect("ordo"))/5,(1000-m.fluid.getFluidAmount())/200)));
        var goo=ForgeRegistries.FLUIDS.getValue(new ResourceLocation("thaumcraft","flux_goo"));
        if(n>0&&goo!=null&&goo!=net.minecraft.world.level.material.Fluids.EMPTY){
            m.extract(aspect("ordo"),n*5,false);matrix.addInstability(-n);m.fluid.fill(new FluidStack(goo,200*n),FluidAction.EXECUTE);
            m.state.putBoolean("stabilized",true);m.changed();
        }
    }
    private static void biome(MachineEntity m){
        int x=m.getBlockPos().getX()+m.server().random.nextInt(100)-m.server().random.nextInt(100),z=m.getBlockPos().getZ()+m.server().random.nextInt(100)-m.server().random.nextInt(100);
        var p=new BlockPos(x,m.getBlockPos().getY(),z);if(!m.server().hasChunkAt(p))return;
        var target=m.server().registryAccess().registryOrThrow(Registries.BIOME).getHolder(ResourceKey.create(Registries.BIOME,new ResourceLocation("thaumcraft",new String[]{"magical_forest","eerie","tainted_land"}[m.mode%3])));
        if(target.isEmpty())return;
        WorldBiomeChanges.changeColumns(m.server(),List.of(p),target.get().unwrapKey().orElseThrow());
    }
    private static void deconstruct(MachineEntity m){
        if(m.owner==null||m.server().getServer().getPlayerList().getPlayer(m.owner)==null)return;
        var stack=m.inventory.getStackInSlot(0);var values=tc4revival.aspect.item.ItemStackAspects.resolve(stack).orElseGet(AspectAmounts::new);
        if(values.isEmpty()){m.progress=0;return;}
        m.progress++;m.changed();if(m.progress<(m.boost?40:80))return;m.progress=0;
        var primals=TCAspects.catalog().toPrimals(values,AspectCatalog.Reduction.SUM);m.inventory.extractItem(0,1,false);
        if(!primals.isEmpty()&&m.server().random.nextInt(80)<primals.total()&&m.server().random.nextInt(8)==0){
            var a=primals.aspects().get(m.server().random.nextInt(primals.size()));
            tc4revival.research.KnowledgeSavedData.get(m.server().getServer()).update(m.owner,m.server().random::nextInt,k->{if(k.poolAmount(a)<32767)k.changePool(a,1);return null;});
        }
    }
    public static boolean interact(MachineEntity m,Player player,InteractionHand hand,Direction side){
        var held=player.getItemInHand(hand);
        if(held.isEmpty()&&player.isShiftKeyDown()&&m.boost&&TechItem.upgradeable(m.kind)) {
            if(!m.getLevel().isClientSide){setBoost(m,false,player);returnItem(player,new ItemStack(TechRegistry.item("potency_gem")));}return true;
        }
        if(held.is(Items.REDSTONE_TORCH)||held.is(Items.GUNPOWDER)||held.is(Items.REDSTONE)){
            if(m.boost&&(m.kind==MachineKind.ITEM_COIL||m.kind==MachineKind.ESSENTIA_COIL))return true;
            int setting=held.is(Items.REDSTONE_TORCH)?2:held.is(Items.REDSTONE)?1:0;
            if(!m.getLevel().isClientSide&&m.redstone!=setting){
                refundRedstone(m,player);m.redstone=setting;
                if(!player.isCreative()){held.shrink(1);m.state.putInt("redstoneItem",setting+1);}m.changed();
            }return true;
        }
        if(m.kind==MachineKind.ITEM_COIL&&(player.isShiftKeyDown()||!held.isEmpty())){
            if(!m.getLevel().isClientSide){if(player.isShiftKeyDown())m.state.remove("itemFilter");else m.state.put("itemFilter",held.copyWithCount(1).save(new CompoundTag()));m.changed();}return true;
        }
        if((m.kind==MachineKind.QUANTUM_JAR||m.kind==MachineKind.ESSENTIA_COIL)&&held.getItem() instanceof JarLabelItem){
            var stored=((JarLabelItem)held.getItem()).storedAspects(held);var a=stored.aspects().isEmpty()?null:stored.aspects().get(0);
            if(a==null)a=m.aspect();if(a==null)return false;
            if(!m.getLevel().isClientSide){m.filter=a;m.changed();if(!player.isCreative())held.shrink(1);}return true;
        }
        if((m.kind.jar()||m.kind==MachineKind.ESSENTIA_COIL)&&held.isEmpty()&&player.isShiftKeyDown()){
            if(!m.getLevel().isClientSide){m.filter=null;m.changed();}return true;
        }
        if(m.kind==MachineKind.BLOOD_DYNAMO&&new ResourceLocation("bloodmagic","divinationsigil").equals(ForgeRegistries.ITEMS.getKey(held.getItem()))){
            if(!m.getLevel().isClientSide){player.displayClientMessage(net.minecraft.network.chat.Component.translatable("screen.technomancy.energy",m.energy,m.kind.energyCapacity),false);player.displayClientMessage(net.minecraft.network.chat.Component.translatable("screen.technomancy.fluid",m.fluid.getFluidAmount()),false);}return true;
        }
        if(m.kind==MachineKind.CREATIVE_JAR&&player.isCreative()&&held.getItem() instanceof StoredAspectItem item){
            var stored=item.storedAspects(held);if(stored.size()!=1)return false;
            if(!m.getLevel().isClientSide){m.filter=stored.aspects().get(0);m.changed();}return true;
        }
        if(FluidUtil.interactWithFluidHandler(player,hand,m.getLevel(),m.getBlockPos(),side))return true;
        return false;
    }
    private static void returnItem(Player player,ItemStack stack){if(!player.addItem(stack))player.drop(stack,false);}
    private static void refundRedstone(MachineEntity m,Player player){
        int stored=m.state.getInt("redstoneItem");
        if(stored>0){returnItem(player,new ItemStack(stored==1?Items.GUNPOWDER:stored==2?Items.REDSTONE:Items.REDSTONE_TORCH));m.state.remove("redstoneItem");}
    }
    public static void setBoost(MachineEntity m,boolean boost,Player player){
        m.boost=boost;
        if(m.kind==MachineKind.ITEM_COIL||m.kind==MachineKind.ESSENTIA_COIL){
            refundRedstone(m,player);m.redstone=boost?0:2;CoilLogic.signal(m,false);
        }m.changed();
    }
    public static Aspect sideAspect(MachineEntity m,Direction d){return d==null?m.aspect():aspect(m.state.getString("aspect"+d.ordinal()));}
    public static int fusionTransfer(MachineEntity m,Aspect a,int n,boolean simulate,boolean insert){
        int moved=0;
        for(var d:Direction.values())if(m.state.getInt("side"+d.ordinal())==(insert?1:2)&&Objects.equals(a,sideAspect(m,d))){
            int present=m.state.getInt("amount"+d.ordinal()),part=Math.min(n-moved,insert?64-present:present);
            if(part>0&&!simulate){if(insert)sideInsert(m,a,part,d);else sideExtract(m,a,part,d);}moved+=Math.max(0,part);
        }
        return moved;
    }
    public static int sideInsert(MachineEntity m,Aspect a,int n,Direction d){
        if(d==null||n<0||m.state.getInt("side"+d.ordinal())!=1||!Objects.equals(a,sideAspect(m,d)))return 0;
        String key="amount"+d.ordinal();int accepted=Math.min(n,64-m.state.getInt(key));if(accepted>0){m.state.putInt(key,m.state.getInt(key)+accepted);m.aspects.add(a,accepted);m.changed();}return accepted;
    }
    public static int sideExtract(MachineEntity m,Aspect a,int n,Direction d){
        if(d==null||n<0||m.state.getInt("side"+d.ordinal())!=2||!Objects.equals(a,sideAspect(m,d)))return 0;
        String key="amount"+d.ordinal();int taken=Math.min(n,m.state.getInt(key));if(taken>0){m.state.putInt(key,m.state.getInt(key)-taken);m.aspects.remove(a,taken);m.changed();}return taken;
    }
    private static List<Direction> inputSides(MachineEntity m){return Arrays.stream(Direction.values()).filter(d->m.state.getInt("side"+d.ordinal())==1).toList();}
    public static Direction outputSide(MachineEntity m){return Arrays.stream(Direction.values()).filter(d->m.state.getInt("side"+d.ordinal())==2).findFirst().orElse(null);}
    public static boolean fusorInteract(MachineEntity m,Player player,InteractionHand hand,Direction d){
        if(d.getAxis()==Direction.Axis.Y)return false;
        var stack=player.getItemInHand(hand);int type=m.state.getInt("side"+d.ordinal());
        if(stack.isEmpty()&&type!=0){
            var out=outputSide(m);
            if(m.state.getInt("amount"+d.ordinal())!=0||out!=null&&m.state.getInt("amount"+out.ordinal())!=0)return true;
            if(!m.getLevel().isClientSide){
                var stored=ItemStack.of(m.state.getCompound("phial"+d.ordinal()));
                m.state.remove("side"+d.ordinal());m.state.remove("aspect"+d.ordinal());m.state.remove("phial"+d.ordinal());
                if(out!=null)m.state.remove("aspect"+out.ordinal());
                if(!stored.isEmpty())returnItem(player,stored);m.changed();
            }return true;
        }
        if(!(stack.getItem() instanceof EssentiaPhialItem)||type!=0)return false;
        if(!m.getLevel().isClientSide&&markSide(m,stack,d)){
            // The bottle is a real sample, not eight units of freely usable stored essentia.
            m.state.put("phial"+d.ordinal(),stack.copyWithCount(1).save(new CompoundTag()));stack.shrink(1);m.changed();
        }return true;
    }
    private static boolean markSide(MachineEntity m,ItemStack stack,Direction d){
        if(m.state.getInt("side"+d.ordinal())!=0)return false;
        var values=((StoredAspectItem)stack.getItem()).storedAspects(stack);
        if(values.isEmpty()&&outputSide(m)==null){m.state.putInt("side"+d.ordinal(),2);}
        else if(values.size()==1&&inputSides(m).size()<2){
            var a=values.aspects().get(0);var inputs=inputSides(m);
            if(!inputs.isEmpty()&&TCAspects.catalog().combine(a,sideAspect(m,inputs.get(0))).isEmpty())return false;
            m.state.putInt("side"+d.ordinal(),1);m.state.putString("aspect"+d.ordinal(),a.tag());
        }else return false;
        var inputs=inputSides(m);var out=outputSide(m);
        if(inputs.size()==2&&out!=null)TCAspects.catalog().combine(sideAspect(m,inputs.get(0)),sideAspect(m,inputs.get(1))).ifPresent(a->m.state.putString("aspect"+out.ordinal(),a.tag()));
        m.changed();return true;
    }
    private static void fusion(MachineEntity m){
        var inputs=inputSides(m);var output=outputSide(m);if(inputs.size()!=2||output==null||sideAspect(m,output)==null)return;
        for(var d:inputs)if(m.state.getInt("amount"+d.ordinal())<64&&loaded(m,m.getBlockPos().relative(d)) instanceof EssentiaTransport source){
            var a=sideAspect(m,d);if(source.canOutput(d.getOpposite())&&Objects.equals(a,source.essentiaType(d.getOpposite()))&&source.suction(d.getOpposite())<128&&source.minimumSuction()<=128)sideInsert(m,a,source.takeEssentia(a,1,d.getOpposite()),d);
        }
        if(!m.runs()||m.state.getInt("amount"+output.ordinal())>=64||inputs.stream().anyMatch(d->m.state.getInt("amount"+d.ordinal())<1)||!m.pay(TechConfig.FUSOR.get()))return;
        for(var d:inputs){m.state.putInt("amount"+d.ordinal(),m.state.getInt("amount"+d.ordinal())-1);m.aspects.remove(sideAspect(m,d),1);}
        m.state.putInt("amount"+output.ordinal(),m.state.getInt("amount"+output.ordinal())+1);m.aspects.add(sideAspect(m,output),1);m.changed();
    }
    public static void beginNode(MachineEntity m){NodeFabrication.begin(m);}
    private static void nodes(MachineEntity m){NodeFabrication.tick(m);}
    private MachineLogic(){}
}
