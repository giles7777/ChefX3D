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
import java.util.ArrayList;

import javax.vecmath.Matrix4f;

import org.chefx3d.model.Entity;
import org.chefx3d.model.PositionableEntity;
import org.chefx3d.model.WorldModel;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.j3d.aviatrix3d.*;

// Local Imports

/**
 * Utilities for calculating transforms across the local scene graph.
 * 
 * @author Rex Melton
 * @version $Revision: 1.9 $
 */	
public class TransformUtils {
    
    /** List for collecting path information to calculate inverse matrices */
    private ArrayList<TransformGroup> pathList;
    
    /** Array for fetching lists of shared parent nodes */
    private Node[] pathNodes;
    
    /** Scratch vecmath objects */
    private Matrix4f mtx;
    
    /**
     * Constructor
     */
    TransformUtils() {
        
        pathList = new ArrayList<TransformGroup>();
        pathNodes = new Node[20];
        mtx = new Matrix4f();
    }
    
    /**
     * Convenience method to walk to the root of the scene and calculate the
     * root to virtual world coordinate location of the given node. If a
     * sharedGroup is found, then take the first parent listed always.
     *
     * @param node The Node to work from
     * @param mat The matrix to put the final result into
     */
    void getLocalToVworld(Node node, Matrix4f mat) {
        
        if (node instanceof TransformGroup) {
            pathList.add((TransformGroup)node);
        }
        
        Node parent = node.getParent();
        
        while (parent != null) {
            if (parent instanceof SharedGroup) {
                
                SharedGroup sg = (SharedGroup)parent;
                
                int num_parents = sg.numParents();
                
                if (num_parents == 0) {
                    break;
                } else if (num_parents > pathNodes.length) {
                    pathNodes = new Node[num_parents];
                }
                
                sg.getParents(pathNodes);
                parent = pathNodes[0];
                
            } else if (parent instanceof SharedNode) {
                
                SharedNode sg = (SharedNode)parent;
                
                int num_parents = sg.numParents();
                
                if (num_parents == 0) {
                    break;
                } else if (num_parents > pathNodes.length) {
                    pathNodes = new Node[num_parents];
                }
                
                sg.getParents(pathNodes);
                parent = pathNodes[0];
                
            } else {
                if (parent instanceof TransformGroup) {
                    pathList.add((TransformGroup)parent);
                }
                parent = parent.getParent();
            }
        }
        
        int num_nodes = pathList.size();
        mat.setIdentity();
        
        for(int i = num_nodes - 1; i >= 0; i--) {
            TransformGroup tg = pathList.get(i);
            tg.getTransform(mtx);
            mat.mul(mtx);
        }
        pathList.clear();
        clear();
    }
    
    /**
     * Convenience method to walk to a target of the scene and calculate the
     * transform to the specified node. If a
     * sharedGroup is found, then take the first parent listed always.
     *
     * @param start The Node to work from
     * @param end The Node to work to
     * @param mat The matrix to put the final result into
     */
    void getLocalToVworld(Node start, Node end, Matrix4f mat) {
        
        //if ((start instanceof TransformGroup) && (start != end)) {
        if (start instanceof TransformGroup) {
            pathList.add((TransformGroup)start);
        }
        
        Node parent = start.getParent();
        
        while ((parent != null) && (parent != end)) {
            if (parent instanceof SharedGroup) {
                
                SharedGroup sg = (SharedGroup)parent;
                
                int num_parents = sg.numParents();
                
                if (num_parents == 0) {
                    break;
                } else if (num_parents > pathNodes.length) {
                    pathNodes = new Node[num_parents];
                }
                
                sg.getParents(pathNodes);
                parent = pathNodes[0];
                
            } else if (parent instanceof SharedNode) {
                
                SharedNode sg = (SharedNode)parent;
                
                int num_parents = sg.numParents();
                
                if (num_parents == 0) {
                    break;
                } else if (num_parents > pathNodes.length) {
                    pathNodes = new Node[num_parents];
                }
                
                sg.getParents(pathNodes);
                parent = pathNodes[0];
                
            } else {
                if (parent instanceof TransformGroup) {
                    pathList.add((TransformGroup)parent);
                }
                parent = parent.getParent();
            }
        }
        
        int num_nodes = pathList.size();
        mat.setIdentity();
        
        for(int i = num_nodes - 1; i >= 0; i--) {
            TransformGroup tg = pathList.get(i);
            tg.getTransform(mtx);
            mat.mul(mtx);
        }
        pathList.clear();
        clear();
    }
    
    /**
     * Clear the path nodes array
     */
    private void clear() {
        for (int i = 0; i < pathNodes.length; i++) {
            pathNodes[i] = null;
        }
    }
    
    /**
     * Get the position of the entity passed in relative to the
     * zone it is a child of.
     * 
     * @param model WorldModel to reference
     * @param entity Entity to start search with
     * @param position Position to accumulate values in, zeroed out
     * @return True if succeeded, false if there was a failure
     */
    public static boolean getLocalToZone(
    		WorldModel model, 
    		Entity entity, 
    		double[] position) {
    	
    	if (entity.isZone()) {
    		
    		return true;
    	}
    	
    	// If entity is not a positionable entity, bail with a result of false
    	if (!(entity instanceof PositionableEntity)) {
    		return false;
    	}
    	
    	// Add on the relative position of the current entity
    	double[] tmpPos = new double[3];
    	((PositionableEntity)entity).getPosition(tmpPos);
    	
    	position[0] += tmpPos[0];
    	position[1] += tmpPos[1];
    	position[2] += tmpPos[2];
    	
    	int parentEntityID = entity.getParentEntityID();
    	Entity parentEntity = model.getEntity(parentEntityID);
    	
    	// If we did not get a valid entity, bail with a result of false
    	if (parentEntity == null) {
    		return false;
    	}
    	
    	return getLocalToZone(model, parentEntity, position);
    }
}
