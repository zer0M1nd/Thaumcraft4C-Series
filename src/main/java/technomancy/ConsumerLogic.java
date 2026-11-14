package technomancy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import tc4revival.aspect.*;
import tc4revival.aspect.item.ItemStackAspects;

public final class ConsumerLogic {
    private static final int[] RADII={9,1,0,5,4,16},HEIGHTS={-1,1,1,-1,9,-1};
    public static void tick(MachineEntity m){
        m.active=false;
        if(!m.runs()||m.energy<TechConfig.CONSUMER.get()||m.aspects.size()>=4||m.aspects.view().values().stream().anyMatch(n->n>4))return;
        int r=RADII[m.mode%6],h=HEIGHTS[m.mode%6],bottom=h<0?m.server().getMinBuildHeight():m.getBlockPos().getY()-h;
        var origin=m.getBlockPos();var bounds=new AABB(origin.getX()-r,bottom,origin.getZ()-r,origin.getX()+r+1,origin.getY(),origin.getZ()+r+1);
        for(var mob:m.server().getEntitiesOfClass(Mob.class,bounds,e->e.isAlive()&&!e.isInvulnerable())){
            if(m.energy<TechConfig.CONSUMER.get())break;
            if(mob.hurt(m.server().damageSources().magic(),Float.MAX_VALUE)){m.pay(TechConfig.CONSUMER.get());m.active=true;}
        }
        for(var item:m.server().getEntitiesOfClass(ItemEntity.class,bounds,e->e.isAlive())){
            if(m.energy<TechConfig.CONSUMER.get())break;
            var values=ItemStackAspects.resolve(item.getItem()).orElseGet(AspectAmounts::new);
            if(m.aspects.total()+values.total()>m.capacity())continue;
            if(m.pay(TechConfig.CONSUMER.get())){append(m,values);item.getItem().shrink(1);if(item.getItem().isEmpty())item.discard();m.active=true;}
        }
        if(m.energy<TechConfig.CONSUMER.get())return;
        // Saved cursor bounds the work per tick, including large empty quarry regions.
        int width=r*2+1,height=origin.getY()-bottom,total=width*width*height;if(total<=0)return;
        int cursor=Math.floorMod(m.state.getInt("quarryCursor"),total);
        for(int scanned=0;scanned<Math.min(total,512);scanned++){
            int i=(cursor+scanned)%total;int y=origin.getY()-1-i/(width*width),x=i%width-r,z=(i/width)%width-r;
            var pos=new BlockPos(origin.getX()+x,y,origin.getZ()+z);
            if(!WorldActions.allowed(m,pos))continue;
            var block=m.server().getBlockState(pos);if(block.isAir()||block.getDestroySpeed(m.server(),pos)<0)continue;
            if(!WorldActions.mayBreak(m,pos))continue;
            if(!block.getFluidState().isEmpty()){
                if(m.server().setBlock(pos,Blocks.AIR.defaultBlockState(),3)){m.pay(TechConfig.CONSUMER.get());m.active=true;m.state.putInt("quarryCursor",(i+1)%total);m.changed();return;}continue;
            }
            var drops=Block.getDrops(block,m.server(),pos,m.server().getBlockEntity(pos),WorldActions.actor(m),ItemStack.EMPTY);
            var values=new AspectAmounts();for(var drop:drops){var tags=ItemStackAspects.resolve(drop).orElseGet(AspectAmounts::new);for(var entry:tags.view().entrySet())values.add(entry.getKey(),Math.multiplyExact(entry.getValue(),drop.getCount()));}
            if(m.aspects.total()+values.total()>m.capacity())continue;
            // Block inventories follow their normal removal behavior and are collected on the next tick.
            if(m.server().removeBlock(pos,false)){m.pay(TechConfig.CONSUMER.get());m.active=true;append(m,values);m.state.putInt("quarryCursor",(i+1)%total);m.changed();return;}
        }
        m.state.putInt("quarryCursor",(cursor+512)%total);m.changed();
    }
    private static void append(MachineEntity m,AspectAmounts values){values.view().forEach(m.aspects::add);m.changed();}
    private ConsumerLogic(){}
}
