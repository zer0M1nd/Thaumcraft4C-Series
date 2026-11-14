package technomancy;

import net.minecraft.core.*;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thaumcraft.api.nodes.NodeApi;
import thaumcraft.api.aspects.AspectList;

/** The two generators face each other six blocks apart. Their midpoint is one block above the bases. */
public final class NodeFabrication {
    public record Parameters(AuraNodeType type,AuraNodeModifier modifier,int strength,int energy){}
    public static Parameters parameters(int aura,int taint){
        int sum=aura+taint;AuraNodeType type=AuraNodeType.NORMAL;AuraNodeModifier modifier=null;
        if(aura==taint&&(sum==122||sum==152||sum==218||sum==510))type=AuraNodeType.PURE;
        else if(sum>256)type=AuraNodeType.HUNGRY;
        else if(aura-64>taint)type=AuraNodeType.UNSTABLE;
        else if(taint-64>aura)type=taint>96?AuraNodeType.TAINTED:AuraNodeType.DARK;
        if(sum<80)modifier=AuraNodeModifier.FADING;
        else if((sum>200&&sum<256)||sum==510)modifier=AuraNodeModifier.BRIGHT;
        else if(sum>350&&sum!=510)modifier=AuraNodeModifier.PALE;
        int strength=sum/2;return new Parameters(type,modifier,strength,(int)Math.round((double)strength*strength*762.939453125));
    }
    private static MachineEntity partner(MachineEntity m){
        if(m.facing().getAxis()==Direction.Axis.Y||!NodeStructure.intact(m))return null;
        var other=MachineLogic.loaded(m,m.getBlockPos().relative(m.facing(),6));
        return other instanceof MachineEntity n&&n.kind==MachineKind.NODE_FABRICATOR&&n.facing()==m.facing().getOpposite()&&NodeStructure.intact(n)?n:null;
    }
    public static BlockPos target(MachineEntity m){return m.getBlockPos().relative(m.facing(),3).above();}
    public static boolean accepts(MachineEntity m,Aspect aspect){
        if(m.getLevel()==null||!m.active||MachineLogic.loaded(m,target(m)) instanceof AuraNodeBlockEntity)return true;
        return aspect.tag().equals(m.facing()==Direction.NORTH||m.facing()==Direction.WEST?"auram":"vitium");
    }
    public static void begin(MachineEntity m){
        var other=partner(m);if(other==null||!m.runs()||!other.runs()||m.state.getBoolean("running")||other.state.getBoolean("running"))return;
        int aura=m.aspects.amount(MachineLogic.aspect("auram"))+other.aspects.amount(MachineLogic.aspect("auram"));
        int taint=m.aspects.amount(MachineLogic.aspect("vitium"))+other.aspects.amount(MachineLogic.aspect("vitium"));
        if(aura+taint<=64||m.energy<parameters(aura,taint).energy()||!m.server().isEmptyBlock(target(m)))return;
        m.state.putBoolean("running",true);m.state.putInt("chargeAura",aura);m.state.putInt("chargeTaint",taint);m.progress=0;m.changed();
    }
    public static void tick(MachineEntity m){
        var other=partner(m);m.active=other!=null;
        if(other==null){if(m.state.getBoolean("running")){m.state.putBoolean("running",false);m.progress=0;m.changed();}return;}
        var p=target(m);var entity=MachineLogic.loaded(m,p);
        if(entity instanceof AuraNodeBlockEntity node){
            if(!m.runs()||m.amount()==0)return;
            var a=m.aspect();var handle=thaumcraft.api.aspects.Aspect.wrap(a);
            if(m.energy>=1000&&NodeApi.insert(node,handle,1,true)==1){m.pay(1000);m.extract(a,1,false);NodeApi.insert(node,handle,1,false);}
            else if(m.boost&&m.energy>=10000&&m.aspects.amount(a)>=10){
                var base=NodeApi.getBase(node);if(base.getAmount(handle)<32767){base.add(handle,1);NodeApi.edit(node,base,node.permanentType(),node.permanentModifier());NodeApi.insert(node,handle,1,false);m.pay(10000);m.extract(a,10,false);}
            }
            return;
        }
        if(!m.state.getBoolean("running")||!m.runs()||!other.runs())return;
        if(!m.server().isEmptyBlock(p)){m.state.putBoolean("running",false);m.progress=0;m.changed();return;}
        m.progress++;m.changed();if(m.progress<200)return;
        int aura=m.state.getInt("chargeAura"),taint=m.state.getInt("chargeTaint");var data=parameters(aura,taint);
        if(m.energy<data.energy()||m.aspects.amount(MachineLogic.aspect("auram"))+other.aspects.amount(MachineLogic.aspect("auram"))<aura||m.aspects.amount(MachineLogic.aspect("vitium"))+other.aspects.amount(MachineLogic.aspect("vitium"))<taint){m.state.putBoolean("running",false);m.progress=0;return;}
        if(!WorldActions.place(m,p,TCArcane.AURA_NODE.get().defaultBlockState())){m.state.putBoolean("running",false);m.progress=0;return;}
        if(m.server().getBlockEntity(p) instanceof AuraNodeBlockEntity node){
            var tags=LegacyBiomeProfiles.get(m.server().getBiome(p)).node().aspectTags();
            String tag=tags.isEmpty()?null:tags.get(m.server().random.nextInt(tags.size()));
            var chosen=tag==null?null:MachineLogic.aspect(tag);
            if(chosen==null)chosen=TCAspects.catalog().primals().get(m.server().random.nextInt(TCAspects.catalog().primals().size()));
            NodeApi.configure(node,AspectList.from(new AspectAmounts().add(chosen,data.strength())),data.type(),data.modifier(),false);
            m.pay(data.energy());consume(m,other,"auram",aura);consume(m,other,"vitium",taint);
        }
        m.state.putBoolean("running",false);m.progress=0;m.changed();
    }
    private static void consume(MachineEntity a,MachineEntity b,String tag,int n){var aspect=MachineLogic.aspect(tag);int first=a.extract(aspect,n,false);b.extract(aspect,n-first,false);}
    private NodeFabrication(){}
}
