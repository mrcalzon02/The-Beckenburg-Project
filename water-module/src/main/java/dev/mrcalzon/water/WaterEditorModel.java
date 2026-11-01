package dev.mrcalzon.water;
import java.util.*;
import java.util.function.Consumer;

/** Transactional authoring state, bounded undo/redo and genuine multi-selection.
 * A host validator can build preview bodies before a change is committed. */
public final class WaterEditorModel {
 private WaterDocument gesture;private WaterDocument document;private final Set<String> selection=new LinkedHashSet<>();
 private List<WaterTerrainSnap.Socket> terrainSockets=List.of();
 private final Deque<WaterDocument> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
 private final List<Runnable> listeners=new ArrayList<>();private Consumer<WaterDocument> validator=d->{};private String saved;
 public WaterEditorModel(WaterDocument initial){document=Objects.requireNonNull(initial);saved=initial.revision();}
 public WaterDocument document(){return document;}
 /** Host-authored river mouths, pool outlets or channel endpoints. */
 public void setTerrainSockets(Collection<WaterTerrainSnap.Socket> sockets){
  Objects.requireNonNull(sockets);
  if(sockets.size()>256)throw new IllegalArgumentException("Too many water terrain sockets");
  terrainSockets=List.copyOf(sockets);notifyChanged();
 }
 public List<WaterTerrainSnap.Socket> terrainSockets(){return terrainSockets;}
 public void validator(Consumer<WaterDocument> validate){validator=Objects.requireNonNull(validate);validator.accept(document);}
 public Runnable listen(Runnable listener){listeners.add(listener);return ()->listeners.remove(listener);}
 private void notifyChanged(){for(var listener:List.copyOf(listeners))listener.run();}
 public List<WaterPlacement> selected(){return document.placements().stream().filter(p->selection.contains(p.id())).toList();}
 public void select(Collection<String> ids){selection.clear();for(var p:document.placements())if(ids.contains(p.id()))selection.add(p.id());notifyChanged();}
 public boolean dirty(){return !saved.equals(document.revision());}
 public void markSaved(){saved=document.revision();notifyChanged();}
 private void change(List<WaterPlacement> values){var next=new WaterDocument(document.level(),values);if(next.equals(document))return;validator.accept(next);if(gesture==null){undo.push(document);while(undo.size()>60)undo.removeLast();redo.clear();}document=next;selection.retainAll(values.stream().map(WaterPlacement::id).toList());notifyChanged();}
 private String unused(Set<String> taken){for(int n=1;n<10000;n++){String id="water-"+n;if(taken.add(id))return id;}throw new IllegalStateException("Water identifier budget");}
 public String add(double x,double y,double z,double width,double depth){var all=new ArrayList<>(document.placements());var ids=new HashSet<>(all.stream().map(WaterPlacement::id).toList());String id=unused(ids);var selected=selected();WaterSettings settings=selected.isEmpty()?(all.isEmpty()?new WaterSettings():all.get(0).settings()):selected.get(0).settings();all.add(new WaterPlacement(id,x,y,z,width,depth,0,settings));change(all);select(List.of(id));return id;}
 public void deleteSelected(){change(document.placements().stream().filter(p->!selection.contains(p.id())).toList());}
 public void duplicateSelected(double dx,double dz){var all=new ArrayList<>(document.placements());var ids=new HashSet<>(all.stream().map(WaterPlacement::id).toList());var selected=selected();var copies=new ArrayList<String>();for(var p:selected){String id=unused(ids);all.add(new WaterPlacement(id,p.x()+dx,p.y(),p.z()+dz,p.width(),p.depth(),p.yaw(),p.settings(),p.shape()));copies.add(id);}change(all);select(copies);}
 public void matchSettings(){var selected=selected();if(selected.isEmpty())return;var settings=selected.get(0).settings();change(document.placements().stream().map(p->selection.contains(p.id())?p.configured(settings):p).toList());}
 public void edit(String key,Object value){var selected=selected();if(selected.isEmpty())return;var anchor=selected.get(0);double n=key.startsWith("$")&&!key.equals("$shape")?Double.parseDouble(String.valueOf(value)):0;
  var all=new ArrayList<WaterPlacement>();for(var p:document.placements()){if(!selection.contains(p.id())){all.add(p);continue;}var next=switch(key){
   case "$shape"->p.shaped(WaterPlacement.Shape.valueOf(String.valueOf(value)));
   case "$x"->p.moved(n-anchor.x(),0,0);case "$y"->p.moved(0,n-anchor.y(),0);case "$z"->p.moved(0,0,n-anchor.z());
   case "$width"->p.resized(n,p.depth());case "$depth"->p.resized(p.width(),n);
   case "$yaw"->new WaterPlacement(p.id(),p.x(),p.y(),p.z(),p.width(),p.depth(),p.yaw()+Math.toRadians(n)-anchor.yaw(),p.settings(),p.shape());
   default->p.configured(p.settings().with(key,value));};all.add(next);}change(all);
 }
 public void transformSelected(java.util.function.UnaryOperator<WaterPlacement> transform){change(document.placements().stream().map(p->selection.contains(p.id())?transform.apply(p):p).toList());}
 public WaterSnap.Match snapSelected(double tolerance,boolean rotate){
  var moving=selected();
  var targets=document.placements().stream().filter(p->!selection.contains(p.id())).toList();
  WaterSnap.Match best=null;
  for(var p:moving){
   var match=WaterSnap.find(p,targets,tolerance,rotate);
   if(match!=null&&(best==null||match.cost()<best.cost()))best=match;
   for(var socket:terrainSockets){
    var attached=WaterTerrainSnap.connect(p,socket,tolerance,rotate);
    if(attached!=null&&(best==null||attached.match().cost()<best.cost()))
     best=attached.match();
   }
  }
  if(best==null)return null;
  var chosen=best;
  var pivot=moving.stream().filter(p->p.id().equals(chosen.movingId()))
   .findFirst().orElseThrow();
  transformSelected(p->WaterSnap.apply(p,chosen,pivot));
  return chosen;
 }
 public void beginGesture(){if(gesture!=null)throw new IllegalStateException("Water gesture already active");gesture=document;}
 public void endGesture(boolean cancel){if(gesture==null)return;var before=gesture;gesture=null;if(cancel){validator.accept(before);document=before;selection.retainAll(before.placements().stream().map(WaterPlacement::id).toList());notifyChanged();}else if(!document.equals(before)){undo.push(before);while(undo.size()>60)undo.removeLast();redo.clear();}}
 private boolean restore(Deque<WaterDocument> from,Deque<WaterDocument> to){if(gesture!=null)endGesture(true);if(from.isEmpty())return false;var next=from.peek();validator.accept(next);from.pop();to.push(document);document=next;selection.retainAll(next.placements().stream().map(WaterPlacement::id).toList());notifyChanged();return true;}
 public boolean undo(){return restore(undo,redo);}
 public boolean redo(){return restore(redo,undo);}
}
