/*****************************************************************************
 *                        Yumetech, Inc Copyright (c) 2007
 *                               Java Source
 *
 * This source is licensed under the BSD license.
 * Please read docs/BSD.txt for the text of the license.
 *
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.actions.awt;

// External imports
import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;

import org.j3d.util.I18nManager;

// Local imports
import org.chefx3d.model.*;

import org.chefx3d.tool.DefaultEntityBuilder;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.SegmentTool;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.view.ViewManager;

/**
 * An action that can be used to delete the selected entity
 * from the model
 * <p>
 *
 * <b>Internationalisation Resource Names</b>
 * <ul>
 * <li>addTitle: The name that appears on the action when an add is being performed</li>
 * <li>joinTitle: The name that appears on the action when a join is being performed</li>
 * <li>description: The short description to go with the action (eg tooltip)</li>
 * <li>splitCommand: The text that gets set in the command for undo/redo</li>
 * </ul>
 *
 * @author Russell Dodds
 * @version $Revision: 1.23 $
 */
public class AddControlPointAction extends AbstractAction {

    /** Name of the property to get the action name */
    private static final String ADD_TITLE_PROP =
        "org.chefx3d.actions.awt.AddControlPointAction.addTitle";

    private static final String JOIN_TITLE_PROP =
        "org.chefx3d.actions.awt.AddControlPointAction.joinTitle";

    /** Name of the property to get the action name */
    private static final String DESCRIPTION_PROP =
        "org.chefx3d.actions.awt.AddControlPointAction.description";

    /** Name of the property to get the action name */
    private static final String SPLIT_CMD_PROP =
        "org.chefx3d.actions.awt.AddControlPointAction.splitCommand";

    /** The world model */
    private WorldModel model;

    /** The ID of the selected Entity in the model. A
    * value of -1 means that no Entity is selected. */
    private int entityID = -1;

    /** The entity selected */
    private SegmentableEntity segmentEntity;

    /** The last world coordinate position */
    private double[] pos;

    private EntityBuilder entityBuilder;

    /** Helper class that knows what is selected */
    private EntitySelectionHelper selectionHelper;

    /**
     * Create an instance of the action class.
     *
     * @param iconOnly True if you want to display the icon only, and no text
     *    labels
     * @param icon The icon
     * @param worldModel The world model
     */
    public AddControlPointAction(boolean iconOnly,
                                 Icon icon,
                                 WorldModel worldModel) {

        I18nManager intl_mgr = I18nManager.getManager();

        if (icon != null)
            putValue(Action.SMALL_ICON, icon);

        if (!iconOnly)
            putValue(Action.NAME, intl_mgr.getString(ADD_TITLE_PROP));

        model = worldModel;

        KeyStroke acc_key = KeyStroke.getKeyStroke(KeyEvent.VK_P, 0);

        putValue(ACCELERATOR_KEY, acc_key);
        putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_P));

        putValue(SHORT_DESCRIPTION, intl_mgr.getString(DESCRIPTION_PROP));

        setEnabled(true);
        segmentEntity = null;

        entityBuilder = DefaultEntityBuilder.getEntityBuilder();
        selectionHelper = EntitySelectionHelper.getEntitySelectionHelper();

    }

    /**
     * Create an instance of the action class.
     *
     * @param standAlone Is this standalone or in a menu
     * @param icon The icon
     * @param model The world model
     * @param entityID The selected entity
     * @param position - The last world coordinate position
     */
    public AddControlPointAction(
            boolean standAlone,
            Icon icon,
            WorldModel model,
            int entityID,
            double[] position) {

        this(standAlone, icon, model);
        this.entityID = entityID;

        pos = new double[3];
        pos[0] = position[0];
        pos[1] = position[1];
        pos[2] = position[2];

        segmentEntity = null;

        // check to see if the correct parts are selected
        Entity entity = model.getEntity(entityID);
        if (entity.getType() == Entity.TYPE_BUILDING) {

            segmentEntity = (SegmentableEntity)entity;

            I18nManager intl_mgr = I18nManager.getManager();


            // TODO: fix this to use new selection model
//            if (segmentEntity.getSelectedVertexID() >= 0) {
//                putValue(Action.NAME, intl_mgr.getString(JOIN_TITLE_PROP));
//                setEnabled(true);
//            } else if (segmentEntity.getSelectedSegmentID() >= 0) {
//                putValue(Action.NAME, intl_mgr.getString(ADD_TITLE_PROP));
//                setEnabled(true);
//            }
        }

        selectionHelper = EntitySelectionHelper.getEntitySelectionHelper();
    }

    //----------------------------------------------------------
    // Methods required by the ActionListener interface
    //----------------------------------------------------------

    /**
     * An action has been performed.
     *
     * @param evt The event that caused this method to be called.
     */
    public void actionPerformed(ActionEvent evt) {

        ArrayList<Entity> selected = selectionHelper.getSelectedList();

        int len = selected.size();
        if (len > 0) {

            for (int i = 0; i < len; i++) {

                Entity entity = selected.get(i);
                if (entity instanceof SegmentEntity) {
                    // add a vertex
                    // split the current segment
                    splitSegment();
                    return;
                } else if (entity instanceof VertexEntity) {
                    // add a vertex
                    // add a segment between new vertex and selected vertex
                    joinSegments();
                    return;
                }

            }

        }

    }

    //----------------------------------------------------------
    // Local methods
    //----------------------------------------------------------

    /**
     * Splits the currently selected segment into two parts.
     * The location of the split is the spot where the user
     * right clicked to bring up the menu.  After completed
     * the selection will be changed to the new vertex that
     * was added.
     */
    private void splitSegment() {
        // If we have a selected segment, then
        // insert a new vertex here and start the
        // drag process.

        // needs to be relative to the position of the segment entity
        double[] entityPos = new double[3];
        ((PositionableEntity)segmentEntity).getPosition(entityPos);

        pos[0] -= entityPos[0];
        pos[1] -= entityPos[1];
        pos[2] -= entityPos[2];


        // stack the commands together
        ArrayList<Command> commandList = new ArrayList<Command>();

        EntitySelectionHelper seletionHelper =
            EntitySelectionHelper.getEntitySelectionHelper();

        SegmentEntity segment = null;
        ArrayList<Entity> selectedList = seletionHelper.getSelectedList();
        int len = selectedList.size();
        for (int i = 0; i < len; i++) {
            Entity check = selectedList.get(i);
            if (check instanceof SegmentEntity) {
                // TODO: use the first selected vertex for now
                segment = (SegmentEntity)check;
                break;
            }
        }

        if (segment == null)
            return;


        VertexEntity startVertexEntity = segment.getStartVertexEntity();
        VertexEntity endVertexEntity = segment.getEndVertexEntity();
        int vertexOrder = segmentEntity.getChildIndex(endVertexEntity.getEntityID());

        // if this is the first segment then we need to
        // place the new after the first vertex
        //int firstVertexID = segmentEntity.getStartVertexID();
        //if (firstVertexID == startVertexID) {
        //    vertexOrder++;
        //}


        /*Author Jonathon Hubbard
         * Formula used to place the vertex
         * correctly on the line
         * First retrieve the index of the two vertexs of the line
         * then retrieve the positions
         */
        double[] startPos = new double[3];
        double[] endPos = new double[3];
        startVertexEntity.getPosition(startPos);
        endVertexEntity.getPosition(endPos);


        /* The full forumla for this process is:
         * tempPoint - startPoint - u(endPoint - startPoint)] dot (endPoint - startPoint)
         * So first we Solve for delta X and Delta z since y is always the same
         * then we solve for u
         */
        double xDelta=startPos[0]-endPos[0];
        double zDelta=startPos[2]-endPos[2];
        double u=(( pos[0]-startPos[0])*xDelta+
                ( pos[2]-startPos[2])*zDelta)/
                (Math.pow(xDelta,2)+Math.pow(zDelta,2));

        //Finally we take the equation of the line and subsistute
        //u*delta or u(endPoint - startPoint)
        pos[0]=startPos[0]+u*xDelta;
        pos[2]=startPos[2]+u*zDelta;
//System.out.println("x: "+pos[0]+" z: "+pos[2]);

        boolean exterior = segment.isExteriorSegment();

        // add the remove segment command
        RemoveSegmentCommand segmentCmd =
            new RemoveSegmentCommand(
                    model,
                    segmentEntity,
                    segment.getEntityID());
        commandList.add(segmentCmd);

        int middleVertexID = model.issueEntityID();

        // TODO: lookup segmentableTool to get vertexTool
//        AddVertexCommand vertexCmd =
//            new AddVertexCommand(
//                    segmentEntity,
//                    middleVertexID,
//                    pos,
//                    vertexOrder,
//                    null);
//        commandList.add(vertexCmd);

        // create the segment command
        int currentSegmentID = model.issueEntityID();

        SegmentTool segmentTool =
            (SegmentTool)segmentEntity.getSegmentTool();

        SegmentEntity newSegmentEntity =
            (SegmentEntity)entityBuilder.createEntity(
                model,
                currentSegmentID,
                new double[] {0,0,0},
                new float[] {0,1,0,0},
                segmentTool);
        newSegmentEntity.setStartVertex(startVertexEntity);
        //newSegmentEntity.setEndIndex(middleVertexID);
        newSegmentEntity.setExteriorSegment(exterior);

        AddSegmentCommand segmentCmd1 =
            new AddSegmentCommand(
                    model,
                    segmentEntity,
                    newSegmentEntity);
        commandList.add(segmentCmd1);

        // create the segment command
        currentSegmentID = model.issueEntityID();

        newSegmentEntity =
            (SegmentEntity)entityBuilder.createEntity(
                model,
                currentSegmentID,
                new double[] {0,0,0},
                new float[] {0,1,0,0},
                segmentTool);
        //newSegmentEntity.setStartIndex(middleVertexID);
        newSegmentEntity.setEndVertex(endVertexEntity);
        newSegmentEntity.setExteriorSegment(exterior);

        AddSegmentCommand segmentCmd2 =
            new AddSegmentCommand(
                    model,
                    segmentEntity,
                    newSegmentEntity);
        commandList.add(segmentCmd2);

        I18nManager intl_mgr = I18nManager.getManager();

        MultiCommand stack = new MultiCommand(
                commandList,
                intl_mgr.getString(SPLIT_CMD_PROP) + currentSegmentID);
        model.applyCommand(stack);

//        List<Entity> selected2 = new ArrayList<Entity>();
//        segmentEntity.setSelectedVertexID(middleVertexID);
//        segmentEntity.setSelectedSegmentID(-1);
//        selected2.add(segmentEntity);
//
//        // create the selection list
//        Selection sel = new Selection(segmentEntity.getEntityID(), -1, middleVertexID);
//        List<Selection> list = new ArrayList<Selection>(1);
//        list.add(sel);

        // send the selecting command
        SelectEntityCommand cmdSelect =
            new SelectEntityCommand(model, newSegmentEntity, true);
        model.applyCommand(cmdSelect);

    }

    /**
     * Adds a new vertex to the list.  The location of the new vertex
     * is the spot where the user right clicked to bring up the menu.
     * A new segment is set between the new and selected vertices.
     * After completed the selection will be changed to the new vertex
     * that was added.
     */
    private void joinSegments() {

        ViewManager viewMgr = ViewManager.getViewManager();
        viewMgr.enableAssociateMode(
                new String[] {segmentEntity.getCategory()},
                SegmentableEntity.BRIDGE_VERTICES_ACTION,
                segmentEntity.toString());

    }

}
