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

import org.j3d.aviatrix3d.*;

// Local imports
import org.chefx3d.model.EntityPropertyListener;
import org.chefx3d.model.PositionableEntity;

import org.chefx3d.ui.LoadingProgressListener;
import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

/**
 * A holder of zone entity data for the Aviatrix3D View.
 *
 * @author Russell Dodds
 * @version $Revision: 1.4 $
 */
class ZoneEntityWrapper extends AV3DEntityWrapper 
    implements 
        NodeUpdateListener,
        EntityPropertyListener {
            
    /**
     * Constructor
     *
     * @param mgmtObserver Reference to the SceneManagerObserver
     * @param entity The entity that the wrapper object is based around
     * @param urlFilter The filter to use for url requests
     * @param reporter The instance to use or null
     */
    ZoneEntityWrapper(
        SceneManagerObserver mgmtObserver,
        PositionableEntity entity, 
        URLFilter urlFilter,
        LoadingProgressListener progressListener,
        ErrorReporter reporter){

        super(mgmtObserver, entity, null, progressListener, reporter);
    }

    //----------------------------------------------------------
    // Local Methods
    //----------------------------------------------------------

}
