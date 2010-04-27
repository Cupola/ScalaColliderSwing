package de.sciss.synth.swing;

import prefuse.data.Node;
import prefuse.visual.VisualTable;
import prefuse.visual.VisualTree;

public class VisualInsertionTree extends VisualTree {
    public static final String HEAD     = "head";
    public static final String TAIL     = "tail";
    public static final String PRED     = "pred";
    public static final String SUCC     = "succ";
    public static final String PARENT   = "parent";
    public static final String[] FIELDS = { HEAD, TAIL, PRED, SUCC, PARENT };

    public VisualInsertionTree(VisualTable nodes, VisualTable edges,
            String nodeKey, String sourceKey, String targetKey) {
        super(nodes, edges, nodeKey, sourceKey, targetKey);
    }

    /**
     *  Assumes the first child is found in
     *  column <code>HEAD</code>
     */
    @Override public Node getFirstChild( Node node ) {
        return getRelatedChild( node, HEAD );
    }

    /**
     *  Assumes the last child is found in
     *  column <code>TAIL</code>
     */
    @Override public Node getLastChild( Node node ) {
        return getRelatedChild( node, TAIL );
    }

    /**
     *  Assumes the previous sibling is found in
     *  column <code>PRED</code>
     */
    @Override public Node getPreviousSibling( Node node ) {
        return getRelatedSibling( node, PRED );
    }

    /**
     *  Assumes the next sibling is found in
     *  column <code>SUCC</code>
     */
    @Override public Node getNextSibling( Node node ) {
        return getRelatedSibling( node, SUCC );
    }

    private Node getRelatedChild( Node node, String relName ) {
        final Node rel = (Node) node.get( relName );
        if( rel == null ) return null;
        final int nodeIdx   = node.getRow();
        final int[] links   = (int[]) m_links.get( nodeIdx, OUTLINKS );
        final int relIdx    = rel.getRow();
        final int idx       = m_links.getInt( relIdx, CHILDINDEX );
        return( idx < 0 ? null : getNode( getTargetNode( links[ idx ])));
    }

    private Node getRelatedSibling( Node node, String relName ) {
        final Node rel = (Node) node.get( relName );
        if( rel == null ) return null;
        final int nodeIdx   = node.getRow();
        final int p         = getParent( nodeIdx );
        if( p < 0 ) return null;
        final int[] links   = (int[]) m_links.get( p, OUTLINKS );
        final int relIdx    = rel.getRow();
        final int idx       = m_links.getInt( relIdx, CHILDINDEX );
        return( idx < 0 ? null : getNode( getTargetNode( links[ idx ])));
    }
}