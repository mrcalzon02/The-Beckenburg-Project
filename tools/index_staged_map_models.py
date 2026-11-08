"""Index existing source work; never substitute a small furnishing sample for it."""
from pathlib import Path
import argparse,collections,hashlib,json,subprocess,os,tempfile

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'development/assets'

def write_atomic(path,raw):
    handle,name=tempfile.mkstemp(prefix='.'+path.name+'-',dir=path.parent)
    stage=Path(name)
    try:
        with os.fdopen(handle,'wb') as stream:stream.write(raw)
        os.replace(stage,path)
    finally:stage.unlink(missing_ok=True)

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--check',action='store_true');parser.add_argument('--query');parser.add_argument('--site')
    args=parser.parse_args()
    target=DEST/'asset-library-index.json'
    if args.query or args.site:
        data=json.loads(target.read_text(encoding='utf-8'))
        rows=data['model_files']
        if args.site:rows=[r for r in rows if args.site.upper() in r['maps'] or '*' in r['maps']]
        if args.query:
            words=args.query.casefold().split();rows=[r for r in rows if all(w in json.dumps(r,ensure_ascii=False).casefold() for w in words)]
        for r in rows:print(r['id']+' | '+r['collection']+' | '+r['path'])
        print(len(rows),'matching model files');return
    base=ROOT/'development/levels/primitive-props-v1'
    catalog=json.loads((base/'catalog.json').read_text(encoding='utf-8'))
    refined=json.loads((base/'refinement/index.json').read_text(encoding='utf-8'))
    aliases=[]
    for row in catalog['models']:
        path=base/'models'/(row['id']+'.glb');assert path.exists(),row['id']
        aliases.append(dict(id=row['id'],label=row['label'],maps=row['maps'],category=row['category'],path=path.relative_to(ROOT).as_posix(),
            collection='existing_map_specific_roughs',status='SOURCE_ROUGH_FOR_REFINEMENT',notes=row.get('notes',''),authoring_hints=row.get('authoring_hints',{})))
    for row in refined['models']:
        path=base/'refinement'/(row['id']+'.glb');assert path.exists(),row['id']
        aliases.append(dict(id=row['id'],label=row['label'],maps=row['maps'],category='refined_site_prop',path=path.relative_to(ROOT).as_posix(),
            collection='existing_map_specific_refinements',status='SOURCE_REFINEMENT_REVIEW_PENDING',mount=row['mount']))
    for path in sorted((ROOT/'assets/runtime/models').glob('*.glb')):
        native=path.stem.startswith('level_hc_native_')
        aliases.append(dict(id=path.stem,label=path.stem,maps=['WLD-02'] if native else [],category='native_map_component' if native else 'runtime_model',
            path=path.relative_to(ROOT).as_posix(),collection='runtime_native_map_parts' if native else 'existing_runtime_library',status='EXISTING_SHIPPED_SOURCE_NOT_NEW_REVIEW_ACCEPTANCE'))
    known={row['path'] for row in aliases}
    paths=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z','--','*.glb'],cwd=ROOT).decode('utf-8').split('\0')
    for name in sorted(set(paths)-known-{''}):
        path=ROOT/name;assert path.is_file(),('Missing indexed model',name)
        if name.startswith('assets/libraries/'):collection='source_library_masters'
        elif any(s in name for s in ['/site-kits/','/previews/','/relic-review/','/crypt-review/','_kit_refined_']):collection='review_assemblies'
        elif any(s in name for s in ['/rebuild/structures/','/blockouts/','/rebuild/furnishings/','/terrain-overall-pass/assemblies/']):collection='native_map_exports'
        elif any(s in name for s in ['/rejected/','/references/','/evidence/']):collection='reference_and_rejected_studies'
        else:collection='additional_custom_model_sources'
        aliases.append(dict(id=path.stem,label=path.stem,maps=[],category=collection,path=name,collection=collection,
            status='EXISTING_SOURCE_CLASSIFY_BEFORE_PLACEMENT',notes='Retained source, alias or assembly. Consult its neighbouring manifest and refinement history; a review assembly is not an individual prop.'))
    by_hash={}
    for row in aliases:
        path=ROOT/row['path'];row['sha256']=hashlib.sha256(path.read_bytes()).hexdigest();row['bytes']=path.stat().st_size
        by_hash.setdefault(row['sha256'],[]).append(row['path'])
    archive=json.loads((ROOT/'development/assets/local-library-manifest.json').read_text(encoding='utf-8'))
    formats=collections.Counter(row['format'] for row in archive['files'])
    source_chats=json.loads((DEST/'model-source-conversations.json').read_text(encoding='utf-8'))['sources']
    result=dict(status='LOCAL_SOURCE_INDEX_COMPLETE_NAMED_CHAT_HISTORY_PARTIALLY_RECOVERED',
        rule='Use map-specific staged props and their refinements first, retaining source history. Rough assets are authorized for spatial blocking, model budgets and placement now; refinement follows. Source rough status is not a reason to replace them with generic furniture or invent duplicates. A furniture pilot is not the master inventory.',
        local_files=len(aliases),unique_local_file_hashes=len(by_hash),collections=dict(collections.Counter(r['collection'] for r in aliases)),
        archive_index=dict(path='development/assets/local-library-manifest.json',entries=len(archive['files']),model_format_entries={k:formats[k] for k in ['.glb','.fbx','.obj','.blend']},note='Archive formats overlap; these are file representations, not a count of unique props. This index does not assert archive files were downloaded or inspected.'),
        named_source_chats=source_chats,
        model_files=aliases,hash_aliases=[dict(sha256=sha,paths=paths) for sha,paths in by_hash.items() if len(paths)>1],
        sites=[dict(id=s['id'],title=s['title'],staged_rough_ids=s['prototypes'],requirements=s['requirements']) for s in catalog['sites']])
    DEST.mkdir(parents=True,exist_ok=True)
    raw=(json.dumps(result,indent=2,ensure_ascii=False)+'\n').encode()
    if args.check:assert target.read_bytes()==raw,'Asset library index is stale; regenerate it'
    else:write_atomic(target,raw)
    pointer=ROOT/'ASSET_LIBRARY.md';text=pointer.read_text(encoding='utf-8')
    start='<!-- asset-library-counts:start -->';end='<!-- asset-library-counts:end -->'
    names={'existing_map_specific_roughs':'Map-specific rough props','existing_map_specific_refinements':'Existing prop refinements','existing_runtime_library':'Runtime model library',
        'runtime_native_map_parts':'Hollow Creek native map components','source_library_masters':'Source-library masters','review_assemblies':'Review/contact assemblies',
        'native_map_exports':'Native map exports','reference_and_rejected_studies':'Reference/rejected studies','additional_custom_model_sources':'Additional custom model sources'}
    table='\n\n| Collection | Local files |\n|---|---:|\n'+''.join(f'| {names[k]} | {count:,} |\n' for k,count in result['collections'].items())
    table+=f'| **Total local GLB files** | **{len(aliases):,}** |\n\n{len(by_hash):,} distinct file hashes. Copies and review assemblies are indexed\nwithout counting them as unique usable props. Source stage and intended use\nremain attached to every entry.\n\n'
    a=text.index(start)+len(start);b=text.index(end);updated=text[:a]+table+text[b:]
    if args.check:assert text==updated,'Asset library pointer counts are stale'
    else:write_atomic(pointer,updated.encode())
    print(result['local_files'],'local model files;',result['unique_local_file_hashes'],'distinct file hashes;',result['collections'])

if __name__=='__main__':main()
