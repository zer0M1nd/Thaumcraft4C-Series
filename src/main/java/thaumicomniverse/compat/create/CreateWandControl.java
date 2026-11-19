package thaumicomniverse.compat.create;

import thaumcraft.api.wands.WandCasting;

/** Owns the persisted legacy flight marker through the public use-mode lifecycle. */
public final class CreateWandControl {
    private CreateWandControl() {}
    public static void setup(){WandCasting.registerUseMode("thaumicomniverse:flight",100,new WandCasting.UseMode(){
        public boolean matches(WandCasting.Request request){return request.wand().hasTag()&&request.wand().getTag().getBoolean("OmniFlyingUse");}
        public boolean enabled(WandCasting.Request request){return thaumicomniverse.RevivalFeatures.Group.CREATE.enabled();}
        public void finish(WandCasting.Request request,WandCasting.CancelReason reason){
            var stack=request.wand();if(!stack.hasTag())return;var tag=stack.getTag();
            try{
                if(request.player().getServer()!=null&&tag.hasUUID("OmniFlyingDepot"))
                    for(var level:request.player().getServer().getAllLevels())
                        if(level.getEntity(tag.getUUID("OmniFlyingDepot")) instanceof ArcaneMobileEntity entity){entity.finishControl();break;}
            }finally{tag.remove("OmniFlyingUse");tag.remove("OmniFlyingDepot");}
        }
    });}
}
