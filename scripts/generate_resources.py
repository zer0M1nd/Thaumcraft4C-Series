"""Generate all currently implemented systems in dependency order. Never reads the base workspace."""
import runpy
from pathlib import Path
for script in ['generate_node_resources.py','generate_golem_resources.py','generate_blood_resources.py','generate_blood_research.py','generate_injector_resources.py','generate_vat_resources.py','generate_vat_infusion_resources.py','generate_containment_resources.py','generate_creature_resources.py','generate_farm_creature_resources.py','generate_pig_resources.py','generate_sheeder_resources.py','generate_wolf_resources.py','generate_horse_resources.py','generate_cat_resources.py','generate_self_resources.py','generate_self_infusion_resources.py','generate_soul_resources.py','generate_soulforge_resources.py','generate_extractor_resources.py','generate_inspiratron_resources.py','generate_beacon_resources.py','generate_mirror_amulet_resources.py','generate_lens_resources.py','generate_illumination_resources.py','generate_liquefaction_resources.py','generate_disintegration_resources.py','generate_alchemite_resources.py','generate_void_putty_resources.py','generate_boat_resources.py','generate_vortex_stabilizer_resources.py','generate_vortex_resources.py','generate_plane_resources.py','generate_slime_resources.py','generate_keystone_resources.py','generate_gateway_resources.py','generate_plane_research.py','generate_cloud_resources.py','generate_decorations.py','generate_creative_tools.py','generate_api_completion.py']:
    runpy.run_path(str(Path(__file__).with_name(script)))







