/*****************************************************************************
*                        Copyright Yumetech, Inc (c) 2009
*                               Java Source
*
* This source is licensed under the GNU LGPL v2.1
* Please read http://www.gnu.org/copyleft/lgpl.html for more information
*
* This software comes with the standard NO WARRANTY disclaimer for any
* purpose. Use it at your own risk. If there's a problem you get to fix it.
*
****************************************************************************/

package org.chefx3d.view.awt.av3d;

// External imports
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

// Local imports
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

import org.chefx3d.model.*;

/**
 * Container for Entity parameters used to process motion commands
 *
 * @author Rex Melton
 * @version $Revision: 1.11 $
 */
class ActionData {

    /** The model */
    WorldModel model;

    /** The mouse position, in screen space */
    float[] mouseDevicePosition;

    /** The mouse position, in world space */
    float[] mouseWorldPosition;

    /** The map of entity wrappers */
    HashMap<Integer, AV3DEntityWrapper> wrapperMap;

    /** The working zone entity wrapper */
    AV3DEntityWrapper zoneWrapper;

    /** The orientation of the editing zone */
    ZoneOrientation zoneOri;

    /** The PickManager */
    PickManager pickManager;

    ///////////////////USED ONLY FOR SEGMENT ENTITIES//////////
    /** The thickness of a segment Entity */
    float thickness;
    //////////////////////////////////////////////////////////

    /** Sidepocketed entity data */
    ArrayList<EntityData> entityList;

    /**
     * Default Constructor
     */
    ActionData() {
        this(null, null, null);
    }

    /**
     * Constructor
     *
     * @param pe The entity to stow parameters from
     * @param mouseDevicePosition The device position of the mouse
     * @param mouseWorldPosition The world position of the mouse
     * @param zoneOri The orientation vectors of the editing zone
     */
    ActionData(
        float[] mouseDevicePosition,
        float[] mouseWorldPosition,
        ZoneOrientation zoneOri) {

        this.mouseDevicePosition = new float[3];
        this.mouseWorldPosition = new float[3];

        setMouseDevicePosition(mouseDevicePosition);
        setMouseWorldPosition(mouseWorldPosition);
        this.zoneOri = zoneOri;

        entityList = new ArrayList<EntityData>();
    }

    /**
     * Initialize the mouse device position
     *
     * @param mouseDevicePosition The device position of the mouse
     */
    void setMouseDevicePosition(float[] mouseDevicePosition) {
        if (mouseDevicePosition != null) {
            this.mouseDevicePosition[0] = mouseDevicePosition[0];
            this.mouseDevicePosition[1] = mouseDevicePosition[1];
            this.mouseDevicePosition[2] = mouseDevicePosition[2];
        }
    }

    /**
     * Initialize the mouse world position
     *
     * @param mouseWorldPosition The world position of the mouse
     */
    void setMouseWorldPosition(float[] mouseWorldPosition) {
        if (mouseWorldPosition != null) {
            this.mouseWorldPosition[0] = mouseWorldPosition[0];
            this.mouseWorldPosition[1] = mouseWorldPosition[1];
            this.mouseWorldPosition[2] = mouseWorldPosition[2];
        }
    }

    /**
     * Initialize data for the entities in play
     */
    void setEntities(ArrayList<Entity> sel) {
        entityList.clear();
        for (int i = 0; i < sel.size(); i++) {
            entityList.add(new EntityData((PositionableEntity)sel.get(i)));
        }
    }

    /**
     * Initialize data for the entities in play
     */
    void setEntity(PositionableEntity pe) {
        entityList.clear();
        entityList.add(new EntityData(pe));
    }

    /**
     * Container for entity parameters
     */
    class EntityData {

        /** the entity */
        PositionableEntity entity;

        /** the parent entity ID */
        int parentEntityID;

        /** the entity's position */
        double[] position;

        /** the entity's rotation */
        float[] rotation;

        /** A list of children at the start of a scale */
        ArrayList<Entity> startChildren;

        /** A list of positionable info */
        ArrayList<PositionableData> startPositions;

        EntityData(PositionableEntity entity) {
            this.entity = entity;
            parentEntityID = entity.getParentEntityID();
            position = new double[3];
            entity.getPosition(position);
            rotation = new float[4];
            entity.getRotation(rotation);

            // copy the children
            startChildren = new ArrayList<Entity>();
            startPositions = new ArrayList<PositionableData>();

            int len = entity.getChildCount();
            Entity child;
            for (int i = 0; i < len; i++) {
                child = entity.getChildAt(i);
                startChildren.add(entity.getChildAt(i));

                if (child instanceof PositionableEntity) {
                    startPositions.add(((PositionableEntity)child).getPositionableData());
                } else {
                    startPositions.add(null);
                }
            }
        }
    }
}
