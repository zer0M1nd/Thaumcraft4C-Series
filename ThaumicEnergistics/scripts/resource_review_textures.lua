-- 0.2.6 correction layer; retain the delivered 0920_02 artwork as an immutable input.
local root=app.params['root']..'/'
local out=root..'art/resource-review-0.2.6/'
local runtime=out..'runtime/assets/thaumicenergistics/textures/modern/'
app.fs.makeAllDirectories(runtime..'drive');app.fs.makeAllDirectories(runtime..'part')
app.fs.makeAllDirectories(out..'aseprite')
local pc=app.pixelColor
local function save(sprite,name,target)
 sprite:saveAs(out..'aseprite/'..name..'.aseprite')
 sprite:saveCopyAs(target)
 sprite:close()
end
for _,tier in ipairs({'1k','4k','16k','64k','256k','1024k','4096k','16384k','quantum','singularity','creative'}) do
 local name='essentia_cell_'..tier
 local sprite=app.open(root..'art/0920_02-handoff/runtime/assets/thaumicenergistics/textures/modern/drive/'..name..'.png')
 local im=sprite.cels[1].image
 -- North UV runs in the opposite world-X direction: AE2's lamp at model X=4..5 maps to image X=1.
 local lamp=im:getPixel(4,3);local shell=im:getPixel(3,3)
 im:drawPixel(4,3,shell);im:drawPixel(1,3,lamp)
 save(sprite,name,runtime..'drive/'..name..'.png')
end
local sprite=Sprite(16,16,ColorMode.RGB)
sprite.layers[1].name='AE2 crafting grid, green; gray frame supplied by AE2 model'
local im=sprite.cels[1].image
-- Preserve the exact masks of AE2's crafting terminal, recoloring only its interior.
local colors={dark={35,72,36},medium={64,115,51},bright={143,186,106}}
for _,layer in ipairs({'dark','medium','bright'}) do
 local source=app.open(root..'references/AE2/src/main/resources/assets/ae2/textures/part/crafting_terminal_'..layer..'.png')
 local mask=Image(source);local color=colors[layer]
 for y=0,15 do for x=0,15 do
  local pixel=mask:getPixel(x,y);local a=pc.rgbaA(pixel)
  if a>0 then
   local intensity=pc.rgbaR(pixel)/255
   im:drawPixel(x,y,pc.rgba(math.floor(color[1]*intensity+.5),math.floor(color[2]*intensity+.5),math.floor(color[3]*intensity+.5),a))
  end
 end end
 source:close()
end
save(sprite,'terminal_green',runtime..'part/terminal_green.png')
print('Corrected 11 drive shells and AE2 green grid overlay')
