local root='E:/AICoding/TC4Revival/Addons/ThaumicEnergistics/'
local legacy=root..'.tooling/visual-redesign-reference/legacy/assets/thaumicenergistics/textures/'
local ae=root..'.tooling/ae2-source/Applied-Energistics-2-forge-v15.4.8/src/main/resources/assets/ae2/textures/'
local paths={
 legacy..'items/essentia.cell.1k.png',legacy..'items/essentia.cell.4k.png',legacy..'items/essentia.cell.16k.png',legacy..'items/essentia.cell.64k.png',
 legacy..'items/storage.component.1k.png',legacy..'items/storage.component.4k.png',legacy..'items/storage.component.16k.png',legacy..'items/storage.component.64k.png',
 ae..'item/item_storage_cell_1k.png',ae..'item/fluid_storage_cell_1k.png',ae..'item/item_storage_cell_64k.png',ae..'item/storage_cell_led.png',
 ae..'item/cell_component_1k.png',ae..'item/cell_component_4k.png',ae..'item/cell_component_16k.png',ae..'item/cell_component_64k.png',
 legacy..'blocks/parts/arcane.crafting.overlay1.png',legacy..'blocks/parts/arcane.crafting.overlay2.png',legacy..'blocks/parts/arcane.crafting.side.png',legacy..'blocks/parts/vis.interface.png',
 ae..'part/crafting_terminal.png',ae..'part/interface.png',ae..'item/blank_pattern.png',ae..'item/crafting_pattern.png',
 legacy..'models/arcane.assembler.png',legacy..'models/gearbox.iron.png',legacy..'models/gearbox.thaumium.png',ae..'block/molecular_assembler.png',
 ae..'block/drive/cells/1k_item_cell.png',ae..'block/drive/cells/1k_fluid_cell.png',ae..'item/cell_component_256k.png',ae..'item/blank_pattern.png',
 root..'.tooling/palette-reference/1k.png',root..'.tooling/palette-reference/4k.png',root..'.tooling/palette-reference/16k.png',root..'.tooling/palette-reference/64k.png',
 root..'.tooling/visual-redesign-reference/mega-cell_component_1m.png',root..'.tooling/visual-redesign-reference/mega-cell_component_4m.png',root..'.tooling/visual-redesign-reference/mega-cell_component_16m.png'
}
local s=Sprite(1024,640,ColorMode.RGB)
local im=Image(1024,640,ColorMode.RGB)
im:clear(app.pixelColor.rgba(53,57,64,255))
for i,p in ipairs(paths) do
 if app.fs.isFile(p) then
 local opened=app.open(p);app.command.ChangePixelFormat{format='rgb'}
 local src=Image(opened);opened:close();local scale=math.floor(112/math.max(src.width,src.height))
  local ox=((i-1)%8)*128+8;local oy=math.floor((i-1)/8)*128+8
  for y=0,src.height-1 do for x=0,src.width-1 do
   local c=src:getPixel(x,y)
   if app.pixelColor.rgbaA(c)>0 then
    for dy=0,scale-1 do for dx=0,scale-1 do im:drawPixel(ox+x*scale+dx,oy+y*scale+dy,c) end end
   end
  end end
 else print('Missing reference '..p) end
end
s.cels[1].image=im
s:saveAs(root..'.tooling/visual-redesign-reference/reference-sheet.png')
print('Reference sheet saved')
