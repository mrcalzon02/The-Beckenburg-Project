package org.beckenburg.editor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Engine-independent editing contract. No Beckenburg game or Swing dependency.
 * Other games implement this adapter to reuse scene navigation, tools, history,
 * palette and asset browser without importing castle geometry.
 */
public interface EditorProject<S> {
    record Node(String id,String kind,String label,double x,double y,double z) {
        public Node {
            if(id==null||id.isBlank()||kind==null||kind.isBlank()||label==null)
                throw new IllegalArgumentException("Invalid scene node");
        }
    }
    record Asset(String uri,String category,String label) {
        public Asset {
            EditorAssets.validateUri(uri);
            if(category==null||category.isBlank()||label==null||label.isBlank())
                throw new IllegalArgumentException("Invalid palette asset");
        }
    }
    /** Included in the standalone editor kit for every project adapter. */
    default DefaultPlayerMotion createPlayerMotion(){return new DefaultPlayerMotion();}
    String id();
    String title();
    Path documentPath();
    List<Node> hierarchy();
    record HierarchyPage(int total,int offset,List<Node> nodes) {
        public HierarchyPage {
            nodes=List.copyOf(nodes);
            if (offset<0 || offset>total || nodes.size()>64 || nodes.size()>total-offset)
                throw new IllegalArgumentException("Invalid native hierarchy page");
        }
        public static int end(int total,int offset,int limit) {
            if (offset<0 || offset>total || limit<1 || limit>64)
                throw new IllegalArgumentException("Expected a valid page offset and limit 1 to 64");
            return offset+Math.min(limit,total-offset);
        }
    }
    default HierarchyPage hierarchyPage(int offset,int limit) {
        var rows=hierarchy();
        return new HierarchyPage(rows.size(),offset,rows.subList(offset,HierarchyPage.end(rows.size(),offset,limit)));
    }
    /** Hosts can resolve one human hierarchy row directly from their native storage. */
    default Node node(String id) {
        for (var row : hierarchy()) if (row.id().equals(id)) return row;
        throw new IllegalArgumentException("Unknown native hierarchy node");
    }
    List<Asset> assets();
    S snapshot();
    void restore(S snapshot);
    void save() throws IOException;
}
