package fmmodern;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.*;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.ModList;

/** Item-aspect JSON validates item existence, so optional items live in dependency-gated builtin packs. */
public final class IntegrationPacks {
    public static void add(AddPackFindersEvent event){
        if(event.getPackType()!=PackType.SERVER_DATA)return;
        for(String mod:new String[]{"bloodmagic","botania","thaumic_tinkerer","lust","aspect_inputs","twilightforest","twilight_lust"}){
            if(mod.equals("lust")){if(ForbiddenConfig.value(ForbiddenConfig.NO_LUST))continue;}
            else if(mod.equals("aspect_inputs")){}
            else if(mod.equals("twilight_lust")){if(!ModList.get().isLoaded("twilightforest")||!ForbiddenConfig.value(ForbiddenConfig.CROSS_MOD)||!ForbiddenConfig.value(ForbiddenConfig.TWILIGHT)||ForbiddenConfig.value(ForbiddenConfig.NO_LUST))continue;}
            else if(!ModList.get().isLoaded(mod)||!ForbiddenConfig.value(ForbiddenConfig.CROSS_MOD))continue;
            if(mod.equals("twilightforest")&&!ForbiddenConfig.value(ForbiddenConfig.TWILIGHT)||mod.equals("botania")&&!fmmodern.ForbiddenConfig.value(ForbiddenConfig.BOTANIA)||mod.equals("bloodmagic")&&!fmmodern.ForbiddenConfig.value(ForbiddenConfig.BLOOD_MAGIC)||mod.equals("thaumic_tinkerer")&&!fmmodern.ForbiddenConfig.value(ForbiddenConfig.TINKERER))continue;
            var root=ModList.get().getModFileById(ForbiddenMagic.ID).getFile().findResource("optional_packs",mod);
            String id="forbidden_magic/"+mod;
            Pack.ResourcesSupplier supplier=key->mod.equals("aspect_inputs")?new AspectInputPack(key,root):new PathPackResources(key,root,true);
            var info=Pack.readPackInfo(id,supplier);if(info==null)throw new IllegalStateException("Missing integration pack "+id);
            var pack=Pack.create(id,Component.literal("Forbidden Magic: "+mod),true,supplier,info,PackType.SERVER_DATA,Pack.Position.BOTTOM,true,PackSource.BUILT_IN);
            event.addRepositorySource(consumer->consumer.accept(pack));
        }
    }
    private IntegrationPacks(){}
}
