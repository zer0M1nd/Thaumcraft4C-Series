package thaumicomniverse.compat.create;

import java.util.*;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.api.contraption.transformable.MovedBlockTransformerRegistries;
import com.simibubi.create.content.contraptions.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.*;
import tc4revival.research.*;
import thaumcraft.api.aspects.MobileEssentiaSources;
import thaumicomniverse.RevivalFeatures;

public class ArcaneContraptions {
    private static final Set<String> NAMESPACES=Set.of("thaumcraft","thaumicomniverse","thaumicbases","thaumic_tinkerer",
        "thaumichorizons","thaumicenergistics","automagy","forbidden_magic","witchinggadgets","gadomancy","nodalmechanics",
        "taintedmagic","warptheory","thaumicinfusion","emt","thaumicjourney","thaumicexpedition");
    public static boolean thaumic(BlockState state){var id=ForgeRegistries.BLOCKS.getKey(state.getBlock());return id!=null&&NAMESPACES.contains(id.getNamespace());}
    public static boolean owned(ResourceLocation id){return id!=null&&NAMESPACES.contains(id.getNamespace());}
    public static boolean enabled(){return RevivalFeatures.Group.CREATE.enabled();}
    public static void setup(){
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(RelocationPassengers::login);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(RelocationPassengers::logout);
        com.simibubi.create.api.boiler.BoilerHeater.REGISTRY.register(ArcaneCreateContent.BURNER.get(),
            (level,pos,state)->enabled()?switch(state.getValue(com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HEAT_LEVEL)){
                case NONE, SMOULDERING -> 0;
                case FADING, KINDLED -> 1;
                case SEETHING -> 2;
            }:-1);
        MovingGolemBindings.setup();
        ArcaneSchematic.setup();
        ArcaneMovement.setup();
        ArcaneDisplaySources.setup();
        BlockMovementChecks.registerMovementNecessaryCheck((state,level,pos)->
            enabled()&&thaumic(state)&&state.hasBlockEntity()?CheckResult.SUCCESS:CheckResult.PASS);
        BlockMovementChecks.registerAttachedCheck((state,level,pos,direction)->{
            if(!enabled())return CheckResult.PASS;
            if(state.is(TCArcane.ARCANE_BORE.get()))
                return CheckResult.of(direction==state.getValue(ArcaneBoreBlock.MOUNT).getOpposite()
                    &&level.getBlockState(pos.relative(direction)).is(TCArcane.ARCANE_BORE_BASE.get()));
            if(state.getBlock() instanceof ItemMirrorBlock)
                return CheckResult.of(direction==state.getValue(ItemMirrorBlock.FACING).getOpposite());
            return CheckResult.PASS;
        });
        BlockMovementChecks.registerMovementAllowedCheck((state,level,pos)->{
            if(!enabled()||!thaumic(state))return CheckResult.PASS;
            if(state.getBlock() instanceof FloatingDepot)return CheckResult.FAIL;
            if(level.getBlockEntity(pos) instanceof ScannableNode)return CheckResult.FAIL;
            if(ArcaneSchematic.fixedStructure(state,level.getBlockEntity(pos)))return CheckResult.FAIL;
            return state.hasBlockEntity()?CheckResult.SUCCESS:CheckResult.PASS;
        });
        for(var block:ForgeRegistries.BLOCKS)if(thaumic(block.defaultBlockState())){
            // Preserve Create's shape-aware stair, slab and wall/floor attachment transforms.
            if(!(block instanceof com.simibubi.create.api.contraption.transformable.TransformableBlock)
                &&!(block instanceof net.minecraft.world.level.block.StairBlock)
                &&!(block instanceof net.minecraft.world.level.block.SlabBlock)
                &&!(block instanceof net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock))
                MovedBlockTransformerRegistries.BLOCK_TRANSFORMERS.register(block,ArcaneContraptions::rotate);
            // Create's fallback accepts only the exact ItemStackHandler class. Its explicit
            // simple type also supports real modifiable inventories exposed by Thaumcraft.
            if(block!=TCArcane.ITEM_MIRROR.get()&&block!=TCArcane.CRUCIBLE.get())
                com.simibubi.create.api.contraption.storage.item.MountedItemStorageType.REGISTRY.register(block,
                    block instanceof FloatingDepot?com.simibubi.create.AllMountedStorageTypes.DEPOT.get():com.simibubi.create.AllMountedStorageTypes.SIMPLE.get());
        }
        for(var type:ForgeRegistries.BLOCK_ENTITY_TYPES){
            var id=ForgeRegistries.BLOCK_ENTITY_TYPES.getKey(type);
            if(id!=null&&NAMESPACES.contains(id.getNamespace()))MovedBlockTransformerRegistries.BLOCK_ENTITY_TRANSFORMERS.register(type,ArcaneRotation::tile);
        }
        MobileEssentiaSources.register(ResourceLocation.parse("thaumicomniverse:contraptions"),(level,bounds)->{
            if(!enabled()||level.isClientSide)return List.of();
            var result=new ArrayList<MobileEssentiaSources.Located>();
            for(var entity:level.getEntitiesOfClass(AbstractContraptionEntity.class,bounds,e->!e.isRemoved()&&e.getContraption()!=null))
                for(var entry:MountedArcaneStorage.of(entity.getContraption()).entries())
                    result.add(new MobileEssentiaSources.Located(entry.position(),entry));
            return result;
        });
        ScanTargetSelector.registerEntitySurface(ResourceLocation.parse("thaumicomniverse:contraptions"),new ScanTargetSelector.EntitySurface(){
            public boolean supports(Entity entity){return enabled()&&entity instanceof AbstractContraptionEntity;}
            public Optional<ScanTargetSelector.Selection> select(ServerPlayer player,Entity entity){return scan(player,(AbstractContraptionEntity)entity);}
        });
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    public static BlockState rotate(BlockState state,StructureTransform transform){
        var result=transform.mirror==null?state:state.mirror(transform.mirror);
        for(var property:state.getProperties()) {
            if(property.getValueClass()==Direction.class){
                var facing=(Direction)state.getValue((Property)property);var next=direction(transform,facing);
                if(property.getPossibleValues().contains(next))result=result.setValue((Property)property,next);
            } else if(property.getValueClass()==Direction.Axis.class){
                var axis=(Direction.Axis)state.getValue((Property)property);
                var next=direction(transform,Direction.fromAxisAndDirection(axis,Direction.AxisDirection.POSITIVE)).getAxis();
                if(property.getPossibleValues().contains(next))result=result.setValue((Property)property,next);
            }
        }
        // Connection bits use directional names rather than a FACING property.
        for(var face:Direction.values()){
            var from=state.getBlock().getStateDefinition().getProperty(face.getName());
            var to=state.getBlock().getStateDefinition().getProperty(direction(transform,face).getName());
            if(from!=null&&to!=null&&from.getValueClass()==Boolean.class&&to.getValueClass()==Boolean.class)
                result=result.setValue((Property)to,state.getValue((Property)from));
        }
        if(direction(transform,Direction.UP)==Direction.DOWN){
            if(state.hasProperty(BlockStateProperties.HALF))result=result.cycle(BlockStateProperties.HALF);
            if(state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF))result=result.cycle(BlockStateProperties.DOUBLE_BLOCK_HALF);
            if(state.hasProperty(BlockStateProperties.HANGING))result=result.cycle(BlockStateProperties.HANGING);
        }
        var id=ForgeRegistries.BLOCKS.getKey(state.getBlock());
        var orientation=state.getBlock().getStateDefinition().getProperty("orientation");
        if(id!=null&&id.getNamespace().equals("gadomancy")&&orientation instanceof IntegerProperty integer
            &&integer.getPossibleValues().equals(java.util.Set.of(2,3,4,5))){
            int next=direction(transform,Direction.from3DDataValue(state.getValue(integer))).get3DDataValue();
            if(integer.getPossibleValues().contains(next))result=result.setValue(integer,next);
        }
        if(state.hasProperty(BlockStateProperties.ROTATION_16)){
            double angle=state.getValue(BlockStateProperties.ROTATION_16)*Math.PI/8;
            var normal=transform.applyWithoutOffsetUncentered(new Vec3(-Math.sin(angle),0,Math.cos(angle)));
            if(normal.horizontalDistanceSqr()>.5)result=result.setValue(BlockStateProperties.ROTATION_16,
                Math.floorMod((int)Math.round(Math.atan2(-normal.x,normal.z)*8/Math.PI),16));
        }
        return result;
    }
    public static Direction direction(StructureTransform transform,Direction face){
        var v=transform.applyWithoutOffsetUncentered(Vec3.atLowerCornerOf(face.getNormal()));return Direction.getNearest(v.x,v.y,v.z);
    }
    public record Hit(BlockPos local,Vec3 world,Direction face,BlockState state){}
    public static Optional<Hit> ray(AbstractContraptionEntity entity,Vec3 eye,Vec3 end){
        return ray(entity,eye,end,1);
    }
    public static Optional<Hit> ray(AbstractContraptionEntity entity,Vec3 eye,Vec3 end,float partial){
        var contraption=entity.getContraption();if(contraption==null)return Optional.empty();
        var from=entity.toLocalVector(eye,partial);var to=entity.toLocalVector(end,partial);
        var view=new ContraptionWorld(entity.level(),contraption);
        var local=view.clip(new ClipContext(from,to,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,entity));
        if(local.getType()==HitResult.Type.MISS)return Optional.empty();
        var info=contraption.getBlocks().get(local.getBlockPos());if(info==null)return Optional.empty();
        return Optional.of(new Hit(info.pos(),entity.toGlobalVector(local.getLocation(),partial),local.getDirection(),info.state()));
    }
    private static Optional<ScanTargetSelector.Selection> scan(ServerPlayer player,AbstractContraptionEntity entity){
        var eye=player.getEyePosition();var end=eye.add(player.getLookAngle().scale(ScanTargetSelector.BLOCK_RANGE));
        var hit=ray(entity,eye,end);if(hit.isEmpty())return Optional.empty();
        var wall=player.level().clip(new ClipContext(eye,hit.get().world(),ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
        if(wall.getType()!=HitResult.Type.MISS&&eye.distanceToSqr(wall.getLocation())+.001<eye.distanceToSqr(hit.get().world()))return Optional.empty();
        return ScanTargetResolver.blockState(hit.get().state()).map(target->new ScanTargetSelector.Selection(target,hit.get().state().getBlock().getName(),hit.get().world(),15));
    }
}
