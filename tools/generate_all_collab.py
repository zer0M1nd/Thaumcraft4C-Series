"""Regenerate authored collaboration resources in dependency order."""
from pathlib import Path
import subprocess
import sys

for script in ['generate_collab_resources.py', 'generate_compasses.py', 'generate_avaritia.py', 'generate_atm.py', 'generate_backpacks.py', 'generate_node_map.py', 'generate_machinery.py']:
    subprocess.run([sys.executable, '-X', 'utf8', str(Path(__file__).with_name(script))], check=True)
