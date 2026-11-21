package thaumicomniverse.compat.create;

import java.util.*;
import com.simibubi.create.content.contraptions.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import tc4revival.content.*;
import thaumcraft.api.golems.GolemLocations;

public class MovingGolemBindings {
    public static final ResourceLocation PROVIDER=ResourceLocation.parse("thaumicomniverse:create");
    public static void setup(){GolemLocations.register(PROVIDER,MovingGolemBindings::resolve);}
    private static GolemLocations.Target resolve(ServerLevel level,UUID id,BlockPos local,Direction side){
        if(!ArcaneContraptions.enabled())return null;
        for(var ref:ContraptionHandler.loadedContraptions.get(level).values()){
            var entity=ref.get();if(entity==null||entity.isRemoved())continue;
            var structure=entity.getContraption();
            if(structure==null||structure.disassembled||!((ContraptionAccess)structure).omniverse$frameId().equals(id)||!structure.getBlocks().containsKey(local))continue;
            var point=entity.toGlobalVector(local.getCenter(),1);
            var normal=entity.toGlobalVector(local.getCenter().add(Vec3.atLowerCornerOf(side.getNormal())),1).subtract(point);
            var storage=structure.getStorage();
            var essentia=MountedArcaneStorage.of(structure).at(local);
            return new GolemLocations.Target(point,Direction.getNearest(normal.x,normal.y,normal.z),storage.getAllItemStorages().get(local),
                storage.getFluids().storages.get(local),essentia==null?null:essentia.storage);
        }return null;
    }
    public static void mount(Contraption structure,Level world,BlockPos offset){
        if(!ArcaneContraptions.enabled()||!(world instanceof ServerLevel level))return;
        // Upgrade legacy loaded references before their world blocks disappear. New references
        // already have persistent frames, so unloaded golems need no entity-NBT rewriting.
        for(var entity:level.getAllEntities())if(entity instanceof GolemEntity golem)GolemMobileBindings.refresh(golem);
        var positions=new HashMap<BlockPos,BlockPos>();
        for(var info:structure.getBlocks().values())positions.put(info.pos().offset(structure.anchor).offset(offset),info.pos());
        GolemLocations.mount(level,PROVIDER,((ContraptionAccess)structure).omniverse$frameId(),positions);
    }
    public static void unmount(Contraption structure,Level world,StructureTransform transform){
        if(world instanceof ServerLevel level)GolemLocations.unmount(level,PROVIDER,((ContraptionAccess)structure).omniverse$frameId(),transform::apply,d->ArcaneContraptions.direction(transform,d));
    }
    public static boolean interact(AbstractContraptionEntity entity,Player player,BlockPos local,Direction side,InteractionHand hand){
        if(hand==null||!ArcaneContraptions.enabled()||entity.getContraption()==null||entity.getContraption().disassembled)return false;
        var stack=player.getItemInHand(hand);
        boolean mirrorItem=stack.getItem() instanceof HandMirrorItem||stack.getItem() instanceof ItemMirrorItem;
        if(!(stack.getItem() instanceof GolemBellItem)&&!(stack.getItem() instanceof GolemPlacerItem)&&!mirrorItem)return false;
        if(!entity.getContraption().getBlocks().containsKey(local))return false;
        if(mirrorItem&&!(entity.getContraption().getBlocks().get(local).state().getBlock() instanceof ItemMirrorBlock))return false;
        if(player.level().isClientSide)return true;
        var point=entity.toGlobalVector(local.getCenter(),1);
        // Create's packet bounds cover the whole contraption. Validate the chosen block too.
        double reach=player.getAttributeValue(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get())+1;
        var eye=player.getEyePosition();
        if(eye.distanceToSqr(point)>reach*reach)return true;
        var ray=ArcaneContraptions.ray(entity,eye,eye.add(player.getLookAngle().scale(reach)));
        if(ray.isEmpty()||!ray.get().local().equals(local))return true;
        var wall=player.level().clip(new net.minecraft.world.level.ClipContext(eye,ray.get().world(),net.minecraft.world.level.ClipContext.Block.OUTLINE,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
        if(wall.getType()!=net.minecraft.world.phys.HitResult.Type.MISS&&eye.distanceToSqr(wall.getLocation())+.01<eye.distanceToSqr(ray.get().world()))return true;
        if(mirrorItem){var mirror=MovingMirror.at(entity.getContraption(),local);if(mirror!=null)ItemMirrorItem.bind(stack,player,mirror);return true;}
        var anchor=GolemLocations.bindMoving((ServerLevel)player.level(),PROVIDER,((ContraptionAccess)entity.getContraption()).omniverse$frameId(),local,ray.get().face());
        var normal=entity.toGlobalVector(local.getCenter().add(Vec3.atLowerCornerOf(ray.get().face().getNormal())),1).subtract(point);
        var face=Direction.getNearest(normal.x,normal.y,normal.z);
        if(stack.getItem() instanceof GolemBellItem)GolemBellItem.changeMarker(stack,player,BlockPos.containing(point),face,anchor);
        else if(!player.isShiftKeyDown())((GolemPlacerItem)stack.getItem()).placeMoving(stack,player,anchor,point,face);
        return true;
    }
}
