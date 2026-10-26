package org.beckenburg.gpu;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.BufferUtils;
import org.beckenburg.GpuWorldRenderer;
import org.beckenburg.BeckenburgDiagnostics;
import org.beckenburg.GlbProps;
import org.beckenburg.LevelMaterials;
import org.beckenburg.MaterialAtlas;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;

/** Actual OpenGL 3.3 PSX scene rasterizer in the live Java 17 game.
 * Offscreen readback preserves existing Swing HUD and depth-tested ghost
 * sprites while migration of the UI to a GPU window is completed.
 */
public final class OpenGlWorldRenderer extends ApplicationAdapter implements GpuWorldRenderer {
 private static final float NEAR=.065f,FAR=180f;
 private static final String VERTEX="""
 #version 330 core
 in vec3 a_position;
 in vec2 a_texCoord0;
 in vec3 a_lighting;
 uniform vec2 u_projection;
 uniform vec2 u_resolution;
 uniform int u_worldSpace;
 uniform vec3 u_cameraPosition;
 uniform vec2 u_cameraAngles;
 uniform float u_vertexSnap;
 out vec2 v_uv;
 noperspective out vec2 v_uvAffine;
 out vec3 v_light;
 void main(){
  vec3 cameraPoint=a_position;
  if(u_worldSpace!=0){
   vec3 offset=a_position-u_cameraPosition;
   float cy=cos(u_cameraAngles.x),sy=sin(u_cameraAngles.x);
   float cp=cos(u_cameraAngles.y),sp=sin(u_cameraAngles.y);
   float right=-offset.x*cy+offset.z*sy;
   float forward=offset.x*sy+offset.z*cy;
   cameraPoint=vec3(right,offset.y*cp-forward*sp,
                    offset.y*sp+forward*cp);
  }
  float zz=cameraPoint.z;
  vec4 clip=vec4(cameraPoint.x*u_projection.x,
   cameraPoint.y*u_projection.y,
   zz*((180.0+0.065)/(180.0-0.065))-2.0*180.0*0.065/(180.0-0.065),zz);
  if(u_vertexSnap>0.0&&zz>0.065){
   vec2 native=(clip.xy/clip.w*0.5+0.5)*u_resolution;
   native=floor(native/u_vertexSnap+0.5)*u_vertexSnap;
   clip.xy=(native/u_resolution*2.0-1.0)*clip.w;
  }
  gl_Position=clip;
  v_uv=a_texCoord0;
  v_uvAffine=a_texCoord0;
  v_light=a_lighting;
 }
 """;
 private static final String FRAGMENT="""
 #version 330 core
 in vec2 v_uv;
 noperspective in vec2 v_uvAffine;
 in vec3 v_light;
 uniform sampler2D u_texture;
 uniform float u_affineWarp;
 out vec4 fragColor;
 void main(){
  vec4 surface=texture(u_texture,mix(v_uv,v_uvAffine,u_affineWarp));
  if(surface.a<0.05)discard;
  fragColor=vec4(clamp(surface.rgb*v_light,0.0,1.0),surface.a);
 }
 """;
 private final AtomicReference<Frame> pending=new AtomicReference<>();
 private final AtomicLong supersededFrames=new AtomicLong();
 private volatile Result latest;
 private volatile boolean ready,closed;
 private volatile String description="OpenGL initialising";
 private final CompletableFuture<String> initialized=new CompletableFuture<>();
 private ShaderProgram shader;
 private FrameBuffer target;
 private int targetWidth,targetHeight;
 private ByteBuffer colorBytes;
 private FloatBuffer depthFloats;
 private final Map<Integer,Texture> textures=new HashMap<>();
 private final Map<Integer,Mesh> meshes=new HashMap<>();
 private final Map<Integer,Integer> capacity=new HashMap<>();
 private long lastSlowReportNs;
 public CompletableFuture<String> initialization(){return initialized;}
 @Override public boolean ready(){return ready&&!closed;}
 @Override public Result latest(){return latest;}
 @Override public void submit(Frame frame){
  if(!ready())throw new IllegalStateException("OpenGL worker no longer active");
  Frame replaced=pending.getAndSet(Objects.requireNonNull(frame));
  if(replaced!=null)supersededFrames.incrementAndGet();
  Gdx.graphics.requestRendering();
 }
 @Override public String rendererDescription(){return description;}
 @Override public void create(){
  try{
   shader=new ShaderProgram(VERTEX,FRAGMENT);
   if(!shader.isCompiled())throw new IllegalStateException("OpenGL shader compile: "+shader.getLog());
   description="OpenGL "+Gdx.gl.glGetString(GL20.GL_RENDERER)+" / "+
      Gdx.gl.glGetString(GL20.GL_VERSION);
   ready=true;initialized.complete(description);
   // No idle spinning: submit() explicitly wakes the OpenGL thread.
   Gdx.graphics.setContinuousRendering(false);
  }catch(Throwable error){
   closed=true;initialized.completeExceptionally(error);throw error;
  }
 }
 private void resizeTarget(int w,int h){
  if(w==targetWidth&&h==targetHeight&&target!=null)return;
  if(target!=null)target.dispose();
  target=new FrameBuffer(Pixmap.Format.RGBA8888,w,h,true);
  targetWidth=w;targetHeight=h;
  colorBytes=BufferUtils.newByteBuffer(w*h*4);
  depthFloats=BufferUtils.newFloatBuffer(w*h);
 }
 private static int sample(int id,double u,double v){
  return id>=GlbProps.TEXTURE_BASE?GlbProps.sample(id,u,v):
   id>=LevelMaterials.BASE?LevelMaterials.sample(id,u,v):MaterialAtlas.sample(id,u,v);
 }
 private Pixmap pixelsFor(int material){
  int width=GlbProps.textureWidth(material),height=GlbProps.textureHeight(material);
  Pixmap bitmap=new Pixmap(width,height,Pixmap.Format.RGBA8888);
  for(int y=0;y<height;y++)for(int x=0;x<width;x++){
   // Pixmap row zero is uploaded at GL v=0. The CPU sampler already owns
   // each material family's V convention: do not invert that convention twice.
   double u=(x+.5)/width,v=(y+.5)/height;
   int rgb=material<0?0xffffff:sample(material,u,v);
   int alpha=material>=GlbProps.TEXTURE_BASE
     &&material!=GlbProps.VAN_MAP_MATERIAL?
     GlbProps.sampleAlpha(material,u,v):255;
   bitmap.drawPixel(x,y,((rgb&0xffffff)<<8)|alpha);
  }
  return bitmap;
 }
 private Texture texture(int material){
  Texture found=textures.get(material);
  if(found!=null&&material==GlbProps.VAN_MAP_MATERIAL){
   // The host-approved map CRT is a LIVE texture. Never freeze it at the
   // first sampled frame or silently lose investigation-map updates.
   Pixmap update=pixelsFor(material);
   found.draw(update,0,0);update.dispose();
  }
  if(found!=null)return found;
  Pixmap bitmap=pixelsFor(material);
  Texture image=new Texture(bitmap);
  bitmap.dispose();
  image.setFilter(Texture.TextureFilter.Nearest,Texture.TextureFilter.Nearest);
  image.setWrap(Texture.TextureWrap.Repeat,Texture.TextureWrap.Repeat);
  textures.put(material,image);return image;
 }
 private Mesh mesh(int material,int vertices){
  Mesh existing=meshes.get(material);
  if(existing!=null&&capacity.get(material)>=vertices)return existing;
  if(existing!=null)existing.dispose();
  int size=1;while(size<vertices)size*=2;
  Mesh created=new Mesh(false,size,0,
   new VertexAttribute(VertexAttributes.Usage.Position,3,"a_position"),
   new VertexAttribute(VertexAttributes.Usage.TextureCoordinates,2,"a_texCoord0"),
   new VertexAttribute(VertexAttributes.Usage.Generic,3,"a_lighting"));
  meshes.put(material,created);capacity.put(material,size);return created;
 }
 private void draw(Frame frame){
  long started=System.nanoTime();
  resizeTarget(frame.width(),frame.height());
  target.begin();
  Gdx.gl.glViewport(0,0,targetWidth,targetHeight);
  Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
  Gdx.gl.glDepthFunc(GL20.GL_LESS);
  Gdx.gl.glDepthMask(true);
  Gdx.gl.glDisable(GL20.GL_CULL_FACE);
  Gdx.gl.glDisable(GL20.GL_BLEND);
  Gdx.gl.glClearColor(0,0,0,0);
  Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT|GL20.GL_DEPTH_BUFFER_BIT);
  shader.bind();
  // Beckenburg's camera-space focal lens, exactly matched to the CPU FOV.
  // SceneRenderer's native projection is 320x200 at focal=153.
  float focal=frame.focal();
  shader.setUniformf("u_projection",
    2*focal/targetWidth,2*focal/targetHeight);
  shader.setUniformf("u_resolution",(float)targetWidth,(float)targetHeight);
  shader.setUniformi("u_worldSpace",frame.worldSpace()?1:0);
  shader.setUniformf("u_cameraPosition",frame.cameraX(),frame.cameraY(),frame.cameraZ());
  shader.setUniformf("u_cameraAngles",frame.yaw(),frame.pitch());
  shader.setUniformf("u_vertexSnap",frame.vertexSnap());
  shader.setUniformf("u_affineWarp",frame.affineWarp());
  shader.setUniformi("u_texture",0);
  for(Surface surface:frame.surfaces()){
   int vertexCount=surface.vertices().length/8;
   if(vertexCount==0)continue;
   Mesh draw=mesh(surface.material(),vertexCount);
   draw.setVertices(surface.vertices());
   texture(surface.material()).bind(0);
   draw.render(shader,GL20.GL_TRIANGLES,0,vertexCount);
  }
  // glReadPixels itself synchronizes the requested framebuffer transfers.
  // glFinish beforehand stalls the entire graphics queue for no benefit.
  long rasterReady=System.nanoTime();
  colorBytes.clear();depthFloats.clear();
  Gdx.gl.glReadPixels(0,0,targetWidth,targetHeight,
    GL20.GL_RGBA,GL20.GL_UNSIGNED_BYTE,colorBytes);
  Gdx.gl.glReadPixels(0,0,targetWidth,targetHeight,
    GL20.GL_DEPTH_COMPONENT,GL20.GL_FLOAT,depthFloats);
  int[] argb=new int[targetWidth*targetHeight];
  float[] depth=new float[argb.length];
  for(int row=0;row<targetHeight;row++){
   int sourceRow=targetHeight-row-1;
   for(int col=0;col<targetWidth;col++){
    int src=sourceRow*targetWidth+col,dst=row*targetWidth+col,i=src*4;
    int r=colorBytes.get(i)&255,g=colorBytes.get(i+1)&255;
    int b=colorBytes.get(i+2)&255,a=colorBytes.get(i+3)&255;
    argb[dst]=(a<<24)|(r<<16)|(g<<8)|b;
    float d=depthFloats.get(src);
    depth[dst]=d>=.999999f?Float.POSITIVE_INFINITY:
        (float)(2*NEAR*FAR/(FAR+NEAR-(2*d-1)*(FAR-NEAR)));
   }
  }
  target.end();
  latest=new Result(frame.sceneId(),frame.frameId(),targetWidth,targetHeight,argb,depth);
  long finished=System.nanoTime();
  if(finished-started>200_000_000L&&finished-lastSlowReportNs>5_000_000_000L){
   lastSlowReportNs=finished;
   BeckenburgDiagnostics.record("GPU FRAME SLOW id="+frame.frameId()+
      " surfaces="+frame.surfaces().size()+
      " target="+targetWidth+"x"+targetHeight+
      " raster_ms="+(rasterReady-started)/1_000_000+
      " readback_ms="+(finished-rasterReady)/1_000_000+
      " superseded="+supersededFrames.get());
  }
 }
 @Override public void render(){
  Frame frame=pending.getAndSet(null);
  if(frame==null||closed)return;
  try{draw(frame);}catch(Throwable error){
   closed=true;ready=false;description="GPU render FAILED: "+error;
   BeckenburgDiagnostics.fail("GPU world framebuffer rendering",error);
  }
 }
 @Override public void dispose(){
  closed=true;ready=false;
  for(Mesh mesh:meshes.values())mesh.dispose();
  for(Texture texture:textures.values())texture.dispose();
  if(target!=null)target.dispose();
  if(shader!=null)shader.dispose();
 }
}
