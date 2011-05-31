/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2006 - 2010
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.rules.util;

//External Imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

//Internal Imports
import org.chefx3d.model.Entity;
import org.chefx3d.view.common.CollisionResultHandler;

/**
 * Collections of various objects.
 *
 * @author Ben Yarger
 * @version $Revision: 1.1 $
 */
public class ChildrenMatches implements CollisionResultHandler{	
	
	/** Break out of model entities matching relationship requirements */
	private ArrayList<Entity> entityMatches;
	
	/** Break out of floor zone collision matches */
	private ArrayList<Entity> floorEntityMatches;
	
	/** Break out of wall collision entities */
	private ArrayList<Entity> wallEntityMatches;
	
	/** Break out of entity zone collision matches */
	private ArrayList<Entity> entityZoneMatches;
	
	/** Break out of generic zone collision entities */
	private ArrayList<Entity> zoneMatches;
	
	/** Break out of entities to replace based on REPLACE_PROD_CLASS_PROP */
	private ArrayList<Entity> replaceEntityMatches;
	
	/** Break out of illegal entities */
	private ArrayList<Entity> illegalEntities;
	
	/** Map of classifications to number of occurrences */
	private HashMap<String, Integer> entityMatchCountMap;
	
	/** Map of classifications to number of occurrences */
	private HashMap<String, Integer> floorMatchCountMap;
	
	/** Map of classifications to number of occurrences */
	private HashMap<String, Integer> wallMatchCountMap;
	
	/** Map of classifications to number of occurrences */
	private HashMap<String, Integer> entityZoneMatchCountMap;
	
	/** Map of classifications to number of occurrences */
	private HashMap<String, Integer> zoneMatchCountMap;
	
	/**
	 * Constructor
	 */
	public ChildrenMatches() {
		entityMatches = new ArrayList<Entity>();
		floorEntityMatches = new ArrayList<Entity>();
		wallEntityMatches = new ArrayList<Entity>();
		entityZoneMatches = new ArrayList<Entity>();
		zoneMatches = new ArrayList<Entity>();
		
		replaceEntityMatches = new ArrayList<Entity>();
		illegalEntities = new ArrayList<Entity>();
		
		entityMatchCountMap = new HashMap<String, Integer>();
		floorMatchCountMap = new HashMap<String, Integer>();
		wallMatchCountMap = new HashMap<String, Integer>();
		entityZoneMatchCountMap = new HashMap<String, Integer>();
		zoneMatchCountMap = new HashMap<String, Integer>();
	}

	//-------------------------------------------------------------------------
	// Methods required by CollisionResultHandler
	//-------------------------------------------------------------------------
	
	/**
	 * Add the entity to the correct result set.
	 * 
	 * @param entity Entity to store in the correct result set
	 * @param colClass Classification name to associate with this entity
	 */
	public void addEntity(Entity entity, String colClass) {
		
		if(entity.getType() == Entity.TYPE_MODEL ||
				entity.getType() == Entity.TYPE_MODEL_WITH_ZONES){
			
			addMatch(entity, colClass, entityMatches, entityMatchCountMap);
			
		} else if (entity.getType() == Entity.TYPE_GROUNDPLANE_ZONE) {
			
			addMatch(entity, colClass, floorEntityMatches, floorMatchCountMap);
			
		} else if (entity.getType() == Entity.TYPE_SEGMENT) {
			
			addMatch(entity, colClass, wallEntityMatches, wallMatchCountMap);
			
		} else if (entity.getType() == Entity.TYPE_MODEL_ZONE) {
			
			addMatch(entity, 
					colClass, 
					entityZoneMatches, 
					entityZoneMatchCountMap);
			
		} else if (entity.getType() == Entity.TYPE_ZONE){
			
			addMatch(entity, colClass, zoneMatches, zoneMatchCountMap);
			
		}
		
	}

	/**
	 * Add an illegal entity to the illegal entity result set.
	 * 
	 * @param entity Entity to add to the illegal entity result set
	 */	
	public void addIllegalEntity(Entity entity) {
		illegalEntities.add(entity);
	}
	
	/**
	 * Add an entity to be replaced to the replace entity result set.
	 * 
	 * @param entity Entity to add to the replace entity result set
	 */
	public void addReplaceEntity(Entity entity) {
		replaceEntityMatches.add(entity);
	}
	
	/**
	 * Get the number of valid matches.
	 * 
	 * @return Number of valid matches
	 */
	public int getNumberOfValidMatches() {
		
		int count = 0;
		count += entityMatches.size();
		count += floorEntityMatches.size();
		count += wallEntityMatches.size();
		count += entityZoneMatches.size();
		count += zoneMatches.size();
		
		return count;
	}
	
	/**
	 * Clear all of the collision results, including replacement and illegal
	 * entities.
	 */
	public void clearAll() {
		
		entityMatches.clear();
		floorEntityMatches.clear();
		wallEntityMatches.clear();
		entityZoneMatches.clear();
		zoneMatches.clear();
		
		illegalEntities.clear();
		replaceEntityMatches.clear();
		
		entityMatchCountMap.clear();
		floorMatchCountMap.clear();
		wallMatchCountMap.clear();
		entityZoneMatchCountMap.clear();
		zoneMatchCountMap.clear();
		
	}

	/**
	 * Print out the contents of the currently held result sets.
	 */
	public void printResults() {
		
		System.out.println();
		System.out.println("--[Children Matches Result Sets]--");
		System.out.println("1) entityMatches...");
		printMatchArrayResults(entityMatches);
		printMatchCountMapResults(entityMatchCountMap);
		System.out.println("2) floorEntityMatches...");
		printMatchArrayResults(floorEntityMatches);
		printMatchCountMapResults(floorMatchCountMap);
		System.out.println("3) wallEntityMatches...");
		printMatchArrayResults(wallEntityMatches);
		printMatchCountMapResults(wallMatchCountMap);
		System.out.println("4) entityZoneMatches...");
		printMatchArrayResults(entityZoneMatches);
		printMatchCountMapResults(entityZoneMatchCountMap);
		System.out.println("5) zoneEntityMatches...");
		printMatchArrayResults(zoneMatches);
		printMatchCountMapResults(zoneMatchCountMap);
		System.out.println("6) replaceEntityMatches...");
		printMatchArrayResults(replaceEntityMatches);
		System.out.println("7) illegalEntities...");
		printMatchArrayResults(illegalEntities);
		System.out.println("--[END MATCH RESULT SETS]--");
		
	}
	
	/**
	 * Copy the contents of the ChildrenMatches passed in to this 
	 * ChildrenMatches object.
	 * 
	 * @param cm ChildrenMatches to copy from
	 */
	public void set(CollisionResultHandler rh) {
		
		if (!(rh instanceof ChildrenMatches)) {
			return;
		}
		
		ChildrenMatches cm = (ChildrenMatches) rh;
		
		entityMatches.clear();
		entityMatches.addAll(cm.getEntityMatches());
		
		entityMatchCountMap.clear();
		entityMatchCountMap.putAll(cm.getEntityMatchCountMap());
		
		floorEntityMatches.clear();
		floorEntityMatches.addAll(cm.getFloorEntityMatches());
		
		floorMatchCountMap.clear();
		floorMatchCountMap.putAll(cm.getFloorMatchCountMap());
		
		wallEntityMatches.clear();
		wallEntityMatches.addAll(cm.getWallEntityMatches());
		
		wallMatchCountMap.clear();
		wallMatchCountMap.putAll(cm.getWallMatchCountMap());
		
		entityZoneMatches.clear();
		entityZoneMatches.addAll(cm.getEntityZoneMatches());
		
		entityZoneMatchCountMap.clear();
		entityZoneMatchCountMap.putAll(cm.getEntityZoneMatchCountMap());
		
		zoneMatches.clear();
		zoneMatches.addAll(cm.getZoneMatches());
		
		zoneMatchCountMap.clear();
		zoneMatchCountMap.putAll(cm.getZoneMatchCountMap());
		
		illegalEntities.clear();
		illegalEntities.addAll(cm.getIllegalEntities());
		
		replaceEntityMatches.clear();
		replaceEntityMatches.addAll(cm.getReplaceEntityMatches());
	}
	
	//-------------------------------------------------------------------------
	// Public Methods
	//-------------------------------------------------------------------------
	
	/**
	 * Get a copy of the entityMatches.
	 * 
	 * @return Copy of entityMatches
	 */
	public ArrayList<Entity> getEntityMatches() {
		
		ArrayList<Entity> copy = new ArrayList<Entity>(entityMatches);
		return copy;
	}
	
	/**
	 * Get a copy of the entityMatchCountMap.
	 * 
	 * @return Copy of entityMatchCountMap
	 */
	public HashMap<String, Integer> getEntityMatchCountMap() {
		
		HashMap<String, Integer> copy = 
			new HashMap<String, Integer>(entityMatchCountMap);
		return copy;
	}
	
	/**
	 * Get a copy of the floorEntityMatches.
	 * 
	 * @return Copy of floorEntityMatches
	 */
	public ArrayList<Entity> getFloorEntityMatches() {
		
		ArrayList<Entity> copy = new ArrayList<Entity>(floorEntityMatches);
		return copy;
	}
	
	/**
	 * Get a copy of the floorMatchCountMap.
	 * 
	 * @return Copy of floorMatchCountMap
	 */
	public HashMap<String, Integer> getFloorMatchCountMap() {
		
		HashMap<String, Integer> copy = 
			new HashMap<String, Integer>(floorMatchCountMap);
		return copy;
	}
	
	/**
	 * Get a copy of the wallEntityMatches.
	 * 
	 * @return Copy of wallEntityMatches
	 */
	public ArrayList<Entity> getWallEntityMatches() {
		
		ArrayList<Entity> copy = new ArrayList<Entity>(wallEntityMatches);
		return copy;
	}
	
	/**
	 * Get a copy of the wallMatchCountMap.
	 * 
	 * @return Copy of wallMatchCountMap
	 */
	public HashMap<String, Integer> getWallMatchCountMap() {
		
		HashMap<String, Integer> copy = 
			new HashMap<String, Integer>(wallMatchCountMap);
		return copy;
	}
	
	/**
	 * Get a copy of the entityZoneMatches.
	 * 
	 * @return Copy of entityZoneMatches
	 */
	public ArrayList<Entity> getEntityZoneMatches() {
		
		ArrayList<Entity> copy = new ArrayList<Entity>(entityZoneMatches);
		return copy;
	}
	
	/**
	 * Get a copy of the entityZoneMatchCountMap. These are the zones that
	 * belong to products.
	 * 
	 * @return Copy of entityZoneMatchCountMap
	 */
	public HashMap<String, Integer> getEntityZoneMatchCountMap() {
		
		HashMap<String, Integer> copy = 
			new HashMap<String, Integer>(entityZoneMatchCountMap);
		return copy;
	}
	
	/**
	 * Get a copy of the zoneMatches.
	 * 
	 * @return Copy of zoneMatches
	 */
	public ArrayList<Entity> getZoneMatches() {
		
		ArrayList<Entity> copy = new ArrayList<Entity>(zoneMatches);
		return copy;
	}
	
	/**
	 * Get a copy of the zoneMatchCountMap. These are the generic
	 * zone entities.
	 * 
	 * @return Copy of zoneMatchCountMap
	 */
	public HashMap<String, Integer> getZoneMatchCountMap() {
		
		HashMap<String, Integer> copy = 
			new HashMap<String, Integer>(zoneMatchCountMap);
		return copy;
	}
	
	/**
	 * Get a copy the replaceEntityMatches.
	 * 
	 * @return Copy of replaceEntityMatches
	 */
	public ArrayList<Entity> getReplaceEntityMatches() {
		
		ArrayList<Entity> copy = new ArrayList<Entity>(replaceEntityMatches);
		return copy;
	}
	
	/**
	 * Get a copy the illegalEntities
	 * 
	 * @return Copy of illegalEntities
	 */
	public ArrayList<Entity> getIllegalEntities() {
		
		ArrayList<Entity> copy = new ArrayList<Entity>(illegalEntities);
		return copy;
	}
	
	//-------------------------------------------------------------------------
	// Private Methods
	//-------------------------------------------------------------------------
	
	/**
	 * Print out the entity match results for the match set passed in.
	 * 
	 * @param matches Match set to print out
	 */
	private void printMatchArrayResults(ArrayList<Entity> matches) {
		
		Entity tmpEntity;
		
		for (int i = 0; i < matches.size(); i++) {
			tmpEntity = matches.get(i);
			System.out.println(
					"   "+(i+1)+") entity: "+tmpEntity.getName()+
					" [entityID: "+tmpEntity.getEntityID()+"]");
		}
	}
	
	/**
	 * Print out the count results of the classification count map passed in.
	 * 
	 * @param countResults Classification count map to print out
	 */
	private void printMatchCountMapResults(
			HashMap<String, Integer> countResults) {
		
		System.out.println(">> Classifications and counts <<");
		
		Iterator iterator = countResults.entrySet().iterator();
		Map.Entry<String, Integer> mapEntity;
		
		while (iterator.hasNext()) {
			
			mapEntity = (Entry<String, Integer>) iterator.next();
			System.out.println(
					">> Classification: "+mapEntity.getKey()+" Count: "+
					mapEntity.getValue());
		}
	}
	
	/**
	 * Add an entity to the correct match list and increment the 
	 * classRelationship in the correct match count map.
	 *
	 * @param entity Entity to store
	 * @param classRelationship Classification name to record
	 * @param matchList Match list to add entity to
	 * @param matchCountMap Match count map to add classification relationship
	 * to
	 */
	private void addMatch(
			Entity entity,
			String classRelationship, 
			ArrayList<Entity> matchList, 
			HashMap<String, Integer> matchCountMap){
		
		matchList.add(entity);
		
		Integer count = matchCountMap.get(classRelationship);
		
		if(count == null){
			matchCountMap.put(classRelationship, 1);
		} else {
			count++;
			matchCountMap.put(classRelationship, count);
		}
	}

}
