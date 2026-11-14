package thaumicexploration;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tc4revival.aspect.*;
import tc4revival.content.*;
import thaumcraft.api.wands.WandRod;

/** The original rod effects use centivis internally (one displayed vis = 100). */
public final class WandBehaviors {
    private WandBehaviors() {}
    public static void register() {
        for(String id:List.of("thaumicexploration:amber","thaumicexploration:amber_staff")) WandRod.registerUpdate(id,WandBehaviors::amber);
        for(String id:List.of("thaumicexploration:transmutative","thaumicexploration:transmutative_staff")) WandRod.registerUpdate(id,WandBehaviors::transmute);
        WandRod.registerUpdate("thaumicexploration:necromancer_staff",WandBehaviors::necro);
        WandRod.registerUpdate("thaumicexploration:bread",(stack,player)->{if(ExplorationConfig.BREAD.get()&&player.getRandom().nextInt(1000)==0)player.displayClientMessage(net.minecraft.network.chat.Component.literal("<").append(stack.getHoverName()).append("> ").append(net.minecraft.network.chat.Component.translatable("thaumicexploration.bread"+player.getRandom().nextInt(5))),false);});
        thaumcraft.api.equipment.BootsBehaviorProviders.registerMovementProvider("thaumicexploration:boots",0,s->s.getItem() instanceof ExplorationBoots,
            (c,current)->!c.flying()&&c.forward()&&!c.grounded()?(c.hovering()?.03F:.05F):current);
    }
    private static void amber(ItemStack stack,Player player) {
        if(player.tickCount%100!=0)return;
        var wand=(CastingWandItem)stack.getItem();
        for(var a:TCAspects.catalog().primals())wand.receiveVis(stack,a,100,false);
    }
    public static int[] transmuteAmounts(int[] amounts,int capacity) {
        int[] result=amounts.clone();int cutoff=capacity/10*9,low=0,excess=0;
        for(int n:amounts)if(n<=cutoff)low++;
        if(low==0)return result;
        for(int i=0;i<amounts.length;i++)if(amounts[i]>cutoff){excess+=amounts[i]-cutoff;result[i]=cutoff;}
        int each=excess<10?0:excess/(low*4);
        for(int i=0;i<amounts.length;i++)if(amounts[i]<=cutoff)result[i]=Math.min(capacity,result[i]+each);
        return result;
    }
    private static void transmute(ItemStack stack,Player player) {
        var wand=(CastingWandItem)stack.getItem();var primals=TCAspects.catalog().primals();
        int[] next=transmuteAmounts(primals.stream().mapToInt(a->wand.vis(stack,a)).toArray(),wand.capacity(stack));
        for(int i=0;i<next.length;i++)stack.getOrCreateTag().putInt(primals.get(i).tag(),next[i]);
    }
    private static void necro(ItemStack stack,Player player) {
        var wand=(CastingWandItem)stack.getItem();var tag=stack.getOrCreateTag();
        // Initialize without penalizing pre-existing charge; thereafter ordinary charging retains 75%.
        var previous=tag.getCompound("txLastVis");var next=new CompoundTag();
        for(var a:TCAspects.catalog().primals()){
            int value=wand.vis(stack,a),last=previous.contains(a.tag())?previous.getInt(a.tag()):value;
            if(value>last)value=last+(value-last)*3/4;
            tag.putInt(a.tag(),value);next.putInt(a.tag(),value);
        }
        // Fix the original comparison against the unregistered WAND_ROD_NECRO. Net orb yield is 4x.
        if(player.takeXpDelay==0)for(var orb:player.level().getEntitiesOfClass(AspectOrbEntity.class,player.getBoundingBox().inflate(3))){
            var a=orb.aspect();if(!orb.isAlive()||orb.age()<1||!a.isPrimal()||wand.receiveVis(stack,a,orb.value()*400,true)<=0)continue;
            wand.receiveVis(stack,a,orb.value()*400,false);next.putInt(a.tag(),wand.vis(stack,a));
            player.takeXpDelay=2;orb.discard();player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,.1F,1);break;
        }
        tag.put("txLastVis",next);
    }
    public static void tickCap(Player player) {
        var stack=player.getMainHandItem();if(!(stack.getItem() instanceof CastingWandItem wand))return;
        if(wand.cap(stack).tag.equals("thaumicexploration:mechanist")&&player.isUsingItem()&&player.getUseItem()==stack){
            WandUseTarget.read(stack.getTag(),player.level().dimension().location().toString()).ifPresent(target->{
                var pos=new BlockPos(target.x(),target.y(),target.z());
                if(player.level().hasChunkAt(pos)&&player.level().getBlockEntity(pos) instanceof AuraNodeBlockEntity node)
                    node.onWandUseTick(stack,player,player.getUseItemRemainingTicks());
            });
        }
        if(!wand.cap(stack).tag.equals("thaumicexploration:sojourner")||player.tickCount%20!=0)return;
        var wanted=TCAspects.catalog().primals().stream().filter(a->wand.vis(stack,a)<wand.capacity(stack)).toList();
        if(wanted.isEmpty())return;
        var nodes=new ArrayList<AuraNodeBlockEntity>();
        for(var pos:BlockPos.betweenClosed(player.blockPosition().offset(-8,-8,-8),player.blockPosition().offset(8,8,8)))
            if(player.level().hasChunkAt(pos)&&player.level().getBlockEntity(pos) instanceof AuraNodeBlockEntity node&&wanted.stream().anyMatch(a->node.aspects().amount(a)>1))nodes.add(node);
        if(nodes.isEmpty())return;
        var tag=stack.getOrCreateTag();long previous=tag.getLong("txDrainPos");
        var node=nodes.stream().filter(n->n.getBlockPos().asLong()==previous).findFirst().orElseGet(()->nodes.get(player.getRandom().nextInt(nodes.size())));
        var eligible=wanted.stream().filter(a->node.aspects().amount(a)>1&&wand.receiveVis(stack,a,100,true)>0).toList();
        if(eligible.isEmpty())return;var a=eligible.get(player.getRandom().nextInt(eligible.size()));
        if(thaumcraft.api.nodes.NodeApi.take(node,thaumcraft.api.aspects.Aspect.getAspect(a.tag()),1)){
            wand.receiveVis(stack,a,100,false);tag.putLong("txDrainPos",node.getBlockPos().asLong());
        }
    }
}
