-- Native Aseprite authoring. Keep AE2 outlines and draw only the specified regions.
local root=app.params['root']..'/'
local art=root..'art/report0921_1/'
local out=art..'runtime/assets/thaumicenergistics/textures/modern/'
app.fs.makeAllDirectories(out..'item');app.fs.makeAllDirectories(out..'block');app.fs.makeAllDirectories(art..'aseprite')
local pc=app.pixelColor
local function save(s,name,folder)
 s:saveAs(art..'aseprite/'..name..'.aseprite');s:saveCopyAs(out..folder..'/'..name..'.png');s:close()
end
local s=app.open(root..'references/AE2/src/main/resources/assets/ae2/textures/item/wireless_crafting_terminal.png')
app.command.ChangePixelFormat{format='rgb'}
local im=s.cels[1].image
for y=0,15 do for x=0,15 do
 local p=im:getPixel(x,y);local r,g,b,a=pc.rgbaR(p),pc.rgbaG(p),pc.rgbaB(p),pc.rgbaA(p)
 -- Purple display only: antennas, chassis, highlights and transparent outline remain unchanged.
 if a>0 and b>g*1.08 and r>g*1.08 then
  local light=math.max(r,b)/255
  im:drawPixel(x,y,pc.rgba(math.floor(143*light),math.floor(186*light),math.floor(106*light),a))
 end
end end
save(s,'wireless_arcane_terminal','item')
s=app.open(root..'references/AE2/src/main/resources/assets/ae2/textures/item/advanced_card.png')
app.command.ChangePixelFormat{format='rgb'}
im=s.cels[1].image
-- Retain the advanced circuit board; its center bears a small green vis rune like AE upgrade-card markings.
local dark=pc.rgba(35,72,36,255);local bright=pc.rgba(143,186,106,255);local mid=pc.rgba(64,115,51,255)
for y=5,10 do for x=4,8 do
 local dx,dy=math.abs(x-6),math.abs(y-7.5)
 if dx+dy<=3 then im:drawPixel(x,y,dx+dy>2 and dark or mid) end
end end
for _,p in ipairs({{6,5},{6,6},{5,7},{6,7},{7,8},{6,9},{6,10}}) do im:drawPixel(p[1],p[2],bright) end
save(s,'vis_charging_card','item')
local reference=app.open(root..'references/AE2/src/main/resources/assets/ae2/textures/block/molecular_assembler.png')
app.command.ChangePixelFormat{format='rgb'}
local native=Image(reference);s=Sprite(16,16,ColorMode.RGB);im=s.cels[1].image
for y=0,15 do for x=0,15 do if x==0 or y==0 or x==15 or y==15 then
 local p=native:getPixel(x,y);local l=(pc.rgbaR(p)+pc.rgbaG(p)+pc.rgbaB(p))/3
 -- AE2's outer rim is already very dark (31..66). Multiplication crushed it
 -- into near-black. Map that relief into the actual thaumium ingot midtones.
 local t=math.max(0,math.min(1,(l-31)/35))
 im:drawPixel(x,y,pc.rgba(math.floor(79+37*t),math.floor(65+34*t),math.floor(121+50*t),255))
end end end
reference:close();save(s,'arcane_outer_rim','block')
print('Authored wireless terminal, vis card and strictly one-pixel outer rim')
