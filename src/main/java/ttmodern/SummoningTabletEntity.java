package ttmodern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.content.ArcanePedestalBlockEntity;

public final class SummoningTabletEntity extends BlockEntity {
    public SummoningTabletEntity(BlockPos pos,BlockState state){super(Tinkerer.SUMMONING_TABLET_ENTITY.get(),pos,state);}
    public void tick(){if(!(level instanceof ServerLevel server)||server.getGameTime()%300!=0||server.hasNeighborSignal(worldPosition))return;attempt(server);}
    public boolean attempt(ServerLevel server){
        if(server.getBlockEntity(worldPosition)!=this||server.hasNeighborSignal(worldPosition))return false;
        var found=new ArrayList<ArcanePedestalBlockEntity>();
        for(int radius=1;radius<=5;radius++){
            for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
                if(radius>1&&Math.abs(x)<radius&&Math.abs(z)<radius)continue;var pos=worldPosition.offset(x,0,z);if(!server.hasChunkAt(pos))continue;
                if(server.getBlockEntity(pos) instanceof ArcanePedestalBlockEntity pedestal&&pedestal.getItem(0).getItem() instanceof MobEssenceItem)found.add(pedestal);
            }
            for(int a=0;a<found.size();a++)for(int b=a+1;b<found.size();b++)for(int c=b+1;c<found.size();c++){
                var pedestals=List.of(found.get(a),found.get(b),found.get(c));var stacks=pedestals.stream().map(p->p.getItem(0).copy()).toList();var types=stacks.stream().map(s->((MobEssenceItem)s.getItem()).aspect()).toList();
                var recipe=MobEssenceCatalog.recipes().stream().filter(r->r.matches(types)).findFirst().orElse(null);if(recipe==null)continue;
                boolean permanent=stacks.stream().allMatch(s->((MobEssenceItem)s.getItem()).grade()==2);
                if(permanent&&(server.getGameTime()%1200!=0||stacks.stream().anyMatch(s->cooldown(s,server.getGameTime()))))continue;
                var entityType=net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(recipe.entity());var created=entityType.create(server);if(!(created instanceof Mob mob))return false;
                mob.moveTo(worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,0,0);
                mob.finalizeSpawn(server,server.getCurrentDifficultyAt(worldPosition),MobSpawnType.MOB_SUMMONED,null,null);
                if(mob instanceof net.minecraft.world.entity.monster.Slime slime)slime.setSize(1,true);
                if(mob instanceof net.minecraft.world.entity.monster.WitherSkeleton)mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(net.minecraft.world.item.Items.STONE_SWORD));
                // Hooks invoked by addFreshEntity can cancel. Never debit before the world accepts the entity.
                if(!server.addFreshEntity(mob))return false;
                // A spawn hook may modify/remove pedestals. Abort without consuming new contents.
                for(int i=0;i<3;i++)if(server.getBlockEntity(pedestals.get(i).getBlockPos())!=pedestals.get(i)||!ItemStack.matches(stacks.get(i),pedestals.get(i).getItem(0))){mob.discard();return false;}
                for(var pedestal:pedestals){var stack=pedestal.getItem(0);if(permanent){var tag=stack.getOrCreateTag();tag.putLong("SummonedAt",server.getGameTime());tag.putString("SummonDimension",server.dimension().location().toString());tag.putLong("SummonTablet",worldPosition.asLong());pedestal.setChanged();pedestal.sync();}else pedestal.removeItem(0,1);}
                for(int index=0;index<pedestals.size();index++)thaumcraft.api.fx.LegacyEffects.emit(level,"essentia",pedestals.get(index).getBlockPos().getCenter().add(0,.5,0),worldPosition.getCenter(),tc4revival.aspect.TCAspects.catalog().require(types.get(index)).color(),2,20,1,0,0);
                mob.playAmbientSound();return true;
            }
        }return false;
    }
    private static boolean cooldown(ItemStack stack,long now){return stack.hasTag()&&stack.getTag().contains("SummonedAt")&&now-stack.getTag().getLong("SummonedAt")<1200;}
}
