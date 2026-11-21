package thaumcraft.api.client;

import java.util.*;
import tc4revival.aspect.*;
import thaumcraft.api.aspects.AspectViews;

/** Immutable reader snapshot is safe for recipe viewer indexing threads. */
public final class ClientAspectViews {
    private ClientAspectViews() {}
    public static AspectViews.Reader reader(){return tc4revival.client.ClientKnowledge.presentationReader();}
    public static boolean visible(String tag){return AspectViews.visible(reader(),tag);}
    public static AspectViews.Appearance view(Aspect aspect){return AspectViews.appearance(reader(),aspect);}
    public static net.minecraft.resources.ResourceLocation texture(String tag){return view(TCAspects.catalog().require(tag)).texture();}
    public static AspectAmounts project(AspectAmounts input,AspectViews.Purpose purpose){return AspectViews.project(reader(),input,purpose);}
    public static List<Aspect> primals(){return TCAspects.catalog().primals().stream().filter(a->visible(a.tag())).toList();}
    public static net.minecraft.network.chat.Component unknownContents(Collection<String> tags){return AspectViews.unknownContents(reader(),tags);}
}
