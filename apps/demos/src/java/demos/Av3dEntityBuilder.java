/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package demos;

// External Imports

// Internal Imports
import org.chefx3d.view.awt.av3d.DefaultDynamicSegmentEntity;
import org.chefx3d.model.Entity;

import org.chefx3d.tool.*;


import org.chefx3d.model.WorldModel;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

import org.j3d.aviatrix3d.ApplicationUpdateObserver;

/**
 * A helper class to construct entities
 *
 * @author Russell Dodds
 * @version $Revision: 1.1 $
 */
public class Av3dEntityBuilder extends EntityBuilder {

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** The singleton class */
    private static Av3dEntityBuilder av3dEntityBuilder;

    protected Av3dEntityBuilder() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Get the singleton EntityBuilder.
     *
     * @return The EntityBuilder
     */
    public static Av3dEntityBuilder getEntityBuilder() {
        if (av3dEntityBuilder == null) {
            av3dEntityBuilder = new Av3dEntityBuilder();
        }

        return av3dEntityBuilder;
    }

    // ----------------------------------------------------------
    // Local methods
    // ----------------------------------------------------------

	/**
	 * Create a DynamicSegmentEntityGenerator to create the segment geometry.
	 * This default implementation will always return null, because it is up
	 * to the project to define how it wants to represent segments and must
	 * overwrite this method to provide that feature.
	 * 
	 * @param mgmtObserver SceneManagerObserver used by DynamicSegmentEntityGenerator
	 * @return DefaultDynamicSegmentEntity
	 */
	public DynamicSegmentEntityGenerator getDynamicSegmentEntityGenerator(
	        ApplicationUpdateObserver mgmtObserver){		
	        
        if(mgmtObserver == null){
            return null;
        }
        
        DefaultDynamicSegmentEntity defaultDynamicSegmentEntity = 
            new DefaultDynamicSegmentEntity();        
        defaultDynamicSegmentEntity.setSceneManagerObserver(mgmtObserver);
        
        return defaultDynamicSegmentEntity;

	}
    
    /**
     * Create an Entity and issue the addEntity command
     *
     * @param model The model
     * @param entityID The entity ID
     * @param position The position in 3 dimensions
     * @param rotation The rotation in 3 dimensions
     * @param tool The tool used to create the entity
     * @return The new Entity
     */
    public Entity createEntity(
            WorldModel model,
            int entityID,
            double[] position,
            float[] rotation,
            Tool tool) {

        return super.createEntity(model, entityID, position, rotation, tool);
    }
    
}
