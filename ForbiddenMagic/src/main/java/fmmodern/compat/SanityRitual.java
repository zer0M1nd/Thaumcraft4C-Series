package fmmodern.compat;

import fmmodern.IntegrationContent;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.ThaumcraftApiHelper;
import tc4revival.content.TCEffects;
import wayoftime.bloodmagic.ritual.*;
import wayoftime.bloodmagic.api.compat.EnumDemonWillType;
import wayoftime.bloodmagic.demonaura.WorldDemonWillHandler;

@RitualRegister("forbidden_magic_sanity")
public final class SanityRitual extends Ritual {
    public SanityRitual(){super("forbidden_magic_sanity",1,20000,"ritual.forbidden_magic.sanity");}
    @Override public int getRefreshCost(){return 15000;}
    @Override public Ritual getNewCopy(){return new SanityRitual();}
    @Override public void performRitual(IMasterRitualStone stone){
        if(!(stone.getWorldObj() instanceof ServerLevel level)||!IntegrationContent.bloodEnabled()||level.getGameTime()%600!=0)return;
        var pos=stone.getMasterBlockPos();
        // Modern Blood Magic replaced ritual reagent tanks with configured demon-will effects.
        boolean boosted=getWillRespectingConfig(level,pos,EnumDemonWillType.STEADFAST,stone.getActiveWillConfig())>=4;
        var players=level.getEntitiesOfClass(ServerPlayer.class,new AABB(pos).inflate(boosted?45:15),p->!p.isSpectator());
        var network=stone.getOwnerNetwork();if(network.getCurrentEssence()<(long)getRefreshCost()*players.size()){network.causeNausea();return;}
        var targets=players.stream().filter(p->{var effect=p.getEffect(TCEffects.WARP_WARD.get());return effect==null||effect.getDuration()<=600;}).toList();
        if(targets.isEmpty())return;int cost=getRefreshCost()*targets.size();if(network.syphon(stone.ticket(cost))<cost)return;
        for(var player:targets){var ward=new MobEffectInstance(TCEffects.WARP_WARD.get(),602,0,true,true);ward.setCurativeItems(List.of());player.addEffect(ward);if(level.getGameTime()%2400==0&&level.random.nextInt(35)<=3)ThaumcraftApiHelper.addWarpToPlayer(player,1+level.random.nextInt(3),true);}
        if(boosted)WorldDemonWillHandler.drainWill(level,pos,EnumDemonWillType.STEADFAST,4,true);
    }
    @Override public void gatherComponents(Consumer<RitualComponent> components){
        addCornerstones(components,4,1,EnumRuneType.DUSK);
        addCornerstones(components,4,2,EnumRuneType.EARTH);
        addCornerstones(components,4,4,EnumRuneType.AIR);
        addOffsetRunes(components,4,1,4,EnumRuneType.EARTH);
        addCornerstones(components,3,4,EnumRuneType.FIRE);
    }
    private void addCornerstones(Consumer<RitualComponent> components,int radius,int y,EnumRuneType type){addRune(components,radius,y,0,type);addRune(components,-radius,y,0,type);addRune(components,0,y,radius,type);addRune(components,0,y,-radius,type);}
}
