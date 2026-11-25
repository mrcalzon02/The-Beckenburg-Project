package org.beckenburg.editor;

import java.util.Arrays;
import static org.beckenburg.editor.EditorTopologyProject.*;

/** UV variables shared only through uncut native half-edge fans. */
final class EditorUvChart {
    final int[] root;
    final boolean[] cut,boundary;

    EditorUvChart(State state,EditorMeshTopology graph,int[] island){
        int count=island.length*3;
        root=new int[count];cut=new boolean[count];boundary=new boolean[count];
        int[] local=new int[state.faces().size()];Arrays.fill(local,-1);
        for(int i=0;i<island.length;i++)local[island[i]]=i;
        for(int i=0;i<count;i++)root[i]=i;
        for(int i=0;i<count;i++){
            int neighbor=EditorTopologyProject.uvNeighbor(state,graph,island[i/3],i%3);
            if(neighbor<0||local[neighbor]<0){cut[i]=true;continue;}
            int t=3*local[neighbor]+graph.edges().get(island[i/3]*3+i%3).twin()%3;
            root[find(i)]=find(next(t));root[find(next(i))]=find(t);
        }
        for(int i=0;i<count;i++)root[i]=find(i);
        for(int i=0;i<count;i++)if(cut[i]){boundary[root[i]]=true;boundary[root[next(i)]]=true;}
    }
    static int next(int i){return i/3*3+(i%3+1)%3;}
    private int find(int i){while(root[i]!=i){root[i]=root[root[i]];i=root[i];}return i;}
}
