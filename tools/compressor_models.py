from pathlib import Path
import re
project=Path(__file__).resolve().parents[1]
legacy=project.parent/'references/phase2/Gadomancy/original-src/makeo/gadomancy/client/models'
methods=[]
for name,method in [('ModelEssentiaCompressor','formed'),('ModelPackedCompressorBlock','packed')]:
 text=(legacy/(name+'.java')).read_text()
 width=int(re.search(r'field_78090_t = (\d+)',text)[1]);height=int(re.search(r'field_78089_u = (\d+)',text)[1])
 parts=[]
 for part,u,v in re.findall(r'this\.(\w+) = new ModelRenderer\(\(ModelBase\)this, (\d+), (\d+)\);',text):
  cube=re.search(r'this\.'+part+r'\.func_78789_a\((.*?)\);',text)[1]
  point=re.search(r'this\.'+part+r'\.func_78793_a\((.*?)\);',text)[1]
  match=re.search(r'this\.setRotation\(this\.'+part+r', (.*?)\);',text)
  rotation=match[1] if match else '0.0f,0.0f,0.0f'
  parts.append(f'root.addOrReplaceChild("{part}",CubeListBuilder.create().texOffs({u},{v}).addBox({cube}),PartPose.offsetAndRotation({point},{rotation}));')
 methods.append(f'public static ModelPart {method}(){{var mesh=new MeshDefinition();var root=mesh.getRoot();'+''.join(parts)+f'return LayerDefinition.create(mesh,{width},{height}).bakeRoot();}}')
(project/'src/main/java/gadomancymodern/client/CompressorModels.java').write_text('package gadomancymodern.client;\nimport net.minecraft.client.model.geom.*;import net.minecraft.client.model.geom.builders.*;\n/** Original geometry and UV coordinates; exporter mirror flags assigned after cubes had no effect. */\npublic class CompressorModels {\n'+'\n'.join(methods)+'\n}\n')
