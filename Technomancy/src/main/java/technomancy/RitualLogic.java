package technomancy;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.tags.FluidTags;

/** The sixteen registered 0.12.5 rituals. Running effects and cursors are saved with the core. */
public final class RitualLogic {
    public record Definition(String id,PlayerPower.Affinity core,List<PlayerPower.Affinity> frame,int radius) {}
    private static final List<Definition> DEFINITIONS=new ArrayList<>();
    static {
        register(new Definition("extraction",PlayerPower.Affinity.EARTH,List.of(PlayerPower.Affinity.DARK,PlayerPower.Affinity.DARK,PlayerPower.Affinity.LIGHT),5));
        register(new Definition("fountain",PlayerPower.Affinity.DARK,List.of(PlayerPower.Affinity.LIGHT,PlayerPower.Affinity.DARK,PlayerPower.Affinity.LIGHT),11));
        for(int tier=3;tier>=1;tier--) {
            add("cave_in",PlayerPower.Affinity.EARTH,tier,2*tier-1);
            add("black_hole",PlayerPower.Affinity.DARK,tier,new int[]{3,5,9}[tier-1]);
            add("water",PlayerPower.Affinity.WATER,tier,new int[]{1,3,9}[tier-1]);
            add("purification",PlayerPower.Affinity.LIGHT,tier,new int[]{0,1,3}[tier-1]);
            if(tier<3)add("fire",PlayerPower.Affinity.FIRE,tier,9);
        }
    }
    private static void add(String family,PlayerPower.Affinity core,int tier,int radius){register(new Definition(family+"_"+tier,core,Collections.nCopies(tier,core),radius));}
    public static void register(Definition definition) {
        if(definition.frame().isEmpty()||definition.frame().size()>3||DEFINITIONS.stream().anyMatch(d->d.id().equals(definition.id())))throw new IllegalArgumentException("Invalid or duplicate ritual");
        DEFINITIONS.add(definition);
    }
    public static List<Definition> definitions(){return List.copyOf(DEFINITIONS);}
    public static PlayerPower.Affinity affinity(MachineKind kind){return PlayerPower.Affinity.valueOf(kind.name().substring(0,kind.name().indexOf('_')));}
    public static List<BlockPos> frame(BlockPos center,int ring) {
        int distance=1+2*ring;var positions=new ArrayList<BlockPos>();
        for(int y=0;y<3;y++)for(int x:new int[]{-distance,distance})for(int z:new int[]{-distance,distance})positions.add(center.offset(x,y,z));
        return positions;
    }
    private static boolean ringMatches(MachineEntity m,int ring,PlayerPower.Affinity affinity) {
        var block=TechRegistry.MACHINES.get(MachineKind.valueOf(affinity.name()+"_CRYSTAL")).get();
        for(var p:frame(m.getBlockPos(),ring))if(!m.server().hasChunkAt(p)||!m.server().getBlockState(p).is(block))return false;
        return true;
    }
    public static boolean matches(MachineEntity m,Definition d) {
        if(!m.kind.catalyst()||affinity(m.kind)!=d.core())return false;
        for(int ring=0;ring<3;ring++) {
            if(ring<d.frame().size()){if(!ringMatches(m,ring,d.frame().get(ring)))return false;}
            else for(var a:PlayerPower.Affinity.values())if(a!=PlayerPower.Affinity.NORMAL&&ringMatches(m,ring,a))return false;
        }
        return true;
    }
    public static void activate(MachineEntity m,Player player) {
        if(m.getLevel().isClientSide||m.state.contains("ritual"))return;
        for(var d:DEFINITIONS)if(matches(m,d)) {
            if(d.id().startsWith("cave_in")&&!movableArea(m,d.radius())) {
                if(player!=null)player.displayClientMessage(Component.translatable("message.technomancy.ritual_obstructed"),true);return;
            }
            for(int ring=0;ring<d.frame().size();ring++)for(var p:frame(m.getBlockPos(),ring))if(!WorldActions.mayBreak(m,p))return;
            if(d.id().equals("extraction")){if(!extract(m,d))return;}
            else if(!d.id().startsWith("purification"))consumeFrame(m,d);
            if(player!=null) {
                m.owner=player.getUUID();PlayerPower.affinity(player,d.core(),5);
                for(var a:d.frame())PlayerPower.affinity(player,a,1);
                PlayerPower.train(player,25*d.frame().size()+2);
            }
            if(d.id().equals("extraction"))return;
            m.state.putString("ritual",d.id());m.state.putInt("ritualCursor",0);m.state.putInt("ritualAge",0);m.active=true;m.changed();
            if(player!=null)player.displayClientMessage(Component.translatable("message.technomancy.ritual_started",Component.translatable("ritual.technomancy."+d.id())),true);
            return;
        }
        if(player!=null)player.displayClientMessage(Component.translatable("message.technomancy.ritual_incomplete"),true);
    }
    private static void consumeFrame(MachineEntity m,Definition d) {
        for(int ring=0;ring<d.frame().size();ring++)for(var p:frame(m.getBlockPos(),ring))m.server().removeBlock(p,false);
    }
    private static boolean movableArea(MachineEntity m,int radius) {
        for(var p:BlockPos.betweenClosed(new BlockPos(m.getBlockPos().getX()-radius,m.server().getMinBuildHeight(),m.getBlockPos().getZ()-radius),m.getBlockPos().offset(radius,-1,radius))) {
            if(!WorldActions.allowed(m,p))return false;
            var tile=m.server().getBlockEntity(p);
            if(tile!=null&&(!(tile instanceof MachineEntity machine)||!machine.kind.crystal()&&!machine.kind.catalyst()))return false;
        }
        return true;
    }
    private static boolean extract(MachineEntity m,Definition d) {
        var nearby=m.server().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new AABB(m.getBlockPos()).inflate(5));
        for(var e:nearby)if(e instanceof Villager&&e.getPersistentData().contains("technomancyTreasure")) {
            String id=e.getPersistentData().getString("technomancyTreasure");
            if(!PlayerEvents.TREASURES.contains(id))continue;
            // Remove before death to prevent the treasure's destruction effect during extraction.
            e.getPersistentData().remove("technomancyTreasure");
            if(!ExistenceLogic.sacrifice(m,e)){e.getPersistentData().putString("technomancyTreasure",id);return false;}
            m.server().addFreshEntity(new ItemEntity(m.server(),m.getBlockPos().getX()+.5,m.getBlockPos().getY()+2,m.getBlockPos().getZ()+.5,new net.minecraft.world.item.ItemStack(TechRegistry.item(id))));
            consumeFrame(m,d);return true;
        }
        if(!nearby.isEmpty())m.server().explode(WorldActions.actor(m),m.getBlockPos().getX()+.5,m.getBlockPos().getY()+.5,m.getBlockPos().getZ()+.5,30,true,net.minecraft.world.level.Level.ExplosionInteraction.BLOCK);
        return false;
    }
    public static void tick(MachineEntity m) {
        if(!m.state.contains("ritual")) {
            if(m.ticks%10==0&&m.server().hasNeighborSignal(m.getBlockPos()))activate(m,m.owner==null?null:m.server().getPlayerByUUID(m.owner));
            return;
        }
        String id=m.state.getString("ritual");var d=DEFINITIONS.stream().filter(def->def.id().equals(id)).findFirst().orElse(null);
        if(d==null){m.state.remove("ritual");m.active=false;m.changed();return;}
        if(id.startsWith("purification")) {
            int tier=d.frame().size(),min=tier==1?0:-1,max=tier==1?3:tier==2?11:m.server().getMaxBuildHeight()-m.getBlockPos().getY();
            var p=m.getBlockPos();
            for(var e:m.server().getEntitiesOfClass(Monster.class,new AABB(p.getX()-d.radius(),p.getY()+min,p.getZ()-d.radius(),p.getX()+d.radius()+1,p.getY()+max+1,p.getZ()+d.radius()+1)))ExistenceLogic.sacrifice(m,e);
            return;
        }
        if(id.equals("fountain")) {
            int collected=m.state.getInt("ritualCollected");
            for(var e:ExistenceLogic.victims(m,11))if(ExistenceLogic.sacrifice(m,e))collected+=ExistenceLogic.value(e);
            m.state.putInt("ritualCollected",collected);m.changed();
            if(collected>=1000) {
                var owner=m.owner;m.server().setBlock(m.getBlockPos(),TechRegistry.MACHINES.get(MachineKind.EXISTENCE_FOUNTAIN).get().defaultBlockState(),3);
                if(m.server().getBlockEntity(m.getBlockPos()) instanceof MachineEntity fountain){fountain.owner=owner;fountain.changed();}
            }
            return;
        }
        int age=m.state.getInt("ritualAge");m.state.putInt("ritualAge",age+1);
        if(id.startsWith("black_hole"))for(var e:ExistenceLogic.victims(m,d.radius()))ExistenceLogic.sacrifice(m,e);
        boolean finished=volumeStep(m,d);
        if(finished&&(!id.startsWith("black_hole")||age>=60))m.server().removeBlock(m.getBlockPos(),false);
        else m.changed();
    }
    private static boolean volumeStep(MachineEntity m,Definition d) {
        String id=d.id();var center=m.getBlockPos();int radius=d.radius(),minY,maxY;
        if(id.startsWith("black_hole")){minY=center.getY()-radius;maxY=center.getY()+radius;}
        else if(id.startsWith("fire")){minY=center.getY()-19;maxY=center.getY()-1;}
        else if(id.startsWith("cave_in")||id.equals("water_3")){minY=m.server().getMinBuildHeight();maxY=center.getY()-1;}
        else {minY=center.getY()-1;maxY=minY;}
        minY=Math.max(minY,m.server().getMinBuildHeight());maxY=Math.min(maxY,m.server().getMaxBuildHeight()-1);
        int width=radius*2+1,layer=width*width,total=Math.max(0,maxY-minY+1)*layer,cursor=m.state.getInt("ritualCursor");
        for(int budget=0;budget<256&&cursor<total;budget++,cursor++) {
            int y=minY+cursor/layer,x=cursor%layer/width-radius,z=cursor%width-radius;
            var p=new BlockPos(center.getX()+x,y,center.getZ()+z);
            if(p.equals(center))continue;
            if(!m.server().hasChunkAt(p))break; // Resume from this exact cell when its chunk is available.
            var state=m.server().getBlockState(p);
            if(id.startsWith("black_hole")){if(!state.isAir()&&WorldActions.mayBreak(m,p))m.server().removeBlock(p,false);}
            else if(id.startsWith("water")){if(state.canBeReplaced()&&state.getFluidState().isEmpty())WorldActions.place(m,p,Blocks.WATER.defaultBlockState());}
            else if(id.equals("fire_1")){if(state.getFluidState().is(FluidTags.WATER))WorldActions.replace(m,p,Blocks.OBSIDIAN.defaultBlockState());}
            else if(id.equals("fire_2")){if(state.isAir())WorldActions.place(m,p,Blocks.LAVA.defaultBlockState());}
            else if(id.startsWith("cave_in")&&!state.isAir()&&!state.canBeReplaced()&&WorldActions.mayBreak(m,p)) {
                for(int targetY=m.server().getMinBuildHeight();targetY<y;targetY++) {
                    var target=new BlockPos(p.getX(),targetY,p.getZ());
                    if(!m.server().getBlockState(target).canBeReplaced())continue;
                    var tile=m.server().getBlockEntity(p);
                    if(tile!=null&&(!(tile instanceof MachineEntity machine)||!machine.kind.crystal()&&!machine.kind.catalyst()))break;
                    var saved=tile==null?null:tile.saveWithoutMetadata();
                    if(!WorldActions.place(m,target,state))break;
                    if(saved!=null&&m.server().getBlockEntity(target)!=null)m.server().getBlockEntity(target).load(saved);
                    m.server().removeBlockEntity(p);m.server().removeBlock(p,false);break;
                }
            }
        }
        m.state.putInt("ritualCursor",cursor);return cursor>=total;
    }
    private RitualLogic(){}
}
