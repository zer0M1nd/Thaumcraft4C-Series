package tc4revival.content;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.pathfinder.*;

/** Route planning uses the same hand-openable door contract as vanilla door AI. */
public class GolemDoorNavigation extends GroundPathNavigation {
    private BlockPos plannedStart;
    private PathFinder routeFinder;
    /** Plan a later leg without moving the entity or replacing its current navigation. */
    public Path planFrom(BlockPos start,BlockPos destination,int accuracy) {
        float range=(float)mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
        int radius=(int)Math.ceil(range)+8;
        var region=new PathNavigationRegion(level,start.offset(-radius,-radius,-radius),start.offset(radius,radius,radius));
        var previousStart=plannedStart;
        try {
            plannedStart=start;
            // Calling navigation.createPath here would change the active target and stuck timer.
            // Center the region on this leg, rather than the golem's current physical location.
            return routeFinder.findPath(region,mob,java.util.Set.of(destination),range,accuracy,1.0f);
        } finally { plannedStart=previousStart; }
    }
    public static final net.minecraft.tags.TagKey<Block> AVOID=BlockTags.create(net.minecraft.resources.ResourceLocation.parse("thaumcraft:golem_path_hazards"));
    public static final net.minecraft.tags.TagKey<Block> PREFERRED=BlockTags.create(net.minecraft.resources.ResourceLocation.parse("thaumcraft:golem_preferred_paths"));
    public GolemDoorNavigation(Mob mob,Level level) {super(mob,level);setCanOpenDoors(true);setCanPassDoors(true);setCanFloat(true);}
    @Override public void recomputePath() {
        if(path==null)return;
        if(level.getGameTime()-timeLastRecompute<=20){hasDelayedRecomputation=true;return;}
        var destination=path.getTarget();
        int accuracy=!path.canReach() || path.getEndNode()==null?0:(int)Math.ceil(path.getEndNode().asBlockPos().distManhattan(destination));
        path=null;
        var replacement=createPath(destination,accuracy);
        if(replacement!=null)moveTo(replacement,speedModifier);
        timeLastRecompute=level.getGameTime();hasDelayedRecomputation=false;
    }
    public static boolean usable(net.minecraft.world.level.block.state.BlockState state) {
        return DoorBlock.isWoodenDoor(state)
                ||state.is(BlockTags.FENCE_GATES)&&state.getBlock() instanceof FenceGateBlock;
    }
    @Override protected PathFinder createPathFinder(int nodes) {
        nodeEvaluator=new WalkNodeEvaluator(){
            @Override public Node getStart() {
                if (plannedStart==null) return super.getStart();
                var node=getNode(plannedStart.getX(),plannedStart.getY(),plannedStart.getZ());
                node.type=getBlockPathType(level,node.x,node.y,node.z);
                node.costMalus=mob.getPathfindingMalus(node.type);
                return node;
            }
            @Override public int getNeighbors(Node[] neighbors,Node current) {
                int count=super.getNeighbors(neighbors,current),kept=0;
                boolean aligned=mob instanceof GolemEntity golem && golem.configuration().axisAligned();
                for (int i=0;i<count;i++) {
                    var node=neighbors[i];
                    if (aligned && node.x!=current.x && node.z!=current.z) continue;
                    if (!level.getBlockState(new BlockPos(node.x,node.y-1,node.z)).is(PREFERRED))
                        node.costMalus=Math.max(node.costMalus,2);
                    neighbors[kept++]=node;
                }
                return kept;
            }
            @Override public BlockPathTypes getBlockPathType(BlockGetter level,int x,int y,int z) {
                var pos=new BlockPos(x,y,z);var state=level.getBlockState(pos);
                // The support block matters for low machines, in particular moving belts.
                if(thaumcraft.api.golems.GolemPathHazards.contains(state)||thaumcraft.api.golems.GolemPathHazards.contains(level.getBlockState(pos.below())))return BlockPathTypes.BLOCKED;
                if(usable(state))return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN)
                        ?BlockPathTypes.DOOR_OPEN:BlockPathTypes.DOOR_WOOD_CLOSED;
                return super.getBlockPathType(level,x,y,z);
            }
        };
        routeFinder=new PathFinder(nodeEvaluator,nodes);
        return routeFinder;
    }
    @Override protected void followThePath() {
        if (!(mob instanceof GolemEntity golem) || !golem.configuration().axisAligned()) { super.followThePath(); return; }
        var position=getTempMobPos();
        if (path!=null && !path.isDone()) {
            var next=path.getNextEntityPos(mob);
            if (Math.abs(mob.getX()-next.x)<.12 && Math.abs(mob.getZ()-next.z)<.12 && Math.abs(mob.getY()-next.y)<1) path.advance();
        }
        doStuckDetection(position);
    }
}
