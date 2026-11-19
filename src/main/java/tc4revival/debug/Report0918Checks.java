package tc4revival.debug;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraftforge.registries.ForgeRegistries;
import tc4revival.content.*;

/** Uses real item attribute events and placed jars, without touching player data. */
public class Report0918Checks {
    public static int verify(CommandSourceStack source) {
        int equipment=0,jars=0;
        for(var item:ForgeRegistries.ITEMS) {
            boolean tool=item instanceof TieredItem tiered && tiered.getTier()==VoidToolTier.INSTANCE;
            if(!tool && !(item instanceof VoidArmorItem) && !(item instanceof VoidRobeArmorItem))continue;
            var stack=new ItemStack(item);
            require(stack.getAttributeModifiers(EquipmentSlot.OFFHAND).get(TCAttributes.WARP.get()).isEmpty(),"Offhand warp: "+ForgeRegistries.ITEMS.getKey(item));
            var slot=item instanceof net.minecraft.world.item.ArmorItem armor?armor.getEquipmentSlot():EquipmentSlot.MAINHAND;
            require(stack.getAttributeModifiers(slot).get(TCAttributes.WARP.get()).stream().anyMatch(m->m.getAmount()>0),"Equipped warp lost: "+ForgeRegistries.ITEMS.getKey(item));
            equipment++;
        }
        require(equipment>=12,"Void equipment coverage incomplete");
        var level=source.getLevel();var pos=new BlockPos(128,200,128);level.getChunkAt(pos);
        var backup=net.minecraftforge.common.util.BlockSnapshot.create(level.dimension(),level,pos);
        try {
            for(var block:ForgeRegistries.BLOCKS) {
                if(!(block instanceof EssentiaJarBlock))continue;
                level.setBlock(pos,block.defaultBlockState(),2);
                var origin=EssentiaSources.visualOrigin(level,pos);
                require(origin.x==pos.getX()+.5 && origin.z==pos.getZ()+.5 && Math.abs(origin.y-pos.getY()-.885)<1e-8,"Jar mouth origin: "+ForgeRegistries.BLOCKS.getKey(block));
                require(level.getBlockState(pos).getCollisionShape(level,pos).max(net.minecraft.core.Direction.Axis.Y)==1,"Jar collision rule changed unexpectedly");
                jars++;
            }
            level.setBlock(pos,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
            require(EssentiaSources.visualOrigin(level,pos).equals(pos.getCenter()),"Non-jar source origin changed");
        } finally { backup.restore(true,false); }
        require(jars>=2,"Normal and void jars not checked");
        String result="0918_1 passed: "+equipment+" void equipment items, no offhand warp; equipped warp preserved; "+jars+" jar origins at model mouth; collision and other origins unchanged.";
        source.sendSuccess(()->Component.literal(result),false);return 1;
    }
    private static void require(boolean condition,String message) { if(!condition)throw new IllegalStateException(message); }
}
