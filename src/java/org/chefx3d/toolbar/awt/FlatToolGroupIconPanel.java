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
import javax.swing.*;

import java.util.*;
import java.util.List;

import java.awt.*;
import java.awt.image.*;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.MalformedURLException;
import java.awt.geom.AffineTransform;
// Local imports
import org.chefx3d.rules.properties.ChefX3DRuleProperties;
import org.chefx3d.tool.*;
import org.chefx3d.cache.ClientCache;
import org.chefx3d.cache.loader.ResourceLoader;
import org.chefx3d.model.WorldModel;
import org.chefx3d.model.Entity;

import org.chefx3d.util.FileLoader;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.FontColorUtils;
import org.chefx3d.view.ViewManager;
import org.chefx3d.toolbar.ToolBarManager;
import org.chefx3d.cache.loader.ResourceLoaderListener;

/**
 * A panel that takes a root tool group and shows all groups and tools
 * underneath it in a flat structure.
 *
 * @author Justin Couch
 * @version $Revision: 1.55 $
 */
public class FlatToolGroupIconPanel extends ToolIconPanel
    implements 
        ToolGroupListener, 
        ItemListener, 
        ResourceLoaderListener {

    /** The image to use while loading an icon */
    private static final String LOADER_IMAGE = "images/2d/ajax-loader.gif";

    /** The image to represent a folder */
    private static final String FOLDER_IMAGE = "images/2d/folderIcon.png";
    
    /** The image to represent a folder */
    private static final String NOT_FOUND_IMAGE = "images/2d/notFoundIcon.png";

    /** The default number of buttons to display per row */
    private static final int BUTTONS_PER_ROW = 2;

    /** The size of the icons in the outlookToolbar in pixels */
    private static final Dimension DEFAULT_ICON_SIZE = new Dimension(80, 80);

    /** Margin around the image and everywhere for the buttons */
    private static final Insets ICON_MARGIN = new Insets(0, 0, 0, 0);

    /** The size allocated for text below the button */
    private static final int TEXT_SPACE_SIZE = 30;

    /** The world model */
    private WorldModel model;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** Root tool group for this panel. */
    private ToolGroup rootToolGroup;

    /** The button group to add all toggle buttons to */
    private ButtonGroup buttonGroup;

    /**
     * The hidden button that is never on the user interface. Used to turn
     * off all buttons in the tool group when no tool from this group is
     * selected.
     */
    private JToolButton hiddenButton;

    /** True to show the tool group's tool */
    private final boolean showToolGroupTools;

    /** Map from the tool name to the Tool instance */
    private HashMap<String, Tool> toolIdToToolMap;

    /** Map from the tool icon url to the JButton instance */
    private HashMap<String, JToolButton> urlToButtonMap;

    /** Map of resource paths to failure counts */
    private HashMap<String, Integer> loadFailureCounts;

    /**
     * The currently active tool. May be null when the active tool is
     * not on this panel.
     */
    private Tool currentTool;

    /** The current size of the button */
    private Dimension iconSize;
    
    /** show the border */ 
    private boolean showBorder;
    
    /** show the border */ 
    private float imageScale;
    
    /** A pool of threaded loaders */
    private ResourceLoader resourceLoader;

    /** The client cache manager */
    private ClientCache clientCache;
    
    private ImageIcon loadingImage;
    private BufferedImage folderImage;
    private BufferedImage notFoundImage;

    private boolean areEnabled;
    
    private Color backgroundColor;
    private Color highlightColor;
    private Color selectedColor;
    
    /** Toggle hover over display of the description */
    private boolean displayHoverover;

    private CatalogFilter catalogFilter;

    /**
     * Construct a new instance that works on the given world.
     *
     * @param model The world model
     * @param toolGroup The root group for this panel
     * @param showToolGroupTools true if the tool from ToolGroup should be
     *   included in the display.
     */
    public FlatToolGroupIconPanel(
            WorldModel model, 
            ToolGroup toolGroup, 
            CatalogFilter catalogFilter,
            boolean showToolGroupTools, 
            int buttonsPerRow, 
            Dimension iconSize, 
            boolean showBorder, 
            float imageScale) {
        
        super(new GridLayout(0, buttonsPerRow));

        //setBorder(BorderFactory.createEmptyBorder(4, 2, 2, 2));     
        
        this.model = model;
        this.catalogFilter = catalogFilter;
        this.rootToolGroup = toolGroup;
        this.showToolGroupTools = showToolGroupTools;
        this.iconSize = iconSize;
        this.showBorder = showBorder;
        this.imageScale = imageScale;
        areEnabled = true;
        displayHoverover = false;
        
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        toolIdToButtonMap = new HashMap<String, JToolButton>();
        toolIdToToolMap = new HashMap<String, Tool>();
        urlToButtonMap = new HashMap<String, JToolButton>();
        loadFailureCounts = new HashMap<String, Integer>();
        
        // setup the client cache manager
        clientCache = ClientCache.getInstance();

        resourceLoader = ResourceLoader.getResourceLoader();

        initialBuild();

        toolGroup.addToolGroupListener(this);
    }
    
    /**
     * Construct a new instance that works on the given world.
     *
     * @param model The world model
     * @param toolGroup The root group for this panel
     * @param showToolGroupTools true if the tool from ToolGroup should be
     *   included in the display.
     */
    public FlatToolGroupIconPanel(
            WorldModel model, 
            ToolGroup toolGroup, 
            CatalogFilter catalogFilter, 
            boolean showToolGroupTools) {
        
        this(model, 
                toolGroup, 
                catalogFilter, 
                showToolGroupTools, 
                BUTTONS_PER_ROW, 
                DEFAULT_ICON_SIZE, 
                false, 
                0.7f);
        
    }

    /**
     * Construct a new instance that works on the given world.
     *
     * @param model The world model
     * @param toolGroup The root group for this panel
     * @param showToolGroupTools true if the tool from ToolGroup should be
     *   included in the display.
     */
    public FlatToolGroupIconPanel(
            WorldModel model,  
            CatalogFilter catalogFilter,
            boolean showToolGroupTools, 
            int buttonsPerRow, 
            Dimension iconSize, 
            boolean showBorder, 
            float imageScale, 
            boolean areEnabled, 
            Color backgroundColor, 
            Color selectedColor, 
            Color highlightColor, 
            boolean displayHoverover) {
        
        super(new GridLayout(0, buttonsPerRow));
       
        //setBorder(BorderFactory.createEmptyBorder(4, 2, 2, 2));   
        setBackground(backgroundColor);

        this.model = model;
        this.catalogFilter = catalogFilter;
        this.showToolGroupTools = showToolGroupTools;
        this.iconSize = iconSize;
        this.showBorder = showBorder;
        this.imageScale = imageScale;
        this.areEnabled = areEnabled;
        this.displayHoverover = displayHoverover;
        
        this.backgroundColor = backgroundColor;
        this.highlightColor = highlightColor;
        this.selectedColor = selectedColor;

        errorReporter = DefaultErrorReporter.getDefaultReporter();

        toolIdToButtonMap = new HashMap<String, JToolButton>();
        toolIdToToolMap = new HashMap<String, Tool>();
        urlToButtonMap = new HashMap<String, JToolButton>();
        loadFailureCounts = new HashMap<String, Integer>();
        
        // setup the client cache manager
        clientCache = ClientCache.getInstance();

        resourceLoader = ResourceLoader.getResourceLoader();

    }

    //----------------------------------------------------------
    // Methods defined by ToolGroupListener
    //----------------------------------------------------------

    /**
     * A tool has been added.  Batched additions will come through
     * the toolsAdded method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolAdded(ToolGroupEvent evt) {
        SimpleTool tool = (SimpleTool)evt.getChild();
        addToolButton(tool, tool.getToolID());
    }

    /**
     * A tool group has been added. Batched adds will come through the
     * toolsAdded method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolGroupAdded(ToolGroupEvent evt) {
        ToolGroup group = (ToolGroup)evt.getChild();
        addToolGroup(group);
    }

    /**
     * A tool has been removed. Batched removes will come through the
     * toolsRemoved method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolRemoved(ToolGroupEvent evt) {
        SimpleTool tool = (SimpleTool)evt.getChild();
        removeToolButton(tool);
    }

    /**
     * A tool has been removed.  Batched removes will come through the
     * toolsRemoved method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolGroupRemoved(ToolGroupEvent evt) {
        ToolGroup group = (ToolGroup)evt.getChild();
        removeToolGroup(group);
    }
    
    /**
     * A tool has been updated.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolUpdated(ToolGroupEvent evt) {   
        
        Tool tool = (Tool)evt.getChild();
        String toolId = tool.getToolID();
        
        if (toolIdToToolMap.containsKey(toolId)) {
            toolIdToToolMap.put(toolId, tool);
        }
        
        // get the button to update
        JToolButton button = toolIdToButtonMap.get(toolId);
        
        // update the hover-over if necessary
        String desc = tool.getDescription();
        if (displayHoverover && desc != null && !desc.equals("")) {
            
            // create the hoverover and re-assign it
            String hoveroverText = generateHoverover(tool);
            button.setToolTipText(hoveroverText);
            
        }

        //
        // now request the icon if necessary
        //
        Boolean hiddenTool =
            (Boolean)tool.getProperty(
                    Entity.DEFAULT_ENTITY_PROPERTIES,
                    ChefX3DRuleProperties.HIDE_IN_CATALOG);
        
        if (hiddenTool == null || !hiddenTool) {
                        
            String iconPath = tool.getIcon();
            
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
                
                // now try to lazy load the actual image
                resourceLoader.loadResource(iconPath, this);
                
            }
 
        }

    }

    /**
     * A tool has been updated.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolGroupUpdated(ToolGroupEvent evt) {
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
        if(evt.getStateChange() != ItemEvent.SELECTED)
            return;

        JToolButton button = (JToolButton)evt.getSource();

        String toolId = button.getActionCommand();
        Tool tool = toolIdToToolMap.get(toolId);
        if(currentTool != tool) {
            ViewManager viewManager = ViewManager.getViewManager();
            viewManager.setTool(tool);

            int type = tool.getToolType();
            if (type != Entity.TYPE_WORLD) {
	            ToolBarManager toolManager = ToolBarManager.getToolBarManager();
	            toolManager.setTool(tool);
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
            
            // clear the failure count if necessary
            if (loadFailureCounts.containsKey(resourcePath)) {
                loadFailureCounts.remove(resourcePath);
            }
            
            //now process the raw data into a buffer
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            for (int readNum; (readNum = resourceStream.read(buf)) != -1;) {
                bos.write(buf, 0, readNum); 
            }
            byte[] bytes = bos.toByteArray();
          
            InputStream in = new ByteArrayInputStream(bytes);
            BufferedImage image = javax.imageio.ImageIO.read(in);  
            
            // try to retrieve from the classpath
            JToolButton button = urlToButtonMap.get(resourcePath);

            // create the necessary icons for button state
            generateButtonIcons(button, image);         
            
            // make sure the filters are correctly applied
            catalogFilter.refreshCurrentToolGroup();

            // allow selection
            button.setLoaded(true);
            button.addItemListener(this);
            button.revalidate();

        } catch (IOException ioe) {
            
            ioe.printStackTrace();
            
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
        
        if (count < 3 && responseCode < 400) {
            
            // now try to lazy load the actual image                   
            resourceLoader.loadResource(resourcePath, this);   
            
        } else {
            
            JToolButton button = urlToButtonMap.get(resourcePath);

            // create the necessary icons for button state
            generateButtonIcons(button, notFoundImage);         
                       
            // make sure the filters are correctly applied
            catalogFilter.refreshCurrentToolGroup();
            
            // allow selection
            button.setLoaded(true);
            button.addItemListener(this);
            button.revalidate();

        }

    }

    // ----------------------------------------------------------
    // Local Methods
    // ----------------------------------------------------------

    /**
     * Set the current tool.
     *
     * @param tool The tool
     */
    public void setTool(Tool tool) {
        Tool oldTool = currentTool;

        currentTool = tool;
      
        if(tool == null && hiddenButton != null) {
            hiddenButton.setSelected(true);
            revalidate();
        } else if(tool != oldTool) {
            String toolId = tool.getToolID();
            JToolButton button = toolIdToButtonMap.get(toolId);
            if(button != null) {
                button.setSelected(true);
            } else {
                currentTool = null;
                if (hiddenButton != null)
                    hiddenButton.setSelected(true);
            }
        }
    }

    /**
     * Set the root tool group and rebuild
     * 
     * @param toolGroup
     */
    public void setRootToolGroup(ToolGroup toolGroup) {
        this.rootToolGroup = toolGroup;

        initialBuild();
        
        if (toolGroup != null)
            toolGroup.addToolGroupListener(this);
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

    /**
     * Build the basics of the groups based on root tool group.
     */
    private void initialBuild() {
        hiddenButton = new JToolButton("If you see me, we're screwed", false);

        buttonGroup = new ButtonGroup();
        buttonGroup.add(hiddenButton);

        // create the loading image
        FileLoader fileLookup = new FileLoader(1);
        Toolkit tk = getToolkit();

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


        // lookup the loading image so it will be in the cache
        try {
            
            Object[] iconURL = fileLookup.getFileURL(LOADER_IMAGE);
            loadingImage = new ImageIcon((java.net.URL)iconURL[0]);
           
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

        addToolGroup(rootToolGroup);
    }

    /**
     * Recursive method to add child groups of the given group.
     * If the flag is set, it will also add a button for this group's tool.
     *
     * @parem parentGroup The group to add the children for
     */
    private void addToolGroup(ToolGroup parentGroup) {
        if(showToolGroupTools) {
            SimpleTool tool = parentGroup.getTool();
            addToolButton(tool, parentGroup.getToolID());
        }

        if (parentGroup == null)
            return;
        
        List<ToolGroupChild> children = parentGroup.getChildren();

        for(int i = 0; i < children.size(); i++) {
            ToolGroupChild child = children.get(i);

            if(child instanceof ToolSwitch) {
                SimpleTool tool = ((ToolSwitch)child).getTool();
                addToolButton(tool, child.getToolID());
            } else if(child instanceof ToolGroup) {
                ToolGroup group = (ToolGroup)child;
                addToolGroup(group);
                group.addToolGroupListener(this);
            } else if(child instanceof SimpleTool) {
                addToolButton((SimpleTool)child, child.getToolID());
            }
        }
    }

    /**
     * Remove this group and any child tools/tool groups below it.
     *
     * @param parentGroup The group to remove from the system
     */
    private void removeToolGroup(ToolGroup parentGroup) {
        if(showToolGroupTools) {
            SimpleTool tool = parentGroup.getTool();
            removeToolButton(tool);
        }

        List<ToolGroupChild> children = parentGroup.getChildren();

        for(int i = 0; i < children.size(); i++) {
            ToolGroupChild child = children.get(i);

            if(child instanceof ToolGroup) {
                ToolGroup group = (ToolGroup)child;
                removeToolGroup(group);
                group.removeToolGroupListener(this);
            } else if(child instanceof SimpleTool) {
                removeToolButton((SimpleTool)child);
            }
        }
    }

    /**
     * Add a button to the panel that represents this tool.
     *
     * @param tool The tool to add the button for
     */
    private void addToolButton(Tool tool, String toolId) {
        
        String iconPath = tool.getIcon();

        // create the button
        JToolButton button = getToggleButton(tool, toolId);
        
        // disable if it is loading
        button.setLoaded(false);
                
        // add to the data maps
        toolIdToButtonMap.put(toolId, button);
        urlToButtonMap.put(iconPath, button);
        toolIdToToolMap.put(toolId, tool);

        // put the proxy image in place
        if (tool instanceof ToolGroup && !(tool instanceof ToolSwitch)) {
                   
            // create the necessary icons for button state
            generateButtonIcons(button, folderImage);         
            
            // allow selection
            button.setLoaded(true);
            button.addItemListener(this);

        } else {
            
            // set count failures to 0
            loadFailureCounts.put(iconPath, 0);
                       
            String category = tool.getCategory();                          
            if (category != null && !category.equals("Category.Loading")) {
                
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
                    
                    // now try to lazy load the actual image
                    resourceLoader.loadResource(iconPath, this);
                    
                }
                
            } else {
                
                // set the loading image
                button.setIcon(loadingImage);

            }

        }

        revalidate();
        
    }

    /**
     * Remove the given button from the system.
     */
    private void removeToolButton(SimpleTool tool) {
        String toolId = tool.getToolID();

        JToolButton button = toolIdToButtonMap.get(toolId);
        button.removeItemListener(this);
        remove(button);

        buttonGroup.remove(button);
        toolIdToButtonMap.remove(toolId);
        toolIdToToolMap.remove(toolId);
		
		revalidate();
    }
    
    private JToolButton getToggleButton(Tool tool, String toolId) {
        
        //String toolId = tool.getToolID();
        String name = tool.getName();
        JToolButton button = new JToolButton(name, tool instanceof ToolGroup);

        String displayName = name;
        // wrap the title
        if (!name.startsWith("<html>")) {
            displayName = "<html><div align='center'>" + name + "</div></html>";
        }

        if (displayHoverover) {
            String hoveroverText = generateHoverover(tool);
            button.setToolTipText(hoveroverText);
        }
        
        button.setPreferredSize(
                new Dimension(
                        iconSize.width, 
                        iconSize.height + TEXT_SPACE_SIZE));
        
        button.setBorderPainted(true);
        button.setBorder(BorderFactory.createEmptyBorder(0 ,0, 0, 0));
        button.setContentAreaFilled(false);

        button.setRolloverEnabled(true);
        button.setActionCommand(toolId);
        button.setText(displayName);
        button.setFont(FontColorUtils.getSmallFont());
        button.setVerticalTextPosition(AbstractButton.BOTTOM);
        button.setHorizontalTextPosition(AbstractButton.CENTER);
        button.setVerticalAlignment(AbstractButton.TOP);
        button.setHorizontalAlignment(AbstractButton.CENTER);
        button.setMargin(ICON_MARGIN);
        button.setEnabled(areEnabled);

        // assign the loading image
        buttonGroup.add(button);
        
        if (toolIdToButtonMap.containsKey(toolId)) {
            JToolButton tempButton = toolIdToButtonMap.get(toolId);
            remove(tempButton);
            buttonGroup.remove(tempButton);
        }
        add(button);

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
     * Take an image and create the necessary version for selection, 
     * highlight, and inactive.
     * 
     * @param button
     * @param baseImage
     */
    private void generateButtonIcons(JToolButton button, BufferedImage image) {
       
        // set up the scaling transform
        AffineTransform at = AffineTransform.getScaleInstance(
                (double)iconSize.width/image.getWidth(),
                (double)iconSize.height/image.getHeight());

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
        g2.drawRenderedImage(image, at);
        
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
        g2.drawRenderedImage(image, at);
        g2.setColor(highlightColor);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(1, 1, iconSize.width - 2, iconSize.height - 2);
        button.setRolloverIcon(new ImageIcon(buffered_image));
                
    }

}
