package ttmodern;

import java.util.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import tc4revival.content.*;
import tc4revival.aspect.Aspect;

public final class DeflectionFocus extends TinkererFocus {
    public static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> IMMUNE=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":deflection_immune"));
    public static final net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> DISCARD=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,net.minecraft.resources.ResourceLocation.parse(Tinkerer.ID+":deflection_discard"));
    public DeflectionFocus(){super("focus_deflection","TTDF",0xffffff);}
    @Override public String ornamentTexture(){return null;}
    @Override public Map<Aspect,Integer> costs(ItemStack focus){return cost("ordo",8,"aer",4);}
    @Override public boolean costPerTick(ItemStack focus){return true;}
    @Override public List<FocusUpgrade> upgradesForRank(ItemStack focus,int rank){return rank==2||rank==4?List.of(FocusUpgrade.FRUGAL,FocusUpgrade.ENLARGE):super.upgradesForRank(focus,rank);}
    @Override public InteractionResultHolder<ItemStack> cast(ItemStack stack,Level level,Player player,InteractionHand hand,BlockHitResult hit){player.startUsingItem(hand);return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);}
    @Override public void usingTick(ItemStack stack,Player player,int remaining){
        if(!(player.level() instanceof net.minecraft.server.level.ServerLevel level))return;
        var wand=(CastingWandItem)stack.getItem();
        if(!wand.consumeForPlayer(stack,player,costs(attached(stack)),false,false,true))return;
        protect(player,wand.focusUpgrade(stack,FocusUpgrade.ENLARGE));
    }
    /** Legacy deflection advances the projectile along its travel direction; it does not reverse ownership. */
    public static void protect(Player player,int enlarge){
        if(!(player.level() instanceof net.minecraft.server.level.ServerLevel level))return;
        var at=player.position();var area=new AABB(at.x-4-enlarge,at.y-4-enlarge,at.z-4-enlarge,at.x+3+enlarge,at.y+3+enlarge,at.z+3+enlarge);
        for(var projectile:level.getEntitiesOfClass(Projectile.class,area,e->e.isAlive()&&e.getOwner()!=player&&!e.getType().is(IMMUNE)&&!BotaniaDeflection.harmless(e,player))){
            var motion=projectile.getDeltaMovement();if(motion.lengthSqr()<1e-12)continue;
            var destination=projectile.position().add(motion.normalize().scale(projectile.distanceTo(player)*2));
            if(!level.hasChunkAt(net.minecraft.core.BlockPos.containing(destination)))continue;
            dust(level,projectile.position(),0xffffff,6,0);projectile.teleportTo(destination.x,destination.y,destination.z);projectile.hasImpulse=true;if(projectile.getType().is(DISCARD))projectile.discard();
        }
    }
}
