package org.beckenburg;

import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/** Bounded glTF 2.0 binary mesh/PNG reader for the licensed Beckenburg PSX scene props.
 * Actual source positions, index buffers, UVs and textures reach SceneRenderer's
 * existing perspective-correct, depth-tested triangle rasterizer.
 */
public final class GlbProps {
    private GlbProps() { }
    public static final int TEXTURE_BASE=256;
    /** Dedicated live-map screen, independent of the other van CRT materials. */
    public static final int VAN_MAP_MATERIAL=Integer.MAX_VALUE-2;
    private static final String[] NAMES={
        "blender_site_map_fixed","blender_objective_board_fixed","blender_clipboards_fixed","blender_computer_fixed",
        "blender_sound_recorder_1","blender_sound_recorder_2","blender_sound_recorder_3",
        "light_switch_1","ceiling_lamp_1_off","door_1_loose","shelf_mp_6",
        "chair_mp_5","ceiling_lamp_1_on","cross_mp_2","flashlight_mp_1",
        "kenney_medieval_wall_fortified","kenney_medieval_wall_fortified_window","kenney_medieval_wall_fortified_gate","kenney_medieval_wall_pane_wood_window","kenney_medieval_tower","kenney_medieval_tower_top","kenney_medieval_tower_base","kenney_medieval_battlement","kenney_medieval_battlement_corner_outer","kenney_medieval_column","kenney_medieval_column_damaged","kenney_medieval_structure_wall","kenney_medieval_stairs_stone","kenney_medieval_detail_crate","kenney_medieval_detail_barrel",
        "armchair_1","sofa_1","dinner_table_1","wardrobe_mp_1",
        "display_cabinet_mp_2","tv_mp_1","tv_table_1_4",
        "lamp_3_on","lamp_3_off","ceiling_lamp_4_on","ceiling_lamp_4_off",
        "pc_keyboard_mp_1",
        "vape_mp_1","dosimeter_1","dosimeter_2","walkie_talkie_2",
        "handheld_tablet_1","monitoring_screen_mp_1","pc_mp_1",
        "button_12","book_mp_1","open_book_mp_1","dosimeter_4","walkie_talkie_1",
        "exploration_emf_detector","exploration_flashlight","exploration_flashlight_poquet","exploration_photo_camera","exploration_spirit_box","exploration_thermal_camera","exploration_thermometer","exploration_voice_recorder","ggbot_car08_van_shell","ggbot_car01","ggbot_car05","ggbot_car07","kenney_road_asphalt_straight","kenney_road_asphalt_side","kenney_road_asphalt_damaged","kenney_detail_light_single","kenney_detail_dumpster_closed","kenney_detail_bench","kenney_detail_barrier_type_a"
    };
    /** Full approved runtime asset index used by the live 3D level editor. */
    public static java.util.Set<String> availableModels(){
        java.util.TreeSet<String> names=new java.util.TreeSet<>();
        names.addAll(java.util.Arrays.asList(NAMES));
        names.addAll(LevelAssets.MODELS);
        // Every individually approved, packaged authored environment mesh is
        // selectable in the editor, not just the legacy starter-model list.
        names.addAll(LevelAssets.GREEN_GROUND);
        names.addAll(LevelAssets.NATIVE_SITE_TERRAIN);
        names.addAll(LevelAssets.GROUNDED_BERMS);
        names.addAll(LevelAssets.SITE_ART);
        names.addAll(LevelAssets.FITTED_TENTS);
        names.addAll(LevelAssets.TAILORED_FABRIC);
        names.addAll(LevelAssets.AUTHORED_SURFACES);
        names.addAll(BlackpineDressing.ASSETS);names.addAll(BlackpineStoryDressing.ASSETS);
        names.addAll(BlackpineProductionAssets.ASSETS);
        names.addAll(LivedAssets.ORIGINAL);names.addAll(LivedAssets.SOURCED);
        for(String name:LivedAssets.ORIGINAL)names.add(LivedAssets.worn(name));
        names.addAll(EquipmentModelCatalog.approvedModels());
        names.addAll(ImportedAssetLibrary.available());
        return java.util.Collections.unmodifiableSet(names);
    }
    public record Vertex(double x,double y,double z,double u,double v,double nx,double ny,double nz) {public Vertex(double x,double y,double z,double u,double v){this(x,y,z,u,v,0,0,0);}}
    public record Triangle(Vertex a,Vertex b,Vertex c,int material) { }
    public record Model(List<Triangle> triangles) { }
    private static final Map<String,Model> cache=new HashMap<>();
    private static final List<String> materialNames=new ArrayList<>();
    public static synchronized String materialName(int material){int i=material-TEXTURE_BASE;return i>=0&&i<materialNames.size()?materialNames.get(i):"";}
    private static final List<BufferedImage> textures=new ArrayList<>();
    // glTF 2.0 origin is upper-left. Apply it to vehicles and new authored
    // surface atlases; legacy art keeps its previously reviewed mapping.
    private static final List<Boolean> vehicleUvMaterials=new ArrayList<>();
    // Original glTF samplers are per texture: the two lighter casing images
    // explicitly clamp while the lit flame's independent image repeats.
    private static final List<Integer> samplerWrapS=new ArrayList<>();
    private static final List<Integer> samplerWrapT=new ArrayList<>();
    // Preserve the source glTF material alphaMode. Texture alpha alone is
    // insufficient: OPAQUE materials must remain opaque even if their PNG
    // contains otherwise-transparent pixels. The 67 original Hollow Creek
    // foliage cards explicitly declare BLEND with mostly transparent PNGs.
    private static final java.util.Set<Integer> gltfBlendMaterials=new java.util.HashSet<>();
    private static final Map<Integer,Double> gltfMaskCutoffs=new HashMap<>();
    static synchronized boolean gltfBlended(int material){return gltfBlendMaterials.contains(material);}
    static synchronized double gltfMaskCutoff(int material){
        return gltfMaskCutoffs.getOrDefault(material,-1.0);
    }
    /** These imported fluorescent GLBs use glTF image-space (top-left) UVs.
     * Keep their original source images and mesh coordinates untouched. */
    static boolean fluorescentGltfUv(String name){
        return name!=null&&(name.matches("ceiling_lamp_[14]_(on|off)")
            ||name.equals("level_utility_light"));
    }
    /** Targeted owner-reported source UV repair. The original metal-barrel
     * and canned-food GLBs map their circular caps into the lower portion of
     * their own PNG atlases (V approx 0.7..1). The legacy bottom-left sampler
     * was instead fetching the upper label/side rows, producing the observed
     * half-black barrel cap and corrupted can labels. Preserve every source
     * GLB and UV byte; only correct the runtime PNG row interpretation.
     * Other purchased models remain unchanged pending their own comparison. */
    static boolean reviewedSourceGltfUv(String name){
        // Exact-source fireplace A/B real-render captures show the wood and
        // green-marble islands aligned only when sampled at glTF's top-left V.
        // No source mesh, UV coordinates, or original PNG pixels are modified.
        // Both wall-sconce power states share the original top-left glTF atlas.
        // Inverting V paints the tube strip onto the housing and gray casing onto the diffuser.
        return "lamp_3_on".equals(name)||"lamp_3_off".equals(name)||
            "level_fireplace".equals(name)||
            "metal_barrel_mp_1".equals(name)||
            "canned_food_mp_1".equals(name)||"canned_food_mp_2".equals(name)||
            "kenney_medieval_detail_barrel".equals(name)||
            (name!=null&&name.matches("book_mp_[2-5]"))||
            "open_book_mp_1".equals(name)||
            "blender_ghost_writing_book_2".equals(name)||
            // These five source GLBs are native Khronos glTF/Blender exports.
            // Their original embedded PNGs and TEXCOORD_0 are top-left
            // image-space; legacy inverted sampling was reading the opposite
            // atlas rows (TV fascia, medical labels and lighter body).
            // Scope this to inspected model identities, not the whole library.
            "tv_mp_1".equals(name)||
            "blender_health_supplies_1".equals(name)||
            "blender_igniter_2".equals(name)||
            "blender_igniter_2_lit".equals(name)||
            "blender_sanity_medication_2".equals(name);
    }
    /** Optional DEV ROOM A/B comparison, never enabled by the normal game.
     * Capture the same approved authored models with PNG V-origin interpreted
     * as glTF top-left, preserving original GLB/UV/source image bytes. */
    static boolean diagnosticBookUvOrigin(String name){
        return Boolean.getBoolean("beckenburg.devroom.bookTopLeftUV")
            &&(bookCoverMaterial(name)||"blender_ghost_writing_book_2".equals(name));
    }
    /** Developer-only A/B atlas origin for the remaining user-flagged
     * environmental/equipment models. Runtime default stays untouched until
     * the original source texture and actual four-angle renders are compared.
     * This property is opt-in only in advisory dev-room capture. */
    static boolean diagnosticRemainingUvOrigin(String name){
        return Boolean.getBoolean("beckenburg.devroom.remainingTopLeftUV")
            &&name!=null&&java.util.Set.of(
                "level_fireplace","tv_mp_1",
                "blender_health_supplies_1","blender_igniter_2",
                "blender_igniter_2_lit","blender_sanity_medication_2"
            ).contains(name);
    }
    // glTF sampler: REPEAT=10497, CLAMP_TO_EDGE=33071, MIRRORED_REPEAT=33648.
    static double wrapUv(double coordinate,int mode){
        if(!Double.isFinite(coordinate))throw new IllegalArgumentException("UV coordinate");
        if(mode==33071)return Math.max(0.0,Math.min(1.0,coordinate));
        double tile=Math.floor(coordinate),part=coordinate-tile;
        if(mode==33648&&Math.abs(tile%2.0)>=1.0)return 1.0-part;
        return part;
    }
    /** Opt-in OLD bottom-left renderer for exact-source before/after
     * screenshots. Normal game retains source-aligned glTF V sampling.
     * This is a diagnostic override for only the individually repaired files.
     * It changes NO embedded PNG, UV buffer, or model in storage. */
    static boolean diagnosticLegacyUvOrigin(String name){
        return Boolean.getBoolean("beckenburg.devroom.legacyBottomLeftUV")
            &&name!=null&&java.util.Set.of(
                "level_fireplace","tv_mp_1","blender_health_supplies_1",
                "blender_igniter_2","blender_igniter_2_lit",
                "blender_sanity_medication_2","open_book_mp_1",
                "book_mp_2","book_mp_3","book_mp_4","book_mp_5",
                "blender_ghost_writing_book_2"
            ).contains(name);
    }
    static int imageRow(double v,int height,boolean gltfTopLeft){
        return imageRow(v,height,gltfTopLeft,10497);
    }
    private static int imageColumn(double u,int width,int wrap){
        return Math.min(width-1,(int)(wrapUv(u,wrap)*width));
    }
    static int imageRow(double v,int height,boolean gltfTopLeft,int wrap){
        if(height<1)throw new IllegalArgumentException("UV image height");
        double coord=wrapUv(v,wrap);
        int row=(int)Math.floor((gltfTopLeft?coord:1.0-coord)*height);
        return Math.max(0,Math.min(height-1,row));
    }
    private static final List<double[]> diffuseFactors=new ArrayList<>();
    private static final Map<Long,Integer> foliagePalettes=new HashMap<>();
    /** Only the legacy purchased book collection uses this very dark source
     * atlas. No blanket changes to library wood, pages or other imported art. */
    static boolean bookCoverMaterial(String resourceName){
        return resourceName!=null&&
            (resourceName.matches("book_mp_[1-5]")||
             resourceName.equals("open_book_mp_1"));
    }
    /** Tone-map the existing source RGB for legibility under dim lights.
     * Identical geometry, original UVs and alpha; source artwork is never edited.
     * A gamma below 1 brightens the deep leather colors, revealing the source
     * detail rather than replacing it with flat recolored quads. */
    static BufferedImage readableBookCover(BufferedImage original){
        var out=new BufferedImage(original.getWidth(),original.getHeight(),
            BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<original.getHeight();y++)for(int x=0;x<original.getWidth();x++){
            int px=original.getRGB(x,y),a=px>>>24;
            int r=bookTone((px>>>16)&255),g=bookTone((px>>>8)&255),
                b=bookTone(px&255);
            out.setRGB(x,y,(a<<24)|(r<<16)|(g<<8)|b);
        }
        return out;
    }
    static int bookTone(int original){
        return (int)Math.round(255*Math.pow(original/255.0,.61));
    }
    private static final Map<String,Integer> surfaceMaterials=new HashMap<>();
    /** Independently revisioned 2D faces used by procedural flat van monitors.
     * Textures are never recreated for a new reading: only the named material
     * pixels change, so static monitor geometry and unrelated VBOs remain warm. */
    private static final Map<Integer,Long> liveSurfaceRevisions=new HashMap<>();
    public static synchronized int liveSurfaceMaterial(String name,int width,int height){
        if(name==null||!name.startsWith("van live screen ")||
                width<64||height<64||width>1024||height>1024)
            throw new IllegalArgumentException("invalid live monitor surface");
        int material=surfaceMaterial(name,new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB));
        liveSurfaceRevisions.putIfAbsent(material,0L);
        return material;
    }
    public static synchronized void updateLiveSurface(int material,BufferedImage image){
        if(!liveSurfaceRevisions.containsKey(material)||image==null)
            throw new IllegalArgumentException("unknown live monitor");
        BufferedImage existing=textures.get(material-TEXTURE_BASE);
        if(existing.getWidth()!=image.getWidth()||existing.getHeight()!=image.getHeight())
            throw new IllegalArgumentException("live monitor dimensions changed");
        textures.set(material-TEXTURE_BASE,image);
        liveSurfaceRevisions.put(material,liveSurfaceRevisions.get(material)+1);
    }

    public static synchronized int surfaceMaterial(String name,BufferedImage image){
        Integer cached=surfaceMaterials.get(name);if(cached!=null)return cached;
        if(image==null||image.getWidth()>2048||image.getHeight()>2048)throw new IllegalArgumentException("terrain diffuse budget");
        int id=TEXTURE_BASE+textures.size();textures.add(image);vehicleUvMaterials.add(false);samplerWrapS.add(10497);samplerWrapT.add(10497);materialNames.add(name);diffuseFactors.add(new double[]{1,1,1,1});surfaceMaterials.put(name,id);return id;
    }
    /** Sixteen bounded texel variants per leaf material. Source UVs and shared source pixels remain intact. */
    public static synchronized int foliageMaterial(int original,long seed){
        int variant=Math.floorMod((int)(seed^(seed>>>32)),16);
        long key=((long)original<<32)|variant;Integer cached=foliagePalettes.get(key);if(cached!=null)return cached;
        BufferedImage source=textures.get(original-TEXTURE_BASE),copy=new BufferedImage(source.getWidth(),source.getHeight(),BufferedImage.TYPE_INT_ARGB);
        double warmth=(variant%4-1.5)*.025,brightness=.89+(variant/4)*.035;
        for(int y=0;y<source.getHeight();y++)for(int x=0;x<source.getWidth();x++){
            int rgb=source.getRGB(x,y),hash=(x*73856093)^(y*19349663)^(variant*83492791);hash^=hash>>>13;
            double speckle=1+(Math.floorMod(hash,17)-8)*.006+.018*Math.sin((x+variant*3)*.24+(y-variant)*.33);
            int r=(int)(((rgb>>16)&255)*(brightness+warmth)*speckle),g=(int)(((rgb>>8)&255)*brightness*speckle),b=(int)((rgb&255)*(brightness-warmth)*speckle);
            copy.setRGB(x,y,(rgb&0xff000000)|(Math.min(255,r)<<16)|(Math.min(255,g)<<8)|Math.min(255,b));
        }
        int id=TEXTURE_BASE+textures.size();textures.add(copy);vehicleUvMaterials.add(vehicleUvMaterials.get(original-TEXTURE_BASE));samplerWrapS.add(samplerWrapS.get(original-TEXTURE_BASE));samplerWrapT.add(samplerWrapT.get(original-TEXTURE_BASE));materialNames.add(materialName(original)+" foliage texel variant "+variant);
        // Every source-preserving foliage tint inherits the original GLB alpha
        // mode as well as its actual alpha pixels. A derived tree must not
        // become a black opaque rectangle merely because its tint differs.
        if(gltfBlendMaterials.contains(original))gltfBlendMaterials.add(id);
        Double mask=gltfMaskCutoffs.get(original);
        if(mask!=null)gltfMaskCutoffs.put(id,mask);
        diffuseFactors.add(diffuseFactors.get(original-TEXTURE_BASE).clone());foliagePalettes.put(key,id);return id;
    }
    private static final Map<String,Integer> televisionMaterials=new HashMap<>();
    private static final Map<Integer,Integer> televisionPhases=new HashMap<>();
    public static synchronized int televisionMaterial(double x,double y,double z){String key=x+":"+y+":"+z;return televisionMaterials.computeIfAbsent(key,k->{int id=TEXTURE_BASE+textures.size();textures.add(new BufferedImage(128,64,BufferedImage.TYPE_INT_RGB));vehicleUvMaterials.add(false);samplerWrapS.add(10497);samplerWrapT.add(10497);diffuseFactors.add(new double[]{1,1,1,1});materialNames.add("Television static surface");return id;});}
    public static synchronized void updateTelevision(int material,boolean active,int tick){int phase=active?tick:-1;if(televisionPhases.getOrDefault(material,-2)==phase)return;televisionPhases.put(material,phase);var im=textures.get(material-TEXTURE_BASE);
        for(int y=0;y<64;y++)for(int x=0;x<128;x++){int hash=(x*73856093)^(y*19349663)^(tick*83492791);hash^=hash>>>13;int grey=active?80+Math.floorMod(hash,151):8;if(active&&(y+tick)%11==0)grey/=2;im.setRGB(x,y,(grey<<16)|(grey<<8)|grey);}
    }
    private static int vanCrtMaterial=-1;
    private static BufferedImage vanCrtImage=blankCrt();
    private static BufferedImage vanMapImage=blankCrt();
    private static long vanMapRevision,vanCrtRevision;
    /** Dense source texture IDs exclude the special live-map sentinel. */
    public static synchronized boolean registeredTexture(int material){return material>=TEXTURE_BASE&&material-TEXTURE_BASE<textures.size();}
    /** Static materials return -1. Dynamic revisions change only on actual
     * source pixel updates, independent of geometry and draw-call count. */
    public static synchronized long dynamicTextureRevision(int material){
        if(material==VAN_MAP_MATERIAL)return vanMapRevision;
        Long live=liveSurfaceRevisions.get(material);
        if(live!=null)return live;
        if(material==vanCrtMaterial&&material>=TEXTURE_BASE)return vanCrtRevision;
        Integer phase=televisionPhases.get(material);
        return phase==null?(televisionMaterials.containsValue(material)?0:-1):Integer.toUnsignedLong(phase)+1;
    }
    public static synchronized void updateVanMap(BufferedImage image){
        if(image==null||image.getWidth()<256||image.getHeight()<256
                ||image.getWidth()>1024||image.getHeight()>1024)
            throw new IllegalArgumentException("invalid map monitor texture");
        vanMapImage=image;vanMapRevision++;
    }
    private static BufferedImage blankCrt(){
        BufferedImage image=new BufferedImage(256,256,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();
        g.setColor(new Color(7,17,22));g.fillRect(0,0,256,256);
        g.setColor(new Color(119,211,174));
        g.setFont(new Font(Font.MONOSPACED,Font.BOLD,17));
        g.drawString("BECKENBURG",20,48);
        g.drawString("VAN TELEMETRY",20,78);
        g.dispose();return image;
    }
    /** Changes only the owner's CRT screen material, never its real GLB casing. */
    public static synchronized void updateVanCrt(VanTelemetry.Display telemetry){
        if(telemetry==null)return;
        BufferedImage image=blankCrt();
        Graphics2D g=image.createGraphics();
        try{
            g.setColor(new Color(7,17,22));g.fillRect(0,0,256,256);
            g.setColor(new Color(142,229,183));
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,17));
            g.drawString("FIELD TELEMETRY",15,27);
            g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,15));
            g.drawString("ACTIVITY "+telemetry.activity()+"/10",15,55);
            int[] people=telemetry.sanity();
            for(int i=0;i<Math.min(6,people.length);i++){
                int number=people[i];
                g.setColor(number<0?new Color(87,119,119):
                    number<30?new Color(245,148,126):new Color(144,224,188));
                g.drawString("H"+(i+1)+" "+(number<0?"OFFLINE":number+"% SAN"),15,80+i*25);
            }
            g.setColor(new Color(83,135,122));
            g.drawString("TICK "+telemetry.tick(),15,244);
        }finally{g.dispose();}
        vanCrtImage=image;vanCrtRevision++;
    }

    public static synchronized Model model(String name){
        if(!Arrays.asList(NAMES).contains(name) && !LevelAssets.approved(name) && !EquipmentModelCatalog.approved(name)
                && !TierModelForge.generatedName(name) && !ImportedAssetLibrary.approved(name))
            throw new IllegalArgumentException("Unapproved model "+name);
        Model m=cache.get(name);
        if(m!=null)return m;
        String resource="/org/beckenburg/models/"+name+".glb";
        if(ImportedAssetLibrary.approved(name)){
            try{
                m=decode(ImportedAssetLibrary.read(name),name);
                cache.put(name,m);return m;
            }catch(IOException failure){throw new IllegalStateException("User imported model unavailable: "+name,failure);}
        }
        try(InputStream in=GlbProps.class.getResourceAsStream(resource)){
            if(in==null)throw new IOException("Missing packaged licensed mesh "+resource);
            byte[] bytes=in.readNBytes(4_000_001);
            if(bytes.length>4_000_000)throw new IOException("GLB resource over budget");
            m=decode(bytes,name);
            cache.put(name,m);
            return m;
        }catch(IOException failure){throw new IllegalStateException("Model unavailable: "+name,failure);}
    }
    /** Preserve authored atlas resolution during GPU upload; tightly packed UV
     * islands cannot survive an unconditional 128px nearest downsample. */
    public static synchronized int textureWidth(int material){int i=material-TEXTURE_BASE;return i>=0&&i<textures.size()?textures.get(i).getWidth():128;}
    public static synchronized int textureHeight(int material){int i=material-TEXTURE_BASE;return i>=0&&i<textures.size()?textures.get(i).getHeight():128;}
    /** Model triangle list is immutable and cannot mutate runtime source geometry. */
    public static synchronized int sample(int material,double u,double v){
        if(material==VAN_MAP_MATERIAL){
            if(!Double.isFinite(u)||!Double.isFinite(v))
                throw new IllegalArgumentException("invalid van map UV");
            BufferedImage image=vanMapImage;
            int xx=Math.min(image.getWidth()-1,(int)((u-Math.floor(u))*image.getWidth()));
            int yy=Math.min(image.getHeight()-1,
                (int)((1.0-(v-Math.floor(v)))*image.getHeight()));
            return image.getRGB(xx,yy)&0xffffff;
        }
        int index=material-TEXTURE_BASE;
        if(index<0||index>=textures.size()||!Double.isFinite(u)||!Double.isFinite(v))
            throw new IllegalArgumentException("Invalid imported texture");
        BufferedImage image=material==vanCrtMaterial?vanCrtImage:textures.get(index);
        int x=imageColumn(u,image.getWidth(),samplerWrapS.get(index));
        // GGBot's Blender source uses the actual glTF image origin at top-left.
        int y=imageRow(v,image.getHeight(),vehicleUvMaterials.get(index),samplerWrapT.get(index));
        int rgb=image.getRGB(x,y)&0xffffff;
        // GLB material baseColorFactor is part of the original material.
        // Previously ignored, washing out the 128px van shell's deliberate
        // graphite-gray diffuse factor and misrepresenting its authored paint.
        double[] factor=diffuseFactors.get(index);
        int red=Math.max(0,Math.min(255,(int)Math.round(((rgb>>16)&255)*factor[0])));
        int green=Math.max(0,Math.min(255,(int)Math.round(((rgb>>8)&255)*factor[1])));
        int blue=Math.max(0,Math.min(255,(int)Math.round((rgb&255)*factor[2])));
        return (red<<16)|(green<<8)|blue;
    }
    /** One original GLB PNG lookup for RGB+alpha, including baseColorFactor.
     * Required for glTF BLEND / MASK; the usual opaque renderer retains its
     * existing sampled RGB and does not accidentally become translucent. */
    public static synchronized int sampleRgba(int material,double u,double v){
        int index=material-TEXTURE_BASE;
        if(index<0||index>=textures.size()||!Double.isFinite(u)||!Double.isFinite(v))
            throw new IllegalArgumentException("Invalid glTF RGBA material");
        BufferedImage image=textures.get(index);
        int x=imageColumn(u,image.getWidth(),samplerWrapS.get(index));
        int y=imageRow(v,image.getHeight(),vehicleUvMaterials.get(index),
            samplerWrapT.get(index));
        int source=image.getRGB(x,y);
        double[] f=diffuseFactors.get(index);
        int a=Math.max(0,Math.min(255,(int)Math.round(((source>>>24)&255)*f[3])));
        int r=Math.max(0,Math.min(255,(int)Math.round(((source>>16)&255)*f[0])));
        int g=Math.max(0,Math.min(255,(int)Math.round(((source>>8)&255)*f[1])));
        int b=Math.max(0,Math.min(255,(int)Math.round((source&255)*f[2])));
        return (a<<24)|(r<<16)|(g<<8)|b;
    }
    /** Preserve original alpha for the shared VFX GLBs; ordinary room materials
     * retain the game's existing opaque software-rasterizer path. */
    public static synchronized int sampleAlpha(int material,double u,double v){
        int index=material-TEXTURE_BASE;
        if(index<0||index>=textures.size()||!Double.isFinite(u)||!Double.isFinite(v))
            throw new IllegalArgumentException("Invalid imported alpha");
        BufferedImage image=textures.get(index);
        int x=imageColumn(u,image.getWidth(),samplerWrapS.get(index));
        int y=imageRow(v,image.getHeight(),vehicleUvMaterials.get(index),samplerWrapT.get(index));
        int texAlpha=(image.getRGB(x,y)>>>24)&255;
        return Math.max(0,Math.min(255,(int)Math.round(
            texAlpha*diffuseFactors.get(index)[3])));
    }
    public static synchronized Model decode(byte[] bytes)throws IOException{
        return decode(bytes,null);
    }
    /** Render a captured native candidate using real runtime materials, then release all transient registrations. */
    static synchronized BufferedImage renderNativeMaterialReview(byte[] bytes,SceneRenderer renderer,double rotation)throws IOException{
        return renderNativeMaterialReview(bytes,renderer,rotation,List.of(),null);
    }
    static synchronized BufferedImage renderNativeMaterialReview(byte[] bytes,SceneRenderer renderer,double rotation,
            List<Long> faceIds,java.util.function.LongPredicate visibleFace)throws IOException{
        return renderNativeMaterialReview(bytes,renderer,rotation,faceIds,visibleFace,0);
    }
    static synchronized BufferedImage renderNativeMaterialReview(byte[] bytes,SceneRenderer renderer,double rotation,
            List<Long> faceIds,java.util.function.LongPredicate visibleFace,int checkerSize)throws IOException{
        renderReview(bytes,renderer,rotation,faceIds,visibleFace,checkerSize,0,null);
        return renderer.image();
    }
    static synchronized MaterialReviewFrame materialReviewFrame(byte[] bytes,SceneRenderer renderer,double rotation,
            org.beckenburg.editor.EditorMeshPipeline.Mesh source,java.util.function.LongPredicate visibleFace,int checkerSize)throws IOException{
        return materialReviewFrame(bytes,renderer,rotation,source,visibleFace,checkerSize,0);
    }
    static synchronized MaterialReviewFrame materialReviewFrame(byte[] bytes,SceneRenderer renderer,double rotation,
            org.beckenburg.editor.EditorMeshPipeline.Mesh source,java.util.function.LongPredicate visibleFace,int checkerSize,int mip)throws IOException{
        var frame=new MaterialReviewFrame(renderer.image(),mip==0);
        renderReview(bytes,renderer,rotation,source.faceIds(),visibleFace,checkerSize,mip,frame);
        frame.bindVisible(source);
        return frame;
    }
    static int reviewTexel(int material,double u,double v){
        int index=material-TEXTURE_BASE;
        if(index<0||index>=textures.size()||!Double.isFinite(u)||!Double.isFinite(v))return -1;
        var image=textures.get(index);
        int x=imageColumn(u,image.getWidth(),samplerWrapS.get(index));
        return x+image.getWidth()*imageRow(v,image.getHeight(),vehicleUvMaterials.get(index),samplerWrapT.get(index));
    }
    private static void renderReview(byte[] bytes,SceneRenderer renderer,double rotation,
            List<Long> faceIds,java.util.function.LongPredicate visibleFace,int checkerSize,int mip,MaterialReviewFrame frame)throws IOException{
        if(checkerSize<0 || checkerSize>2048)throw new IOException("Invalid checker size");
        List<List<?>> tables=List.of(textures,vehicleUvMaterials,samplerWrapS,samplerWrapT,materialNames,diffuseFactors);int[] lengths=tables.stream().mapToInt(List::size).toArray();int first=TEXTURE_BASE+textures.size();
        try{
            var model=decode(bytes,null);
            if(checkerSize>0)for(int i=lengths[0];i<textures.size();i++){
                var image=textures.get(i);
                var checker=new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_INT_ARGB);
                var row=new int[image.getWidth()];
                for(int y=0;y<image.getHeight();y++){
                    image.getRGB(0,y,row.length,1,row,0,row.length);
                    for(int x=0;x<row.length;x++){
                        int shade=((x/checkerSize+y/checkerSize)&1)==0?0xeeeeee:0x555555;
                        row[x]=(row[x]&0xff000000)|shade;
                    }
                    checker.setRGB(0,y,row.length,1,row,0,row.length);
                }
                textures.set(i,checker);
                diffuseFactors.set(i,new double[]{1,1,1,diffuseFactors.get(i)[3]});
            }
            if(mip>0)for(int i=lengths[0];i<textures.size();i++){
                int material=TEXTURE_BASE+i;
                boolean alpha=gltfBlendMaterials.contains(material)||gltfMaskCutoffs.containsKey(material);
                textures.set(i,MaterialReviewMip.level(textures.get(i),mip,alpha));
            }
            var ids=faceIds;
            if(frame!=null && model.triangles().size()!=faceIds.size())throw new IOException("Native face order unavailable");
            if(visibleFace!=null){
                if(model.triangles().size()!=faceIds.size())throw new IOException("Native face order unavailable");
                var faces=new ArrayList<Triangle>();
                var visibleIds=new ArrayList<Long>();
                for(int i=0;i<faceIds.size();i++)if(visibleFace.test(faceIds.get(i))){faces.add(model.triangles().get(i));visibleIds.add(faceIds.get(i));}
                ids=visibleIds;
                model=new Model(List.copyOf(faces));
            }
            renderer.drawNativeMaterialReview(model,rotation,ids,frame);
        }
        finally{for(int i=0;i<tables.size();i++)tables.get(i).subList(lengths[i],tables.get(i).size()).clear();gltfBlendMaterials.removeIf(id->id>=first);gltfMaskCutoffs.keySet().removeIf(id->id>=first);}
    }
    /** Developer asset room only: preserve the actual named asset's UV origin
     * behavior while auditing source GLBs that are NOT runtime approved.
     * This method does not modify the normal game's model allowlist.
     */
    static synchronized Model decodeForReview(byte[] bytes,String resourceName)throws IOException{
        if(resourceName==null||!resourceName.matches("[A-Za-z0-9_-]{1,90}"))
            throw new IOException("Invalid dev room source ID");
        return decode(bytes,resourceName);
    }
    private static synchronized Model decode(byte[] bytes,String resourceName)throws IOException{
        var source=org.beckenburg.editor.EditorGlbReader.read(bytes);int[] materialIds=new int[source.materials().size()];
        for(var material:source.materials()){
            int i=material.index();var picture=ImageIO.read(new ByteArrayInputStream(material.png()));if(bookCoverMaterial(resourceName))picture=readableBookCover(picture);
            materialIds[i]=TEXTURE_BASE+textures.size();if(material.alpha().equals("BLEND"))gltfBlendMaterials.add(materialIds[i]);else if(material.alpha().equals("MASK"))gltfMaskCutoffs.put(materialIds[i],material.cutoff());
            textures.add(picture);samplerWrapS.add(material.wrapS());samplerWrapT.add(material.wrapT());
            boolean external=resourceName==null||ImportedAssetLibrary.approved(resourceName);
            vehicleUvMaterials.add(external?material.defaultOrigin().equals("TOP_LEFT"):(resourceName!=null&&(resourceName.startsWith("ggbot_car")||resourceName.startsWith("level_hc_native_")||LevelAssets.AUTHORED_SURFACES.contains(resourceName)||LevelAssets.TAILORED_FABRIC.contains(resourceName)||LevelAssets.FITTED_TENTS.contains(resourceName)||LevelAssets.SITE_ART.contains(resourceName)||LevelAssets.GROUNDED_BERMS.contains(resourceName)||LevelAssets.GREEN_GROUND.contains(resourceName)||LevelAssets.NATIVE_SITE_TERRAIN.contains(resourceName)||fluorescentGltfUv(resourceName)||reviewedSourceGltfUv(resourceName)||diagnosticBookUvOrigin(resourceName)||diagnosticRemainingUvOrigin(resourceName)) && !diagnosticLegacyUvOrigin(resourceName)));
            if(material.explicitOrigin()!=null)vehicleUvMaterials.set(vehicleUvMaterials.size()-1,material.explicitOrigin().equals("TOP_LEFT"));
            materialNames.add(material.name());diffuseFactors.add(material.color());if("monitoring_screen_mp_1".equals(resourceName)&&"crt_screen_mp_2".equals(material.name()))vanCrtMaterial=materialIds[i];
        }
        var result=new ArrayList<Triangle>();for(var face:source.faces()){var primitive=face.primitive();int i=face.triangle()*3;
            var a=primitive.world().get(primitive.indices().get(i));var b=primitive.world().get(primitive.indices().get(i+1));var c=primitive.world().get(primitive.indices().get(i+2));result.add(new Triangle(runtimeCorner(a),runtimeCorner(b),runtimeCorner(c),materialIds[primitive.material()]));
        }
        return new Model(List.copyOf(result));
    }
    private static Vertex runtimeCorner(org.beckenburg.editor.EditorMeshPipeline.Vertex v){var n=v.normal();return new Vertex(v.x(),v.y(),v.z(),v.u(),v.v(),n==null?0:n.x(),n==null?0:n.y(),n==null?0:n.z());}
}
