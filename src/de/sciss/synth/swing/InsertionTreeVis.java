package de.sciss.synth.swing;

import prefuse.Visualization;
import prefuse.data.Graph;
import prefuse.data.Schema;
import prefuse.data.Tree;
import prefuse.data.expression.Predicate;
import prefuse.data.tuple.TupleManager;
import prefuse.util.PrefuseLib;
import prefuse.visual.VisualTable;
import prefuse.visual.VisualTree;
import prefuse.visual.tuple.TableEdgeItem;
import prefuse.visual.tuple.TableNodeItem;

public class InsertionTreeVis extends Visualization {
    @Override public synchronized VisualTree addTree(String group, Tree tree,
            Predicate filter, Schema nodeSchema, Schema edgeSchema)
    {
    	checkGroupExists(group); // check before adding sub-tables
        String ngroup = PrefuseLib.getGroupName(group, Graph.NODES);
        String egroup = PrefuseLib.getGroupName(group, Graph.EDGES);

        VisualTable nt, et;
        nt = addTable(ngroup, tree.getNodeTable(), filter, nodeSchema);
        et = addTable(egroup, tree.getEdgeTable(), filter, edgeSchema);

        VisualTree vt = new VisualInsertionTree(nt, et, tree.getNodeKeyField(),
                tree.getEdgeSourceField(), tree.getEdgeTargetField());
        vt.setVisualization(this);
        vt.setGroup(group);

        addDataGroup(group, vt, tree);

        TupleManager ntm = new TupleManager(nt, vt, TableNodeItem.class);
        TupleManager etm = new TupleManager(et, vt, TableEdgeItem.class);
        nt.setTupleManager(ntm);
        et.setTupleManager(etm);
        vt.setTupleManagers(ntm, etm);

        return vt;
    }
}
