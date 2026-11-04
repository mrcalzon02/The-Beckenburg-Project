"""Reference-led articulated field harness. Blender Z-up metres.

Original construction informed by Met 42.50.27aâ€“n and 2013.28, not a replica.
Separate shells are intentional moving plates, with overlapping lames and
physical straps/rivets. No disconnected rectangular tasset stand-ins.
"""
import bpy, math, bmesh
from mathutils import Vector

def build(material,box,rod,ellipsoid):
    steel=material('Shaped field harness steel',(.28,.30,.32),'iron')
    edge=material('Turned steel borders',(.36,.37,.38),'iron')
    leather=material('Oxhide suspension straps',(.085,.045,.028),'leather')
    lining=material('Burgundy arming lining',(.10,.018,.025),'cloth')
    rivet=material('Peened brass fasteners',(.37,.29,.14),'brass')
    stone=material('Museum limestone plinth',(.34,.33,.29))
    steel.node_tree.nodes.get('Principled BSDF').inputs['Roughness'].default_value=.34
    edge.node_tree.nodes.get('Principled BSDF').inputs['Roughness'].default_value=.28

    def shell(name,rings,mat,segments=24,start=-math.pi,end=math.pi,cx=0,ridge=0,omit=None):
        """Each ring: z, halfwidth, halfdepth, centreY, optional centreX.
        Surface sheet is closed with real thickness after shaped edges are made.
        """
        verts=[];faces=[];uv=[]
        for j,row in enumerate(rings):
            z,w,d,y,*offset=row;xcentre=offset[0] if offset else cx
            for k in range(segments+1):
                angle=start+(end-start)*k/segments
                forward=max(0,math.cos(angle))
                x=xcentre+w*math.sin(angle)
                yy=y-d*math.cos(angle)-ridge*forward**12
                zz=z
                if name=='Ridged breastplate' and j==len(rings)-1:
                    zz-=.10*abs(math.sin(angle))**3
                if name=='Ridged breastplate' and j==0:
                    zz-=.04*forward**6
                verts.append((x,yy,zz));uv.append((k/segments,j/(len(rings)-1)))
        for j in range(len(rings)-1):
            for k in range(segments):
                if omit and omit(j,k):continue
                a=j*(segments+1)+k;faces.append((a,a+1,a+segments+2,a+segments+1))
        mesh=bpy.data.meshes.new(name);mesh.from_pydata(verts,[],faces);mesh.uv_layers.new()
        obj=bpy.data.objects.new(name,mesh);bpy.context.collection.objects.link(obj);mesh.materials.append(mat)
        for poly in mesh.polygons:
            poly.use_smooth=True
            for loop in poly.loop_indices:mesh.uv_layers.active.data[loop].uv=uv[mesh.loops[loop].vertex_index]
        modifier=obj.modifiers.new('Forged plate thickness','SOLIDIFY');modifier.thickness=.005;modifier.offset=-1
        if name=='Ridged breastplate':
            for e in mesh.edges:
                if all(abs(mesh.vertices[i].co.x)<1e-6 for i in e.vertices):e.use_edge_sharp=True
            split=obj.modifiers.new('Forged medial ridge normal','EDGE_SPLIT');split.use_edge_angle=False;split.use_edge_sharp=True
        return obj

    def polyline(name,points,radius,mat):
        for a,b in zip(points,points[1:]):rod(name,a,b,radius,mat,8)

    def ring_edge(name,z,w,d,y,start,end,cx=0,segments=24,ridge=0):
        pts=[]
        for i in range(segments+1):
            t=start+(end-start)*i/segments
            pts.append((cx+w*math.sin(t),y-d*math.cos(t)-ridge*max(0,math.cos(t))**12,z))
        polyline(name,pts,.0035,edge)

    def stud(name,x,y,z):
        # Short shank passes through the plate instead of a hovering gold dot.
        rod(name+' shank',(x,y+.009,z),(x,y-.004,z),.004,rivet,8)
        ellipsoid(name+' peened head',(x,y-.005,z),(.006,.003,.006),rivet,8,4)

    box('Exhibit plinth foot',(0,0,.045),(.73,.63,.09),stone,.012)
    box('Exhibit plinth crown',(0,0,.13),(.65,.55,.08),stone,.009)

    # Narrow waist, pronounced medial ridge, curved chest and real arm cutouts.
    chest=[(1.18,.155,.115,0),(1.25,.175,.175,0),(1.38,.215,.182,0),
           (1.51,.235,.145,0),(1.61,.220,.106,0),(1.655,.195,.080,0)]
    shell('Ridged breastplate',chest,steel,32,-1.64,1.64,ridge=.035)
    shell('Separate backplate',[(1.18,.166,.10,.015),(1.32,.226,.12,.015),
          (1.50,.25,.11,.015),(1.62,.195,.075,.015)],steel,24,1.50,2*math.pi-1.50)
    # Front neckline and armhole rims follow the actual shaped boundary.
    rim=[]
    for i in range(33):
        t=-1.64+3.28*i/32
        rim.append((.195*math.sin(t),-.080*math.cos(t)-.035*max(0,math.cos(t))**12,1.655-.10*abs(math.sin(t))**3))
    polyline('Rolled neck and arm opening border',rim,.004,edge)
    for sign in [-1,1]:
        x=sign*.17
        box('Cuirass side leather closure',(sign*.175,.025,1.26),(.028,.09,.035),leather,.003)
        for z in [1.30,1.47]:stud('Side closure rivet',sign*.225,-.07,z)
    # Gorget rises continuously from the shoulders to the helmet's neck ring.
    for j in range(3):
        z=1.635+j*.032;w=.15-j*.025;d=.12-j*.017
        shell('Overlapping gorget lame',[(z,w,d,0),(z+.047,w-.016,d-.008,0)],steel,24)
        ring_edge('Gorget turned border',z,w,d,0,-math.pi,math.pi)

    # Four nested fauld lames overlap by 16mm; widest lower lame carries tassets.
    for j in range(4):
        z=1.19-j*.039;w=.18+j*.022;d=.135+j*.015
        shell('Articulated waist lame '+str(j),[(z-.050,w+.013,d+.009,0),(z+.010,w,d,0)],steel,28)
        ring_edge('Fauld rolled lower edge',z-.05,w+.013,d+.009,0,-math.pi,math.pi)
        for sign in [-1,1]:stud('Waist articulation rivet',sign*w*.72,-d*.73,z-.025)

    # Paired curved, five-lame thigh defenses. Each is hung by two real straps.
    for sign in [-1,1]:
        centre=sign*.151
        for sx in [-.07,.07]:
            x=centre+sx;anchor=centre+sx*.55
            anchorY=-.155*math.sqrt(max(.01,1-(anchor/.211)**2))
            y=-.195*math.sqrt(max(.05,1-(x/.315)**2))
            points=[(anchor,anchorY,1.14),(x,(anchorY+y)/2,1.105),(x,y+.003,1.06)]
            vertices=[(xx+side*.012,yy,zz) for xx,yy,zz in points for side in [-1,1]]
            mesh=bpy.data.meshes.new('Bent flat tasset strap');mesh.from_pydata(vertices,[],[(0,1,3,2),(2,3,5,4)]);mesh.uv_layers.new()
            strap=bpy.data.objects.new('Fauld to tasset leather suspension',mesh);bpy.context.collection.objects.link(strap);mesh.materials.append(leather)
            mod=strap.modifiers.new('Leather thickness','SOLIDIFY');mod.thickness=.004
            stud('Tasset fauld anchor',anchor,anchorY-.002,1.14)
            box('Tasset strap buckle',(x,(anchorY+y)/2-.004,1.105),(.031,.012,.022),rivet,.002)
        for j in range(5):
            top=1.09-j*.052;half=.117+j*.003;vertices=[];uvs=[]
            for row in range(2):
                for k in range(13):
                    u=-1+2*k/12
                    y=-.195*math.sqrt(max(.05,1-((centre+u*half)/.315)**2))-j*.005
                    z=top-row*.073+.012*(1-u*u)
                    vertices.append((centre+u*half,y,z));uvs.append(((u+1)/2,row))
            faces=[(k,k+1,k+14,k+13) for k in range(12)]
            mesh=bpy.data.meshes.new('Curved overlapping tasset lame');mesh.from_pydata(vertices,[],faces);mesh.uv_layers.new()
            obj=bpy.data.objects.new(('Left' if sign<0 else 'Right')+' tasset articulated lame '+str(j),mesh);bpy.context.collection.objects.link(obj);mesh.materials.append(steel)
            for poly in mesh.polygons:
                for loop in poly.loop_indices:mesh.uv_layers.active.data[loop].uv=uvs[mesh.loops[loop].vertex_index]
            modifier=obj.modifiers.new('Steel tasset thickness','SOLIDIFY');modifier.thickness=.005;modifier.offset=-1
            polyline('Turned tasset lame edge',[vertices[k+13] for k in range(13)],.003,edge)
            for u in [-.78,.78]:
                x=centre+u*half;y=-.195*math.sqrt(max(.05,1-(x/.315)**2))-j*.005
                stud('Tasset sliding rivet',x,y,top-.025)
                if j<4:rod('Hidden tasset leather bearer',(x,y+.005,top-.01),(x,y+.005,top-.075),.007,leather,6)

    # Anatomical limb plates: thigh, winged knee and tapered shin/calf.
    for sign in [-1,1]:
        x=sign*.153
        shell('Shaped cuisse',[(.80,.089,.095,0,x),(.91,.107,.123,0,x),(1.06,.112,.125,0,x)],steel,20,ridge=.012)
        for j in range(2):
            z=.78+j*.038;shell('Knee articulation lame',[(z-.018,.091,.104,-.005,x),(z+.035,.097,.105,-.005,x)],steel,20)
        shell('Shaped knee cop',[(.715,.077,.085,0,x),(.755,.106,.126,-.006,x),(.80,.091,.10,0,x)],steel,20,ridge=.009)
        wing=[(x+sign*.07,-.01,.80),(x+sign*.095,-.005,.775),(x+sign*.10,-.015,.73),(x+sign*.08,-.04,.705)]
        mesh=bpy.data.meshes.new('Knee wing');mesh.from_pydata(wing,[],[(0,1,2,3)]);mesh.uv_layers.new()
        obj=bpy.data.objects.new('Shaped knee wing',mesh);bpy.context.collection.objects.link(obj);mesh.materials.append(steel)
        mod=obj.modifiers.new('Knee wing steel','SOLIDIFY');mod.thickness=.008
        polyline('Rolled knee wing edge',wing+[wing[0]],.0035,edge)
        shell('Calf and ridged greave',[(.285,.052,.065,0,x),(.40,.059,.077,.005,x),
             (.55,.082,.107,.012,x),(.675,.077,.093,.005,x),(.71,.071,.08,0,x)],steel,24,ridge=.016)
        for z,w,d in [(.30,.053,.065),(.69,.075,.087)]:ring_edge('Greave turned rim',z,w,d,0,-math.pi,math.pi,x)
        for z in [.38,.62]:stud('Greave hinge',x+sign*.067,-.025,z)
        # Individual overlapping sabaton lames follow the foot, not an ellipsoid.
        for j in range(7):
            y=-.205+j*.046;width=.063+.022*math.sin(math.pi*j/8)
            z=.215+.07*(j/6)**1.4;vertices=[]
            for yy in [y-.030,y+.030]:
                for k in range(9):
                    u=-1+2*k/8;vertices.append((x+u*width,yy,z+.038*(1-u*u)))
            faces=[(k,k+1,k+10,k+9) for k in range(8)]
            mesh=bpy.data.meshes.new('Articulated foot plate');mesh.from_pydata(vertices,[],faces);mesh.uv_layers.new()
            obj=bpy.data.objects.new('Overlapping sabaton lame',mesh);bpy.context.collection.objects.link(obj);mesh.materials.append(steel)
            for poly in mesh.polygons:
                for loop in poly.loop_indices:
                    co=mesh.vertices[mesh.loops[loop].vertex_index].co;mesh.uv_layers.active.data[loop].uv=(co.x*4,co.y*4)
            mod=obj.modifiers.new('Sabatons steel','SOLIDIFY');mod.thickness=.005
            polyline('Sabatons turned edge',vertices[:9],.0025,edge)
        box('Boot sole',(x,-.065,.1865),(.15,.33,.035),leather,.008)
        shell('Leather boot beneath foot lames',[(.202,.074,.162,-.065,x),(.235,.074,.150,-.065,x),
              (.280,.053,.085,.005,x),(.305,.050,.065,.005,x)],leather,20)

    # Shoulder caps and overlapping articulated upper-arm lames.
    for sign in [-1,1]:
        shell('Shaped pauldron cap',[(1.535,.125,.155,0,sign*.282),(1.60,.132,.145,0,sign*.275),
              (1.655,.091,.111,0,sign*.255),(1.678,.030,.045,0,sign*.245),(1.687,.003,.004,0,sign*.245)],steel,24)
        for j in range(4):
            z=1.55-j*.038;cx=sign*(.275+j*.008)
            shell('Pauldron descending lame',[(z-.034,.115,.135,0,cx),(z+.02,.120,.139,0,cx)],steel,20)
            ring_edge('Shoulder lame border',z-.034,.115,.135,0,-math.pi,math.pi,cx,20)
            stud('Pauldron articulation rivet',cx+sign*.095,-.06,z-.01)
        shell('Upper arm rerebrace',[(1.245,.069,.075,0,sign*.320),(1.33,.079,.087,0,sign*.315),
              (1.45,.085,.09,0,sign*.305)],steel,20)
        # Cloth at elbow opening connects the armour parts without a toy ball joint.
        shell('Elbow arming cloth',[(1.17,.066,.065,0,sign*.325),(1.27,.067,.067,0,sign*.323)],lining,16)
        shell('Elbow couter',[(1.185,.063,.075,0,sign*.325),(1.23,.083,.097,-.008,sign*.325),
              (1.275,.068,.08,0,sign*.323)],steel,20,ridge=.008)
        x=sign*.325
        wing=[(x+sign*.04,.02,1.285),(x+sign*.07,.015,1.255),(x+sign*.07,-.025,1.185),(x+sign*.04,-.045,1.165)]
        mesh=bpy.data.meshes.new('Elbow wing');mesh.from_pydata(wing,[],[(0,1,2,3)]);mesh.uv_layers.new()
        obj=bpy.data.objects.new('Flared elbow wing',mesh);bpy.context.collection.objects.link(obj);mesh.materials.append(steel)
        mod=obj.modifiers.new('Elbow wing steel','SOLIDIFY');mod.thickness=.007
        polyline('Couter turned wing edge',wing+[wing[0]],.0035,edge)
        shell('Tapered forearm vambrace',[(.995,.048,.055,-.025,sign*.315),(1.08,.063,.071,-.012,sign*.324),
              (1.18,.073,.076,0,sign*.325)],steel,20,ridge=.006)
        shell('Flared gauntlet cuff',[(.94,.065,.067,-.03,sign*.31),(1.015,.049,.058,-.025,sign*.315)],steel,20)
        for j in range(3):
            z=.945-j*.025;shell('Gauntlet metacarpal lame',[(z-.033,.046,.052,-.035,sign*.305),(z+.012,.052,.055,-.03,sign*.308)],steel,16)
        for finger in range(4):
            fx=sign*.305+(finger-1.5)*.021
            rod('Finger backing glove',(fx,-.034,.855),(fx,-.051,.905),.010,leather,6)
            for j in range(2):rod('Finger articulation plate',(fx,-.053-j*.006,.889-j*.019),(fx,-.056-j*.006,.868-j*.019),.0105,steel,6)
        rod('Gauntlet thumb',(sign*.26,-.025,.917),(sign*.245,-.06,.88),.019,steel,8)

    # Close helmet: skull, separate visor/bevor and actual open slit/perforations.
    shell('Helmet skull',[(1.745,.090,.083,.018),(1.84,.12,.105,.018),(1.94,.108,.098,.018),
          (2.005,.059,.058,.015),(2.023,.012,.018,.008)],steel,32,1.08,2*math.pi-1.08)
    shell('Helmet brow and crown',[(1.883,.116,.119,.005),(1.94,.106,.104,.01),
          (2.005,.059,.058,.015),(2.023,.012,.018,.008)],steel,28,-1.2,1.2,ridge=.012)
    shell('Pointed visor lower face',[(1.758,.077,.09,0),(1.79,.098,.127,0),(1.82,.106,.143,0),
          (1.845,.11,.137,0),(1.865,.113,.129,0)],steel,24,-1.2,1.2,ridge=.012,
          omit=lambda row,col:row==2 and col in [6,9,14,17])
    shell('Dark inner helmet lining',[(1.76,.075,.078,.015),(1.90,.092,.085,.015),(1.96,.07,.07,.015)],lining,24)
    # The 18mm eye slit is actual space between independently modeled visor halves.
    ring_edge('Visor rolled upper edge',1.865,.113,.129,0,-1.2,1.2,ridge=.012)
    for sign in [-1,1]:
        rod('Visor hinge axle',(sign*.102,0,1.875),(sign*.127,0,1.875),.012,rivet,10)
        ellipsoid('Visor hinge pin',(sign*.129,0,1.875),(.006,.018,.018),rivet,10,6)
    profile=[(-.09,1.96,1.948),(-.04,2.039,1.997),(.02,2.06,2.018),(.09,1.99,1.95)]
    vertices=[(x,y,z) for x in [-.006,.006] for y,top,bottom in profile for z in [bottom,top]]
    faces=[]
    for i in range(3):
        a=i*2;faces.extend([(a,a+2,a+3,a+1),(a+8,a+9,a+11,a+10),(a+1,a+3,a+11,a+9),(a,a+8,a+10,a+2)])
    faces.extend([(0,1,9,8),(6,14,15,7)])
    mesh=bpy.data.meshes.new('Continuous forged helmet comb');mesh.from_pydata(vertices,[],faces);mesh.uv_layers.new()
    comb=bpy.data.objects.new('Solid medial helmet comb',mesh);bpy.context.collection.objects.link(comb);mesh.materials.append(edge)

    # Repair normals independently of primitive defaults; preserve all intentional
    # shell islands and their material/UV boundaries rather than remeshing them.
    for obj in bpy.context.scene.objects:
        if obj.type!='MESH':continue
        bm=bmesh.new();bm.from_mesh(obj.data);bmesh.ops.recalc_face_normals(bm,faces=list(bm.faces));bm.to_mesh(obj.data);bm.free()
        # Hand-shaped wings, straps and comb also need authored UVs. An image
        # node with every corner sampling texel0 is still an untextured part.
        mesh=obj.data
        if not mesh.uv_layers:mesh.uv_layers.new()
        if all(loop.uv.length<1e-8 for loop in mesh.uv_layers.active.data):
            lo=[min(v.co[i] for v in mesh.vertices) for i in range(3)]
            hi=[max(v.co[i] for v in mesh.vertices) for i in range(3)]
            for poly in mesh.polygons:
                axis=max(range(3),key=lambda i:abs(poly.normal[i]));directions=[i for i in range(3) if i!=axis]
                for loop in poly.loop_indices:
                    co=mesh.vertices[mesh.loops[loop].vertex_index].co
                    mesh.uv_layers.active.data[loop].uv=tuple((co[i]-lo[i])/(hi[i]-lo[i]) if hi[i]-lo[i]>1e-8 else .5 for i in directions)
