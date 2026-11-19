package gadomancymodern.item;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

/** Legacy package format, with bounded decoding before allocating nested item data. */
public class PackageCodec {
    public static final int COMPRESSED_LIMIT=65536;
    public static final int EXPANDED_LIMIT=2*1024*1024;
    public static Optional<byte[]> encode(List<ItemStack> contents){
        if(contents.size()>1024)return Optional.empty();
        List<ItemStack> merged=new ArrayList<>();
        for(ItemStack input:contents){
            if(input.isEmpty())continue;
            if(input.getItem() instanceof ArcanePackageItem){
                Optional<List<ItemStack>> nested=decode(input.getTag()==null?new byte[0]:input.getTag().getByteArray("items"));
                if(nested.isEmpty()||nested.get().stream().anyMatch(i->i.getItem() instanceof ArcanePackageItem))return Optional.empty();
            }
            ItemStack rest=input.copy();
            for(ItemStack existing:merged){
                if(ItemStack.isSameItemSameTags(existing,rest)){
                    int moved=Math.min(rest.getCount(),existing.getMaxStackSize()-existing.getCount());
                    existing.grow(moved);rest.shrink(moved);
                }
                if(rest.isEmpty())break;
            }
            while(!rest.isEmpty()){int count=Math.min(rest.getMaxStackSize(),rest.getCount());merged.add(rest.copyWithCount(count));rest.shrink(count);}
            if(merged.size()>1024)return Optional.empty();
        }
        merged.sort(Comparator.comparing(i->i.save(new CompoundTag()).toString()));
        ListTag list=new ListTag();merged.forEach(i->list.add(i.save(new CompoundTag())));
        CompoundTag root=new CompoundTag();root.put("data",list);
        try{
            ByteArrayOutputStream raw=new ByteArrayOutputStream();NbtIo.write(root,new DataOutputStream(raw));
            if(raw.size()>EXPANDED_LIMIT)return Optional.empty();
            ByteArrayOutputStream compressed=new ByteArrayOutputStream();NbtIo.writeCompressed(root,compressed);
            return compressed.size()>COMPRESSED_LIMIT?Optional.empty():Optional.of(compressed.toByteArray());
        }catch(IOException ex){return Optional.empty();}
    }
    public static Optional<List<ItemStack>> decode(byte[] bytes){
        if(bytes.length==0||bytes.length>COMPRESSED_LIMIT)return Optional.empty();
        try{
            byte[] raw;
            try(var gzip=new GZIPInputStream(new ByteArrayInputStream(bytes))){raw=gzip.readNBytes(EXPANDED_LIMIT+1);}
            if(raw.length>EXPANDED_LIMIT)return Optional.empty();
            CompoundTag root=NbtIo.read(new DataInputStream(new ByteArrayInputStream(raw)),new NbtAccounter(EXPANDED_LIMIT));
            if(root==null||!root.contains("data",Tag.TAG_LIST))return Optional.empty();
            ListTag list=root.getList("data",Tag.TAG_COMPOUND);if(list.size()>1024)return Optional.empty();
            List<ItemStack> result=new ArrayList<>();
            for(int i=0;i<list.size();i++){ItemStack stack=ItemStack.of(list.getCompound(i));if(!stack.isEmpty())result.add(stack);}
            return Optional.of(List.copyOf(result));
        }catch(IOException|RuntimeException ex){return Optional.empty();}
    }
}
