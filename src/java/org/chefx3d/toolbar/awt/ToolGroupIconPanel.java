/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2010
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/
package org.chefx3d.toolbar.awt;

// External Imports
import java.util.*;
import java.util.List;
import java.awt.geom.AffineTransform;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.*;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

// Local imports
import org.chefx3d.model.Entity;
import org.chefx3d.model.WorldModel;
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.rules.properties.accessors.RulePropertyAccessor;
import org.chefx3d.tool.Tool;
import org.chefx3d.tool.SimpleTool;
import org.chefx3d.tool.ToolGroup;
import org.chefx3d.tool.ToolGroupChild;
import org.chefx3d.tool.ToolSwitch;
import org.chefx3d.toolbar.ToolBarManager;
import org.chefx3d.toolbar.ToolBar;
import org.chefx3d.util.ApplicationParams;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.FileLoader;
import org.chefx3d.util.FontColorUtils;
import org.chefx3d.view.ViewManager;
import org.chefx3d.cache.ClientCache;
import org.chefx3d.cache.loader.ResourceLoaderListener;
import org.chefx3d.cache.loader.ResourceLoader;
import org.chefx3d.catalog.CatalogProgressListener;
import org.j3d.util.I18nManager;

/**
 * A panel that takes a root tool group or catalog and shows
 * all immeadiate subtools and groups as children
 *
 * @author Justin Couch, Daniel Joyce
 * @version $Revision: 1.59 $
 */
public class ToolGroupIconPanel extends ToolIconPanel
    implements
        ToolBar,
        ItemListener,
        ResourceLoaderListener, 
        CatalogProgressListener {

    /** The image to represent a folder */
    private static final String FOLDER_IMAGE = "images/2d/folderIcon.png";
    
    /** The image to represent a folder */
    private static final String NOT_FOUND_IMAGE = "images/2d/notFoundIcon.png";

    /** The size of the icons in the outlookToolbar in pixels */
    private static final Dimension DEFAULT_ICON_SIZE = new Dimension(80, 80);

    /** The size allocated for text below the button */
    private static final int TEXT_SPACE_SIZE = 30;
    
    /** Try to get icon how many times */
    private static final int RETRY_COUNT = 2;

    /** Margin around the image and everywhere for the buttons */
    private static final Insets ICON_MARGIN = new Insets(2, 2, 2, 2);

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** The button group to add all toggle buttons to */
    private ButtonGroup buttonGroup;

    /**
     * The hidden button that is never on the user interface. Used to turn
     * off all buttons in the tool group when no tool from this group is
     * selected.
     */
    private JToolButton hiddenButton;

    /** Map from the tool name to the Tool instance */
    private Map<String, ToolGroupChild> toolIdToToolMap;

    /** Map from the tool icon url to the JButton instance */
    private HashMap<String, JToolButton> urlToButtonMap;

    /** Map of resource paths to failure counts */
    private HashMap<String, Integer> loadFailureCounts;

    /**
     * The currently active tool. May be null when the active tool is
     * not on this panel.
     */
    private Tool currentTool;

    /** Toggle hover over display of the description */
    private boolean displayHoverover;

    private ExpandableToolbar toolbar;
    
    /** An image to use for an icon, based on {@link #FOLDER_IMAGE} */
    private BufferedImage folderImage;
    
    /** Image to use for iconNotFound, based on {@link #NOT_FOUND_IMAGE} */
    private BufferedImage notFoundImage;

    private ResourceLoader resourceLoader;

    /** The client cache manager */
    private ClientCache clientCache;

    private Color backgroundColor;
    private Color highlightColor;
    private Color selectedColor;

    private Dimension iconSize;

    /** Track start and stop times of the load process */
    private boolean trackLoadTimes;

    /** Map from the tool icon url to the load time */
    private HashMap<String, Long> urlToLoadTimeMap;

    private static AffineTransform identityTransform;

    /** Should folders have icons down loaded: if true will down load image. 
     * if false then the generic folder icon will be used */
    private boolean iconsPerFolder;
    
    private JPanel buttonPanel;
    private JPanel loadingPanel;
    private JPanel noItemsPanel;
    
    private JProgressBar loadingProgress;
      
    private int totalNeeded;
    private int processedCount;
    
    /** The internationalization manager used to get resources */
    protected I18nManager intlMgr;

    private CatalogFilter catalogFilter;
    
    private String bgColorString;
    
    private ToolGroupIconPanel toolPanel;
    
    static {
        identityTransform = new AffineTransform();
    }

    /**
     * Construct a new instance that works on the given world.
     *
     * @param model The world model
     * @param toolGroup The root group for this panel
     * @param showToolGroupTools true if the tool from ToolGroup should be
     *   included in the display.
     */
    ToolGroupIconPanel(
            WorldModel model,
            ToolGroup toolGroup,
            ExpandableToolbar toolbar,
            CatalogFilter catalogFilter,
            Color backgroundColor,
            Color selectedColor,
            Color highlightColor,
            int columns,
            Dimension iconSize,
            boolean displayHoverover) {

        super(new BorderLayout());

        this.toolbar = toolbar;
        this.catalogFilter = catalogFilter;
        this.backgroundColor = backgroundColor;
        this.highlightColor = highlightColor;
        this.selectedColor = selectedColor;
        this.iconSize = iconSize;
        this.displayHoverover = displayHoverover;

        setBackground(backgroundColor);

        // convert the color to be used in the HTML labels
        bgColorString = 
        	Integer.toHexString(backgroundColor.getRGB() & 0x00ffffff);
        
        buttonPanel = new JPanel(new GridLayout(0, columns, 1, 1));
        buttonPanel.setBackground(backgroundColor);
        buttonPanel.setVisible(false);
        add(buttonPanel, BorderLayout.NORTH);
        
        loadingProgress = new JProgressBar();
        loadingProgress.setIndeterminate(true);
        loadingProgress.setPreferredSize(new Dimension(200, 20));
        
        loadingPanel = new JPanel();
        loadingPanel.setBackground(backgroundColor);
        loadingPanel.add(loadingProgress);
        add(loadingPanel, BorderLayout.CENTER);  
        
        intlMgr = I18nManager.getManager();
        String noItemsMsg = 
            intlMgr.getString("org.chefx3d.toolbar.awt.ToolGroupIconPanel.noItemsMsg");
               
        JLabel noItemsLabel = new JLabel(noItemsMsg);
        noItemsLabel.setFont(FontColorUtils.getSmallFont());
        
        noItemsPanel = new JPanel();
        noItemsPanel.setVisible(false);
        noItemsPanel.setBackground(backgroundColor);
        noItemsPanel.add(noItemsLabel);
        add(noItemsPanel, BorderLayout.SOUTH);

        toolIdToButtonMap = new HashMap<String, JToolButton>();
        orderedButtonList = new ArrayList<String>();
        toolIdToToolMap =
            Collections.synchronizedMap(new HashMap<String, ToolGroupChild>());
        urlToButtonMap = new HashMap<String, JToolButton>();
        loadFailureCounts = new HashMap<String, Integer>();

        ToolBarManager.getToolBarManager().addToolBar(this);

        errorReporter = DefaultErrorReporter.getDefaultReporter();

        // setup the client cache manager
        clientCache = ClientCache.getInstance();

        resourceLoader = ResourceLoader.getResourceLoader();

        // setup some metrics
        trackLoadTimes = (Boolean)ApplicationParams.get("trackLoadTimes");
        if (trackLoadTimes) {
            urlToLoadTimeMap = new HashMap<String, Long>();
        }

        iconsPerFolder = false;
        
        initialize(toolGroup);
        
        processedCount = 0;

    }

    /**
     * Construct a new instance that works on the given world.
     *
     * @param model The world model
     * @param toolGroup The root group for this panel
     * @param showToolGroupTools true if the tool from ToolGroup should be
     *   included in the display.
     */
    ToolGroupIconPanel(
            WorldModel model,
            ToolGroup toolGroup,
            ExpandableToolbar toolbar,
            CatalogFilter catalogFilter,
            Color backgroundColor,
            Color selectedColor,
            Color highlightColor) {

        this(model,
                toolGroup,
                toolbar,
                catalogFilter,
                backgroundColor,
                selectedColor,
                highlightColor,
                3,
                DEFAULT_ICON_SIZE,
                false);
    }

    //----------------------------------------------------------
    // Methods defined by Toolbar
    //----------------------------------------------------------
    /**
     * Set the current tool.
     *
     * @param tool The tool
     */
    public void setTool(Tool tool) {

        currentTool = tool;

        if (tool == null) {
            
            ButtonModel bm = buttonGroup.getSelection();
            buttonGroup.setSelected(bm, false);
            
            hiddenButton.setSelected(true);
        } else {
            String toolId = tool.getToolID();
            JToolButton button = toolIdToButtonMap.get(toolId);
            if (button != null) {
                button.setSelected(true);
            } else {
                toolIdToButtonMap.remove(toolId);
                currentTool = null;
                hiddenButton.setSelected(true);
            }
        }
    }

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        if (errorReporter == null) {
            errorReporter = DefaultErrorReporter.getDefaultReporter();
        }
    }

    //----------------------------------------------------------
    // Methods defined by AuthoringComponent
    //----------------------------------------------------------
    /**
     * Get the component used to render this.
     *
     * @return The component
     */
    public Object getComponent() {
        return this;
    }

    // ----------------------------------------------------------
    // Methods defined by ItemListener
    // ----------------------------------------------------------

    /**
     * Invoked when an item has been selected or deselected by the user.
     *
     * @param evt The event that caused this method to be called
     */
    public void itemStateChanged(ItemEvent evt) {
        
        if (evt.getStateChange() != ItemEvent.SELECTED) {
            hiddenButton.setSelected(true);
            return;
        }

        JToolButton button = (JToolButton) evt.getSource();

        String toolId = button.getActionCommand();

        ToolGroupChild tgc = toolIdToToolMap.get(toolId);
        if (tgc != null) {
            
            // if the tool's parent is not the active panel's tool group then
            // ignore the request
            if (!tgc.getParent().getToolID().equals(toolbar.getActiveGroupID())) {
                return;
            }

            ToolBarManager toolManager = ToolBarManager.getToolBarManager();
            ViewManager viewManager = ViewManager.getViewManager();

            if (tgc instanceof ToolSwitch) {
                // update the toolbar with the group
                ToolSwitch toolGroup = (ToolSwitch) tgc;
                toolManager.setTool(toolGroup);

                // update the editor with the tool
                viewManager.setTool(toolGroup.getTool());
            } else if (tgc instanceof ToolGroup) {

                ToolGroup toolGroup = (ToolGroup) tgc;

                toolbar.generatePanel(toolGroup);
                toolManager.setTool(toolGroup);

                toolbar.expandNode(toolGroup);

            } else {
                Tool tool = tgc;
                if (currentTool == null || 
                    !currentTool.getToolID().equals(tgc.getToolID())) {

                    toolbar.generatePanel(tool);

                    viewManager.setTool(tool);

                    int type = tool.getToolType();
                    if (type != Entity.TYPE_WORLD) {
                        toolManager.setTool(tool);
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------
    // Methods defined by ResourceLoaderListener
    // ----------------------------------------------------------

    /**
     * The resource requested has been loaded
     *
     * @param resourceURL The url to load
     * @param resourceStream The input stream of the resource
     */
    public void resourceLoaded(
            String resourcePath,
            InputStream resourceStream) {

        try {

            JToolButton button = urlToButtonMap.get(resourcePath);
            if (button != null) {

                //now process the raw data into a buffer
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                for (int readNum; (readNum = resourceStream.read(buf)) != -1;) {
                    bos.write(buf, 0, readNum);
                }
                byte[] bytes = bos.toByteArray();
    
                // create the necessary icons for button state
                InputStream in = new ByteArrayInputStream(bytes);
                BufferedImage image = javax.imageio.ImageIO.read(in);
    
                generateButtonIcons(button, image);
                
                // update progress 
                incrementValue(1);
                
            }
        } catch (IOException ioe) {

            ioe.printStackTrace();

        }

        if (trackLoadTimes) {

            long startTime = urlToLoadTimeMap.get(resourcePath);
            long endTime = Calendar.getInstance().getTimeInMillis();
            endTime = endTime - startTime;

            int beginIndex = resourcePath.lastIndexOf("/") + 1;
            int endIndex = resourcePath.lastIndexOf("_");
            String iconUUID = resourcePath.substring(beginIndex, endIndex);

            System.out.println("LoadIcon Success " + iconUUID + " " + endTime);

        }
                
    }  

    /**
     * The resource requested was not found
     *
     * @param resourceURL The url to load
     * @param responseCode The response code
     */
    public void resourceNotFound(String resourcePath, int responseCode) {

        // request up to 3 times then truly fail
        Integer count = loadFailureCounts.get(resourcePath);
        count++;
        loadFailureCounts.put(resourcePath, count);

        if (count < RETRY_COUNT && responseCode < 400) {

            // now try to lazy load the actual image
            resourceLoader.loadResource(resourcePath, this);

        } else {

            JToolButton button = urlToButtonMap.get(resourcePath);
            if (button != null) {
                if( button.isToolGroup() ){
                	// create the necessary icons for button state
                	generateButtonIcons(button, folderImage);
            	} else {
                	// create the necessary icons for button state
                	generateButtonIcons(button, notFoundImage);
            	}
            }
            
            // update progress 
            incrementValue(1);

            if (trackLoadTimes) {

                long startTime = urlToLoadTimeMap.get(resourcePath);
                long endTime = Calendar.getInstance().getTimeInMillis();
                endTime = endTime - startTime;

                int beginIndex = resourcePath.lastIndexOf("/") + 1;
                int endIndex = resourcePath.lastIndexOf("_");
                String iconUUID = resourcePath.substring(beginIndex, endIndex);

                System.out.println("LoadIcon Failed " +
                        iconUUID + " " + endTime);

            }
        }
    }
    
    //----------------------------------------------------------
    // Methods defined by CatalogProgressListener
    //----------------------------------------------------------

    /**
     * Set the total progress increments required 
     * 
     * @param totalNeeded
     */
    public void setMaximum(int totalNeeded) {
        
        this.totalNeeded = totalNeeded;
        
        // set up the progress bar now
        loadingProgress.setMaximum(totalNeeded);
        loadingProgress.setStringPainted(true);
        loadingProgress.setValue(processedCount);
        loadingProgress.setIndeterminate(false);

        // show the nothing found panel
        noItemsPanel.setVisible(false);

    }
       
    /** 
     * Set the current increment
     *  
     * @param value the number of items that have loaded
     */
    public void incrementValue(int value) {
                  
        processedCount += value;
        loadingProgress.setValue(processedCount);
        
    }
	
	/** 
	 * Indicate that progress is done.
	 */
	public void progressComplete() {
				
		toolPanel = this;
		
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(
            new Runnable() {
                public void run() {

			        int len = orderedButtonList.size();
			        
			        if (len > 0) {
			            
			            // add all the buttons and listeners            
			            for (int i = 0; i < len; i++) {
			                
			                String toolID = orderedButtonList.get(i);               
			                JToolButton button = toolIdToButtonMap.get(toolID);
			                
			                if (button != null) {
			                    button.setLoaded(true);
			                    button.addItemListener(toolPanel);
			                    
			                    buttonGroup.add(button);                
			                    buttonPanel.add(button); 
			                }
			                
			            }
			            
			            // show the button panel
			            buttonPanel.setVisible(true);
			            noItemsPanel.setVisible(false);
			
			        } else {
			            
			            // show the nothing found panel
			            buttonPanel.setVisible(false);
			            noItemsPanel.setVisible(true);
			            
			        }
			        
			        // hide the loading panel
			        if (loadingPanel.isVisible())
			        	loadingPanel.setVisible(false);           
			        
			        // make sure the filters are correctly applied
			        catalogFilter.refreshCurrentToolGroup();
			        
			        revalidate();
			    }
            }
        );
	}

    // ----------------------------------------------------------
    // Public Methods
    // ----------------------------------------------------------

    /**
     * Build the basics of the groups based on root tool group.
     */
    private void initialize(ToolGroup rootToolGroup) {
        hiddenButton = new JToolButton("If you see me, we're screwed", false);

        buttonGroup = new ButtonGroup();
        buttonGroup.add(hiddenButton);

        // create the loading image
        FileLoader fileLookup = new FileLoader(1);

        // lookup the folder image so it will be in the cache already
        try {
            Object[] iconURL = fileLookup.getFileURL(FOLDER_IMAGE);

            //now process the raw data into a buffer
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            for (int readNum; (readNum = ((InputStream)iconURL[1]).read(buf)) != -1;) {
                bos.write(buf, 0, readNum);
            }
            byte[] bytes = bos.toByteArray();

            InputStream in = new ByteArrayInputStream(bytes);
            folderImage = javax.imageio.ImageIO.read(in);

        } catch (Exception ioe) {
            ioe.printStackTrace();
        }

        // lookup the not found image so it will be in the cache
        try {
            Object[] iconURL = fileLookup.getFileURL(NOT_FOUND_IMAGE);

            //now process the raw data into a buffer
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            for (int readNum; (readNum = ((InputStream)iconURL[1]).read(buf)) != -1;) {
                bos.write(buf, 0, readNum);
            }
            byte[] bytes = bos.toByteArray();

            InputStream in = new ByteArrayInputStream(bytes);
            notFoundImage = javax.imageio.ImageIO.read(in);

        } catch (Exception ioe) {
            ioe.printStackTrace();
        }

        addRootToolGroup(rootToolGroup);

    }
      
    /**
     * Add a button to the panel that represents this tool.
     * <br>
     * Called by DrillDownToolbar.
     * 
     * @param tool The tool to add the button for
     */
    public void addToolButton(ToolGroupChild tool) {
        
        // set the oder of the objects       
        String toolId = tool.getToolID();        
        orderedButtonList.add(toolId);
        
    }
           
    /**
     * Remove a button to the panel that represents this tool.
     * <br>
     * Called by DrillDownToolbar.
     * 
     * @param tool The tool to remove the button for
     */
    public void updateToolButton(ToolGroupChild tool) {
        
        // don't create "hidden" items
        Boolean hiddenTool = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    tool,
                    ChefX3DRuleProperties.HIDE_IN_CATALOG);
  
        if (!hiddenTool) {
            
            // create the button
            generateButton(tool);             
 
        } else {
            
            // clean up the place holder
            String toolId = tool.getToolID();   
            orderedButtonList.remove(toolId);
                        
            // update progress
            incrementValue(1);
            
        }

    }
       
    /**
     * Remove a button to the panel that represents this tool.
     *
     * @param tool The tool to remove the button for
     */
    public void removeToolButton(ToolGroupChild tool) {

        String toolId = tool.getToolID();

        if (toolIdToButtonMap.containsKey(toolId)) {

            JToolButton tempButton = toolIdToButtonMap.get(toolId);
            remove(tempButton);
            buttonGroup.remove(tempButton);

            toolIdToButtonMap.remove(toolId);

            String iconPath = tool.getIcon();
            urlToButtonMap.remove(iconPath);

        }

    }

    /**
     * Add a button to the panel that represents this tool.
     * <br>
     * Called by DrillDownToolbar.
     * 
     * @param tool The tool to add the button for
     */
    public void addToolGroupButton(ToolGroup toolGroup) {
            
        // don't create "hidden" items
        Boolean hiddenTool = 
            (Boolean)RulePropertyAccessor.getRulePropertyValue(
                    toolGroup,
                    ChefX3DRuleProperties.HIDE_IN_CATALOG);
  
        if (!hiddenTool) {
            
            // set the oder of the objects  
            if (!(toolGroup instanceof ToolSwitch)) {

                String toolId = toolGroup.getToolID();        
                orderedButtonList.add(toolId);
                
            }

            // create the button
            generateButton(toolGroup);   
            
        }
        
    }

    /**
     * Disable then re-enable all the buttons.  Only was we've
     * found to reset the roll-over state.
     */
    public void resetRollover() {
        JToolButton[] button = toolIdToButtonMap.values().toArray(new JToolButton[0]);
        for (int i = 0; i < button.length; i++) {

            boolean enabled = button[i].isEnabled();
            button[i].setEnabled(!enabled);
            button[i].setEnabled(enabled);
        }
        // make sure the panel is reset
        setTool(null);
        
        revalidate();
    }

    // ----------------------------------------------------------
    // Private Methods
    // ----------------------------------------------------------
  
    /**
     * Method to add child groups of the given group.
     * If the flag is set, it will also add a button for this group's tool.
     * 
     * @param parentGroup The group to add the children for
     */
    private void addRootToolGroup(ToolGroup parentGroup) {

        List<ToolGroupChild> children = parentGroup.getChildren();

        for (int i = 0; i < children.size(); i++) {
            ToolGroupChild child = children.get(i);

            if(child instanceof ToolSwitch) {
                ToolGroup group = (ToolGroup) child;
                addToolGroupButton(group);
            } else if (child instanceof ToolGroup) {
                ToolGroup group = (ToolGroup) child;
                addToolGroupButton(group);
            } else if (child instanceof SimpleTool) {
                addToolButton((SimpleTool) child);
            }
        }
    }

    /**
     * Create the JToolButton for the provided item
     * EMF: Adjusted this method in order to treat ToolGroups
     * as Tools.  This means we will use the loadResource
     * method in order to get the icon of the ToolGroup with 
     * a webservice call.
     * 
     * @param tool a Tool
     */
    private void generateButton(ToolGroupChild tool) {

        String iconPath = tool.getIcon();
        String toolId = tool.getToolID();

        // create the button
        JToolButton button = getToggleButton(tool);

        // add to the data maps
        toolIdToButtonMap.put(toolId, button);
        urlToButtonMap.put(iconPath, button);
        toolIdToToolMap.put(toolId, tool);
        
        String firstToolID = "";
        if (tool instanceof ToolSwitch) {
            firstToolID = ((ToolSwitch)tool).getTool().getToolID();
        }
        
        if (orderedButtonList.contains(firstToolID)) {
            
            //
            // a tool switch has been added.  clean up garbage buttons
            //
            
            // clear out the tools now in the group
            ToolSwitch toolSwitch = (ToolSwitch)tool;
            int len = orderedButtonList.size();
            for (int i = len - 1; i >= 0; i--) {
                String checkID = orderedButtonList.get(i);
                Tool check = toolSwitch.getTool(checkID);
                if (check != null && !checkID.equals(firstToolID)) {
                    orderedButtonList.remove(i);
                }
            }
            
            // set the first tool to the new button
            toolIdToButtonMap.put(firstToolID, button);             
            toolIdToToolMap.put(firstToolID, tool);
            
            // another icon request will be made, so update the max amount
            setMaximum(totalNeeded + 1);
            
        } 
        
        //
        // now request the icon if necessary
        //                     
        boolean isFolder = false;
        if (tool instanceof ToolGroup && !(tool instanceof ToolSwitch)) {
            isFolder = true;
        }
        
        if (isFolder && !iconsPerFolder) {
            
            generateButtonIcons(button, folderImage);
           
        } else {
            
            // check the cache for the resource
            if (clientCache.doesAssetExist(iconPath)) {
                
                // call the resource loaded directly
                try {
                    InputStream resourceStream =
                        clientCache.retrieveAsset(iconPath);
                    resourceLoaded(iconPath, resourceStream);
                } catch (IOException io) {
                    errorReporter.errorReport(io.getMessage(), io);
                }                
                
            } else {
                
                // set count failures to 0
                loadFailureCounts.put(iconPath, 0);

                // now try to lazy load the actual image
                resourceLoader.loadResource(iconPath, this);

            }

        }

    }
    
    /**
     * 
     * @param tool
     * @return
     */
    private JToolButton getToggleButton(Tool tool) {

    	String toolId = tool.getToolID();
        String name = tool.getName();
    	
        JToolButton button = new JToolButton(name, tool instanceof ToolGroup);

        String displayName = generateDisplayName(name);

        if (displayHoverover) {
            String hoveroverText = generateHoverover(tool);
            button.setToolTipText(hoveroverText);
        }

        button.setPreferredSize(
                new Dimension(
                        iconSize.width,
                        iconSize.height + TEXT_SPACE_SIZE));

        button.setBorderPainted(true);
        button.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        button.setContentAreaFilled(false);

        button.setRolloverEnabled(true);
        button.setActionCommand(toolId);
        button.setText(displayName);
        button.setFont(FontColorUtils.getSmallFont());
        button.setForeground(FontColorUtils.getForegroundColor());
        button.setVerticalTextPosition(AbstractButton.BOTTOM);
        button.setHorizontalTextPosition(AbstractButton.CENTER);
        button.setVerticalAlignment(AbstractButton.TOP);
        button.setHorizontalAlignment(AbstractButton.CENTER);
        button.setMargin(ICON_MARGIN);
        button.setBackground(backgroundColor);
        return button;
    }

    /**
     * Generate the hover text using the name, desc, and image
     *
     * @param tool
     * @return
     */
    private String generateHoverover(Tool tool) {

        String name = tool.getName();
        String desc = tool.getDescription();
        String image = tool.getIcon();

        StringBuilder hoverText = new StringBuilder();
        hoverText.append("<html>");
        hoverText.append("<div style='width:200px;background-color:white'>");
        hoverText.append("<h3>");
        hoverText.append(name);
        hoverText.append("</h3>");
        if (desc != null && !desc.equals("")) {
            hoverText.append("<p>");
            hoverText.append(desc);
            hoverText.append("</p>");
        }
        if (image != null && !image.equals("")) {

            FileLoader fileLookup = new FileLoader(1);

            try {
                Object[] iconURL = fileLookup.getFileURL(image);
                if (iconURL[1] != null && image.startsWith("http")) {
                    hoverText.append("<p align='center'><img src='");
                    hoverText.append(image);
                    hoverText.append("' alt='image'></p>");
                }
            } catch (Exception ex) {
                // there was a problem finding the image, just don't display it
            }
        }

        hoverText.append("<br>");
        hoverText.append("</div>");
        hoverText.append("</html>");

        return hoverText.toString();

    }

    /**
     * Create a formated button name using HTML
     *
     * @param name The name to display
     * @return the formated HTML text
     */
    private String generateDisplayName(String name) {

        StringBuilder displayText = new StringBuilder();
        displayText.append("<html><div width='");
        displayText.append(iconSize.width);
        displayText.append("' align='center' bgcolor='#");
        displayText.append(bgColorString);
        displayText.append("'>");
        displayText.append(name);
        displayText.append("</div></html>");
        return displayText.toString();

    }

    /**
     * Take an image and create the necessary version for selection,
     * highlight, and inactive.
     *
     * @param button
     * @param baseImage
     */
    private void generateButtonIcons(JToolButton button, BufferedImage image) {

        image = scalePretty(image, iconSize.width, iconSize.height);

        // create the gradient
        GradientPaint gradient = new GradientPaint(
                0, 0, backgroundColor, 0, iconSize.height, selectedColor, true);

        //
        // set up the inactive icon
        //
        BufferedImage buffered_image =
            new BufferedImage(iconSize.width, iconSize.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buffered_image.createGraphics();
        g2.setPaint(gradient);
        g2.fillRect(0, 0, iconSize.width, iconSize.height);
        g2.setPaint(Color.BLACK);
        g2.drawRenderedImage(image, identityTransform);

        button.setIcon(new ImageIcon(buffered_image));

        //
        // set up the highlighted icon
        //
        buffered_image =
            new BufferedImage(iconSize.width, iconSize.height, BufferedImage.TYPE_INT_ARGB);
        g2 = buffered_image.createGraphics();
        g2.setPaint(gradient);
        g2.fillRect(0, 0, iconSize.width, iconSize.height);
        g2.setPaint(Color.BLACK);
        g2.drawRenderedImage(image, identityTransform);
        g2.setColor(highlightColor);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(1, 1, iconSize.width - 2, iconSize.height - 2);
        button.setRolloverIcon(new ImageIcon(buffered_image));

        //
        // set up the disabled icon
        //
        Color gray =
            new Color(
                    Color.GRAY.getRed(),
                    Color.GRAY.getGreen(),
                    Color.GRAY.getBlue(),
                    192);

        buffered_image =
            new BufferedImage (iconSize.width, iconSize.height, BufferedImage.TYPE_BYTE_GRAY);
        g2 = buffered_image.createGraphics();
        g2.drawRenderedImage(image, identityTransform);
        g2.setColor(gray);
        g2.fillRect(0, 0, iconSize.width, iconSize.height);
        button.setDisabledIcon(new ImageIcon(buffered_image));

    }

    /**
     * Scale an image using the most pretty method.
     *
     * @param image The image to scale
     * @param newWidth The new width
     * @param newHeight The new height
     */
    private BufferedImage scalePretty(BufferedImage image, int newWidth,
        int newHeight) {

        java.awt.Image rimg = image.getScaledInstance(
            newWidth,
            newHeight,
            java.awt.Image.SCALE_AREA_AVERAGING);

        boolean hasAlpha = image.getColorModel().hasAlpha();

        BufferedImage ret_image = null;
        // Not sure why .getType doesn't work right for this
        if (hasAlpha) {
            ret_image = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        } else {
            ret_image = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        }
        java.awt.Graphics2D g2 = ret_image.createGraphics();
        g2.drawImage(rimg, 0, 0, null);

        return(ret_image);
    }

}
