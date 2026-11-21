-- Loaded by generate_item_textures.py with SRC, OUT and GROUP locals.
-- Recolor source pixels only: silhouettes, handles and needle positions stay intact.
local pc = app.pixelColor
local function rgb(hex)
  return {tonumber(hex:sub(1,2),16),tonumber(hex:sub(3,4),16),tonumber(hex:sub(5,6),16)}
end
local function color(hex, alpha)
  local c=rgb(hex)
  return pc.rgba(c[1],c[2],c[3],alpha)
end
local function ramp(value, stops)
  for i=2,#stops do
    if value <= stops[i][1] then
      local lo,hi=stops[i-1],stops[i]
      local a,b=rgb(lo[2]),rgb(hi[2])
      local t=(value-lo[1])/(hi[1]-lo[1])
      return pc.rgba(math.floor(a[1]+(b[1]-a[1])*t+.5),math.floor(a[2]+(b[2]-a[2])*t+.5),math.floor(a[3]+(b[3]-a[3])*t+.5),255)
    end
  end
end
local function recolor(source, target, frames, transform)
  local original=app.open(SRC..source..'.png')
  -- Mekanism's source PNG is indexed; read actual RGBA colors, not palette indices.
  app.activeSprite=original
  app.command.ChangePixelFormat{format='rgb'}
  local input=original.cels[1].image
  local result=Sprite(original.width,original.height*frames,ColorMode.RGB)
  local output=result.cels[1].image
  output:clear()
  for frame=0,frames-1 do
    for y=0,original.height-1 do
      for x=0,original.width-1 do
        local pixel=input:getPixel(x,y)
        if pc.rgbaA(pixel)>0 then
          output:drawPixel(x,y+frame*original.height,transform(pixel,frame))
        end
      end
    end
  end
  result:saveCopyAs(OUT..target..'.png')
  result:close()
  original:close()
end
local materials={
  thaumium={{0,'120F1C'},{68,'2A2342'},{140,'4F4179'},{180,'625197'},{216,'8374B4'},{255,'9F93C5'}},
  void={{0,'000000'},{68,'030105'},{140,'150824'},{180,'200D36'},{216,'30184D'},{255,'432469'}}
}
if GROUP=='all' or GROUP=='tools' then
  for material,stops in pairs(materials) do
    for _,tool in ipairs({'knife','paxel'}) do
      recolor('iron_'..tool,material..'_'..tool,1,function(pixel)
        local r,g,b=pc.rgbaR(pixel),pc.rgbaG(pixel),pc.rgbaB(pixel)
        if math.max(r,g,b)-math.min(r,g,b)<=2 then return ramp(r,stops) end
        return pixel -- Preserve the original wood handle, including its outline.
      end)
    end
  end
end
local certificate={
  ['4D4D4D']='40354F',['6E6E6E']='625773',['8D8D8D']='8A7C91',
  ['AFAFAF']='ABA0AF',['CECECE']='CBC1C8',['E6E6E6']='E0D8D5',
  ['FFFFFF']='F1E9DE',['92F65D']='9273C0',['ACFF7F']='B09BD5'
}
local function key(pixel)
  return string.format('%02X%02X%02X',pc.rgbaR(pixel),pc.rgbaG(pixel),pc.rgbaB(pixel))
end
if GROUP=='all' or GROUP=='certificate' then
  recolor('nan_certificate','thaumaturge_certificate',1,function(pixel)
    return color(assert(certificate[key(pixel)],'Unknown GTM palette color'),pc.rgbaA(pixel))
  end)
end
local sinister={
  ['181717']='14101E',['2F2F2F']='231D32',['353535']='302741',
  ['4F4D4D']='69516F',['5E5E5E']='4A3B61',['646464']='82708E',
  ['828282']='62517F',['A8A8A8']='8873A6',['D8D8D8']='9F93C5',
  ['FFFFFF']='B7A5D0',['BE1515']='76334D',['CB1A1A']='8C3D5C',['FF1414']='B04D70'
}
local primal={
  ['181717']='20172C',['2F2F2F']='191225',['353535']='281C37',
  ['4F4D4D']='574668',['5E5E5E']='675336',['646464']='87739E',
  ['828282']='957543',['A8A8A8']='B49665',['D8D8D8']='CBB286',['FFFFFF']='DCC79F'
}
local function hsv(h,s,v)
  local i=math.floor(h*6);local f=h*6-i
  local p,q,t=v*(1-s),v*(1-f*s),v*(1-(1-f)*s)
  local c=({{v,t,p},{q,v,p},{p,v,t},{p,q,v},{t,p,v},{v,p,q}})[i%6+1]
  return pc.rgba(math.floor(c[1]*255+.5),math.floor(c[2]*255+.5),math.floor(c[3]*255+.5),255)
end
if GROUP=='all' or GROUP=='compasses' then
  for angle=0,31 do
    local source=string.format('compass_%02d',angle)
    recolor(source,string.format('sinister_compass_%02d',angle),1,function(pixel)
      return color(assert(sinister[key(pixel)],'Unknown vanilla compass color'),pc.rgbaA(pixel))
    end)
    recolor(source,string.format('primal_compass_%02d',angle),12,function(pixel,frame)
      local k=key(pixel)
      if primal[k] then return color(primal[k],pc.rgbaA(pixel)) end
      local value=assert(({BE1515=.55,CB1A1A=.63,FF1414=.76})[k],'Unknown needle color')
      return hsv(frame/12,.58,value)
    end)
  end
end
