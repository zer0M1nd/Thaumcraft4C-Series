from pathlib import Path
import json, shutil, zipfile, hashlib
ROOT=Path(__file__).resolve().parents[3]
for name,mid,pkg,title in [('ThaumicInfusion','thaumicinfusion','timodern','Thaumic Infusion'),('TaintedMagic','taintedmagic','tmmodern','Tainted Magic')]:
 p=ROOT/'Addons'/name
 def write(rel,s):
  f=p/rel;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(s,encoding='utf-8')
 for rel in ['gradlew','gradlew.bat','gradle/wrapper/gradle-wrapper.jar','gradle/wrapper/gradle-wrapper.properties']:
  f=p/rel;f.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(ROOT/'TC4'/rel,f)
 (p/'libs').mkdir(exist_ok=True)
 shutil.copy2(ROOT/'TC4/releases/0.4.47/Thaumcraft-4-Modern-Port-0.4.47.jar',p/'libs/thaumcraft-0.4.47.jar')
 shutil.copy2(ROOT/'Addons/ForbiddenMagic/libs/junit-platform-console-standalone-1.10.2.jar',p/'libs/junit-platform-console-standalone-1.10.2.jar')
 write('settings.gradle',f"pluginManagement {{ repositories {{ gradlePluginPortal(); maven {{ url='https://maven.minecraftforge.net/' }} }} }}\nrootProject.name='{title.replace(' ','-')}-Modern-Port'\n")
 write('gradle.properties','org.gradle.jvmargs=-Xmx2G -Dfile.encoding=UTF-8\norg.gradle.daemon=false\norg.gradle.workers.max=2\norg.gradle.java.installations.auto-download=false\norg.gradle.java.installations.auto-detect=false\norg.gradle.java.installations.fromEnv=JAVA_HOME\n')
 write('build.gradle',f'''plugins {{ id 'java'; id 'net.minecraftforge.gradle' version '6.0.42' }}
group='{pkg}'
version='0.1.0'
base.archivesName='{title.replace(' ','-')}-Modern-Port'
java.toolchain.languageVersion=JavaLanguageVersion.of(17)
java.withSourcesJar()
repositories {{ flatDir {{ dirs 'libs' }}; maven {{ url='https://maven.theillusivec4.top/' }}; mavenCentral() }}
dependencies {{
 minecraft 'net.minecraftforge:forge:1.20.1-47.4.23'
 implementation fg.deobf('local:thaumcraft:0.4.47')
 compileOnly fg.deobf('top.theillusivec4.curios:curios-forge:5.14.1+1.20.1:api')
 runtimeOnly fg.deobf('top.theillusivec4.curios:curios-forge:5.14.1+1.20.1')
 testImplementation files('libs/junit-platform-console-standalone-1.10.2.jar')
 annotationProcessor 'org.spongepowered:mixin:0.8.5:processor'
}}
sourceSets.main.resources.srcDir 'src/generated/resources'
minecraft {{
 mappings channel:'official',version:'1.20.1'
 runs {{
  configureEach {{
   args '--mixin.config','tc4revival.mixins.json','--mixin.config','{mid}.mixins.json'
   property 'mixin.env.remapRefMap','true'
   property 'mixin.env.refMapRemappingFile',layout.buildDirectory.file('createSrgToMcp/output.srg').get().asFile.absolutePath
   property 'forge.enabledGameTestNamespaces','{mid}'
   mods {{ {mid} {{ source sourceSets.main }} }}
  }}
  client {{ workingDirectory file('run-client');jvmArgs '-Xmx3G' }}
  server {{ workingDirectory file('run-server');args '--nogui';jvmArgs '-Xmx2G' }}
  gameTestServer {{ workingDirectory file('run-gametest');jvmArgs '-Xmx2G' }}
 }}
}}
tasks.withType(JavaCompile).configureEach {{ options.encoding='UTF-8';options.release=17 }}
test {{ useJUnitPlatform() }}
tasks.named('jar') {{ finalizedBy 'reobfJar';manifest.attributes('MixinConfigs':'{mid}.mixins.json') }}
tasks.named('processResources') {{ inputs.property 'version',project.version;filesMatching('META-INF/mods.toml') {{ expand version:project.version }} }}
tasks.register('verifyArchives') {{
 dependsOn 'jar','reobfJar','sourcesJar'
 doLast {{ ['jar','sourcesJar'].each {{ name -> new java.util.zip.ZipFile(tasks.named(name).get().archiveFile.get().asFile).withCloseable {{ zip -> zip.entries().each {{ e -> if(e.name ==~ /.*\\.(png|ogg|lang|obj)/ || e.name.startsWith('tc4revival/') || e.name.startsWith('thaumcraft/api/')) throw new GradleException('Foreign assets/classes: '+e.name) }} }} }} }}
}}
''')
 write('src/main/resources/META-INF/mods.toml',f'''modLoader="javafml"
loaderVersion="[47,)"
license="See REFERENCES.md"
[[mods]]
modId="{mid}"
version="${{version}}"
displayName="{title}"
description="Modern port of the original 1.7.10 addon. Original assets are loaded from the original author's CurseForge JAR."
[[dependencies.{mid}]]
modId="thaumcraft"
mandatory=true
versionRange="[0.4.114,)"
ordering="AFTER"
side="BOTH"
''')
 write('src/main/resources/pack.mcmeta',json.dumps({'pack':{'pack_format':15,'description':title+' modern resources'}}))
 write(f'src/main/resources/{mid}.mixins.json',json.dumps({'required':True,'minVersion':'0.8','package':pkg+'.mixin','compatibilityLevel':'JAVA_17','mixins':[],'client':[],'injectors':{'defaultRequire':1}},indent=2))
 write('STATUS.md',f'# {title} 移植状态\n\n2026-10-02：正在开发，尚未可交付。源码/资源来源已核实，功能清单见 FEATURES.csv。编译、单元测试、服务端测试、客户端验收均未完成；不得以构建成功替代功能完整性。\n')
 write('BUGFIXES.md','# 移植修复记录\n\n所有修复记录旧版行为、现代实现与验证结果；尚无已验证修复。\n')
