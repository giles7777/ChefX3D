/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.model;

// External Imports
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;

//Internal Imports
import org.chefx3d.util.DOMUtils;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A command for spliting a segment into 2 segments.
 *
 * @deprecated
 * @author Alan Hudson
 * @version $Revision: 1.9 $
 */
public class SplitSegmentCommand implements Command {

    /** The model */
    private WorldModel model;

    /** The entityID */
    private SegmentableEntity entity;

    /** The vertexID */
    private int vertexID;

    /** The segmentID */
    private int segmentID;

    /** The vertex */
    private SegmentEntity vertex;

    /** Is this a local add */
    private boolean local;

    /** The description of the <code>Command</code> */
    private String description;

    /** The flag to indicate transient status */
    private boolean transientState;

    /** The flag to indicate undoable status */
    private boolean undoableState;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;
    
    /** A list of strings of class names of rules to ignore*/
    private HashSet<String> ignoreRuleList;

    /**
     * Add a segment to an entity.
     *
     * @param worldModel The model to change
     * @param entity The entity
     * @param segmentID The segment to split
     * @param vertexID The new vertex to bisect with
     */
    public SplitSegmentCommand(
            WorldModel worldModel,
            SegmentableEntity entity,
            int segmentID,
            int vertexID) {

        // Cast to package definition to access protected methods
        this.model = worldModel;
        this.entity = entity;
        this.segmentID = segmentID;
        this.vertexID = vertexID;

        local = true;

        init();
    }

    /**
     * Add a segment to an entity.
     *
     * @param model The model to change
     */
    public SplitSegmentCommand(WorldModel model) {
        // Cast to package definition to access protected methods
        this.model = (BaseWorldModel) model;

        init();
    }


    /**
     * Common initialization code.
     */
    private void init() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        description = "SplitSegment -> " + segmentID;

        // TODO: change when implemented
        undoableState = false;
        transientState = false;
    }

    /**
     * Execute the command.
     */
    public void execute() {
        System.out.println("SplitSegment no longer supported");
    }

    /**
     * Set the local flag.
     *
     * @param isLocal Is this a local update
     */
    public void setLocal(boolean isLocal) {
        local = isLocal;
    }

    /**
     * Is the command locally generated.
     *
     * @return Is local
     */
    public boolean isLocal() {
        return local;
    }

    /**
     * Undo the affects of this command.
     */
    public void undo() {
        // TODO: Need to implement
        System.out.println("SplitSegment no longer supported");
    }

    /**
     * Redo the affects of this command.
     */
    public void redo() {
        execute();
    }

    /**
     * Get the text description of this <code>Command</code>.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the text description of this <code>Command</code>.
     */
    public void setDescription(String desc) {
        description = desc;
    }

    /**
     * Get the state of this <code>Command</code>.
     */
    public boolean isTransient() {
        return transientState;
    }

    /**
     * Get the transactionID for this command.
     *
     * @return The transactionID
     */
    public int getTransactionID() {
        return 0;
    }

    /**
     * Get the undo setting of this <code>Command</code>. true =
     * <code>Command</code> may be undone false = <code>Command</code> may
     * never undone
     */
    public boolean isUndoable() {
        return undoableState;
    }

    /**
     * Serialize this command.
     *
     * @param method What method should we use
     * @param os The stream to output to
     */
    public void serialize(int method, OutputStream os) {
        switch (method) {
        case METHOD_XML:
            /*
             * <SplitSegmentCommand entityID='' vertexID='' />
             */

            StringBuilder sbuff = new StringBuilder();
            sbuff.append("<SplitSegmentCommand entityID='");
            sbuff.append(entity.getEntityID());
            sbuff.append("' vertexID='");
            sbuff.append(vertexID);
            sbuff.append("' />");

            String st = sbuff.toString();

            PrintStream ps = new PrintStream(os);
            ps.print(st);

            break;
        case METHOD_XML_FAST_INFOSET:
            errorReporter.messageReport("Unsupported serialization method");
            break;
        }
    }

    /**
     * Deserialize a stream
     *
     * @param st The xml string to deserialize
     */
    public void deserialize(String st) {
        Document doc = DOMUtils.parseXML(st);

        Element e = (Element) doc.getFirstChild();

        int entityID = Integer.parseInt(e.getAttribute("entityID"));
        entity = (SegmentableEntity)model.getEntity(entityID);

        vertexID = Integer.parseInt(e.getAttribute("vertexID"));

        local = false;
    }

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        if(errorReporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    public HashSet<String> getIgnoreRuleList() {
        // TODO Auto-generated method stub
        return ignoreRuleList;
    }

    public void setIgnoreRuleList(HashSet<String> ignoreRuleList) {
        this.ignoreRuleList = ignoreRuleList;
    }

}
