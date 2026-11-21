"""Reproducible project setup; never copies legacy implementation or assets to src."""
from pathlib import Path
import shutil, json, zipfile, hashlib, csv

ROOT=Path(__file__).resolve().parents[3]
for folder,modid,title,ref,jar,legacy,cf in [
 ('ElectroMagicTools','emt','Electro-Magic Tools','ElectroMagicTools','ElectroMagicTools-1.2.6.2.jar','EMT','https://www.curseforge.com/minecraft/mc-mods/electro-magic-tools/files/2336582'),
 ('Gadomancy','gadomancy','Gadomancy','Gadomancy','gadomancy-1.7.10-1.0.7.3.jar','gadomancy','https://www.curseforge.com/minecraft/mc-mods/gadomancy/files/2338768')]:
 p=ROOT/'Addons'/folder
 def write(name,data):
  f=p/name;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(data,encoding='utf-8')
 for f in ['gradlew','gradlew.bat','gradle/wrapper/gradle-wrapper.jar','gradle/wrapper/gradle-wrapper.properties']:
  out=p/f;out.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(ROOT/'Addons/ThaumicTinkerer'/f,out)
 write('settings.gradle',"pluginManagement { repositories { gradlePluginPortal(); maven { url='https://maven.minecraftforge.net/' } } }\nrootProject.name='"+folder+"-Modern-Port'\n")
 write('gradle.properties','org.gradle.jvmargs=-Xmx2G -Dfile.encoding=UTF-8\norg.gradle.daemon=false\norg.gradle.workers.max=2\norg.gradle.java.installations.auto-download=false\norg.gradle.java.installations.auto-detect=false\norg.gradle.java.installations.fromEnv=JAVA_HOME\nmod_version=0.1.0\n')
 write('.gitignore','.gradle/\nbuild/\nrun-client/\nrun-server/\nlibs/\n.tooling/\n')
 libs=p/'libs';libs.mkdir(exist_ok=True)
 shutil.copyfile(ROOT/'TC4/releases/0.4.47/Thaumcraft-4-Modern-Port-0.4.47.jar',libs/'thaumcraft-0.4.47.jar')
 ic2=''
 if modid=='emt':
  shutil.copyfile(ROOT/'Addons/references/phase2/ElectroMagicTools/ic2-forge-2.10.33-ex120.jar',libs/'ic2-2.10.33-ex120.jar')
  ic2="    implementation fg.deobf('local.revival:ic2:2.10.33-ex120')\n"
 write('build.gradle',f'''plugins {{ id 'java'; id 'net.minecraftforge.gradle' version '6.0.42' }}
version=mod_version
group='{modid}modern'
base.archivesName='{folder}-Modern-Port'
java.toolchain.languageVersion=JavaLanguageVersion.of(17)
java.withSourcesJar()
sourceSets.main.resources.srcDir 'src/generated/resources'
repositories {{ flatDir {{ dirs 'libs' }}; maven {{ url='https://maven.theillusivec4.top/' }}; mavenCentral() }}
dependencies {{
 minecraft 'net.minecraftforge:forge:1.20.1-47.4.23'
 implementation fg.deobf('local.revival:thaumcraft:0.4.47')
{ic2} implementation fg.deobf('top.theillusivec4.curios:curios-forge:5.14.1+1.20.1')
 testImplementation 'org.junit.jupiter:junit-jupiter:5.10.3'
}}
minecraft {{
 mappings channel:'official',version:'1.20.1'
 runs {{
  configureEach {{
   args '--mixin.config','tc4revival.mixins.json'
   property 'mixin.env.remapRefMap','true'
   property 'mixin.env.refMapRemappingFile',layout.buildDirectory.file('createSrgToMcp/output.srg').get().asFile.absolutePath
   property 'forge.enableGameTest','true'
   property 'forge.enabledGameTestNamespaces','{modid}'
   mods {{ {modid} {{ source sourceSets.main }} }}
  }}
  client {{ workingDirectory project.file('run-client') }}
  server {{ workingDirectory project.file('run-server'); args '--nogui'; jvmArgs '-Xmx2G' }}
  data {{ workingDirectory project.file('run-data'); args '--mod','{modid}','--all','--output',file('src/generated/resources/'),'--existing',file('src/main/resources/') }}
 }}
}}
tasks.withType(JavaCompile).configureEach {{ options.encoding='UTF-8'; options.release=17 }}
tasks.named('test') {{ useJUnitPlatform() }}
tasks.named('processResources') {{ inputs.property 'version',project.version; filesMatching('META-INF/mods.toml') {{ expand version:project.version }} }}
tasks.named('jar') {{ finalizedBy 'reobfJar' }}
tasks.withType(AbstractArchiveTask).configureEach {{ preserveFileTimestamps=false; reproducibleFileOrder=true }}
tasks.register('verifyDistribution') {{
 dependsOn 'jar','reobfJar','sourcesJar'
 doLast {{ ['jar','sourcesJar'].each {{ name -> new java.util.zip.ZipFile(tasks.named(name).get().archiveFile.get().asFile).withCloseable {{ zip -> zip.entries().each {{ entry ->
  if(entry.name==~ /.*\\.(png|ogg|lang|obj)/)throw new GradleException('Bundled legacy asset: '+entry.name)
  if(entry.name.startsWith('tc4revival/')||entry.name.startsWith('thaumcraft/')||entry.name.startsWith('ic2/'))throw new GradleException('Bundled foreign class: '+entry.name)
 }} }} }} }}
}}
''')
 deps=''
 if modid=='emt':deps='\n[[dependencies.emt]]\nmodId="ic2"\nmandatory=true\nversionRange="[2.10.33,)"\nordering="AFTER"\nside="BOTH"\n'
 write('src/main/resources/META-INF/mods.toml',f'''modLoader="javafml"
loaderVersion="[47,)"
license="{'GPL-3.0-or-later' if modid=='emt' else 'LGPL-3.0-or-later'}"
[[mods]]
modId="{modid}"
version="${{version}}"
displayName="{title}"
description="1.20.1 port; original assets are loaded from a separately supplied legacy JAR."
[[dependencies.{modid}]]
modId="minecraft"
mandatory=true
versionRange="[1.20.1,1.20.2)"
ordering="NONE"
side="BOTH"
[[dependencies.{modid}]]
modId="thaumcraft"
mandatory=true
versionRange="[0.4.114,)"
ordering="AFTER"
side="BOTH"
{deps}''')
 write('src/main/resources/pack.mcmeta',json.dumps({'pack':{'pack_format':15,'description':title+' Modern Port'}}))
 source=ROOT/'Addons/references/phase2'/ref/jar
 with zipfile.ZipFile(source) as z:
  mapping={('assets/'+modid+'/'+n.split('/',2)[2]).lower():n for n in z.namelist() if n.startswith('assets/') and not n.endswith('/')}
  for n in z.namelist():
   if n.startswith('assets/') and n.endswith('.lang'):
    target=n.lower().removesuffix('.lang')+'.json';mapping[target]=n
 write(f'src/main/resources/data/{modid}/thaumcraft/original_resources.json',json.dumps({'name':jar,'legacy_modid':legacy,'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'download':cf,'mapping':mapping},ensure_ascii=False,indent=2))
 write('STATUS.md',f'''# {title} 移植状态\n\n2026-10-02：开发中，尚不可称为完整移植。基线为本体 0.4.47；原作者最后 1.7.10 版限定玩法，GTNH 仅用于维护修复参考。\n\n实现、测试和客户端对照分别列在 FEATURES.csv；未运行的检查不得标成通过。原作者资源仅映射到运行目录 thaumcraft 下的原 JAR，发行包不内置。\n''')
 write('BUGFIXES.md','# 移植修复记录\n\n仅记录实际修改及依据；后续填入各系统发现的旧版缺陷与现代适配。\n')
 write('REFERENCES.md',f'''# 来源与边界\n\n- MCMOD：https://www.mcmod.cn/class/{232 if modid=='emt' else 520}.html 。用于了解玩家操作和内容范围。\n- 维护源码：https://github.com/GTNewHorizons/{'Electro-Magic-Tools' if modid=='emt' else 'Gadomancy'} 。工作副本在 ../references/phase2/{ref}/gtnh；禁止执行其构建脚本。\n- 原作者资源：{cf}；SHA-256 `{hashlib.sha256(source.read_bytes()).hexdigest()}`。下载自该文件的 CurseForge CDN，绝不使用 GTNH JAR 作为资源源。\n- original-src 是原作者发布包的只读反编译，交叉核实 GTNH 变化，不复制其 Java 实现。\n- 本体：Thaumcraft 4 Modern Port 0.4.47；仅依赖公开本地 API。\n'''+('''- IC2： https://github.com/craftorio/ic2/tree/forge/1.20.1 ，发布 https://github.com/craftorio/ic2/releases/tag/2.10.33-ex120 。本移植使用实际 IElectricItem 和 EU 网络接口，不将 Forge Energy 当成 EU。\n''' if modid=='emt' else ''))
 classes=[]
 for f in (ROOT/'Addons/references/phase2'/ref/'original-src').rglob('*.java'):
  s=str(f.relative_to(ROOT/'Addons/references/phase2'/ref/'original-src')).replace('\\','/')
  if any(x in s for x in ['/items/','/item/','/blocks/','/tile/','/aura/','/entity/']):classes.append([s,'not_implemented','not_run','not_run','not_run',''])
 with (p/'FEATURES.csv').open('w',encoding='utf-8',newline='') as f:
  w=csv.writer(f);w.writerow(['legacy_source','implementation','unit_test','server_test','client_test','notes']);w.writerows(classes)
