package tc4revival.resources;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import static org.junit.jupiter.api.Assertions.*;

class ClientComputationBoundaryTest {
    @Test void localWandCatalogUsesItsOwnMappings()throws Exception{
        try(var input=getClass().getResourceAsStream("/tc4revival/research/BrowserRecipes.class")){
            var type=new ClassNode();new ClassReader(input).accept(type,0);
            for(var method:type.methods)if(method.name.equals("legacy")||method.name.equals("arcane"))
                for(var instruction:method.instructions)if(instruction instanceof MethodInsnNode call)
                    assertNotEquals("tc4revival/aspect/item/LegacyItemMappings",call.owner,"Client catalog cannot read server reload globals");
        }
    }
    @Test void clientDoesNotRequestTablesOrCatalogsAndServerDoesNotRebroadcastVanillaInputs()throws Exception{
        for(String name:new String[]{"client/ClientItemAspects","compat/jei/ThaumcraftJei","aspect/sync/ServerAspectData"}){
            try(var input=getClass().getResourceAsStream("/tc4revival/"+name+".class")){
                assertNotNull(input);var type=new ClassNode();new ClassReader(input).accept(type,0);
                for(var method:type.methods)for(var instruction:method.instructions){
                    if(instruction instanceof TypeInsnNode allocation){
                        assertNotEquals("tc4revival/network/AspectDataRequest",allocation.desc);
                        assertNotEquals("tc4revival/network/BrowserRecipesRequest",allocation.desc);
                        assertNotEquals("net/minecraft/network/protocol/game/ClientboundUpdateRecipesPacket",allocation.desc);
                        assertNotEquals("net/minecraft/network/protocol/game/ClientboundUpdateTagsPacket",allocation.desc);
                    }
                    if(instruction instanceof MethodInsnNode call&&name.equals("client/ClientItemAspects")){
                        assertNotEquals("tc4revival/compat/jei/JeiInferenceExport",call.owner,"Authoritative inference cannot mix live client recipes or candidates");
                        assertFalse(call.owner.equals("tc4revival/aspect/sync/AspectDataSet")&&(call.name.equals("merge")||call.name.equals("missing")));
                    }
                }
            }
        }
    }
}
