"""Fit two small, covered construction benches into the existing terrain."""
import struct

import bpy

from compose_room_terrain import split_native

INTAKE_BOX=(30.18,-28.32,31.66,-27.68)
TAILRACE_BOX=(73.56,45.10,74.22,46.30)
VALLEY_NAME="Blackwater joined valley terrain"
BENCHES=(INTAKE_BOX,TAILRACE_BOX)


def split(poly,dimension,boundary,sign):
    inside=[]
    outside=[]
    for a,b in zip(poly,poly[1:]+poly[:1]):
        va=(a[dimension]-boundary)*sign
        vb=(b[dimension]-boundary)*sign
        if va>=0:
            inside.append(a)
        else:
            outside.append(a)
        if (va < 0 < vb) or (vb < 0 < va):
            t=va/(va-vb)
            hit=tuple(x+(y-x)*t for x,y in zip(a,b))
            inside.append(hit)
            outside.append(hit)
        elif va==0 and vb<0:
            outside.append(a)
        elif vb==0 and va<0:
            outside.append(b)
    return inside,outside


def triangles(poly):
    for b,c in zip(poly[1:],poly[2:]):
        a=poly[0]
        area=(b[0]-a[0])*(c[2]-a[2])-(b[2]-a[2])*(c[0]-a[0])
        if abs(area)>1e-10:
            yield a,b,c


def cut(triangle,box):
    retained=[]
    poly=list(triangle)
    for d,edge,sign in ((0,box[0],1),(2,box[1],1),(0,box[2],-1),(2,box[3],-1)):
        poly,outside=split(poly,d,edge,sign)
        retained.extend(triangles(outside))
        if not list(triangles(poly)):
            return None
    return retained


def replace_surface(source,name,box):
    mesh=source.data
    mesh.calc_loop_triangles()
    result=[]
    changed=0
    uv=mesh.uv_layers.active
    for face in mesh.loop_triangles:
        triangle=[]
        for i,li in zip(face.vertices,face.loops):
            p=source.matrix_world@mesh.vertices[i].co
            texture=tuple(uv.data[li].uv) if uv else (p.x,p.y)
            triangle.append((p.x,p.z,-p.y,*texture))
        fragments=cut(tuple(triangle),box)
        if fragments is None:
            fragments=[tuple(triangle)]
        else:
            changed+=1
        result.extend((tri,face.material_index,mesh.polygons[face.polygon_index].use_smooth) for tri in fragments)
    assert 0<changed<200,(source.name,changed)
    coordinates=[]
    indices={}
    polygons=[]
    for triangle,_,_ in result:
        row=[]
        for x,y,z,*_ in triangle:
            key=tuple(round(v,7) for v in (x,-z,y))
            if key not in indices:
                indices[key]=len(coordinates)
                coordinates.append((x,-z,y))
            row.append(indices[key])
        polygons.append(tuple(row))
    target=bpy.data.meshes.new(name)
    target.from_pydata(coordinates,[],polygons)
    target.update()
    for mat in mesh.materials:
        target.materials.append(mat)
    tex=target.uv_layers.new(name=uv.name if uv else "Terrain UV")
    for poly,(triangle,finish,smooth) in zip(target.polygons,result):
        poly.material_index=finish
        poly.use_smooth=smooth
        for loop,pt in zip(poly.loop_indices,triangle):
            tex.data[loop].uv=pt[3:]
    obj=bpy.data.objects.new(name,target)
    bpy.context.scene.collection.objects.link(obj)
    for key,value in source.items():
        obj[key]=value
    bpy.data.objects.remove(source,do_unlink=True)
    return obj,changed


def replace_native(source,target):
    parts=split_native(source)
    revised=[]
    changed_faces=0
    changed_parts={}
    for name,count,old in parts:
        box=INTAKE_BOX if name==VALLEY_NAME else TAILRACE_BOX if name.startswith("terrain-") else None
        if box is None:
            revised.append((count,old))
            continue
        header=2+len(name.encode())
        faces=old[header+5:]
        records=[]
        cut_faces=0
        for off in range(0,len(faces),37):
            raw=faces[off:off+37]
            *coords,flags=struct.unpack(">9fB",raw)
            tri=tuple(tuple(coords[i:i+3]) for i in range(0,9,3))
            fragments=cut(tri,box)
            if fragments is None:
                records.append(raw)
            else:
                cut_faces+=1
                for a,b,c in fragments:
                    records.append(struct.pack(">9fB",*a,*b,*c,flags))
        if cut_faces:
            assert old[header]==0,name
            changed_faces+=cut_faces
            changed_parts[name]=cut_faces
            revised.append((len(records),old[:header+1]+struct.pack(">I",len(records))+b"".join(records)))
        else:
            revised.append((count,old))
    assert 2<=len(changed_parts)<7,changed_parts
    head=struct.pack(">III",0x48435332,len(revised),sum(n for n,_ in revised))
    target.write_bytes(head+b"".join(raw for _,raw in revised))
    return changed_faces,changed_parts
