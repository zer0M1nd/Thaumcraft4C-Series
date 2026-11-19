package tc4revival.content;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.*;
public  class LegacyOreBlock extends Block {
    private final int metadata;
    public LegacyOreBlock(int metadata){this(metadata,Properties.of().strength(1.5f,5).sound(SoundType.STONE).requiresCorrectToolForDrops());}
    protected LegacyOreBlock(int metadata,Properties properties){super(properties);this.metadata=metadata;}
    public int metadata(){return metadata;}
    public static String path(int metadata){return LegacyOreNames.path(metadata);}
    public static String tag(int metadata){return "forge:ores/"+(metadata==0?"cinnabar":metadata==7?"amber":"infused_"+ShardType.values()[metadata-1].material());}
    @Override public int getExpDrop(BlockState state,net.minecraft.world.level.LevelReader level,net.minecraft.util.RandomSource random,BlockPos pos,int fortune,int silkTouch){return silkTouch>0||metadata==0?0:random.nextInt(4)+(metadata==7?1:0);}
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientBlockExtensions> consumer){consumer.accept(new net.minecraftforge.client.extensions.common.IClientBlockExtensions(){
        @Override public boolean addHitEffects(BlockState state,net.minecraft.world.level.Level level,net.minecraft.world.phys.HitResult hit,net.minecraft.client.particle.ParticleEngine engine){if(metadata>0&&metadata<6&&hit instanceof net.minecraft.world.phys.BlockHitResult blockHit)tc4revival.client.ClientFocusEffects.infusedOreHit(level,blockHit.getBlockPos(),ShardType.values()[metadata-1]);return false;}
    });}
}
