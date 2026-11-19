"""Explicit legacy object-tag registrations. Recipe-derived aspects are generated separately."""
from alchemy_recipes import ELEMENTS, PRIMALS


def generate(write):
    entries = []
    def add(item, aspects):
        entries.append({'items': ['thaumicbases:' + item], 'aspects': [{'aspect': key, 'amount': value} for key, value in aspects.items()]})
    for item, aspects in [
        ('iron_greatwood', {'arbor': 1, 'metallum': 1}), ('plax_seeds', {'messis': 1, 'pannus': 1}),
        ('aurelia', {'herba': 1, 'auram': 3, 'lux': 1, 'sensus': 1, 'victus': 1}),
        ('aurelia_petal_block', {'herba': 1, 'auram': 6, 'sensus': 1}),
        ('aurelia_petal', {'herba': 1, 'auram': 6}), ('briar_seedbag', {'herba': 1, 'sano': 1}),
        ('tobacco_leaves', {'herba': 1, 'humanus': 1}), ('tobacco_pile', {'herba': 1, 'humanus': 1, 'perditio': 0}),
        ('mortar', {'arbor': 6, 'instrumentum': 1}),
        ('focus_experience', {'metallum': 24, 'lucrum': 17, 'vitreus': 12, 'terra': 7, 'vacuos': 5, 'fames': 5})]:
        add(item, aspects)
    for element, aspect in zip(ELEMENTS + ['tainted'], PRIMALS + ['vitium']):
        add('knose_fragment_' + element, {'herba': 1, aspect: 6})
    add('knose_fragment_mixed', {'herba': 1, **dict.fromkeys(PRIMALS, 2)})
    for kind, extra in [('golden_oak', {}), ('peaceful', {'victus': 1}), ('nether', {'ignis': 1}), ('ender', {'alienis': 1})]:
        add(kind + '_leaves', {'herba': 1, **extra})
        add(kind + '_sapling', {'herba': 3, **({'lucrum': 1} if kind == 'golden_oak' else extra)})
    add('peaceful_log', {'arbor': 3, 'sano': 1, 'victus': 1})
    add('nether_log', {'arbor': 3, 'ignis': 2})
    add('ender_log', {'arbor': 3, 'alienis': 1, 'vacuos': 1})
    # Obtained through interaction / creative mode, without legacy object tags or a recipe.
    add('bloody_cloth', {})
    add('concentrated_taint', {})
    write('data/thaumicbases/item_aspects/explicit.json', {'format': 1, 'entries': entries})
    # TC4 getBonusTags merges these with recipe aspects by maximum, not addition.
    # Shears/hoes use old durability thresholds; sword uses material damage + 1.
    bonuses = []
    for item, aspect, amount in [
        ('thauminite_pickaxe', 'perfodio', 4), ('thauminite_axe', 'instrumentum', 4),
        ('thauminite_shovel', 'instrumentum', 4), ('thauminite_sword', 'telum', 3),
        ('thauminite_hoe', 'meto', 4), ('thauminite_shears', 'meto', 4), ('void_shears', 'meto', 3),
        ('thauminite_helmet', 'tutamen', 3), ('thauminite_chestplate', 'tutamen', 8),
        ('thauminite_leggings', 'tutamen', 6), ('thauminite_boots', 'tutamen', 3),
        ('bloody_chestplate', 'tutamen', 6), ('bloody_leggings', 'tutamen', 5), ('bloody_boots', 'tutamen', 2)]:
        bonuses.append({'items': ['thaumicbases:' + item], 'aspects': [{'aspect': aspect, 'amount': amount}]})
    write('data/thaumicbases/item_bonus_aspects/equipment.json', {'format': 1, 'entries': bonuses})
