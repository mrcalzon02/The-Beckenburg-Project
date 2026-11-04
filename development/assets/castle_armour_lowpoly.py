"""Sharp, low-count museum harness with an authored component texture atlas.

Budget reference: shipped cars372Ã¢â‚¬â€œ457 triangles; Matthis proxy1239 triangles.
Target<=1250 including the display plinth. Rivets, etching, narrow lames,
perforations and edge wear are texture details, not hundreds of small meshes.
"""
import bpy, math, random, bmesh, json
from pathlib import Path
from mathutils import Vector
from mathutils.geometry import intersect_ray_tri
from mathutils.bvhtree import BVHTree

def build(*unused):
    size=512
    # Pixel-space regions follow actual object functions and relative visual area.
    regions={
        'chest':(0,0,128,160),'back':(128,0,128,160),'helmet':(256,0,128,112),
        'shoulder':(384,0,128,112),'arm':(256,112,64,144),'hand':(320,112,64,112),
        'shin':(384,112,64,192),'knee':(448,112,64,96),'waist':(0,160,128,64),
        'tasset':(128,160,128,128),'foot':(0,288,128,128),'leather':(128,288,64,128),
        'stone':(256,320,128,128)}
    # Owner direction: generated material artwork, original UV islands unchanged.
    image=bpy.data.images.load(str(Path(__file__).with_name('castle-museum')/'textures/armour-imagegen-master.png'))
    image.name='Castle field harness / generated component atlas';image.scale(512,512)
    image.filepath_raw=str(Path(__file__).with_name('castle-museum')/'textures/armour-runtime-atlas.png');image.file_format='PNG';image.save();image.pack()
    mat=bpy.data.materials.new('Castle field harness / painted steel leather stone');mat.use_nodes=True
    shader=mat.node_tree.nodes.get('Principled BSDF');shader.inputs['Roughness'].default_value=.58;shader.inputs['Metallic'].default_value=.32
    tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=image;tex.interpolation='Closest';mat.node_tree.links.new(tex.outputs['Color'],shader.inputs['Base Color'])
    verts=[];faces=[];uvs=[]

    def face(points,coords,region):
        ox,oy,w,h=regions[region];start=len(verts);verts.extend(points)
        faces.append(tuple(range(start,start+len(points))))
        uvs.append([((ox+2+u*(w-4))/size,(oy+2+v*(h-4))/size) for u,v in coords])

    def loft(name,rings,n,region,start=-math.pi,end=math.pi,cx=0,ridge=0,cap=False):
        # rings: height,halfwidth,halfdepth,centreY, optional centreX.
        first_face=len(faces);rows=[]
        for z,w,d,y,*offset in rings:
            xc=offset[0] if offset else cx
            rows.append([(xc+w*math.sin(start+(end-start)*i/n),
                          y-d*math.cos(start+(end-start)*i/n)-ridge*max(0,math.cos(start+(end-start)*i/n))**8,z) for i in range(n+1)])
        for j in range(len(rows)-1):
            for k in range(n):
                slot=region;u0=k/n;u1=(k+1)/n
                if name=='cuirass':
                    a=start+(end-start)*k/n;b=start+(end-start)*(k+1)/n
                    if math.cos((a+b)/2)>=0:
                        slot='chest';u0=(a+math.pi/2)/math.pi;u1=(b+math.pi/2)/math.pi
                    else:
                        slot='back'
                        if a<0:a+=2*math.pi;b+=2*math.pi
                        u0=(a-math.pi/2)/math.pi;u1=(b-math.pi/2)/math.pi
                face([rows[j][k],rows[j][k+1],rows[j+1][k+1],rows[j+1][k]],
                     [(u0,j/(len(rows)-1)),(u1,j/(len(rows)-1)),(u1,(j+1)/(len(rows)-1)),(u0,(j+1)/(len(rows)-1))],slot)
        if cap:
            for row in [rows[0],list(reversed(rows[-1]))]:
                face(row[:-1],[(.5+.45*math.sin(2*math.pi*i/n),.5+.45*math.cos(2*math.pi*i/n)) for i in range(n)],region)
        return list(range(first_face,len(faces)))

    def front_surface_y(indices,x,z):
        hits=[]
        for index in indices:
            points=[Vector(verts[i]) for i in faces[index]]
            for i in range(1,len(points)-1):
                hit=intersect_ray_tri(points[0],points[i],points[i+1],Vector((0,1,0)),Vector((x,-2,z)),True)
                if hit is not None:hits.append(hit.y)
        if not hits:raise RuntimeError('Unsupported armour attachment at '+str((x,z)))
        return min(hits)

    def block(x,y,z,w,d,h,region):
        p=[(x+sx*w/2,y+sy*d/2,z+sz*h/2) for sz in [-1,1] for sy in [-1,1] for sx in [-1,1]]
        for indices in [(0,1,3,2),(4,6,7,5),(0,4,5,1),(2,3,7,6),(0,2,6,4),(1,5,7,3)]:
            face([p[i] for i in indices],[(0,0),(1,0),(1,1),(0,1)],region)

    block(0,0,.04,.73,.63,.08,'stone');block(0,0,.125,.65,.55,.09,'stone')
    # Chest keeps an angular medial ridge and a tapered, pointed lower front.
    loft('cuirass',[(1.18,.16,.105,0),(1.30,.19,.17,0),(1.48,.235,.14,0),(1.64,.19,.075,0)],
         12,'chest',ridge=.028)
    loft('gorget',[(1.635,.14,.105,0),(1.77,.085,.075,0)],8,'waist')
    fauld_faces=loft('fauld',[(1.05,.255,.18,0),(1.19,.18,.13,0)],12,'waist')
    # Two shaped thigh defenses; narrow lames/rivets live in their own atlas island.
    for sign in [-1,1]:
        centre=sign*.15;rows=[]
        for j in range(3):
            width=.115+j*.01;z=1.105-j*.155
            rows.append([(centre+u*width,-.198*math.sqrt(max(.06,1-((centre+u*width)/.32)**2))-j*.006,z+.012*(1-u*u)) for u in [-1,-.5,0,.5,1]])
        tasset_start=len(faces)
        for j in range(2):
            for k in range(4):
                face([rows[j][k],rows[j][k+1],rows[j+1][k+1],rows[j+1][k]],
                     [(k/4,1-j/2),((k+1)/4,1-j/2),((k+1)/4,1-(j+1)/2),(k/4,1-(j+1)/2)],'tasset')
        tasset_faces=list(range(tasset_start,len(faces)))
        for sx in [-.07,.07]:
            x=centre+sx;anchor=centre+sx*.55
            rows=[]
            for z,xx,support,depth in [(1.135,anchor,fauld_faces,.001),(1.115,anchor,fauld_faces,-.001),
                                       (1.085,x,tasset_faces,-.001),(1.065,x,tasset_faces,.001)]:
                # Query actual faceted plate triangles, not an approximate ellipse.
                rows.append([(q,front_surface_y(support,q,z)+depth,z) for q in [xx-.011,xx+.011]])
            for j in range(3):
                face([rows[j][0],rows[j][1],rows[j+1][1],rows[j+1][0]],[(0,1-j/3),(1,1-j/3),(1,1-(j+1)/3),(0,1-(j+1)/3)],'leather')

        # Broad plate shapes provide the limb anatomy; small seams/hinges are painted.
        x=sign*.153
        loft('thigh',[(.80,.085,.092,0,x),(1.06,.11,.12,0,x)],8,'shin')
        loft('knee',[(.71,.073,.08,0,x),(.76,.098,.12,-.005,x),(.81,.084,.095,0,x)],8,'knee')
        loft('greave',[(.285,.052,.063,0,x),(.52,.080,.105,.01,x),(.735,.073,.085,0,x)],8,'shin',ridge=.012)
        wing=[(x+sign*.06,-.025,.80),(x+sign*.10,-.02,.765),(x+sign*.10,-.025,.72),(x+sign*.06,-.04,.70)]
        face(wing,[(0,1),(1,.8),(1,.2),(0,0)],'knee')

        # A closed toe cap, enclosed sides and a rising instep form one coherent
        # foot silhouette. Horizontal articulation is painted, not loose strips.
        foot=[(-.24,.035,.216),(-.22,.071,.238),(-.16,.084,.252),(-.09,.077,.276),
              (-.025,.063,.309),(.045,.058,.304),(.085,.048,.263)]
        footrows=[]
        for y,w,top in foot:
            footrows.append([(x+w*math.sin(-math.pi/2+math.pi*k/6),y,
                              .184+(top-.184)*max(0,math.cos(-math.pi/2+math.pi*k/6))**.65) for k in range(7)])
        for j in range(len(footrows)-1):
            for k in range(6):
                face([footrows[j][k],footrows[j][k+1],footrows[j+1][k+1],footrows[j+1][k]],
                     [(k/6,j/6),((k+1)/6,j/6),((k+1)/6,(j+1)/6),(k/6,(j+1)/6)],'foot')
            face([footrows[j][0],footrows[j+1][0],footrows[j+1][-1],footrows[j][-1]],[(0,0),(1,0),(1,1),(0,1)],'leather')
        for row in [footrows[0],list(reversed(footrows[-1]))]:
            face(row,[(.2+.6*k/6,.08+.04*math.sin(math.pi*k/6)) for k in range(7)],'foot')
        # Shaped sole seats at the plinth, with no rectangular shoe protrusion.
        outline=[(x-w,y,.17) for y,w,top in foot]+[(x+w,y,.17) for y,w,top in reversed(foot)]
        face(list(reversed(outline)),[(.5+(p[0]-x)/.18,(p[1]+.24)/.325) for p in reversed(outline)],'leather')
        for j,a in enumerate(outline):
            b=outline[(j+1)%len(outline)]
            face([a,b,(b[0],b[1],.184),(a[0],a[1],.184)],[(0,0),(1,0),(1,1),(0,1)],'leather')
        loft('heel and ankle',[(.22,.055,.058,.023,x),(.315,.053,.061,.012,x)],8,'foot')

        # Shoulder outline has a cap and falling skirt, not a spherical joint.
        loft('pauldron',[(1.43,.105,.123,0,sign*.29),(1.57,.133,.147,0,sign*.275),
             (1.665,.075,.09,0,sign*.25)],8,'shoulder',cap=True)
        loft('upper arm',[(1.245,.062,.07,0,sign*.325),(1.46,.075,.081,0,sign*.30)],8,'arm')
        loft('elbow',[(1.18,.062,.075,0,sign*.325),(1.23,.078,.09,-.006,sign*.325),
             (1.28,.065,.078,0,sign*.323)],8,'knee')
        loft('forearm',[(.995,.047,.052,-.02,sign*.31),(1.20,.065,.071,0,sign*.325)],8,'arm')
        loft('gauntlet',[(.86,.042,.047,-.035,sign*.305),(.94,.052,.06,-.03,sign*.31),
             (1.015,.047,.052,-.02,sign*.31)],6,'hand',cap=True)
        ex=sign*.325
        face([(ex+sign*.04,.01,1.28),(ex+sign*.085,.01,1.25),(ex+sign*.08,-.025,1.19),(ex+sign*.04,-.035,1.165)],[(0,1),(1,.8),(1,.2),(0,0)],'knee')

    # Separate faceted visor halves leave a genuine narrow sight aperture.
    loft('helmet crown',[(1.885,.113,.115,.01),(1.97,.09,.09,.015),(2.025,.025,.035,.01)],
         8,'helmet',cap=True)
    loft('helmet back',[(1.75,.082,.08,.015),(1.90,.113,.10,.015)],6,'helmet',1.15,2*math.pi-1.15)
    loft('visor and bevor',[(1.755,.077,.09,0),(1.82,.105,.143,0),(1.866,.11,.128,0)],
         6,'helmet',-1.18,1.18,ridge=.008)
    loft('helmet interior',[(1.76,.073,.07,.018),(1.90,.09,.08,.018)],6,'leather')
    # A single sharp forged comb is structural silhouette, not a wire handle.
    crest=[(-.085,1.955),(-.025,2.055),(.03,2.055),(.095,1.97)]
    for sx in [-.005,.005]:
        face([(sx,y,z) for y,z in crest],[(0,0),(.35,1),(.65,1),(1,0)],'helmet')

    mesh=bpy.data.meshes.new('Reference-shaped low-poly field harness');mesh.from_pydata(verts,[],faces);mesh.uv_layers.new()
    obj=bpy.data.objects.new('Museum harness / low-poly silhouette and painted articulation',mesh);bpy.context.collection.objects.link(obj);mesh.materials.append(mat)
    for poly,coords in zip(mesh.polygons,uvs):
        for loop,uv in zip(poly.loop_indices,coords):mesh.uv_layers.active.data[loop].uv=uv
    bm=bmesh.new();bm.from_mesh(mesh);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=.000001)
    bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(mesh);bm.free()
    mesh.calc_loop_triangles()
    obj['triangle_budget_reference']='Cars372Ã¢â‚¬â€œ457; Matthis proxy1239; target<=1250 including plinth'
    obj['authored_triangle_count']=len(mesh.loop_triangles)
    if len(mesh.loop_triangles)>1250:raise RuntimeError('Armour exceeds measured character-model budget: '+str(len(mesh.loop_triangles)))
    audit_contacts(mesh)

def audit_contacts(mesh,phase='source',source_sha256=None):
    """A shared object name is not connectivity. Audit actual surface contact.
    Intentional separate plates must intersect or touch within1mm, and every
    component must reach the floor plinth through that physical-contact graph.
    """
    adjacency=[set() for v in mesh.vertices]
    for edge in mesh.edges:
        a,b=edge.vertices;adjacency[a].add(b);adjacency[b].add(a)
    groups=[];remaining=set(range(len(mesh.vertices)))
    while remaining:
        seed=remaining.pop();group={seed};queue=[seed]
        while queue:
            for neighbor in adjacency[queue.pop()]:
                if neighbor in remaining:remaining.remove(neighbor);group.add(neighbor);queue.append(neighbor)
        groups.append(group)
    points=[v.co.copy() for v in mesh.vertices]
    components=[]
    for group in groups:
        triangles=[tuple(t.vertices) for t in mesh.loop_triangles if t.vertices[0] in group]
        components.append(dict(vertices=group,triangles=triangles,bvh=BVHTree.FromPolygons(points,triangles,all_triangles=True),
            lo=[min(points[i][a] for i in group) for a in range(3)],hi=[max(points[i][a] for i in group) for a in range(3)]))
    contacts=[set() for c in components];epsilon=.001
    def intersect(a,b):
        for index in a['vertices']:
            hit=b['bvh'].find_nearest(points[index],epsilon)
            if hit[0] is not None and hit[3]<=epsilon:return True
        for index in b['vertices']:
            hit=a['bvh'].find_nearest(points[index],epsilon)
            if hit[0] is not None and hit[3]<=epsilon:return True
        for ai,bi in a['bvh'].overlap(b['bvh']):
            ta=[points[i] for i in a['triangles'][ai]];tb=[points[i] for i in b['triangles'][bi]]
            for first,second in [(ta,tb),(tb,ta)]:
                for i in range(3):
                    origin=first[i];direction=first[(i+1)%3]-origin
                    hit=intersect_ray_tri(*second,direction,origin,True)
                    if hit is not None and direction.length_squared>1e-14:
                        t=(hit-origin).dot(direction)/direction.length_squared
                        if -1e-6<=t<=1+1e-6:return True
        return False
    for i,a in enumerate(components):
        for j in range(i):
            b=components[j]
            if any(a['lo'][axis]>b['hi'][axis]+epsilon or b['lo'][axis]>a['hi'][axis]+epsilon for axis in range(3)):continue
            if intersect(a,b):contacts[i].add(j);contacts[j].add(i)
    roots={i for i,c in enumerate(components) if c['lo'][2]<.001};reached=set(roots);queue=list(roots)
    while queue:
        for i in contacts[queue.pop()]:
            if i not in reached:reached.add(i);queue.append(i)
    report=dict(status='PASS' if len(reached)==len(components) else 'FAIL',phase=phase,source_sha256=source_sha256,triangle_count=len(mesh.loop_triangles),
        tolerance_metres=epsilon,criterion='Every geometry component reaches the floor plinth through actual surface intersection/contact, not shared object name or bounding-box overlap',
        components=[dict(id=i,vertices=len(c['vertices']),bounds=[c['lo'],c['hi']],contacts=sorted(contacts[i]),supported=i in reached) for i,c in enumerate(components)])
    out=Path(__file__).with_name('castle-museum')/('armour-contact-audit'+('' if phase=='source' else '-'+phase)+'.json');out.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    if report['status']!='PASS':raise RuntimeError('Detached armour components: '+str([c for c in report['components'] if not c['supported']]))
    return report
