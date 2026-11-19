package tc4revival.content;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;

/** Each flattened material item retains the old portable golem customization tags. */
public  class GolemPlacerItem extends Item {
    private final GolemMaterial material;
    private final Supplier<EntityType<GolemEntity>> entityType;
    public GolemPlacerItem(Properties properties,GolemMaterial material,Supplier<EntityType<GolemEntity>> entityType) {
        super(properties.stacksTo(1)); this.material=material; this.entityType=entityType;
    }
    public GolemMaterial material() { return material; }
    @Override public void appendHoverText(ItemStack stack,Level level,java.util.List<net.minecraft.network.chat.Component> tooltip,net.minecraft.world.item.TooltipFlag flag) {
        if (!stack.hasTag()) return;
        var tag=stack.getTag();
        if (tag.contains("core")) tooltip.add(net.minecraft.network.chat.Component.translatable("item.ItemGolemCore.name").append(": ").append(net.minecraft.network.chat.Component.translatable("item.ItemGolemCore."+tag.getByte("core")+".name").withStyle(net.minecraft.ChatFormatting.GOLD)));
        if (tag.contains("advanced")) tooltip.add(net.minecraft.network.chat.Component.translatable("tc.adv"));
        if (tag.contains("upgrades")) {
            var line=net.minecraft.network.chat.Component.empty().withStyle(net.minecraft.ChatFormatting.BLUE);
            for (byte upgrade:tag.getByteArray("upgrades")) if (upgrade>=0) line.append(net.minecraft.network.chat.Component.translatable("item.ItemGolemUpgrade."+upgrade+".name")).append(" ");
            tooltip.add(line);
        }
        if (tag.contains("markers")) tooltip.add(net.minecraft.network.chat.Component.literal(tag.getList("markers",Tag.TAG_COMPOUND).size()+" ").append(net.minecraft.network.chat.Component.translatable("tc.markedloc")).withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
        if (tag.contains("deco")) {
            var line=net.minecraft.network.chat.Component.empty().withStyle(net.minecraft.ChatFormatting.DARK_GREEN);
            for (int i=0;i<8;i++) if (tag.getString("deco").indexOf("HGBFRVPM".charAt(i))>=0) line.append(net.minecraft.network.chat.Component.translatable("item.ItemGolemDecoration."+i+".name")).append(" ");
            tooltip.add(line);
        }
    }
    @Override public boolean doesSneakBypassUse(ItemStack stack,LevelReader level,BlockPos pos,Player player) { return true; }
    @Override public InteractionResult onItemUseFirst(ItemStack stack,UseOnContext context) {
        var player=context.getPlayer(); Level level=context.getLevel();
        if (player==null || player.isShiftKeyDown() || level.isClientSide) return InteractionResult.PASS;
        var clicked=context.getClickedPos(); var face=context.getClickedFace(); var destination=clicked.relative(face);
        var block=level.getBlockState(clicked);
        double offset=face==Direction.UP && block.is(net.minecraft.tags.BlockTags.FENCES)?.5:0;
        var golem=entityType.get().create(level);
        if (golem!=null) {
            golem.moveTo(destination.getX()+.5,destination.getY()+offset,destination.getZ()+.5,level.random.nextFloat()*360,0);
            restore(golem,stack,material,BlockPos.containing(golem.position()),face,player.getGameProfile().getName());
            if (level.addFreshEntity(golem)) {
                golem.playAmbientSound();
                if (!player.getAbilities().instabuild) stack.shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }
    public static void restore(GolemEntity golem,ItemStack stack,GolemMaterial material,BlockPos home,Direction face,String owner) {
        CompoundTag source=stack.hasTag()?stack.getTag():new CompoundTag();
        var configTag=new CompoundTag();
        configTag.putByte("GolemType",(byte)material.ordinal());
        configTag.putBoolean("advanced",source.contains("advanced"));
        if (source.contains("core")) configTag.putByte("Core",source.getByte("core"));
        configTag.putByteArray("upgrades",source.getByteArray("upgrades"));
        configTag.putString("Decoration",source.getString("deco"));
        configTag.putByte("toggles",source.getByte("toggles"));
        configTag.putBoolean("AxisAligned",source.getBoolean("AxisAligned"));
        for(String key:java.util.List.of("AddonMaterial","AddonTask","BlockAppearance","AddonData"))if(source.contains(key))configTag.put(key,source.get(key).copy());
        var config=GolemConfiguration.load(configTag);
        golem.state().configure(config); golem.state().home(home,face); golem.state().owner(owner);
        golem.restrictTo(home,32);
        golem.markers().load(source.getList("markers",Tag.TAG_COMPOUND));
        var samples=new CompoundTag(); samples.put("Inventory",source.getList("Inventory",Tag.TAG_COMPOUND).copy());
        samples.putByteArray("colors",source.getByteArray("colors"));
        golem.filters().load(samples,config); golem.refreshConfiguration(); golem.setHealth(golem.getMaxHealth());
        if (stack.hasCustomHoverName()) golem.setCustomName(stack.getHoverName());
    }
    public boolean placeMoving(ItemStack stack,Player player,thaumcraft.api.golems.GolemAnchor anchor,net.minecraft.world.phys.Vec3 center,Direction face){
        if(player.level().isClientSide)return true;
        var golem=entityType.get().create(player.level());if(golem==null)return false;
        var destination=center.add(net.minecraft.world.phys.Vec3.atLowerCornerOf(face.getNormal()).scale(.8));
        golem.moveTo(destination.x,destination.y,destination.z,player.getYRot(),0);
        restore(golem,stack,material,BlockPos.containing(center).relative(face),face,player.getGameProfile().getName());
        golem.state().homeAnchor(anchor);
        if(!player.level().addFreshEntity(golem))return false;
        golem.playAmbientSound();if(!player.getAbilities().instabuild)stack.shrink(1);return true;
    }
}
