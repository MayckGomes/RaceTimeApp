"""Verificações estáticas. Não substitui Gradle, JUnit ou validação no aparelho."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
main = root / 'app/src/main'
for path in (main / 'res').rglob('*.xml'):
    ET.parse(path)
ET.parse(main / 'AndroidManifest.xml')
resources = {item.attrib['name'] for p in (main / 'res/values').glob('*.xml')
             for item in ET.parse(p).getroot() if 'name' in item.attrib}
refs = {name for p in (main / 'java').rglob('*.kt')
        for name in re.findall(r'R\.string\.([a-zA-Z0-9_]+)', p.read_text())}
assert refs <= resources, refs - resources
for folder in ['values', 'values-pt-rBR', 'values-es', 'values-fr', 'values-it']:
    assert (main / 'res' / folder / 'strings.xml').exists(), folder
for file, key in [('driversRepository.kt', 'driversFavorites'), ('teamsRepository.kt', 'teamsFavorites')]:
    assert key in (main / 'java/mayckgomes/com/racetimeapp/data/repository' / file).read_text()
assert 'applicationId = "mayckgomes.com.racetimeapp"' in (root / 'app/build.gradle.kts').read_text()
try:
    from tree_sitter import Language, Parser
    import tree_sitter_kotlin
except ImportError:
    print('Parser Kotlin opcional ausente: análise sintática não executada.')
else:
    parser = Parser(Language(tree_sitter_kotlin.language()))
    paths = list((root / 'app/src').rglob('*.kt'))
    errors = [str(p.relative_to(root)) for p in paths if parser.parse(p.read_bytes()).root_node.has_error]
    assert not errors, errors
    print(f'Sintaxe: {len(paths)} arquivos Kotlin sem erros do parser.')
print('XMLs, recursos de texto, cinco idiomas, IDs de favoritos e applicationId verificados.')
