-- Original pixel artwork authored in Aseprite. References are never copied into the output.
local root='E:/AICoding/TC4Revival/Addons/ThaumicEnergistics/'
local out=root..'src/main/resources/assets/thaumicenergistics/textures/modern/'
local source=root..'art/aseprite/'
app.fs.makeAllDirectories(out);app.fs.makeAllDirectories(source)
local function rgba(hex,a)
 return app.pixelColor.rgba(tonumber(hex:sub(1,2),16),tonumber(hex:sub(3,4),16),tonumber(hex:sub(5,6),16),a or 255)
end
local im
local function px(x,y,c,a) if x>=0 and y>=0 and x<im.width and y<im.height then im:drawPixel(x,y,rgba(c,a)) end end
local function rect(x,y,w,h,c,a) for yy=y,y+h-1 do for xx=x,x+w-1 do px(xx,yy,c,a) end end end
local function line(x,y,x2,y2,c)
 local steps=math.max(math.abs(x2-x),math.abs(y2-y));if steps==0 then px(x,y,c);return end
 for i=0,steps do px(math.floor(x+(x2-x)*i/steps+.5),math.floor(y+(y2-y)*i/steps+.5),c) end
end
local function poly(points,c)
 for y=0,im.height-1 do
  local intersections={}
  for i,p in ipairs(points) do local q=points[i%#points+1]
   if (p[2]<=y and q[2]>y) or (q[2]<=y and p[2]>y) then
    table.insert(intersections,p[1]+(y-p[2])*(q[1]-p[1])/(q[2]-p[2]))
   end
  end
  table.sort(intersections)
  for i=1,#intersections-1,2 do for x=math.ceil(intersections[i]),math.floor(intersections[i+1]) do px(x,y,c) end end
 end
 for i,p in ipairs(points) do local q=points[i%#points+1];line(p[1],p[2],q[1],q[2],c) end
end
local function start() im=Image(16,16,ColorMode.RGB);im:clear() end
local function save(name,layer)
 local s=Sprite(16,16,ColorMode.RGB);s.layers[1].name=layer or name;s.cels[1].image=im
 s:saveAs(source..name..'.aseprite');s:saveAs(out..name..'.png');s:close()
end
-- Five low tiers follow legacy AE capacity colours. The mega tiers use violet circuits
-- and increasingly dense layouts, as in MEGACells, with distinct accent shades.
local tiers={
 {'1k','ba582e','f39b59','793625'}, {'4k','ded54a','ffffa0','9d9634'},
 {'16k','46bd4d','a0f589','266d3f'}, {'64k','38c6d5','a0f8f8','28778b'},
 {'256k','5c91ed','b4dafa','345792'}, {'1024k','8451d2','cda5fa','4c2f82'},
 {'4096k','b35bea','e6b7ff','70389b'}, {'16384k','ce63b2','ffd0ed','823878'},
 {'quantum','79daca','d6fff4','386d70'}, {'singularity','403b63','9985c2','242135'},
 {'creative','d1a655','fff0b1','7f603b'}
}
for i,t in ipairs(tiers) do
 start()
 -- Chamfered metal cartridge, with separate lit top and two dark thickness faces.
 poly({{7,0},{15,7},{15,10},{9,15},{6,15},{0,9},{0,6}},'393344')
 poly({{1,7},{8,12},{8,14},{1,9}},'857296')
 poly({{8,12},{14,7},{14,10},{9,14},{8,14}},'69587b')
 poly({{7,1},{14,7},{8,13},{1,7}},'b7a6cc')
 line(7,1,1,7,'e1d4ed');line(7,1,14,7,'cabcdb')
 line(1,7,8,13,'9987ae');line(8,13,14,7,'8b789f')
 poly({{7,3},{11,6},{8,9},{4,6}},'534763')
 poly({{7,4},{10,6},{8,8},{5,6}},t[2]);line(7,4,9,6,t[3]);px(8,8,t[4])
 line(3,8,6,10,'dbceea');line(6,11,8,12,'cabcdb')
 line(10,10,12,8,'8f7aa8');px(12,7,'dacde5')
 -- Colour strip on the side is capacity, not the independently tinted activity LED.
 line(1,7,1,9,t[4]);line(2,8,2,10,t[2]);px(2,8,t[3])
 px(9,12,'2d2935');px(9,13,'534763')
 if i>=6 then px(5,11,'efe4ff');px(4,10,'705387') end
 if i>=7 then px(4,11,'d9beef') end
 if i>=8 then px(3,10,'d9beef') end
 if t[1]=='quantum' then px(7,5,'dbfff9');px(8,6,'dbfff9');px(7,7,'dbfff9') end
 if t[1]=='singularity' then px(7,5,'222035');px(8,6,'171523');px(7,7,'8a71b8') end
 if t[1]=='creative' then px(6,6,'e07885');px(8,5,'75d6b0');px(9,6,'a2c6ff') end
 save('essentia_cell_'..t[1],'Lavender casing and '..t[1]..' capacity inset')
 -- The drive front uses AE2's native six-by-two slot, leaving its LED to AE2.
 start();rect(0,0,16,16,'9987ae');rect(0,0,6,2,'cabcdb');rect(0,2,6,2,'ac98c2')
 rect(0,4,6,2,'69587b');line(0,2,5,2,'d8c9e8');px(0,3,t[2]);px(1,3,t[3]);px(5,3,'77618e')
 save('drive_cell_'..t[1],'Cartridge front; LED rendered separately by AE2')
 if i<=8 then
  start()
  poly({{5,1},{10,1},{14,5},{14,10},{10,14},{5,14},{1,10},{1,5}},'37363e')
  poly({{5,2},{10,2},{13,5},{13,10},{10,13},{5,13},{2,10},{2,5}},'736d80')
  line(5,2,10,2,'b4a9c4');line(2,5,2,10,'a59bb3');line(5,13,10,13,'4e475e')
  rect(6,0,4,4,'c8b9db');rect(6,12,4,4,'8e7da5');rect(0,6,4,4,'b4a1ca');rect(12,6,4,4,'8e7da5')
  rect(7,0,2,4,t[2]);rect(7,12,2,4,t[4]);rect(0,7,4,2,t[2]);rect(12,7,4,2,t[4])
  rect(4,4,8,8,'272b31');line(4,4,11,4,'9990a3');line(4,4,4,11,'9990a3')
  local count=({1,2,4,6,9,1,4,9})[i]
  if count==1 then rect(6,6,4,4,'ac8ace');rect(7,7,2,2,'8d4edb');px(7,7,'d8b5ff')
  elseif count==2 then for _,x in ipairs({5,9}) do rect(x,6,2,4,'9861d8');px(x,6,'dab4ff') end
  else
   local coords=count==4 and {5,9} or {5,7,9}
   local n=0
   for _,y in ipairs(coords) do for _,x in ipairs(coords) do if n<count then rect(x,y,2,2,'9460cf');px(x,y,'cc9efd');n=n+1 end end end
  end
  if i>=6 then line(3,3,12,3,t[2]);line(3,12,12,12,t[4]);px(3,3,t[3]);px(12,3,t[3]) end
  save('storage_component_'..t[1],'Purple essentia cores, tier traces and metal carrier')
 end
end
start();px(9,12,'ffffff');save('cell_led','Independent status tint mask')
-- Empty housing and the arcane counterpart of AE2's encoded pattern.
for _,name in ipairs({'storage_casing','encoded_arcane_pattern'}) do
 start();poly({{7,1},{15,8},{9,15},{6,15},{0,9},{0,7}},'4e465b')
 poly({{7,2},{14,8},{8,14},{1,8}},name=='storage_casing' and 'b7a6cc' or 'c9cfd1')
 line(7,2,1,8,'f0e6f7');line(7,2,14,8,'ddd7e5');line(1,9,7,14,'9886ad')
 poly({{7,4},{11,7},{8,10},{4,7}},'38343e')
 if name=='encoded_arcane_pattern' then
  poly({{7,5},{10,7},{8,9},{5,7}},'357744');px(7,5,'8adc85');px(8,6,'57b768');px(8,8,'285437')
  line(2,8,5,11,'59b47a');line(2,9,5,12,'2b6d59')
 end
 save(name)
end
-- 16px block palettes: crop UV rectangles to real face dimensions in Blockbench.
start();rect(0,0,16,16,'4a4657')
for y=0,15 do for x=0,15 do if (x+y*3)%11==0 then px(x,y,'514c60') end end end
line(0,0,15,0,'c9bfd5');line(0,1,15,1,'9383aa');line(0,14,15,14,'756489');line(0,15,15,15,'332f40')
for _,x in ipairs({1,6,10,14}) do px(x,0,'e6dcef');px(x,1,'605571') end
save('machine_alloy','Purple grey machined metal')
start();rect(0,0,16,16,'3b3748');rect(1,1,14,14,'766788');rect(2,2,12,12,'4e465b')
for _,x in ipairs({3,12}) do for _,y in ipairs({3,12}) do px(x,y,'bfb0d1');px(x,y+1,'332d3e') end end
rect(5,5,6,6,'242832');rect(6,6,4,4,'82718e');rect(7,7,2,2,'5b5268');save('machine_back')
start();rect(0,0,16,16,'403b47');rect(2,2,12,12,'83788d');rect(3,3,10,10,'193725')
for y=4,10,3 do for x=4,10,3 do rect(x,y,2,2,'397e40');px(x,y,'61aa56');px(x+1,y+1,'2a6334') end end
line(2,2,13,2,'c8bccf');line(2,2,2,13,'a798b7');line(3,13,13,13,'5d506c')
save('arcane_terminal','Green nine cell arcane display')
for i,name in ipairs({'input','output','creative'}) do
 start();rect(0,0,16,16,'62566f');rect(4,4,8,8,'b9afc1');rect(5,5,6,6,'594964');rect(6,6,4,4,'2e293b')
 local col=({'96b2d1','c9a968','7edac7'})[i]
 line(6,5,9,5,col);line(5,6,5,9,col);line(10,6,10,9,'84728f');line(6,10,9,10,'84728f')
 rect(7,6,2,4,'9071be');rect(6,7,4,2,'9071be');px(7,7,'dec7fc');px(8,8,'5d447d')
 if i==3 then px(6,6,'daf7ec');px(9,9,'daf7ec') end
 save('vis_'..name,'Arcane collector lens and AE metal frame')
end
start()
-- Glass is cutout rather than a uniform translucent sheet, like modern AE2.
line(2,2,13,2,'8895b4');line(2,2,2,13,'8895b4');line(2,13,13,13,'556077');line(13,2,13,13,'556077')
line(4,3,6,3,'e0eefa');line(3,4,3,6,'c4dbed');px(4,4,'deeff9');px(11,11,'b4cce1');px(12,10,'daeaf6')
save('assembler_glass','Clear chamber with sparse corner reflections')
-- Gear metal swatches also serve the static inventory version of the gearboxes.
for _,t in ipairs({{'iron','b9b8b4','ece9e1','636368'},{'thaumium','837092','c9b7d9','44394e'}}) do
 start();rect(0,0,16,16,t[2]);line(0,0,15,0,t[3]);line(0,0,0,15,t[3]);line(15,0,15,15,t[4]);line(0,15,15,15,t[4])
 rect(4,4,8,8,t[4]);rect(5,5,6,6,t[2]);px(5,5,t[3]);save('gear_'..t[1])
end
print('Authored 45 pixel textures and editable Aseprite sources')
