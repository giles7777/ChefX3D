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

//External imports
import java.util.*;
import java.util.prefs.Preferences;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import org.j3d.device.input.TrackerState;
import org.j3d.util.I18nManager;

//Internal imports
import org.chefx3d.model.*;

import org.chefx3d.preferences.PersistentPreferenceConstants;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;

import org.chefx3d.tool.*;

import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.ErrorReporter;

import org.chefx3d.view.common.EditorGrid;

/**
 * Responds to add SegmentableEntity vertex events. The appropriate command
 * is issued for the event.
 *
 * @author Ben Yarger
 * @version $Revision: 1.21 $
 */
class AddVertexEntityResponse implements TrackerEventResponse, AV3DConstants {
	
	private static final String SPLITSEGMENT_IGNORE_WALL_INTERSECTION = 
		"emerson.closetmaid.rules.definitions.AddWallCheckForIntersectionRule";
	
	private static final String SPLITSEGMENT_ADD_ANGLE_SNAP = 
		"emerson.closetmaid.rules.definitions.AddVertexAngleRestrictedRule";
	
	/** The base name for internationalisation text pertinent to this class */
	private static final String I18N_BASE =
		"org.chefx3d.view.awt.av3d.AddVertexEntityResponse.";
	
	/** Vector of the x axis */
	private static final Vector3d X_AXIS = new Vector3d(1, 0, 0);
	
	/** Reference to world model */
	private WorldModel model;
	
	/** The controlleer to send commands to */
	private CommandController controller;
	
	/** Reference to error reporter */
	private ErrorReporter reporter;
	
	/** Reference to entity builder */
	private EntityBuilder entityBuilder;
	
	/** The current active tool */
	private SegmentableTool segmentableTool;
	
	private SegmentEntity selectedSegmentEntity;
	private SegmentEntity splittingSegment;
	
	/** The internationalization manager used to get resources */
	private I18nManager intlMgr;
	
	/** Utility for aligning the model with the editor grid */
	private EditorGrid editorGrid;
	
	/** Scratch arrays */
	private double[] fixed_position;
	private Vector3d vec;
	private Vector3d cp;
	
	/** Wall angle snap value */
	private double angleSnap;
	
	/**
	 * Constructor 
	 *
	 * @param model
	 * @param controller
	 * @param reporter
	 * @param editorGrid
	 */
	public AddVertexEntityResponse(
		WorldModel model, 
		CommandController controller,
		ErrorReporter reporter,
		EditorGrid editorGrid) {
		
		this.model = model;
		this.controller = controller;
		this.reporter = reporter;
		this.editorGrid = editorGrid;
		
		segmentableTool = null;
		splittingSegment = null;
		entityBuilder = DefaultEntityBuilder.getEntityBuilder();
		
		// setup the internationalization manager
		intlMgr = I18nManager.getManager();
		
		fixed_position = new double[3];
		vec = new Vector3d();
		cp = new Vector3d();
		
		String appName = (String) ApplicationParams.get(ApplicationParams.APP_NAME);
		Preferences prefs = Preferences.userRoot().node(appName);
		
		double angleVal = prefs.getDouble(
			PersistentPreferenceConstants.WALL_ANGLE_SNAP, 
			PersistentPreferenceConstants.DEFAULT_WALL_ANGLE_SNAP);
		
		angleSnap = Math.abs(Math.toRadians(angleVal));
	}
	
	/**
	 * Begins the processing required to generate a command in response
	 * to the input received.
	 *
	 * @param trackerID The id of the tracker calling the original handler
	 * @param trackerState The event that started this whole thing off
	 * @param entities The array of entities to handle
	 * @param tool The tool that is used in the action (can be null)
	 */
	public void doEventResponse(
		int trackerID,
		TrackerState trackerState,
		Entity[] entities,
		Tool tool) {
		
		if (tool instanceof SegmentableTool) {
			
			segmentableTool = (SegmentableTool)tool;
			
			if (entities != null) {
				
				int num_entities = entities.length;
				if ((num_entities >= 1) && (entities[0] instanceof SegmentableEntity)){
					
					SegmentableEntity segmentableEntity = (SegmentableEntity)entities[0];
					
					double[] s_position = new double[3];
					double[] position = new double[3];
					
					// Assumes a top down view when placing wall vertices, therefore
					// each y value should always be the maximum wall height.
					float height = (float)ChefX3DRuleProperties.MAXIMUM_WALL_HEIGHT;
					
					Object vtx_height_prop = segmentableTool.getVertexTool().getProperty(
						Entity.EDITABLE_PROPERTIES,
						VertexEntity.HEIGHT_PROP);  
					
					if ((vtx_height_prop != null) && (vtx_height_prop instanceof Float)) {
						height = ((Float)vtx_height_prop).floatValue();
					}
					
					if ((num_entities == 2) && (entities[1] instanceof SegmentEntity)) {
						
						ArrayList<Command> commandList = new ArrayList<Command>();
						
						SegmentEntity segment = (SegmentEntity)entities[1];
						VertexEntity startVertex = segment.getStartVertexEntity();
						
						boolean isFirstSegment = AV3DUtils.isShadow(startVertex);
						if (isFirstSegment) {
							// for the first segment, the start vertex must be created
							startVertex.getPosition(s_position);
							s_position[2] = height;
							startVertex = addVertex(
								segmentableEntity, 
								s_position, 
								false, 
								commandList);
							
						} else {
							// otherwise, the start vertex is 'live', unselect it
							// since the end vertex will become selected
							Command cmd = new SelectEntityCommand(
								model, 
								startVertex, 
								false);
							cmd.setErrorReporter(reporter);
							commandList.add(cmd);
						}
						
						// create the end vertex and segment
						position[0] = trackerState.worldPos[0];
						position[1] = trackerState.worldPos[1];
						position[2] = height;
						
						// if this is the first segment, restrict it's
						// angle with the x axis to be an increment of
						// the snap angle
						if (isFirstSegment) {
							adjustAngle(s_position, position);
						}
						addVertexAndSegment(
							segmentableEntity,
							position,
							-1,
							startVertex, 
							commandList);
						
						String commandInformation = intlMgr.getString(
							I18N_BASE + "addSegment");
						
						// Add the commands to the MultiCommand and send it
						MultiCommand multi = new MultiCommand(
							commandList,
							commandInformation);
						multi.setErrorReporter(reporter);
						controller.execute(multi);
						
					} else if ((num_entities == 3) && 
						(entities[1] instanceof VertexEntity) &&
						(entities[2] instanceof VertexEntity)) {

						ArrayList<Command> commandList = new ArrayList<Command>();
						
						VertexEntity startVertex = (VertexEntity)entities[1];
						VertexEntity endVertex = (VertexEntity)entities[2];
						
						// rem: should the vertices be checked for shadow status?
						insertSegment(
							segmentableEntity,
							startVertex,
							endVertex,
							commandList);
						
						if (commandList.size() > 0) {
							String commandInformation = intlMgr.getString(
								I18N_BASE + "addSegment");
							
							// Add the commands to the MultiCommand and send it
							MultiCommand multi = new MultiCommand(
								commandList,
								commandInformation);
							multi.setErrorReporter(reporter);
							controller.execute(multi);
						}
						
					} else if (num_entities == 1) {
						// rem: the remainder of this is 'Jon Hubba' code,
						// what it does, is a mystery.....
						
						position[0] = trackerState.worldPos[0];
						position[1] = trackerState.worldPos[1];
						position[2] = height;
						
						VertexEntity selectedVertexEntity = null;
						
						EntitySelectionHelper selectionHelper = 
							EntitySelectionHelper.getEntitySelectionHelper();
						
						ArrayList<Entity> selectedList = selectionHelper.getSelectedList();
						
						int len = selectedList.size();
						for (int i = 0; i < len; i++) {
							
							Entity check = selectedList.get(i);               
							if (check instanceof VertexEntity) {
								
								// use the first selected vertex, if not a shadow
								VertexEntity ve = (VertexEntity)check;
								boolean isShadow = AV3DUtils.isShadow(ve);
								if (!isShadow) {
									selectedVertexEntity = ve;
									break;
								}
							} else if (check instanceof SegmentEntity) {
								
								// use the end vertex of the segment
								selectedSegmentEntity = (SegmentEntity)check;
								selectedVertexEntity = selectedSegmentEntity.getEndVertexEntity();
								break;                   
							}
						}
						
						if (splittingSegment != null) {
							selectedVertexEntity = null;
							selectedSegmentEntity = splittingSegment;
						}
						
						if ((selectedVertexEntity == null) && (selectedSegmentEntity == null)) {
							
							ArrayList<VertexEntity> vtx_list = segmentableEntity.getVertices();
							if (vtx_list != null) {
								int num = vtx_list.size();
								for (int i = num - 1; i >= 0; i--) {
									VertexEntity ve = vtx_list.get(i);
									if (!AV3DUtils.isShadow(ve)) {
										selectedVertexEntity = ve;
										break;
									}
								}
							}
						}
						
						/*
						 * Processed based on if a vertex is selected or a segment.
						 * If a vertex is selected, add the new vertex where the user has
						 * selected. If a segment is selected, split the segment with a new
						 * vertex where the user selected.
						 */
						
						if (selectedVertexEntity != null) {
							/*
							 * Check if the SegmentableEntity is a line or not. A line is a
							 * contiguous set of points with a distinct start and end. If the
							 * SegmentableEntity is not a line, it is a network of lines
							 * which includes branching and the ability of the network to
							 * loop back on itself.
							 */
							
							ArrayList<Command> commandList = new ArrayList<Command>();
							
							if (segmentableEntity.isLine()) {
								
								// force the end vertex to be selected
								VertexEntity startVertex = 
									segmentableEntity.getVertex(segmentableEntity.getEndVertexID());
								
								addVertexAndSegment(
									segmentableEntity,
									position,
									-1,
									startVertex,
									commandList);
								
							} else {
								
								//if (mergingVertex == null) {
									
									addVertexAndSegment(
										segmentableEntity,
										position,
										-1,
										selectedVertexEntity,
										commandList);
								//} else {
								//	
								//	mergeSegment(segmentableEntity, selectedVertexEntity, mergingVertex);
								//	mergingVertex = null;
								//}
								
							}
							if (commandList.size() > 0) {
								
								String commandInformation = intlMgr.getString(
									I18N_BASE + "addVertexAndSegment");
								
								// Add the commands to the MultiCommand and send it
								MultiCommand multi = new MultiCommand(
									commandList,
									commandInformation);
								multi.setErrorReporter(reporter);
								controller.execute(multi);
							}
							
						} else if (selectedSegmentEntity != null) {
							
							splitSegment(segmentableEntity, position);
							
						} else {
							
							ArrayList<Command> commandList = new ArrayList<Command>();
							// otherwise it is the first vertex
							addVertex(
								segmentableEntity, 
								position, 
								true, 
								commandList);
							
							String commandInformation = intlMgr.getString(
								I18N_BASE + "addVertex");
							
							// Add the commands to the MultiCommand and send it
							MultiCommand multi = new MultiCommand(
								commandList,
								commandInformation);
							multi.setErrorReporter(reporter);
							controller.execute(multi);
						}
					}	
				}
			}
		}
	}
	
	//--------------------------------------------------------------------
	// Local methods
	//--------------------------------------------------------------------
	
	/**
	 * Add a vertex at the specified position.
	 *
	 * @param parent The entity to add the vertex to
	 * @param position The position for the requested vertex
	 * @param select Flag indicating that the vertex should be selected
	 * @param commandList The list to append any generated commands to
	 */
	private VertexEntity addVertex(
		SegmentableEntity parent,
		double[] position,
		boolean select,
		ArrayList<Command> commandList) {
		
		VertexTool vertexTool = segmentableTool.getVertexTool();
		
		editorGrid.alignPositionToGrid(position);
		
		int endVertexID = model.issueEntityID();
		VertexEntity newVertexEntity = (VertexEntity)entityBuilder.createEntity(
			model,
			endVertexID, 
			position, 
			new float[] { 0, 1, 0, 0 },
			vertexTool);
		
		AddVertexCommand vertexCmd = new AddVertexCommand(
			parent,
			newVertexEntity, 
			-1);
		
		vertexCmd.setErrorReporter(reporter);
		commandList.add(vertexCmd);
		
		if (select) {
			SelectEntityCommand selectCmd = new SelectEntityCommand(
				model, 
				newVertexEntity, 
				true);
			selectCmd.setErrorReporter(reporter);
			commandList.add(selectCmd);
		}
		
		return(newVertexEntity);
	}
	
	/**
	 * Add a vertex at the location.  Then place a segment between
	 * the selected vertex and the newly created one.
	 *
	 * @param entity The entity to add the vertex to
	 * @param position The position for the requested vertex
	 * @param append -1 if this should appended to the end
	 * @param startVertexID The start of the segment
	 */
	private void addVertexAndSegment(
		SegmentableEntity segmentableEntity,
		double[] position,
		int index,
		VertexEntity startVertex,
		ArrayList<Command> commandList) {
		
		// Before adding check to make sure it doesn't exist
		if (segmentableEntity.contains(position)) {
			// rem: is this really the right thing to do?
			return;
		}
		
		// create the vertex command
		int endVertexID = model.issueEntityID();
		
		VertexTool vertexTool = segmentableTool.getVertexTool();
		
		startVertex.getStartingPosition(fixed_position);
		editorGrid.alignVectorToGridSpacing(position, fixed_position);
		
		VertexEntity newVertexEntity = (VertexEntity)entityBuilder.createEntity(
			model,
			endVertexID,
			position,
			new float[] {0,1,0,0},
			vertexTool);
		
		AddVertexCommand vertexCmd = new AddVertexCommand(
			(SegmentableEntity)segmentableEntity,
			newVertexEntity,
			index);
		
		vertexCmd.setErrorReporter(reporter);
		commandList.add(vertexCmd);
		
		// create the segment command
		int currentSegmentID = model.issueEntityID();
		
		SegmentTool segmentTool = (SegmentTool)segmentableTool.getSegmentTool();
		
		SegmentEntity newSegmentEntity = (SegmentEntity)entityBuilder.createEntity(
			model,
			currentSegmentID,
			new double[] {0,0,0},
			new float[] {0,1,0,0},
			segmentTool);
		
		newSegmentEntity.setStartVertex(startVertex);
		newSegmentEntity.setEndVertex(newVertexEntity);
		
		AddSegmentCommand segmentCmd = new AddSegmentCommand(
			model,
			segmentableEntity,
			newSegmentEntity);
		segmentCmd.setErrorReporter(reporter);
		commandList.add(segmentCmd);
		
		// Unselect the vertex entity
		if (startVertex.isSelected()) {
			SelectEntityCommand unselectVertexCmd = new SelectEntityCommand(
				model, 
				startVertex, 
				false);
			commandList.add(unselectVertexCmd);
		}
		if (selectedSegmentEntity != null ) {
			SelectEntityCommand unselectSegementCmd = new SelectEntityCommand(
				model, 
				selectedSegmentEntity, 
				false);
			commandList.add(unselectSegementCmd);
		}
		
		// Select the vertex entity
		SelectEntityCommand selectCmd = new SelectEntityCommand(
			model, 
			newVertexEntity, 
			true);
		commandList.add(selectCmd);
	}
	
	/**
	 * Split the current line in two parts, maintaining the
	 * current line vector.
	 * 
	 * @param segmentableEntity The container entity
	 * @param position The clicked position, this will be translated
	 * into a point along the line.
	 */
	private void splitSegment(
		SegmentableEntity segmentableEntity, 
		double position[] ) {
		
		// set up the ignore rules list
		HashSet<String> ignoreSegmentRuleList = new HashSet<String>();
		ignoreSegmentRuleList.add(SPLITSEGMENT_IGNORE_WALL_INTERSECTION);
		
		HashSet<String> ignoreVertexRuleList = new HashSet<String>();
		ignoreVertexRuleList.add(SPLITSEGMENT_ADD_ANGLE_SNAP);
		
		// The list to place commands
		ArrayList<Command> commandList = new ArrayList<Command>();
		
		int startVertexID = selectedSegmentEntity.getStartVertexEntity().getEntityID();
		int endVertexID = selectedSegmentEntity.getEndVertexEntity().getEntityID();
		
		int vertexOrder = segmentableEntity.getChildIndex(startVertexID);
		
		// if this is the first segment then we need to
		// place the new after the first vertex
		int firstVertexID = segmentableEntity.getStartVertexID();
		int lastVertexID = segmentableEntity.getEndVertexID();
		
		if (firstVertexID == startVertexID || lastVertexID == endVertexID) {
			vertexOrder++;
		} 
		
		Object thickness = selectedSegmentEntity.getProperty(
			Entity.EDITABLE_PROPERTIES, 
			SegmentEntity.WALL_THICKNESS_PROP);
		
		Object wallFacing = selectedSegmentEntity.getProperty(
			Entity.EDITABLE_PROPERTIES, 
			SegmentEntity.STANDARD_FACING_PROP);
		
		// add the remove segment command
		RemoveSegmentCommand segmentCmd = new RemoveSegmentCommand(
			model,
			segmentableEntity,
			selectedSegmentEntity.getEntityID());
		
		segmentCmd.setErrorReporter(reporter);
		commandList.add(segmentCmd);
		
		/*
		 * Author Jonathon Hubbard
		 *
		 * Formula used to place the vertex correctly on the line.
		 * First retrieve the index of the two vertices of the line
		 * then retrieve the positions.
		 */
		double[] startPos = new double[3];
		selectedSegmentEntity.getStartVertexEntity().getPosition(startPos);
		
		double[] endPos = new double[3];
		selectedSegmentEntity.getEndVertexEntity().getPosition(endPos);
		
		/*
		 * The full formula for this process is:
		 * tempPoint - startPoint - u(endPoint - startPoint)] dot (endPoint - startPoint)
		 * So first we Solve for Delta X, Delta y and Delta z then we solve for u.
		 */
		double posMinusStartX = position[0] - startPos[0];
		double posMinusStartZ = position[1] - startPos[1];
		double xDelta = endPos[0] - startPos[0];
		double zDelta = endPos[1] - startPos[1];
		
		double u = (posMinusStartX * xDelta + posMinusStartZ * zDelta) /
			(xDelta * xDelta + zDelta * zDelta);
		
		/*
		 * Finally we take the equation of the line and substitute
		 * u*delta or u(enPoint - startPoint)
		 */
		position[0] = startPos[0] + u * xDelta;
		position[1] = startPos[1] + u * zDelta;
		
		editorGrid.alignVectorToGridSpacing(position, startPos);
		
		// create and add the vertex
		VertexTool vertexTool = segmentableTool.getVertexTool();
		
		int middleVertexID = model.issueEntityID();
		VertexEntity newVertexEntity = (VertexEntity)entityBuilder.createEntity(
			model,
			middleVertexID,
			position,
			new float[] {0,1,0,0},
			vertexTool);
		
		AddVertexCommand vertexCmd = new AddVertexCommand(
			(SegmentableEntity)segmentableEntity,
			newVertexEntity,
			vertexOrder);
		vertexCmd.setIgnoreRuleList(ignoreVertexRuleList);
		
		vertexCmd.setErrorReporter(reporter);
		commandList.add(vertexCmd);
		
		VertexEntity startVertex = selectedSegmentEntity.getStartVertexEntity();
		VertexEntity endVertex = selectedSegmentEntity.getEndVertexEntity();
		
		if(!(Boolean)wallFacing) {
			Entity temp = startVertex;
			startVertex = endVertex;
			endVertex = (VertexEntity)temp;
		}
		
		// create the segment command
		SegmentTool segmentTool = (SegmentTool)segmentableTool.getSegmentTool();
		
		segmentTool.setProperty( 
			Entity.EDITABLE_PROPERTIES,
			SegmentEntity.STANDARD_FACING_PROP,
			wallFacing);
		
		segmentTool.setProperty( 
			Entity.EDITABLE_PROPERTIES,
			SegmentEntity.WALL_THICKNESS_PROP,
			thickness);
		
		int currentSegmentID = model.issueEntityID();
		SegmentEntity newSegmentEntity = (SegmentEntity)entityBuilder.createEntity(
			model,
			currentSegmentID,
			new double[] {0,0,0},
			new float[] {0,1,0,0},
			segmentTool);
		
		newSegmentEntity.setStartVertex(startVertex);
		newSegmentEntity.setEndVertex(newVertexEntity);
		
		AddSegmentCommand segmentCmd1 = new AddSegmentCommand(
			model,
			segmentableEntity,
			newSegmentEntity);
		segmentCmd1.setErrorReporter(reporter);
		
		segmentCmd1.setIgnoreRuleList(ignoreSegmentRuleList);
		commandList.add(segmentCmd1);
		
		// create the segment command
		currentSegmentID = model.issueEntityID();
		
		newSegmentEntity = (SegmentEntity)entityBuilder.createEntity(
			model,
			currentSegmentID,
			new double[] {0,0,0},
			new float[] {0,1,0,0},
			segmentTool);
		newSegmentEntity.setStartVertex(newVertexEntity);
		newSegmentEntity.setEndVertex(endVertex);
		
		segmentCmd1 = new AddSegmentCommand(
			model,
			segmentableEntity,
			newSegmentEntity);
		segmentCmd1.setErrorReporter(reporter);
		segmentCmd1.setIgnoreRuleList(ignoreSegmentRuleList);
		commandList.add(segmentCmd1);
		
		// Update selection
		SelectEntityCommand cmdSelect = new SelectEntityCommand(
			model, 
			newVertexEntity, 
			true);
		commandList.add(cmdSelect);
		
		String commandInformation = intlMgr.getString(I18N_BASE + "addSplitSegment");
		
		// Create and apply the multi-command stack
		MultiCommand multi = new MultiCommand(
			commandList,
			commandInformation + middleVertexID);
		multi.setErrorReporter(reporter);
		controller.execute(multi);
		
		splittingSegment = null;
		selectedSegmentEntity = null;
	}
	
	/**
	 * Sets the correct entity builder to use
	 * @param builder
	 */
	public void setEntityBuilder(EntityBuilder builder){
		entityBuilder = builder;
	}
	
	/**
	 * Sets the correct entity builder to use
	 * @param builder
	 */
	public void setSplittingSegment(SegmentEntity splitSegment){
		splittingSegment = splitSegment;
	}
	
	/**
	 * Insert a segment between two existing vertices
	 * 
	 * @param segmentableEntity
	 * @param startVertex
	 * @param endVertex
	 * @param commandList
	 */
	private void insertSegment(
		SegmentableEntity segmentableEntity,
		VertexEntity startVertex, 
		VertexEntity endVertex,
		ArrayList<Command> commandList) {
		
		if (startVertex != endVertex) {
			
			int num_s = 0;
			ArrayList<SegmentEntity> s_segment_list = segmentableEntity.getSegments(startVertex);
			if (s_segment_list != null) {
				pruneShadows(s_segment_list);
				num_s = s_segment_list.size();
			}
			int num_e = 0;
			ArrayList<SegmentEntity> e_segment_list = null;
			if (num_s == 1) {
				e_segment_list = segmentableEntity.getSegments(endVertex);
				if (e_segment_list != null) {
					pruneShadows(e_segment_list);
					num_e = e_segment_list.size();
				}
			}
			VertexEntity s_vertex = startVertex;
			VertexEntity e_vertex = endVertex;
			// if possible, fit the segment so that it's facing matches
			// the adjoining segments
			if (num_e == 1) {
				SegmentEntity s_segment = s_segment_list.get(0);
				VertexEntity s_vtx_0 = s_segment.getStartVertexEntity();
				VertexEntity s_vtx_1 = s_segment.getEndVertexEntity();
				
				SegmentEntity e_segment = e_segment_list.get(0);
				VertexEntity e_vtx_0 = e_segment.getStartVertexEntity();
				VertexEntity e_vtx_1 = e_segment.getEndVertexEntity();
				
				if (((s_vtx_0 == startVertex) && (e_vtx_1 == endVertex)) ||
					((s_vtx_1 == endVertex) && (e_vtx_0 == startVertex))) {
					
					e_vertex = startVertex;
					s_vertex = endVertex;
				}
			}
			
			SegmentTool segmentTool = (SegmentTool)segmentableTool.getSegmentTool();
			SegmentEntity newSegmentEntity = (SegmentEntity)entityBuilder.createEntity(
				model,
				model.issueEntityID(),
				new double[] {0,0,0},
				new float[] {0,1,0,0},
				segmentTool);
			newSegmentEntity.setStartVertex(s_vertex);
			newSegmentEntity.setEndVertex(e_vertex);
			
			AddSegmentCommand segmentCmd = new AddSegmentCommand(
				model,
				segmentableEntity,
				newSegmentEntity);
			segmentCmd.setErrorReporter(reporter);
			commandList.add(segmentCmd);
		}
		// Select the vertex entity
		SelectEntityCommand selectCmd = new SelectEntityCommand(
			model, 
			endVertex, 
			true);
		commandList.add(selectCmd);
	}
	
	/**
	 * Remove any shadows from the argument list
	 *
	 * @param segment_list The list to prune of shadows
	 */
	private void pruneShadows(ArrayList<SegmentEntity> segment_list) {
		int num_segment = segment_list.size();
		// purge the segment list of shadows
		for (int i = num_segment - 1; i >= 0; i--) {
			SegmentEntity se = segment_list.get(i);
			if (AV3DUtils.isShadow(se)) {
				segment_list.remove(i);
			}
		}
	}
	
	/**
	 * Adjust the end point as necessary to configure the
	 * snap angle of the segment relative to the x axis.
	 *
	 * @param s The start point of the segment
	 * @param e The requested end point of the segment.
	 */
	private void adjustAngle(double[] s, double[] e) {
		
		double delta_x = e[0] - s[0];
		double delta_y = e[1] - s[1];
		if ((delta_x != 0) | (delta_y != 0)) {
			vec.set(delta_x, delta_y, 0);
			cp.cross(X_AXIS, vec);
			int sign = 1;
			if (cp.z < 0) {
				sign = -1;
			}
			double angle = X_AXIS.angle(vec);
			int inc = (int)(angle / angleSnap);
			double rem = angle % angleSnap;
			if (rem > (angleSnap * 0.5)) {
				inc++;
			}
			angle = sign * inc * angleSnap;
			double mag = vec.length();
			e[0] = s[0] + mag * Math.cos(angle);
			e[1] = s[1] + mag * Math.sin(angle);
		}
	}
}
