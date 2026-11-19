package tc4revival.content;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import tc4revival.aspect.AspectAmounts;

public  class NodeJarBlockEntity extends BlockEntity implements WandInteractable,RevealedAspectContainer,ScannableNode {
    private NodeJarData contents=new NodeJarData(new AspectAmounts(),AuraNodeType.NORMAL,null,"");
    private AspectAmounts base=new AspectAmounts();
    public boolean drop=true;
    public long animateUntil;
    public NodeJarBlockEntity(BlockPos pos,BlockState state) { this(TCArcane.NODE_JAR_ENTITY.get(),pos,state); }
    protected NodeJarBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state) { super(type,pos,state); }
    public NodeJarData contents() { return contents; }
    public void configure(NodeJarData data) { contents=data; base=data.capacity(); changed(); }
    private void changed() { setChanged(); if (level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3); }
    @Override public AspectAmounts aspects() { return contents.aspects(); }
    public AspectAmounts base() { return base.copy(); }
    @Override public AuraNodeType nodeType() { return contents.type(); }
    @Override public AuraNodeModifier nodeModifier() { return contents.modifier(); }
    @Override public String nodeId() { return contents.id(); }
    public int addToContainer(tc4revival.aspect.Aspect aspect,int amount) {
        if (amount<0) throw new IllegalArgumentException("Negative node transfer");
        var values=aspects(); int accepted=(int)Math.min(amount,Math.max(0,(long)base.amount(aspect)-values.amount(aspect)));
        values.add(aspect,accepted); contents=new NodeJarData(values,contents.type(),contents.modifier(),contents.id(),base); changed();
        return amount-accepted;
    }
    public boolean takeFromContainer(tc4revival.aspect.Aspect aspect,int amount) {
        if (amount<0) throw new IllegalArgumentException("Negative node transfer");
        var values=aspects(); if (values.amount(aspect)<amount) return false;
        values.remove(aspect,amount); contents=new NodeJarData(values,contents.type(),contents.modifier(),contents.id(),base); changed(); return true;
    }
    public boolean takeFromContainer(AspectAmounts requested) { return false; }
    public ItemStack asItem() { var stack=new ItemStack(TCItems.NODE_JAR.get()); stack.setTag(contents.write()); return stack; }
    @Override public AspectAmounts revealedAspects() { return contents.aspects(); }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); tc4revival.aspect.AspectNbt.write(tag,aspects());
        tc4revival.aspect.AspectNbt.write(tag,"AspectsBase",base);
        tag.putString("nodeId",contents.id()); tag.putByte("type",(byte)contents.type().ordinal());
        tag.putByte("modifier",contents.modifier()==null ? -1 : (byte)contents.modifier().ordinal());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("nodetype")) { contents=NodeJarData.read(tag); base=contents.capacity(); return; }
        var catalog=tc4revival.aspect.TCAspects.catalog();
        contents=new NodeJarData(tc4revival.aspect.AspectNbt.read(tag,catalog).amounts(),AuraNodeType.fromId(tag.getByte("type")),
                AuraNodeModifier.fromId(tag.contains("modifier") ? tag.getByte("modifier") : -1),tag.getString("nodeId"));
        base=tc4revival.aspect.AspectNbt.read(tag,"AspectsBase",catalog).amounts();
        if (base.isEmpty() && tag.getShort("nodeVisBase")>0) for (var aspect:aspects().aspects()) base.merge(aspect,tag.getShort("nodeVisBase"));
        contents=new NodeJarData(contents.aspects(),contents.type(),contents.modifier(),contents.id(),base);base=contents.capacity();
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public boolean triggerEvent(int id,int parameter) {
        if (id==9) {
            animateUntil=System.currentTimeMillis()+1000;
            if (level!=null && level.isClientSide) for (int x=-1;x<=1;x++) for (int y=-1;y<=2;y++) for (int z=-1;z<=1;z++)
                tc4revival.client.ClientFocusEffects.blockSparkle(level,worldPosition.offset(x,y,z),-9999,5);
            return true;
        }
        return super.triggerEvent(id,parameter);
    }
    @Override public InteractionResult onWandUseFirst(ItemStack wand,UseOnContext context) {
        var player=context.getPlayer();
        if (level==null || player==null) return InteractionResult.PASS;
        if (!level.isClientSide) {
            if (!level.mayInteract(player,worldPosition) || !player.mayUseItemAt(worldPosition,context.getClickedFace(),wand)) return InteractionResult.FAIL;
            var payload=contents; var original=getBlockState(); var originalData=saveWithoutMetadata(); drop=false;
            if (!level.setBlock(worldPosition,TCArcane.AURA_NODE.get().defaultBlockState(),3)) { drop=true; return InteractionResult.FAIL; }
            if (!(level.getBlockEntity(worldPosition) instanceof AuraNodeBlockEntity node)) {
                level.setBlock(worldPosition,original,3);
                if (level.getBlockEntity(worldPosition) instanceof NodeJarBlockEntity restored) { restored.load(originalData); restored.changed(); }
                return InteractionResult.FAIL;
            }
            node.configure(payload.capacity(),payload.type(),payload.modifier(),payload.id(),true);
            for(var aspect:payload.aspects().aspects())node.addCharge(aspect,payload.aspects().amount(aspect));
            level.levelEvent(2001,worldPosition,net.minecraft.world.level.block.Block.getId(original));
            level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.GLASS_BREAK,net.minecraft.sounds.SoundSource.BLOCKS,1,.9f+level.random.nextFloat()*.2f);
        }
        player.swing(context.getHand()); return InteractionResult.SUCCESS;
    }
    @Override public Optional<ItemStack> onWandUse(Level level,ItemStack wand,Player player) { return Optional.of(wand); }
    @Override public void onWandUseTick(ItemStack wand,Player player,int remaining) {}
    @Override public void onWandRelease(ItemStack wand,Level level,Player player,int remaining) {}
}
