/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2010
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

// External Imports
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Rectangle;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JProgressBar;

import javax.vecmath.Matrix4f;

import org.chefx3d.model.*;

import org.chefx3d.util.ApplicationParams;

import org.chefx3d.view.awt.scenemanager.PerFrameObserver;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;

/**
 * A blocking dialog that accepts a SceneManagerObserver, a list of EntityIDs,
 * and a CommandController in order to execute the following sequence of steps:
 *
 * First:  A ThumbnailProcessor is created, and added to the scene, via the
 *         SceneManagerObserver
 *
 * Second: This class will iterate through the EntityIDs, and select them one at
 *         a time, and take a thumbnail while each is selected.
 *
 * Third:  The captured thumbnails are returned.
 *
 * @author christopher
 * @version $Revision: 1.15 $
 */
public class PreviewThumbnailCreator implements
	PerFrameObserver,
	ActionListener,
	EntitySelectionListener, 
	Runnable {
	
	/**
	 * Text for the capturing button.
	 * TODO:  Internationalise
	 */
	private static final String BUTTON_TEXT = "Capture thumbnail index: ";
	
	/** Number of frames to wait before issuing capture call */
	private static final int CAPTURE_FRAME_DELAY = 20;
	
	/** The dialog UI component */
	private JDialog dialog;
	
	/** The list of thumbnails generated */
	private ArrayList<BufferedImage> thumbnails;
	
	/** The Preview instance */
	private final Preview preview;
	
	/** The PreviewLayerManager instance */
	private PreviewLayerManager plm;
	
	/** The PreviewLegendLayerManager instance */
	private PreviewLegendLayerManager legend;
	
	/** Visibility handler, for imaging zones */
	private LocationVisibilityHandler vis_orthographic;
	
	/** Visibility handler, for imaging perspective views */
	private PreviewVisibilityHandler vis_perspective;
	
	/** Zone viewpoint calculator */
	private ZoneView zoneView;
	
	/** The scene manager Observer*/
	private SceneManagerObserver mgmtObserver;
	
	/** The location that is being imaged */
	private LocationEntity location;
	
	/** An orthographic viewpoint entity for zone imaging */
	private ViewpointEntity ortho;
	
	/** List of entities that need to be selected before imaging */
	private Entity[] entities;
	
	/** CommandController to do the selecting */
	private CommandController controller;
	
	/** The world model, to look up entities */
	private WorldModel model;
	
	/** Index into the entities array, to know which one to select
	*  and then thumbnail next. */
	private int index;
	
	/** The button that allows for UI driven capturing, to provide a real
	*  awesome interactive look into the thumbnail capturing. */
	private JButton captureButton;
	
	/** Progress bar to indicate how much more work is to be done */
	private JProgressBar captureProgress;
	
	/** Determine which mode we're in, because the UI elements are different */
	private boolean debugMode = false;
	
	/** Sidepocketed ground normal for the location */
	private float[] groundNormal;
	
	/** Frame count from selection event to screen capture */
	private int frame_delay;
	
	/** Synchronization variable indicating that the capture process is running */
	private boolean captureInProgress;
	
	/** Flag indicating that a change of location has occurred. */
	private boolean locationSelected;
	
	/** Synchronization variable indicating that the dialog has been initialized */
	private boolean dialogInitialized;
	
	/**
	 * Constructor
	 *
	 * @param model The WorldModel
	 * @param preview The Preview UI component
	 */
	public PreviewThumbnailCreator(WorldModel model, Preview p) {
		
		this.preview = p;
		mgmtObserver = preview.getSceneManagerObserver();
		controller = mgmtObserver.getCommandController();
		this.model = model;
		
		thumbnails = new ArrayList<BufferedImage>();
		
		preview.enablePerformanceMonitor(false);
		
		plm = (PreviewLayerManager)preview.getLayerManager();
		plm.setNavigationEnabled(false);
		
		legend = (PreviewLegendLayerManager)preview.getLegendLayerManager();
		
		vis_orthographic = new LocationVisibilityHandler(mgmtObserver);
		
		vis_perspective = plm.getVisibilityHandler();
		
		EventQueue.invokeLater(this);
	}
	
	//---------------------------------------------------------------
	// Methods defined by Runnable
	//---------------------------------------------------------------
	
	/**
	 * Initialize the UI
	 */
	public void run() {
		
		dialog = new JDialog((Frame)null, false);
		dialog.setAlwaysOnTop(true);
		
		// in case of a 'pre-mature' close, cleanup
		dialog.addWindowListener(
			new WindowAdapter() {
				public void windowClosing(WindowEvent we) {
					shutdown();
				}
			}
		);
		
		Dimension thumbnailSize = new Dimension(
			AV3DConstants.THUMBNAIL_WIDTH,
			AV3DConstants.THUMBNAIL_HEIGHT);
		preview.setPreferredSize(thumbnailSize);
		
		// layout the components
		dialog.add(preview, BorderLayout.CENTER);
		
		debugMode = (Boolean)ApplicationParams.get(ApplicationParams.DEBUG_MODE);
		if (debugMode) {
			captureButton = new JButton(BUTTON_TEXT + "0");
			captureButton.addActionListener(this);
			dialog.add(captureButton, BorderLayout.SOUTH);
		} else {
			captureProgress = new JProgressBar(JProgressBar.HORIZONTAL, 0, 1);
			dialog.add(captureProgress, BorderLayout.SOUTH); 
		}
		
		dialog.pack();
		
		// center the dialog on the screen
		Dimension screenSize = dialog.getToolkit().getScreenSize();
		Dimension dialogSize = dialog.getSize();
		dialog.setLocation(
			((screenSize.width - dialogSize.width)/2),
			((screenSize.height - dialogSize.height)/2));
		
		dialog.setVisible(true);
		
		synchronized(this) {
			dialogInitialized = true;
			notify();
		}
	}
	
	//---------------------------------------------------------------
	// Methods defined by PerFrameObserver
	//---------------------------------------------------------------
	
	/**
	 * A new frame tick is observed, so do some processing now.
	 */
	public void processNextFrame() {
		
		if (frame_delay > 0) {
			
			frame_delay--;
			
		} else if (frame_delay == 0) {
			
			if (locationSelected) {
				queueCapture(index);
			} else {
				processCapture();
				frame_delay = -1;
			}
		}
	}
	
	//-------------------------------------------------------------------------
	// Methods defined by ActionListener
	//-------------------------------------------------------------------------
	
	/**
	 * An action event has been dispatched
	 */
	public void actionPerformed(ActionEvent e) {
		
		Object src = e.getSource();
		if (src == captureButton) {
			// process the capture of the selected entity
			captureButton.setEnabled(false);
			processCapture();
		}
	}
	
	//-------------------------------------------------------------------------
	// Methods defined by EntitySelectionListener
	//-------------------------------------------------------------------------
	
	/**
	 * A selection event has occured
	 */
	public void selectionChanged(int entityID, boolean selection) {
		
		Entity entity = model.getEntity(entityID);
		if (selection) {
			if (entity instanceof LocationEntity) {
				
				locationSelected = true;
				location.removeEntitySelectionListener(this);
				
				if (!debugMode) {
					frame_delay = CAPTURE_FRAME_DELAY;
					mgmtObserver.addObserver(this);
				} else {
					// with the location loaded, queue up the entities
					// to capture
					queueCapture(index);
				}
				
			} else if ((entity instanceof ViewpointEntity) ||
				(entity instanceof ZoneEntity)) {
				
				locationSelected = false;
				
				// the entity to capture has been selected
				if (!debugMode) {
					// the capture will be processed automagically,
					// after the required frame delay
					frame_delay = CAPTURE_FRAME_DELAY;
				}
				if (!(entity == ortho)) {
					entity.removeEntitySelectionListener(this);
				}
			}
		}
	}
	
	/**
	 * A highlighted event has occured
	 */
	public void highlightChanged(int entityID, boolean selection) {
		// Ignored
	}
	
	//-------------------------------------------------------------------------
	// Local Methods - public
	//-------------------------------------------------------------------------
	
	/**
	 * Return the screen capture images
	 * 
	 * @return The screen capture images
	 */
	public BufferedImage[] getThumbnails() {
		if (captureInProgress) {
			try {
				synchronized (this) {
					while (captureInProgress) { 
						wait(); 
					}
				}
			} catch (InterruptedException ie) {
			}
		}
		int num_images = thumbnails.size();
		BufferedImage[] images = new BufferedImage[num_images];
		thumbnails.toArray(images);
		thumbnails.clear();
			
		return(images);
	}
	
	/**
	 * Initialize the entities for screen capture
	 *
	 * @param location The active location entity
	 * @param entities The set of entities to be imaged
	 */
	public void setEntities(
		LocationEntity location,
		Entity[] entities) {
		
		if (!dialogInitialized) {
			try {
				synchronized(this) {
					while(!dialogInitialized) {
						wait();
					}
				}
			} catch (InterruptedException ie) {
			}
		}
		if ((location != null) && (entities != null) && (entities.length > 0)) {
			
			captureInProgress = true;
			thumbnails.clear();
			
			this.location = location;
			this.entities = entities;
			
			int num_images = entities.length;
			
			index = 0;
			frame_delay = -1;
			
			if (!debugMode && dialogInitialized) {
				captureProgress.setMaximum(entities.length);
				captureProgress.setValue(index);
			}
			
			groundNormal = location.getGroundNormal();
			
			AV3DEntityManager mngr = plm.getAV3DEntityManager(location);
			legend.setEntityManager(mngr);
			vis_orthographic.setEntityManager(mngr);
			
			zoneView = new ZoneView(
				plm.getViewEnvironment(),
				model,
				controller,
				null);
			zoneView.setLocationEntity(location);
			zoneView.setEntityManager(mngr);
		
			// ensure that the location is active
			location.addEntitySelectionListener(this);
			ArrayList<Command> cmdList = new ArrayList<Command>();
			Command cmd = null;
			if (location.isSelected()) {
				cmd = new SelectEntityCommand(model, location, false);
				cmdList.add(cmd);
				controller.execute(cmd);
			}
			cmd = new SelectEntityCommand(model, location, true);
			cmdList.add(cmd);
			
			MultiCommand multiCommand = new MultiCommand(
				cmdList,
				"Selection Occured",
				false);
			controller.execute(multiCommand);
		}
	}
	
	/**
	 * Dispose of this dialog
	 */
	public void shutdown() {
		
		preview.enablePerformanceMonitor(true);
		plm.setNavigationEnabled(true);
		
		if (captureInProgress) {
			cleanup();
		}
		
		// dispose of the dialog
		EventQueue.invokeLater(
			new Runnable() {
				public void run() {
					dialog.setVisible(false);
					dialog.removeAll();
					dialog.dispose();
				}
			}
		);
	}
	
	//-------------------------------------------------------------------------
	// Local Methods - private
	//-------------------------------------------------------------------------
	
	/**
	 * Generate the commands that will select the entities that 
	 * require snapshots.
	 *
	 * @param index An index into the entities array
	 */
	private void queueCapture(int index) {
		
		// Make sure it's a valid request, otherwise, exit
		if ((index >= 0) && (index < entities.length)) {
			
			Entity entity = entities[index];
			
			Command cmd = null;
			MultiCommand multiCmd = null;
			
			ArrayList<Command> cmdList = new ArrayList<Command>();
			
			// Different commands for entity types
			switch (entity.getType()) {
			case Entity.TYPE_VIEWPOINT:
				
				vis_perspective.setEnabled(true);
				
				entity.addEntitySelectionListener(this);
				
				location.setGroundNormal(groundNormal);
				
				if (entity.isSelected()) {
					cmd = new SelectEntityCommand(model, entity, false);
					cmdList.add(cmd);
				}
				cmd = new SelectEntityCommand(model, entity, true);
				cmdList.add(cmd);
				
				multiCmd = new MultiCommand(cmdList, "Selection Occured", false) ;
				controller.execute(multiCmd);
				
				break;
				
			case Entity.TYPE_GROUNDPLANE_ZONE:
				legend.setActiveZoneEntity((ZoneEntity)entity);
				legend.enableFloorLegend(true);
				break;
				
			case Entity.TYPE_SEGMENT:
				legend.setActiveZoneEntity((ZoneEntity)entity);
				legend.enableSegmentLegend(true);
				if (preview.getRenderingMode() == Preview.RENDER_LINE_ART) {
					legend.enableProductLegend(true);
				}
				break;
				
			case Entity.TYPE_MODEL_ZONE:
				legend.setActiveZoneEntity((ZoneEntity)entity);
				if (preview.getRenderingMode() == Preview.RENDER_LINE_ART) {
					legend.enableProductLegend(true);
				}
				break;
			}
			if (entity.isZone()) {
				
				ZoneEntity ze = (ZoneEntity)entity;
				if (ortho == null) {
					// create a temporary viewpoint for zone imaging
					ortho = createOrthographicViewpoint();
					ViewpointContainerEntity vce =
						location.getViewpointContainerEntity();
					cmd = new AddEntityChildCommand(
						model,
						model.issueTransactionID(),
						vce,
						ortho,
						true);
					cmdList.add(cmd);
					
					ortho.addEntitySelectionListener(this);
				}
				
				vis_perspective.setEnabled(false);
				
				location.setGroundNormal(new float[]{0, 1, 0});
				
				// set the visibility of products by zone
				vis_orthographic.setActiveZoneEntity(ze);
				
				// note: always select the viewpoint before configuring
				// it's parameters
				if (ortho.isSelected()) {
					cmd = new SelectEntityCommand(model, ortho, false);
					cmdList.add(cmd);
				}
				cmd = new SelectEntityCommand(model, ortho, true);
				cmdList.add(cmd);
				
				multiCmd = new MultiCommand(cmdList, "Selection Occured", false) ;
				controller.execute(multiCmd);
				
				// configure the viewpoint parameters
				zoneView.configView(ze, ortho);
			}
			if (dialogInitialized) {
				captureProgress.setMaximum(entities.length);
				captureProgress.setValue(index + 1);
			}
		}
	}
	
	/**
	 * Request to perform the screen capture
	 */
	private void processCapture() {
		
		//////////////////////////////////////////////
		try {
			Point p = preview.getLocationOnScreen();
			BufferedImage imgx = new Robot().createScreenCapture(
				new Rectangle(
				p.x, 
				p.y, 
				preview.getWidth(), 
				preview.getHeight()));
			thumbnails.add(imgx);
		} catch (Exception e) {
			System.out.println("Exception during screen capture: "+ e.getMessage());
		}
		//////////////////////////////////////////////
		
		Entity entity = entities[index];
		switch (entity.getType()) {
		case Entity.TYPE_GROUNDPLANE_ZONE:
			legend.enableFloorLegend(false);
			break;
		case Entity.TYPE_SEGMENT:
			legend.enableSegmentLegend(false);
			legend.enableProductLegend(false);
			break;
		}
		
		index++;
		
		// Make sure there is more to do
		if (index < entities.length) {
			
			if (debugMode) {
				// Re-enable the capture button and update it's text
				captureButton.setEnabled(true);
				captureButton.setText(BUTTON_TEXT + index);
			}
			queueCapture(index);
			
		} else {
			cleanup();
		}
	}
	
	/**
	 * Create and return a viewpoint entity for zone imaging
	 *
	 * @return A ViewpointEntity
	 */
	private ViewpointEntity createOrthographicViewpoint() {
		
		HashMap<String, Map<String, Object>> vpProps =
			new HashMap<String, Map<String, Object>>();
		HashMap<String, Object> vpParams = new HashMap<String, Object>();
		HashMap<String, Object> vpPropSheet = new HashMap<String, Object>();
		
		vpPropSheet.put(
			Entity.NAME_PROP,
			this.getClass().getName());
		
		vpPropSheet.put(
			ViewpointEntity.START_VIEW_MATRIX_PROP,
			new float[16]);
		
		vpPropSheet.put(
			ViewpointEntity.PROJECTION_TYPE_PROP,
			AV3DConstants.ORTHOGRAPHIC);
		
		vpPropSheet.put(
			ViewpointEntity.VIEW_IDENTIFIER_PROP,
			preview.getIdentifier());
		
		vpProps.put(Entity.ENTITY_PARAMS, vpParams);
		vpProps.put(Entity.DEFAULT_ENTITY_PROPERTIES, vpPropSheet);
		
		ViewpointEntity ve = new ViewpointEntity(
			model.issueEntityID(),
			vpProps);
		
		return(ve);
	}
	
	/** Restore normality */
	private void cleanup() {
		
		if (!debugMode) {
			mgmtObserver.removeObserver(this);
		}
		
		// remove the temporary ortho viewpoint
		if (ortho != null) {
			ortho.removeEntitySelectionListener(this);
			
			ViewpointContainerEntity vce =
				location.getViewpointContainerEntity();
			Command cmd = new RemoveEntityChildCommand(model, vce, ortho, false);
			controller.execute(cmd);
			
			ortho = null;
		}
		
		// make sure that the 'extra' scene graph components are removed
		legend.enableFloorLegend(false);
		legend.enableSegmentLegend(false);
		legend.enableProductLegend(false);
			
		// restore viewpoint and navigation
		location.setGroundNormal(groundNormal);
		
		ViewpointContainerEntity vce = location.getViewpointContainerEntity();
		List<ViewpointEntity> viewpoint_list = vce.getViewpoints();
		
		String view_id = preview.getIdentifier();
		for (ViewpointEntity vp : viewpoint_list) {
			String id = (String)vp.getProperty(
				Entity.DEFAULT_ENTITY_PROPERTIES,
				ViewpointEntity.VIEW_IDENTIFIER_PROP);
			
			String name = (String) vp.getProperty(
				Entity.DEFAULT_ENTITY_PROPERTIES,
				ViewpointEntity.NAME_PROP);
			
			if (AV3DConstants.DEFAULT_VIEWPOINT_NAME.equals(name) &&
				view_id.equals(id)) {
				ArrayList<Command> cmdList = new ArrayList<Command>();
				if (vp.isSelected()) {
					Command cmd = new SelectEntityCommand(model, vp, false);
					cmdList.add(cmd);
				}
				Command cmd = new SelectEntityCommand(model, vp, true);
				cmdList.add(cmd);
				
				MultiCommand multiCommand = new MultiCommand(
					cmdList,
					"Selection Occured",
					false);
				controller.execute(multiCommand);
				break;
			}
		}
		vis_perspective.setEnabled(true);
		
		synchronized (this) {
			captureInProgress = false;
			notify();
		}
	}
}
