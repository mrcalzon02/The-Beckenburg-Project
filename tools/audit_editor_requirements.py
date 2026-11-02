"""Check the owner-source requirements chain without claiming feature acceptance.

Text hashes normalize line endings; original PDF hashes are verified only when
an explicit archived binary path exists. Missing originals are reported as gaps.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--require-originals', action='store_true')
parser.add_argument('--output', type=Path)
args = parser.parse_args()
register = json.loads((ROOT / 'docs/editor/requirements/DOCUMENT_CHAIN.json').read_text(encoding='utf-8'))
errors, gaps, sources = [], [], []


def registered_path(relative):
    path = (ROOT / relative).resolve()
    if not path.is_relative_to(ROOT) or not path.is_file():
        errors.append(f'Missing or unsafe registered file: {relative}')
        return None
    return path


required_sources = {'editor-gold-standards', 'terrain-editor-requirements', 'landscape-layering', 'topological-editor', 'soundscape-environment-manager', 'engine-base', 'multi-genre-game-creation-engine', 'texture-painting'}
source_ids = [s['id'] for s in register['sources']]
if len(source_ids) != len(set(source_ids)) or not required_sources.issubset(source_ids):
    errors.append('Expected all eight distinct registered owner sources')
for source in register['sources']:
    result = {'id': source['id'], 'originalPdf': 'UNAVAILABLE', 'textArchive': 'NOT_ARCHIVED'}
    for relative in source.get('analysisPaths', []):
        registered_path(relative)
    if source.get('textArchivePath'):
        path = registered_path(source['textArchivePath'])
        if path:
            text = path.read_text(encoding='utf-8')
            digest = hashlib.sha256(text.encode('utf-8')).hexdigest()
            markers = re.findall(r'<PARSED TEXT FOR PAGE: (\d+) / (\d+)>', text)
            result.update(textArchive='CHECKED', textSha256Lf=digest, pageMarkers=len(markers))
            if digest != source.get('textArchiveSha256Lf'):
                errors.append(f'{source["id"]}: normalized text archive hash mismatch')
            if len(markers) != source['textPageMarkers'] or len({m[0] for m in markers}) != len(markers):
                errors.append(f'{source["id"]}: missing or duplicate extracted page markers')
    if source.get('originalPdfPath'):
        path = registered_path(source['originalPdfPath'])
        if path:
            digest = hashlib.sha256(path.read_bytes()).hexdigest()
            if digest != source.get('originalPdfSha256'):
                errors.append(f'{source["id"]}: ORIGINAL PDF hash mismatch')
            else:
                result['originalPdf'] = 'HASH_VERIFIED'
    else:
        gaps.append(f'{source["id"]}: original PDF bytes unavailable; recorded PDF hash cannot be reverified')
    if source.get('originalPdfSha256') and not re.fullmatch('[a-f0-9]{64}', source['originalPdfSha256']):
        errors.append(f'{source["id"]}: invalid recorded original hash')
    sources.append(result)
for relative in register['earlierLinkedContracts']:
    registered_path(relative)
stage_ids = {stage['id'] for stage in register['stages']}
if stage_ids != {f'G{i}' for i in range(13)}:
    errors.append('Expected all gates G0–G12')
for stage in register['stages']:
    if not set(stage['dependsOn']).issubset(stage_ids):
        errors.append(f'{stage["id"]}: unknown dependency')
report = {'schema': 'beckenburg.editor-requirements-audit/v1', 'sources': sources,
          'errors': errors, 'provenanceGaps': gaps,
          'g0Accepted': not errors and not gaps,
          'featureAcceptance': 'Not evaluated by this provenance check'}
encoded = json.dumps(report, ensure_ascii=False, indent=2) + '\n'
if args.output:
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(encoded, encoding='utf-8')
else:
    print(encoded, end='')
raise SystemExit(1 if errors else 2 if args.require_originals and gaps else 0)
