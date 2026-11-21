package technomancy;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.ItemHandlerHelper;

public final class ExistenceLogic {
    public static int capacity(MachineKind kind) {
        return switch(kind) {
            case EXISTENCE_BURNER->100;case DYNAMIC_EXISTENCE_BURNER->150;
            case EXISTENCE_FOUNTAIN,EXISTENCE_SEALING_DEVICE->1000000;
            case EXISTENCE_PYLON_1->5;case EXISTENCE_PYLON_2->25;case EXISTENCE_PYLON_3->125;
            case EXISTENCE_CROP_FERTILISER,EXISTENCE_CROP_HARVESTER->10000;
            default->0;
        };
    }
    public static int power(MachineEntity m){return m.state.getInt("existence");}
    public static int change(MachineEntity m,int delta) {
        int old=power(m),next=Math.max(0,Math.min(capacity(m.kind),old+delta));
        if(next!=old){m.state.putInt("existence",next);m.changed();}return next-old;
    }
    public static boolean producer(MachineKind k){return k==MachineKind.EXISTENCE_BURNER||k==MachineKind.DYNAMIC_EXISTENCE_BURNER||k==MachineKind.EXISTENCE_FOUNTAIN;}
    public static boolean pylon(MachineKind k){return k==MachineKind.EXISTENCE_PYLON_1||k==MachineKind.EXISTENCE_PYLON_2||k==MachineKind.EXISTENCE_PYLON_3;}
    public static int rate(MachineKind k){return k==MachineKind.EXISTENCE_FOUNTAIN?2000:producer(k)?4:pylon(k)?capacity(k):capacity(k)/50;}
    public static int value(LivingEntity entity){return entity instanceof Villager?50:entity instanceof Monster?1:entity.getType().getCategory()==MobCategory.CREATURE||entity.getType().getCategory()==MobCategory.AMBIENT?5:1;}
    public static List<LivingEntity> victims(MachineEntity m,int radius) {
        return m.server().getEntitiesOfClass(LivingEntity.class,new AABB(m.getBlockPos()).inflate(radius),e->e.isAlive()&&!(e instanceof Player)&&!e.isInvulnerable());
    }
    public static boolean sacrifice(MachineEntity m,LivingEntity victim) {
        if(victim instanceof Player||!victim.isAlive()||victim.isInvulnerable())return false;
        // Normal damage dispatch respects cancellation, death hooks and modded invulnerability.
        victim.hurt(m.server().damageSources().indirectMagic(WorldActions.actor(m),WorldActions.actor(m)),Float.MAX_VALUE);
        return !victim.isAlive();
    }
    public static void tick(MachineEntity m) {
        if(!m.runs())return;
        if(producer(m.kind)) {
            if(m.kind==MachineKind.EXISTENCE_FOUNTAIN){change(m,500);return;}
            boolean dynamic=m.kind==MachineKind.DYNAMIC_EXISTENCE_BURNER;
            if(power(m)>=capacity(m.kind)||dynamic&&m.energy<10000)return;
            for(var e:victims(m,3)) {
                if(power(m)>=capacity(m.kind)||dynamic&&m.energy<10000)break;
                if(sacrifice(m,e)) {
                    int value=e instanceof Villager?(dynamic?20:15):e instanceof Monster?(dynamic?2:1):(dynamic?4:2);
                    change(m,value);if(dynamic)m.pay(10000);
                }
            }
        }else if(pylon(m.kind))transfer(m);
        else if(m.kind==MachineKind.EXISTENCE_CROP_FERTILISER)fertilise(m);
        else if(m.kind==MachineKind.EXISTENCE_CROP_HARVESTER)harvest(m);
        else if(m.kind==MachineKind.EXISTENCE_SEALING_DEVICE&&power(m)>=500000) {
            for(var e:m.server().getEntitiesOfClass(Villager.class,new AABB(m.getBlockPos()).inflate(3))) {
                var d=e.getPersistentData();
                if(d.contains("technomancyTreasure")&&d.getInt("technomancySeal")==0) {d.putInt("technomancySeal",80);change(m,-500000);break;}
            }
        }
    }
    private static void transfer(MachineEntity m) {
        // Cache positions, never references: unloading a neighbour must immediately stop transfers.
        if(m.ticks%20==1||!m.state.contains("existenceLinks")) {
            var positions=new ArrayList<Long>();
            for(var p:BlockPos.betweenClosed(m.getBlockPos().offset(-7,-1,-7),m.getBlockPos().offset(7,1,7)))
                if(!p.equals(m.getBlockPos())&&MachineLogic.loaded(m,p) instanceof MachineEntity other&&capacity(other.kind)>0)positions.add(p.asLong());
            m.state.putLongArray("existenceLinks",positions);m.changed();
        }
        var targets=new ArrayList<MachineEntity>();
        for(long p:m.state.getLongArray("existenceLinks"))if(MachineLogic.loaded(m,BlockPos.of(p)) instanceof MachineEntity other)targets.add(other);
        for(var source:targets)if(producer(source.kind)&&source.runs())move(source,m,rate(source.kind));
        for(var target:targets)if(!producer(target.kind)&&!pylon(target.kind))move(m,target,rate(target.kind));
        for(var target:targets)if(pylon(target.kind)&&target.runs())move(m,target,rate(target.kind));
    }
    private static void move(MachineEntity from,MachineEntity to,int rate) {
        int amount=Math.min(rate,Math.min(power(from),capacity(to.kind)-power(to)));
        if(amount>0){change(from,-amount);change(to,amount);}
    }
    private static void fertilise(MachineEntity m) {
        if(power(m)<=605)return;
        for(var p:BlockPos.betweenClosed(m.getBlockPos().offset(-5,2,-5),m.getBlockPos().offset(5,2,5))) {
            if(!WorldActions.allowed(m,p)||!WorldActions.mayBreak(m,p))continue;
            var s=m.server().getBlockState(p);
            if(s.getBlock() instanceof net.minecraftforge.common.IPlantable) {
                if(s.getBlock() instanceof BonemealableBlock grow) {
                    if(power(m)>=30&&grow.isValidBonemealTarget(m.server(),p,s,false)) {grow.performBonemeal(m.server(),m.server().random,p,s);change(m,-30);}
                }else if(power(m)>=5&&s.isRandomlyTicking()){s.randomTick(m.server(),p,m.server().random);change(m,-5);}
            }
        }
    }
    private static void harvest(MachineEntity m) {
        // Persist every drop before starting another harvest; automation can drain the output slots.
        for(int i=0;i<m.inventory.getSlots();i++)if(!m.inventory.getStackInSlot(i).isEmpty())return;
        if(power(m)<30)return;
        for(var p:BlockPos.betweenClosed(m.getBlockPos().offset(-5,0,-5),m.getBlockPos().offset(5,0,5))) {
            if(!WorldActions.allowed(m,p))continue;var s=m.server().getBlockState(p);
            if(!(s.getBlock() instanceof BonemealableBlock grow)||grow.isValidBonemealTarget(m.server(),p,s,false))continue;
            var age=s.getProperties().stream().filter(property->property instanceof net.minecraft.world.level.block.state.properties.IntegerProperty&&property.getName().equals("age"))
                .map(property->(net.minecraft.world.level.block.state.properties.IntegerProperty)property).findFirst().orElse(null);
            if(age==null||!WorldActions.mayBreak(m,p))continue;
            var replantedState=s.setValue(age,Collections.min(age.getPossibleValues()));
            var tool=new ItemStack(net.minecraft.world.item.Items.DIAMOND_HOE);
            tool.enchant(net.minecraft.world.item.enchantment.Enchantments.BLOCK_FORTUNE,1);
            var drops=Block.getDrops(s,m.server(),p,null,WorldActions.actor(m),tool);
            var seed=s.getCloneItemStack(new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(p),net.minecraft.core.Direction.UP,p,false),m.server(),p,WorldActions.actor(m));
            boolean replanted=false;
            for(var stack:drops)if(!replanted&&ItemStack.isSameItemSameTags(seed,stack)){stack.shrink(1);replanted=true;}
            if(!replanted)continue;
            // If modded drops exceed storage, defer without touching the crop.
            var trial=new net.minecraftforge.items.ItemStackHandler(m.inventory.getSlots());
            for(var stack:drops)if(!ItemHandlerHelper.insertItemStacked(trial,stack,true).isEmpty())return;else ItemHandlerHelper.insertItemStacked(trial,stack,false);
            if(!WorldActions.replace(m,p,replantedState))continue;
            for(var stack:drops)ItemHandlerHelper.insertItemStacked(m.inventory,stack,false);
            change(m,-30);return;
        }
    }
    private ExistenceLogic(){}
}
