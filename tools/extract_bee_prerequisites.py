"""Normalize referenced legacy bee data for the missing modern predecessor branch.

Evidence only: inherited numeric/allele data and references, never Java sources in the mod.
"""
import json
import re
from pathlib import Path

base = Path(__file__).resolve().parents[1]
source = base / 'evidence/collab-legacy/magicbees-1.7.10-2.4.4/magicbees/bees'
genomes = (source / 'BeeGenomeManager.java').read_text(encoding='utf-8')
species = (source / 'BeeSpecies.java').read_text(encoding='utf-8')
products = (source / 'BeeProductHelper.java').read_text(encoding='utf-8')
methods = dict(re.findall(r'(?:private|public) static IAllele\[\] (\w+)\(\) \{(.*?)\n    \}', genomes, re.S))
templates = dict(re.findall(r'(\w+)\.registerGenomeTemplate\(BeeGenomeManager\.(\w+)\(\)\)', species))
required = ['ATTUNED', 'INFERNAL', 'ELDRITCH', 'CHARMED', 'ENCHANTED', 'SUPERNATURAL',
            'ETHEREAL', 'WINDY', 'FIREY', 'WATERY', 'EARTHY', 'SKULKING', 'BATTY', 'GHASTLY',
            'BIGBAD', 'MUTABLE', 'TRANSMUTING', 'BRAINY', 'CHICKEN', 'BEEF', 'PORK']

def genome(method, ancestry=()):
    if method in ancestry:
        raise ValueError('Cyclic genome inheritance: ' + method)
    body = methods[method]
    parent = re.search(r'IAllele\[\] genome = BeeGenomeManager\.(\w+)\(\);', body)
    result = genome(parent[1], ancestry + (method,)) if parent else {}
    for gene, value in re.findall(r'genome\[EnumBeeChromosome\.(\w+)\.ordinal\(\)\] = (.*?);', body):
        if gene == 'SPECIES':
            continue
        vanilla = re.fullmatch(r'Allele.getBaseAllele\(\(String\)"([^"]+)"\)', value)
        result[gene] = {'forestry': vanilla[1]} if vanilla else {'legacy': value.removeprefix('Allele.')}
    return result

rows = []
for name in required:
    metadata = re.search(r'^    ' + name + r'\((.*?)\)[,;]', species, re.M)[1]
    rows.append({'name': name.lower(), 'template': templates[name], 'metadata': metadata,
                 'genome': genome(templates[name]),
                 'product_reference': [line.strip() for line in products.splitlines()
                                       if 'BeeSpecies.' + name + '.' in line]})
target = base / 'evidence/magic-bee-prerequisites.json'
target.write_text(json.dumps({'source': str(source.relative_to(base)), 'species': rows},
                            ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(f'Normalized inherited genomes for {len(rows)} missing prerequisite species')
print('Extra alleles:', sorted({str(value) for row in rows for value in row['genome'].values() if 'legacy' in value}))

# Emit typed API calls, preserving all inherited genome values rather than hand-copying only overrides.
aliases = {
    'speedNorm': 'SPEED_NORMAL', 'fertilityLow': 'FERTILITY_1', 'fertilityNormal': 'FERTILITY_2',
    'fertilityHigh': 'FERTILITY_3', 'territoryDefault': 'TERRITORY_AVERAGE',
    'flowersVanilla': 'FLOWER_TYPE_VANILLA', 'flowersSnow': 'FLOWER_TYPE_SNOW',
    'flowersCacti': 'FLOWER_TYPE_CACTI', 'flowersNether': 'FLOWER_TYPE_NETHER',
    'boolTrue': 'TRUE', 'boolFalse': 'FALSE'}
chromosomes = {'FLOWER_PROVIDER': 'FLOWER_TYPE', 'FLOWERING': 'POLLINATION',
               'TOLERANT_FLYER': 'TOLERATES_RAIN', 'NOCTURNAL': 'ACTIVITY'}
custom = {'effectTransmuting': 'transmuting', 'spawnBats': 'batty', 'spawnGhast': 'ghastly', 'spawnWolf': 'wolf',
          'spawnBrainyZombie': 'brainy', 'spawnChicken': 'chicken', 'spawnCow': 'cow', 'spawnPig': 'pig'}

def constant(name):
    if name in aliases:
        return aliases[name]
    for prefix, modern in [('speed', 'SPEED'), ('lifespan', 'LIFESPAN'), ('flowering', 'POLLINATION'),
                           ('territory', 'TERRITORY'), ('effect', 'EFFECT'), ('tolerance', 'TOLERANCE')]:
        if name.startswith(prefix):
            suffix = re.sub(r'(?<=[a-zA-Z])(?=[0-9])', '_', name[len(prefix):]).upper()
            return modern + '_' + suffix
    raise ValueError('Unmapped allele: ' + name)

lines = ['package thaumicomniverse.compat.bees;', '',
         'import forestry.api.IForestryApi;', 'import forestry.api.genetics.alleles.BeeChromosomes;',
         'import forestry.api.plugin.IGenomeBuilder;',
         'import static forestry.api.genetics.alleles.ForestryAlleles.*;', '',
         '/** Generated from reviewed inherited genome data by extract_bee_prerequisites.py. */',
         'public final class LegacyBeeGenomes {', '    private LegacyBeeGenomes() {}',
         '    public static void configure(String name, IGenomeBuilder genome) {', '        switch (name) {']
for row in rows:
    lines.append(f'            case "{row["name"]}" -> {{')
    for gene, value in row['genome'].items():
        chromosome = chromosomes.get(gene, gene)
        if gene == 'NOCTURNAL':
            allele = 'ACTIVITY_CATHEMERAL' if value['forestry'] == 'boolTrue' else 'ACTIVITY_DIURNAL'
        elif 'legacy' in value:
            effect = custom[value['legacy']]
            allele = ('IForestryApi.INSTANCE.getAlleleManager().registryAllele('
                      f'MagicBeesPlugin.key("effect_legacy_{effect}"), BeeChromosomes.EFFECT)')
        else:
            allele = constant(value['forestry'])
        lines.append(f'                genome.set(BeeChromosomes.{chromosome}, {allele});')
    lines.append('            }')
lines += ['            default -> throw new IllegalArgumentException("Unknown legacy bee: " + name);',
          '        }', '    }', '}', '']
target = base / 'src/main/java/thaumicomniverse/compat/bees/LegacyBeeGenomes.java'
target.write_text('\n'.join(lines), encoding='utf-8')
print('Generated typed native genomes:', target.name)

vanilla = json.loads((base.parents[1] / 'Collaboration/evidence/vanilla/registry.json').read_text(encoding='utf-8'))
definitions = []
for row in rows:
    parts = [part.strip() for part in row['metadata'].split(',')]
    temp_index = next(i for i, part in enumerate(parts) if part.startswith('EnumTemperature.'))
    colors = [int(part, 0) for part in parts[3:temp_index]]
    if len(colors) == 1:
        colors.append(16743462)
    flags = [part == 'true' for part in parts[temp_index + 2:]]
    if len(flags) == 2:
        glow, dominant = flags; secret = True
    elif len(flags) == 3:
        secret, glow, dominant = flags
    else:
        glow, secret, counted, dominant, nocturnal = flags
    definition = {'name': row['name'], 'legacy_name': parts[0].strip('"'),
        'latin': parts[1].strip('"'), 'genus': parts[2].split('.')[-1].lower(),
        'colors': colors, 'glint': glow, 'dominant': dominant, 'secret': secret,
        'temperature': parts[temp_index].split('.')[-1], 'humidity': parts[temp_index + 1].split('.')[-1],
        'products': []}
    for line in row['product_reference']:
        for product_type, expression, chance in re.findall(r'add(Product|Specialty)\((.*?), ([0-9.E-]+)f\)', line):
            comb = re.search(r'Config.combs.getStackForType\(CombType.(\w+)\)', expression)
            pollen = re.search(r'Config.pollen.getStackForType\(PollenType.(\w+)\)', expression)
            item = re.search(r'(Items|Blocks)\.(field_\w+)', expression)
            forestry_comb = re.search(r'ForestryHelper.Comb\.(\w+)\.ordinal', expression)
            if comb:
                item_id = 'thaumicomniverse:bee_comb_legacy_' + comb[1].lower()
            elif pollen:
                item_id = 'thaumicomniverse:bee_pollen_' + pollen[1].lower()
            elif item:
                item_id = vanilla['net.minecraft.init.' + item[1] + '.' + item[2]]
                if item[1] == 'Items' and item_id == 'minecraft:melon':
                    item_id = 'minecraft:melon_slice'
            elif forestry_comb:
                item_id = 'forestry:bee_comb_' + forestry_comb[1].lower()
            elif 'ThaumcraftHelper.MiscResource.ZOMBIE_BRAIN' in expression:
                item_id = 'thaumcraft:zombie_brain'
            elif 'ThaumcraftHelper.nugget' in expression:
                meat = re.search(r'ThaumcraftHelper.nugget(Chicken|Beef|Pork)', expression)
                if not meat:
                    raise ValueError('Unresolved meat product: ' + expression)
                item_id = 'thaumcraft:' + meat[1].lower() + '_nugget'
            else:
                raise ValueError('Unresolved prerequisite product: ' + expression)
            definition['products'].append({'item': item_id, 'chance': float(chance), 'specialty': product_type == 'Specialty'})
    definitions.append(definition)
target = base / 'src/main/resources/data/thaumicomniverse/forestry/legacy_species.json'
target.parent.mkdir(parents=True, exist_ok=True)
target.write_text(json.dumps(definitions, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print('Generated native species metadata and products:', len(definitions))
