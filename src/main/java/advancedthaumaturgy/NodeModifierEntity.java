package advancedthaumaturgy;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.network.chat.Component;
import tc4revival.content.*;
import tc4revival.aspect.*;

public class NodeModifierEntity extends SyncedEntity implements MenuProvider, RevealedAspectContainer {
    private final EssentiaAccess supply=new EssentiaAccess();
    private NodeOperation operation;
    private int remainingEssentia,remainingEssences,ticks;
    private String selected="";
    private CompoundTag target=new CompoundTag();
    public NodeModifierEntity(BlockPos pos,BlockState state){super(AdvancedThaumaturgy.NODE_ENTITY.get(),pos,state);}
    public boolean active(){return operation!=null;}
    public NodeOperation operation(){return operation;}
    public int remainingEssentia(){return remainingEssentia;}
    public int remainingEssences(){return remainingEssences;}
    public NodeJarBlockEntity jar(){return level!=null&&level.getBlockEntity(worldPosition.above()) instanceof NodeJarBlockEntity j?j:null;}
    public List<NodeOperation> available(){
        var jar=jar();if(jar==null)return List.of();
        return Arrays.stream(NodeOperation.values()).filter(o->o.available(jar.nodeType().name(),jar.nodeModifier()==null?"":jar.nodeModifier().name(),jar.base().size()))
            .filter(o->o!=NodeOperation.INCREASE_ASPECT||jar.base().amount(jar.base().aspects().get(0))<1000000).toList();
    }
    private List<ArcanePedestalBlockEntity> pedestals(){
        var list=new ArrayList<ArcanePedestalBlockEntity>();
        for(int x=-4;x<4;x++)for(int z=-4;z<4;z++) {
            var p=worldPosition.offset(x,0,z);if(level.hasChunkAt(p)&&level.getBlockEntity(p) instanceof ArcanePedestalBlockEntity pedestal)list.add(pedestal);
        }
        return list;
    }
    private static String essence(ItemStack stack){
        if(!(stack.getItem() instanceof WispEssenceItem item))return null;
        var aspects=item.storedAspects(stack);return aspects.size()==1?aspects.aspects().get(0).tag():null;
    }
    public boolean start(NodeOperation op,Player player){
        if(!(level instanceof ServerLevel)||active()||!ATConfig.enabled("node_modifier")||!available().contains(op))return false;
        if(player instanceof net.minecraft.server.level.ServerPlayer server&&!player.getAbilities().instabuild&&!thaumcraft.api.ThaumcraftApiHelper.isResearchComplete(server,"advancedthaumaturgy:NODEMODIFIER"))return false;
        var pedestals=pedestals();String selected=op.essence;
        if(op==NodeOperation.ADD_ASPECT){
            var values=new HashSet<String>();for(var p:pedestals){String tag=essence(p.getItem(0));if(tag!=null)values.add(tag);}
            if(values.size()!=1)return false;
            selected=values.iterator().next();
            if(jar().base().amount(TCAspects.catalog().require(selected))>0)return false;
        }
        final String expected=selected;
        long count=pedestals.stream().filter(p->{String a=essence(p.getItem(0));return a!=null&&(expected.isEmpty()||expected.equals(a));}).count();
        if(count<op.essences)return false;
        operation=op;this.selected=selected;remainingEssentia=op.amount;remainingEssences=op.essences;ticks=0;target=jar().contents().write();sync();
        level.playSound(null,worldPosition,TCSounds.CRAFT_START.get(),net.minecraft.sounds.SoundSource.BLOCKS,.5f,1);
        return true;
    }
    public void cancel(){operation=null;remainingEssentia=remainingEssences=ticks=0;selected="";target=new CompoundTag();sync();}
    public void tick(){
        if(!(level instanceof ServerLevel server)||!active())return;
        var jar=jar();if(jar==null||!jar.contents().write().equals(target)||!ATConfig.enabled("node_modifier")){cancel();return;}
        ++ticks;
        if(ticks%20==0)server.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,worldPosition.getX()+.5,worldPosition.getY()+1.3,worldPosition.getZ()+.5,4,.3,.2,.3,.02);
        if(remainingEssentia>0){
            if(ticks%20==0&&supply.take(server,worldPosition,TCAspects.catalog().require(operation.essentia),16,8)>0){remainingEssentia--;sync();if(remainingEssentia==0)ticks=0;}
            return;
        }
        if(remainingEssences>0){
            if(ticks<200)return;
            for(var pedestal:pedestals()){
                String tag=essence(pedestal.getItem(0));if(tag==null||!selected.isEmpty()&&!selected.equals(tag))continue;
                pedestal.removeItem(0,1);remainingEssences--;ticks=0;sync();
                tc4revival.network.FocusEffectPacket.nodeZap(level,pedestal.getBlockPos().getCenter(),worldPosition.above().getCenter());break;
            }
            return;
        }
        var data=jar.contents();var base=data.capacity();var charge=data.aspects();var type=data.type();var quality=data.modifier();
        switch(operation){
            case REMOVE_UNSTABLE,REMOVE_SINISTER,REMOVE_TAINTED,REMOVE_HUNGRY->type=AuraNodeType.NORMAL;
            case ADD_HUNGRY->type=AuraNodeType.HUNGRY;case ADD_TAINTED->type=AuraNodeType.TAINTED;
            case ADD_SINISTER->type=AuraNodeType.DARK;case ADD_PURE->type=AuraNodeType.PURE;
            case ADD_BRIGHT->quality=AuraNodeModifier.BRIGHT;case REMOVE_PALE->quality=null;case REMOVE_FADING->quality=AuraNodeModifier.PALE;
            case ADD_ASPECT->{var a=TCAspects.catalog().require(selected);base.add(a,1);charge.add(a,1);}
            case INCREASE_ASPECT->{var a=base.aspects().get(0);if(base.amount(a)<1000000)base.add(a,1);}
        }
        jar.configure(new NodeJarData(charge,type,quality,data.id(),base));
        level.playSound(null,worldPosition,TCSounds.ZAP.get(),net.minecraft.sounds.SoundSource.BLOCKS,.3f,1);cancel();
    }
    @Override public AspectAmounts revealedAspects(){return operation==null?new AspectAmounts():new AspectAmounts().add(TCAspects.catalog().require(operation.essentia),remainingEssentia);}
    @Override protected void saveAdditional(CompoundTag nbt){super.saveAdditional(nbt);nbt.putInt("operation",operation==null?-1:operation.ordinal());nbt.putInt("remainingEssentia",remainingEssentia);nbt.putInt("remainingEssences",remainingEssences);nbt.putInt("ticks",ticks);nbt.putString("selected",selected);nbt.put("target",target.copy());}
    @Override public void load(CompoundTag nbt){super.load(nbt);operation=NodeOperation.byId(nbt.contains("operation")?nbt.getInt("operation"):-1).orElse(null);remainingEssentia=Math.max(0,nbt.getInt("remainingEssentia"));remainingEssences=Math.max(0,nbt.getInt("remainingEssences"));ticks=Math.max(0,nbt.getInt("ticks"));selected=nbt.getString("selected");target=nbt.getCompound("target").copy();}
    @Override public Component getDisplayName(){return Component.translatable("block.advancedthaumaturgy.node_modifier");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new NodeModifierMenu(id,inventory,this);}
}
