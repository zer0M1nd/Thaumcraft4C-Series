local root='E:/AICoding/TC4Revival/Addons/ThaumicEnergistics/'
local dir=root..'src/main/resources/assets/thaumicenergistics/textures/modern/'
local paths={};for _,p in ipairs(app.fs.listFiles(dir)) do if p:match('%.png$') then table.insert(paths,p) end end;table.sort(paths)
local s=Sprite(768,math.ceil(#paths/8)*96,ColorMode.RGB);local im=Image(s.width,s.height,ColorMode.RGB)
im:clear(app.pixelColor.rgba(53,57,64,255))
for i,p in ipairs(paths) do
 local pic=Image{fromFile=dir..p};local ox=((i-1)%8)*96+8;local oy=math.floor((i-1)/8)*96+8
 for y=0,15 do for x=0,15 do local c=pic:getPixel(x,y)
  if app.pixelColor.rgbaA(c)>0 then for dx=0,4 do for dy=0,4 do im:drawPixel(ox+x*5+dx,oy+y*5+dy,c) end end end
 end end
 print(i..' '..p)
end
s.cels[1].image=im;s:saveAs(root..'.tooling/visual-redesign-reference/authored-sheet.png');s:close()
