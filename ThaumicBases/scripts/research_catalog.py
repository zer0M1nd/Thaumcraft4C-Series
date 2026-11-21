"""Translate legacy research declarations and bind pages to the already generated modern recipes."""
import json
import re
from research_source import arguments, research_calls, uncomment
from research_icons import item
from book_recipes import CLASS, generate as recipe_displays

ASPECTS = dict(zip(
    'AIR FIRE WATER EARTH ORDER ENTROPY VOID LIGHT WEATHER MOTION COLD CRYSTAL LIFE POISON ENERGY EXCHANGE METAL DEATH FLIGHT DARKNESS SOUL HEAL TRAVEL ELDRITCH MAGIC AURA TAINT SLIME PLANT TREE BEAST FLESH UNDEAD MIND SENSES MAN CROP MINE TOOL HARVEST WEAPON ARMOR HUNGER GREED CRAFT CLOTH MECHANISM TRAP'.split(),
    'aer ignis aqua terra ordo perditio vacuos lux tempestas motus gelum vitreus victus venenum potentia permutatio metallum mortuus volatus tenebrae spiritus sano iter alienis praecantatio auram vitium limus herba arbor bestia corpus exanimis cognitio sensus humanus messis perfodio instrumentum meto telum tutamen fames lucrum fabrico pannus machina vinculum'.split()))


def aspects(expression):
    result = {}
    for match in re.finditer(r'\.add\s*\(', expression):
        values, _ = arguments(expression, match.end()-1)
        if len(values) == 1 and values[0].startswith('primals('):
            amount = int(arguments(values[0], values[0].index('('))[0][0])
            for name in ['aer', 'ignis', 'aqua', 'terra', 'ordo', 'perditio']:
                result[name] = result.get(name, 0) + amount
        elif len(values) == 2:
            name = 'ignis' if values[0] == 'nether' else ASPECTS[values[0].removeprefix('Aspect.')]
            result[name] = result.get(name, 0) + int(values[1])
        else:
            raise ValueError('Unsupported research aspects: ' + expression)
    return result


def generate(write, out, root, texture):
    source = uncomment((root/'references/ThaumicBases-GTNH/src/main/java/tb/init/TBThaumonomicon.java').read_text(encoding='utf-8'))
    types = recipe_displays(write, out)
    index = {}
    for name in ['alchemy', 'equipment', 'machine']:
        index.update(json.loads((out/f'tbmodern/{name}_recipe_index.json').read_text(encoding='utf-8')))
    aliases = {}
    for match in re.finditer(r'new\s+FakeResearchItem\s*\(', source):
        values, _ = arguments(source, match.end()-1)
        aliases[json.loads(values[0])] = json.loads(values[2])
    warp = {key: int(amount) for key, amount in re.findall(r'addWarpToResearch\("([^"]+)"\s*,\s*(\d+)\)', source)}
    for name, modern in {'quicksilverBlock': 'quicksilver_block', 'quicksilver': 'quicksilver_from_quicksilver_block',
                         'quicksilverBrick': 'quicksilver_brick', 'thauminiteIngot': 'thauminite_ingot',
                         'thauminiteNugget': 'thauminite_nugget_from_thauminite_ingot', 'thauminiteBlock': 'thauminite_block'}.items():
        index[name] = 'thaumicbases:' + modern
    groups = {name: [value for key, value in index.items() if key.startswith(name + '_')] for name in ['toolsRec', 'oldRec', 'shards', 'cBlocks', 'cTaintedBlocks']}
    groups['slabs'] = sorted(key for key in types if key.endswith('_slab'))
    upgrades = {'accuracyRec': 'accuracy', 'atropodsRec': 'bane_of_arthropods', 'eldritchBRec': 'bane_of_eldritch', 'duelingRec': 'dueling',
                'efficiencyRec': 'efficiency', 'eldritchRec': 'eldritch', 'heavyRec': 'heavy', 'knowledgeRec': 'knowledge',
                'piercingRec': 'piercing', 'powerRec': 'power', 'primalRec': 'primal', 'silverRec': 'silver', 'speedRec': 'speed', 'taintedRec': 'tainted', 'voidRec': 'void'}
    for name, upgrade in upgrades.items():
        groups[name] = sorted(key for key in types if key.startswith('thaumicbases:revolver/' + upgrade + '_'))
    def artwork(path):
        texture(path.removeprefix('textures/').removesuffix('.png'))
        return 'thaumicbases:' + path.lower()
    def page(expression):
        values, _ = arguments(expression, expression.index('('))
        assert len(values) == 1
        value = values[0]
        if 'Loader.isModLoaded("essentialcraft")' in value:
            return {'type': 'TEXT', 'text': 'tb.rec.lazullia.page.0'}
        if value.startswith('"'):
            return {'type': 'TEXT', 'text': json.loads(value)}
        if 'Loader.isModLoaded' in value:
            return {'type': 'TEXT', 'text': 'tb.rec.rainCacti.page.0'}
        if value.startswith('TBRecipes.recipes.get('):
            value = json.loads(arguments(value, value.index('('))[0][0])
        if value.startswith('new ShapelessArcaneRecipe[]'):
            references = [index[key] for key in arguments(value, value.index('{'))[0]]
        else:
            references = groups[value] if value in groups else [index[value]]
        if not references or len({types[key] for key in references}) != 1:
            raise ValueError('Invalid handbook recipe group: ' + value)
        return {'type': types[references[0]], 'recipe': {'kind': 'reference', 'keys': references, 'class': CLASS}}
    module_ranges = []
    for match in re.finditer(r'if\s*\(TBConfig\.(allowTobacco|enableBloodyMess|enableBracelets|enableFlora|enableNodeTools|enableRevolver)\)\s*\{', source):
        _, end = arguments(source, match.end()-1)
        module_ranges.append((match[1], source[:match.start()].count('\n')+1, source[:end].count('\n')+1))
    modules = {}
    entries, pages = [], []
    for row in research_calls(source):
        key = row['key']
        for module, start, end in module_ranges:
            if start <= row['source_line'] <= end:
                modules.setdefault(module, []).append(key)
        args = row['constructor']
        entry = {'key': key, 'category': 'THAUMICBASES', 'aspects': aspects(args[2]), 'displayColumn': int(args[3]),
                 'displayRow': int(args[4]), 'complexity': max(1, min(3, int(args[5]))), 'warp': warp.get(key, 0)}
        icon = args[6]
        if icon.startswith('new ResourceLocation'):
            values, _ = arguments(icon, icon.index('('))
            entry['icon_resource'] = artwork(json.loads(values[1]))
        else:
            entry['icon_item'] = item(icon)
        for call in row['calls']:
            method, values = call['method'], call['arguments']
            if method == 'setPages':
                book_pages = [page(value) for value in values]
                entry['pageCount'] = len(book_pages)
                pages.append({'key': key, 'pages': book_pages})
            elif method in ['setParents', 'setParentsHidden']:
                parents = ['TB.NodeMan', 'TB.PRIMPEARL'] if values == ['bParents'] else [json.loads(value) for value in values]
                entry['parents' if method == 'setParents' else 'parentsHidden'] = [aliases.get(parent, parent) for parent in parents]
            elif method in ['setRound', 'setAutoUnlock', 'setConcealed', 'setSecondary', 'setSpecial']:
                entry['is' + method[3:]] = True
            elif method != 'registerResearchItem':
                raise ValueError('Unmapped research method: ' + method)
        entries.append(entry)
    write('data/thaumicbases/research_catalog/modern.json', {'format': 1, 'categories': [{'key': 'THAUMICBASES',
          'icon': artwork('textures/thaumonomicon/bases.png'), 'background': artwork('textures/thaumonomicon/background.png'), 'research': entries}]})
    write('data/thaumicbases/research_pages/modern.json', {'format': 1, 'entries': pages})
    write('tbmodern/research-modules.json', modules)

# Finish imported original captures using the shared modern book schema.
import runpy as _book_runpy
from pathlib import Path as _BookPath
_book_workspace = next(p for p in _BookPath(__file__).resolve().parents if (p / 'Tools/workspace/book_schema.py').is_file())
_book_runpy.run_path(str(_book_workspace / 'Tools/workspace/book_schema.py'))['migrate'](_BookPath(__file__).resolve().parents[1])
