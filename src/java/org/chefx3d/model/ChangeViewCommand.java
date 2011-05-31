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

//External Imports
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Locale;
import org.w3c.dom.*;

//Internal Imports
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.DOMUtils;

/**
 * A command for moving the master view.
 * 
 * @author Alan Hudson
 * @version $Revision: 1.16 $
 */
public class ChangeViewCommand implements Command {
    /** The model */
    private BaseWorldModel model;

    /** The position */
    private double[] pos;

    /** The rotation */
    private float[] rot;

    /** The field of view */
    private float fov;

    /** Is this a local add */
    private boolean local;

    /** The description of the <code>Command</code> */
    private String description;

    /** The flag to indicate treansient status */
    private boolean transientState;

    /** The flag to indicate undoable status */
    private boolean undoableState;

    /** The transactionID */
    private int transactionID;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;
    
    /** A list of strings of class names of rules to ignore*/
    private HashSet<String> ignoreRuleList;

    /**
     * Master View changed.
     * 
     * @param model The model to change
     * @param transID The transactionID
     * @param position The position in world coordinates(meters, Y-UP, X3D
     *        System).
     * @param orientation The orientation
     * @param fov The field of view
     */
    public ChangeViewCommand(WorldModel model, int transID, double[] position,
            float[] orientation, float fov) {

        // Cast to package definition to access protected methods
        this.model = (BaseWorldModel) model;
        transactionID = transID;

        pos = new double[3];
        pos[0] = position[0];
        pos[1] = position[1];
        pos[2] = position[2];

        rot = new float[4];
        rot[0] = orientation[0];
        rot[1] = orientation[1];
        rot[2] = orientation[2];
        rot[3] = orientation[3];

        this.fov = fov;

        local = true;

        init();
    }

    public ChangeViewCommand(WorldModel model) {
        // Cast to package definition to access protected methods
        this.model = (BaseWorldModel) model;

        init();
    }

    /**
     * Common initialization code.
     */
    private void init() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        description = "ChangeView";
        
        undoableState = true;
        transientState = false;
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
     * Execute the command.
     */
    public void execute() {
        model.setViewParams(local, pos, rot, fov, null);
    }

    /**
     * Undo the affects of this command.
     */
    public void undo() {
        errorReporter.messageReport("Undo not implemented for ChangeViewCommand");
    }

    /**
     * Redo the affects of this command.
     */
    public void redo() {
        errorReporter.messageReport("Redo not implemented for ChangeViewCommand");
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
        return transactionID;
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
             * <ChangeViewCommand tID='' x='' y='' z='' rx='' ry='' rz='' ra=''
             * fov=''/>
             */

            // TODO: double length is pretty damn long, cut to float for now?
            StringBuilder sbuff = new StringBuilder();

            // TODO: can't stop it padding 0 on right, impossible with this
            // class?
            sbuff.append("<ChangeViewCommand tID='");
            sbuff.append(transactionID);
            sbuff.append("' x='");
            sbuff.append(String.format((Locale) null, "%.3f", pos[0]));
            sbuff.append("' y='");
            sbuff.append(String.format((Locale) null, "%.3f", pos[1]));
            sbuff.append("' z='");
            sbuff.append(String.format((Locale) null, "%.3f", pos[2]));
            sbuff.append("' rx='");
            sbuff.append(String.format((Locale) null, "%.3f", rot[0]));
            sbuff.append("' ry='");
            sbuff.append(String.format((Locale) null, "%.3f", rot[1]));
            sbuff.append("' rz='");
            sbuff.append(String.format((Locale) null, "%.3f", rot[2]));
            sbuff.append("' ra='");
            sbuff.append(String.format((Locale) null, "%.3f", rot[3]));
            sbuff.append("' fov='");
            sbuff.append(String.format((Locale) null, "%.2f", fov));
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
        String d;

        pos = new double[3];
        d = e.getAttribute("x");
        pos[0] = Double.parseDouble(d);
        d = e.getAttribute("y");
        pos[1] = Double.parseDouble(d);
        d = e.getAttribute("z");
        pos[2] = Double.parseDouble(d);

        rot = new float[4];

        d = e.getAttribute("rx");
        rot[0] = Float.parseFloat(d);
        d = e.getAttribute("ry");
        rot[1] = Float.parseFloat(d);
        d = e.getAttribute("rz");
        rot[2] = Float.parseFloat(d);
        d = e.getAttribute("ra");
        rot[3] = Float.parseFloat(d);

        d = e.getAttribute("fov");
        fov = Float.parseFloat(d);

        transactionID = Integer.parseInt(e.getAttribute("tID"));

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