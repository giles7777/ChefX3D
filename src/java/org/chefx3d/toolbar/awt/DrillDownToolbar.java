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
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashMap;


// Local imports
import org.chefx3d.catalog.CatalogManager;
import org.chefx3d.catalog.CatalogManagerListener;
import org.chefx3d.catalog.Catalog;
import org.chefx3d.catalog.CatalogListener;
import org.chefx3d.catalog.CatalogProgressListener;
import org.chefx3d.model.WorldModel;
import org.chefx3d.tool.*;
import org.chefx3d.toolbar.ToolBar;
import org.chefx3d.toolbar.ToolBarManager;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.FontColorUtils;
import org.chefx3d.view.ViewManager;


/**
 * This BreadCrumbToolbar will use a 'breadcrumb' trail and a set of icons/tabs
 * within a outlookToolBar tab to a`llow browsing of tree of toolgroups and tools
 * At any given time, only one branch and its lineal descendants are shown.
 *
 * Clicking on the breadcrumbs allows the user to go up and down a branch, or
 * switch branches. Unexpanded groups are rendered as icons until selected and
 * expand to a tab, with subtools and subgroups rendered as icons beneath it.
 *
 * @author Russell Dodds, Daniel Joyce
 * @version $Revision: 1.64 $
 */
public class DrillDownToolbar
        implements
            ToolBar,
            CatalogManagerListener,
            CatalogListener,
            CatalogProgressListener,
            ToolGroupListener,
            ExpandableToolbar {

    /*
     * Yes, I am using inner-classes, but these are pretty tightly coupled, and the
     * whole toolbar framework would need to be refactored to properly leverage
     * the common code/behaviours amongst all the toolbars and their supporting
     * classes.
     */

    /** The panel to place the catalog items */
    private JPanel displayPanel;

    /** The world model */
    private WorldModel model;

    /** The view manager */
    private ViewManager viewManager;

    /** The catalog manager */
    private CatalogManager catalogManager;

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;

    /** The catalog that we should be watching for changes on */
    private String catalogName;

    /** The BreadCrumbComponent's vertical size */
    private Dimension breadCrumbSize;

    /** The panel that contains the bread crumb components*/
    private JPanel crumbPanel = new JPanel();
    
    /** The seperator that goes between the breadcrumb components*/
    private String breadCrumbSeperator = ":";

    /** The currently expanded toolGroupChild */
    ToolGroup expandedToolGroupNode = null;

    /** The root ToolGroup */
    private ToolGroup rootGroup;

    /** The name of the root group to look up, displays all if not found */
    private String rootGroupName;

    /**
     * The currently active tool. May be null when the active tool is
     * not on this panel.
     */
    private Tool currentTool;

    private ToolGroupIconPanel currentToolPanel;

    /** Cache for panels to avoid recreating each time */
    private Map<ToolGroup, ToolGroupIconPanel> flatPanelCache;

    /** Catalog filter for controlling access to products */
    private CatalogFilter catalogFilter;

    /** colors to be used */
    private Color backgroundColor;
    private Color highlightColor;
    private Color selectedColor;

    /** The number of columns */
    private int columns;

    /** The szie of the icon */
    private Dimension iconSize;
   
    /** Toggle hover over display of the description */
    private boolean displayHoverover;
    
    /** The active tool group */
    private String activeToolGroup;
    
    private boolean navigationEnabled;
    
    private String bgColorString;
    
    private JPanel contentPanel;
    private GridBagConstraints c;
        
    /**
     * Construct a new instance of the bread crumb tool bar
     *
     * @param worldModel The model that holds the data
     * @param rootGroupName The name of the root tool group
     * @param breadCrumbSeperator The separator string
     * @param breadCrumbVSize The size to allocate to A SINGLE ROW of the bread crumb trail
     * @param catalogFilter Filter to use on the displayed catalog, can be null
     * @param backgroundColor
     * @param selectedColor
     * @param highlightColor
     * @param columns
     * @param iconSize
     */
    public DrillDownToolbar(
            WorldModel worldModel,
            CatalogManager catalogManager, 
            String rootGroupName,
            String breadCrumbSeperator,
            Dimension breadCrumbSize,
            CatalogFilter catalogFilter, 
            Color backgroundColor, 
            Color selectedColor, 
            Color highlightColor,
            int columns, 
            Dimension iconSize, 
            boolean displayHoverover) {
    	
        this.model = worldModel;
        this.rootGroupName = rootGroupName;
        this.breadCrumbSeperator = breadCrumbSeperator;
        this.breadCrumbSize = breadCrumbSize;
        this.catalogFilter = catalogFilter;
        this.backgroundColor = backgroundColor;
        this.highlightColor = highlightColor;
        this.selectedColor = selectedColor;
        this.columns = columns;
        this.iconSize = iconSize;
        this.displayHoverover = displayHoverover;

        viewManager = ViewManager.getViewManager();

        this.catalogManager = catalogManager;
        catalogManager.addCatalogManagerListener(this);

        errorReporter = DefaultErrorReporter.getDefaultReporter();
        ToolBarManager.getToolBarManager().addToolBar(this);

        displayPanel = new JPanel();
        displayPanel.setBackground(backgroundColor);
        
        flatPanelCache = 
        	Collections.synchronizedMap(new HashMap<ToolGroup, ToolGroupIconPanel>());
        
        navigationEnabled = true;
        
        // convert the color to be used in the HTML labels
        bgColorString = 
        	Integer.toHexString(backgroundColor.getRGB() & 0x00ffffff);


    }

    /**
     * Construct a new instance of the bread crumb tool bar
     *
     * @param model The model that holds the data
     * @param breadCrumbSeperator The separator string
     * @param breadCrumbVSize The size of a SINGLE ROW of the bread
     * crumb trail; the height will scaled to make room for additional rows.
     */
    public DrillDownToolbar(
            WorldModel model,
            CatalogManager catalogManager, 
            String breadCrumbSeperator,
            Dimension breadCrumbSize, 
            Color backgroundColor, 
            Color selectedColor, 
            Color highlightColor) {

        this(
                model,
                catalogManager, 
                null, 
                breadCrumbSeperator, 
                breadCrumbSize, 
                null, 
                backgroundColor, 
                selectedColor, 
                highlightColor, 
                3, 
                new Dimension(80, 80), 
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

        // null is passed when the tool is deactivated
        if (tool == null){
        
            // Don't do any other processing except deselect tool
        	ViewManager.getViewManager().setTool(null);
        	
            if (currentToolPanel != null) {
                currentToolPanel.setTool(null);                
            }
            return;
        } 

        // only update if the group is in the catalog
        //if (catalogManager.findTool(tool.getToolID()) != null) {

            // and only if it is in the branch being displayed
            ToolGroup tg = (ToolGroup)((ToolGroupChild)tool).getParent();

            currentTool = tool;
            if (checkBranch(tg)) {
                expandNode(currentTool);
            } else if (currentToolPanel != null){
                    currentToolPanel.setTool(null);
            }
        //}

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
    	contentPanel = new JPanel();
    	contentPanel.setBackground(backgroundColor);
        
    	contentPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.BOTH;
        
        flowLayout = new FlowLayout(FlowLayout.LEFT, 2, 2);
        crumbPanel.setLayout(flowLayout);
                
        originalHeight = 21;
        breadCrumbSize.height = originalHeight;
        
        crumbPanel.setPreferredSize(breadCrumbSize);
        crumbPanel.setMaximumSize(breadCrumbSize);
        crumbPanel.setMinimumSize(breadCrumbSize);
        crumbPanel.setBackground(backgroundColor);

        contentPanel.add(crumbPanel, c);
        
        c.gridy++;
        contentPanel.add(displayPanel, c);
        
        return contentPanel;
    }
    
    FlowLayout flowLayout;
    int originalHeight;

    //----------------------------------------------------------
    // Methods defined by ActionListener
    //----------------------------------------------------------
    /**
     * Process an action event from one of the icon buttons.
     *
     * @param e The event that caused this method to be called
     */
    public void actionPerformed(ActionEvent e) {

        String toolName = e.getActionCommand();

        if (toolName != null) {
            Tool tool = catalogManager.findTool(toolName);
            viewManager.setTool(tool);
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

        // notify the current tool panel
        if (currentToolPanel != null)
            currentToolPanel.setMaximum(totalNeeded);
        
        // disable navigation
        navigationEnabled = false;
                
    }
    
    /** 
     * Set the current increment
     *  
     * @param value the number of items that have loaded
     */
    public void incrementValue(int value) {
                    
        // notify the current tool panel
        if (currentToolPanel != null) {
            currentToolPanel.incrementValue(value);            
        }

    }
    
	/** 
	 * Indicate that progress is done.
	 */
    public void progressComplete() {
        
        // notify the current tool panel
        if (currentToolPanel != null)
            currentToolPanel.progressComplete();
        
        // enable navigation
        navigationEnabled = true;
        
   }

    //----------------------------------------------------------
    // Methods defined by CatalogListener
    //----------------------------------------------------------
    /**
     * A tool group has been added. Batched adds will come through the
     * toolsAdded method.
     *
     * @param name The catalog name
     * @param group The toolGroup added to
     */
    public void toolGroupAdded(String name, ToolGroup group) {
        if (catalogName == null) {
            return;
        }
        if (catalogName.equals(name)) {

            if (expandedToolGroupNode == null) {

                // use the passed group just in case
                rootGroup = group;

                // lets look for the actual root
                traverseGroups(group);

                // now only load from the root group
                expandNode(rootGroup);

            } else if (group.equals(expandedToolGroupNode)) {
                // redisplay that toolgroup node
                expandNode(group);
            } else if (group.getName().equals(rootGroupName)) {
                // change to that toolgroup node
                expandNode(group);
            }

            group.addToolGroupListener(this);

        }
    }

    /**
     * A group of tool groups have been added.
     *
     * @param name The catalog name
     * @param groups The list of tool groups added
     */
    public void toolGroupsAdded(String name, List<ToolGroup> groups) {
        if (catalogName == null) {
            return;
        }
        if (catalogName.equals(name)) {
            //just redo the node
            /* Depending on how big this list is, the searching for a
             * exact match could take longer than redisplaying this node
             */
            expandNode(expandedToolGroupNode);
        }
    }

    /**
     * A tool has been removed.  Batched removes will come through the
     * toolsRemoved method.
     *
     * @param name The catalog name
     * @param group The toolGroup removed from
     */
    public void toolGroupRemoved(String name, ToolGroup group) {
        if (!name.equals(catalogName)) {
            return;
        }
        ToolGroupChild tgc = expandedToolGroupNode;
        if (tgc != null) {
            // Is the toolgroup that was removed a parent of the currently
            // expanded node?
            do {
                if (group.equals(tgc)) {
                    expandNode(group);
                }
            } while ((tgc = tgc.getParent()) != null);

            group.removeToolGroupListener(this);

        }
    }

    /**
     * A group of tool groups have been removed.
     *
     * @param name The catalog name
     * @param groups The list of tool groups that have been removed
     */
    public void toolGroupsRemoved(String name, List<ToolGroup> groups) {
        if (!name.equals(catalogName)) {
            return;
        }
        ToolGroupChild tgc = expandedToolGroupNode;
        if (tgc != null) {
            for (ToolGroup group : groups) {
                do {
                    if (group.equals(tgc)) {
                        expandNode(group);
                    }
                } while ((tgc = tgc.getParent()) != null);
            }
        // Are any of the removed toolgroups a parent of the current node?
        }
    }

    //----------------------------------------------------------
    // Methods defined by ToolGroupListener
    //----------------------------------------------------------

    /**
     * A tool has been added.  Batched additions will come through
     * the toolsAdded method.
     * <br>
     * EMF - common stack trace looks something like this: <ul><li>
     * org.chefx3d.toolbar.awt.DrillDownToolbar.toolAdded( right here )</li><li>
     * org.chefx3d.tool.ToolGroupListenerMulticaster.toolAdded(ToolGroupListenerMulticaster.java:207)</li><li>
     * org.chefx3d.tool.ToolGroupListenerMulticaster.toolAdded(ToolGroupListenerMulticaster.java:198)</li><li>
     * org.chefx3d.tool.ToolGroup.fireToolAdded(ToolGroup.java:601)</li><li>
     * org.chefx3d.tool.ToolGroup.addTool(ToolGroup.java:461)</li><li>
     * com.yumetech.chefx3d.io.CatalogParser.createTool(CatalogParser.java:817)</li><li>
     * com.yumetech.chefx3d.io.CatalogParser.productIDsLoaded(CatalogParser.java:324)</li></ul>
	 *
     * @param evt The event that caused this method to be called
     */
    public void toolAdded(ToolGroupEvent evt) {
        ToolGroupChild tool = (ToolGroupChild)evt.getChild();
        ToolGroupChild parent = tool.getParent();
       
        if (parent instanceof ToolSwitch) {
            return;
        }

        ToolGroupIconPanel iconPanel = flatPanelCache.get(parent);
        
        if (iconPanel != null) {
            iconPanel.addToolButton(tool);
        }
    }

    /**
     * A tool group has been added. Batched adds will come through the
     * toolsAdded method.
     *<br>
     * EMF - common stack trace looks something like this: <ul><li>
     * org.chefx3d.toolbar.awt.DrillDownToolbar.toolGroupAdded( you're lookin' at it)</li><li>
     * org.chefx3d.tool.ToolGroupListenerMulticaster.toolGroupAdded(ToolGroupListenerMulticaster.java:284)</li><li>
     * org.chefx3d.tool.ToolGroupListenerMulticaster.toolGroupAdded(ToolGroupListenerMulticaster.java:275)</li><li>
     * org.chefx3d.tool.ToolGroup.fireToolGroupAdded(ToolGroup.java:648)</li><li>
     * org.chefx3d.tool.ToolGroup.addToolGroup(ToolGroup.java:497)</li><li>
     * com.yumetech.chefx3d.io.CatalogParser.categoryLoaded(CatalogParser.java:266)</li><li>
     * com.yumetech.chefx3d.io.LoadCategoryHandler.run(LoadCategoryHandler.java:91)</li></ul>
     *  
     * @param evt The event that caused this method to be called
     */
    public void toolGroupAdded(ToolGroupEvent evt) {
        ToolGroup group = (ToolGroup)evt.getChild();
        group.addToolGroupListener(this);

        ToolGroupChild parent = group.getParent();

        ToolGroupIconPanel iconPanel = flatPanelCache.get(parent);
        if (iconPanel != null) {
            iconPanel.addToolGroupButton(group);
        }
    }

    
    /**
     * A tool has been updated.
     * <br>
     * EMF - common stack trace looks something like this: <ul><li>
     * org.chefx3d.toolbar.awt.DrillDownToolbar.toolUpdated( right here )</li><li>
     * org.chefx3d.tool.ToolGroupListenerMulticaster.toolUpdated(ToolGroupListenerMulticaster.java:258)</li><li>
     * org.chefx3d.tool.ToolGroup.fireToolUpdated(ToolGroup.java:695)</li><li>
     * org.chefx3d.tool.ToolGroup.addTool(ToolGroup.java:454)</li><li>
     * com.yumetech.chefx3d.io.CatalogParser.updateTool(CatalogParser.java:836)</li><li>
     * com.yumetech.chefx3d.io.CatalogParser.productLoaded(CatalogParser.java:417)</li><li>
     * com.yumetech.chefx3d.io.LoadProductHandler.run(LoadProductHandler.java:86)</li></ul>
     *
     * @param evt The event that caused this method to be called
     */
    public void toolUpdated(ToolGroupEvent evt) {    
        ToolGroupChild tool = (ToolGroupChild)evt.getChild();
        ToolGroupChild parent = tool.getParent();

        ToolGroupIconPanel iconPanel = flatPanelCache.get(parent);
        
        if (iconPanel != null) {
            iconPanel.updateToolButton(tool);
        }

    }
    
    
    /**
     * A tool has been removed. Batched removes will come through the
     * toolsRemoved method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolRemoved(ToolGroupEvent evt) {        
        // handle removing tool switch place holders
        
        ToolGroupChild tool = (ToolGroupChild)evt.getChild();
        ToolGroupChild parent = tool.getParent();
       
        ToolGroupIconPanel iconPanel = flatPanelCache.get(parent);
        
        if (iconPanel != null) {
            iconPanel.removeToolButton(tool);
        }

    }

    /**
     * A tool has been removed.  Batched removes will come through the
     * toolsRemoved method.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolGroupRemoved(ToolGroupEvent evt) {
        // not implemented
    }


    /**
     * A tool has been updated.
     *
     * @param evt The event that caused this method to be called
     */
    public void toolGroupUpdated(ToolGroupEvent evt) {
        // not implemented
    }

    // ----------------------------------------------------------
    // Methods defined by CatalogManagerListener
    // ----------------------------------------------------------

    /**
     * A catalog has been added.
     *
     * @param catalog
     */
    public void catalogAdded(Catalog catalog) {
        if (catalogName == null) {
            return;
        }

        if (catalogName.equals(catalog.getName())) {
            catalog.addCatalogListener(this);
        }
    }

    /**
     * A catalog has been removed.
     *
     * @param catalog
     */
    public void catalogRemoved(Catalog catalog) {
        if (catalogName == null) {
            return;
        }

        // If the catalog that we're watching is removed, clean out the
        // outlookToolbar.
        if (catalogName.equals(catalog.getName())) {
            displayPanel.removeAll();

            catalog.removeCatalogListener(this);
            flatPanelCache.clear();
        }
    }

    // ----------------------------------------------------------
    // Methods defined by ExpandableToolbar
    // ----------------------------------------------------------

    /**
     * Open the catalog to the group provided
     *
     * @param toolGroup
     */
    public synchronized void expandNode(Tool tool) {

        // if the tool is already displayed in the current panel then
        // don't try to load a different group
        if (currentToolPanel != null && tool != null && 
                currentToolPanel.getButton(tool.getToolID()) != null) {
        	return;
        }


        ToolGroup tg = null;
        if (tool instanceof ToolSwitch) {
            return;
        } else if (tool instanceof ToolGroup) {
            tg = (ToolGroup)tool;
        } else if (tool instanceof Tool) {
            tg = (ToolGroup)((ToolGroupChild)tool).getParent();
        }
        
        //
        // Do not refresh the flatPanel if a group has not finished
        // loading
        //
        if ( activeToolGroup == tg.getToolID() ){
        	return;
        }

        boolean inBranch = checkBranch(tg);
        
        if (inBranch && tg != null) {
            this.expandedToolGroupNode = tg;
            
            // get the new panel
            currentToolPanel = (flatPanelCache.get(expandedToolGroupNode));

            if (currentToolPanel == null) {
                // add the new panel
                currentToolPanel =
                    new ToolGroupIconPanel(
                            model, 
                            expandedToolGroupNode, 
                            this,
                            catalogFilter, 
                            backgroundColor, 
                            selectedColor, 
                            highlightColor, 
                            columns, 
                            iconSize, 
                            displayHoverover);
                
                flatPanelCache.put(expandedToolGroupNode, currentToolPanel);
            } else {
                currentToolPanel.resetRollover();
            }
            
            if (currentToolPanel != null) {
	            
	            // update the current active tool group
	            activeToolGroup = tg.getToolID();

                // add new contents
                if (currentToolPanel != null) {
                	     	            
                	try {
                    	// clean up the old contents
                    	displayPanel.removeAll();                           

                    	// update the visible panel
        	            displayPanel.add(currentToolPanel, 0);               	            	  
                	} catch (java.lang.IndexOutOfBoundsException oex) {
                		// try again
                		expandNode(tool);
                	}
    	            
    	            displayPanel.repaint();
                }
                
	            // update the bread crumb trail
	            updateBreadCrumbPath(); 
	            
	            // update the filter
	            catalogFilter.setCurrentToolGroup(model, tg, currentToolPanel);  
	            
            }
        }
                      
    }
    
    /**
     * Generate the panel to hold the buttons
     * 
     * @param tool
     */
    public void generatePanel(Tool tool) {
        
        ToolGroup tg = null;
        if (tool instanceof ToolGroup) {
            tg = (ToolGroup)tool;
        } else if (tool instanceof Tool) {
            tg = (ToolGroup)((ToolGroupChild)tool).getParent();
        }
        
        currentToolPanel = (flatPanelCache.get(tg));
        if (currentToolPanel == null) {
            // add the new panel
            currentToolPanel =
                new ToolGroupIconPanel(
                        model, 
                        tg, 
                        this,
                        catalogFilter, 
                        backgroundColor, 
                        selectedColor, 
                        highlightColor, 
                        columns, 
                        iconSize, 
                        displayHoverover);
            
            flatPanelCache.put(tg, currentToolPanel);
        } 
        
        // update the current active tool group
        // -no longer here, but rather in the expandNode method.
        //
        // that way, we know not to refresh when clicking
        // the currently-selected group, which can interrupt
        // the display of a loading group.
        //
        // activeToolGroup = tg.getToolID();
        

    }
    
    /**
     * Set the current group tool ID.
     * 
     * @param toolID
     */
    public void setActiveGroupID(String toolID) {
        activeToolGroup = toolID;
    }
    
    /**
     * Get the current active tool group ID.
     * 
     * @return
     */
    public String getActiveGroupID() {
        return activeToolGroup;
    }
    
    // ----------------------------------------------------------
    // Local Methods
    // ----------------------------------------------------------

    /**
     * Change which catalog that this outlookToolbar should be watching for changes
     * on.
     *
     * @param catalogName The name of the catalog to watch.
     */
    public void setCatalog(String catalogName) {
        this.catalogName = catalogName;
    }

    /**
     * Recurse through the tool tree to find the root group
     * to use.
     *
     * @param group The group to check
     */
    private void traverseGroups(ToolGroup group) {

        List<ToolGroup> children = group.getToolGroups();
        int len = children.size();
        for (int i = 0; i < len; i++) {

            ToolGroup child = children.get(i);
            String groupName = child.getName();
            if (groupName.equals(rootGroupName)) {
                rootGroup = child;
                return;
            }

            traverseGroups(child);

        }

    }

    /**
     * Check each parent until you find the root group name.
     *
     * @param group The group to check
     * @return true if the group is under the root group, false otherwise
     */
    private boolean checkBranch(ToolGroupChild group) {

        if (group == null)
            return false;
        
        String groupName = group.getName();
        if (groupName.equals(rootGroupName)) {
            return true;
        }      

        ToolGroupChild parent = group.getParent();

        if (parent == null)
            return false;

        groupName = parent.getName();
        if (groupName.equals(rootGroupName)) {
            return true;
        }
        return checkBranch(parent);

    }

    /**
     * Sets the active tool group
     *
     * @param tgc The ToolGroupChild to activate
     */
    private void setToolGroupActive(ToolGroupChild tgc) {
        // If it is a tool, ignore it, since the breadcrumb display is kept
        // in sync with the current active tool via code on the toolbar side
    	setTool(null);

        if (tgc instanceof ToolSwitch) {
            // don't expand switches
        } else if (tgc instanceof ToolGroup) {
        	expandNode((ToolGroup) tgc);
        } else {
        	expandNode((SimpleTool) tgc);
        }
    }

    /**
     * Rebuild the bread crumb trail based on the current group expanded
     */
    private void updateBreadCrumbPath() {
    	
    	
        List<JComponent> components = new ArrayList<JComponent>();
        crumbPanel.removeAll();

        // Add toolgroups/tool path breadcrumbs
        ToolGroup tg = expandedToolGroupNode;
        if (tg != null) {

            do {
                components.add(createBreadCrumb(tg));
                components.add(createCrumbSepComp());

                // for some reason I cannot be the while statement
                // to work right so this breaks the loop  when
                // necessary.
                if (tg.getName().equals(rootGroupName)) {
                    break;
                }

            } while ((tg = (ToolGroup) tg.getParent()) != null);
        }

        // Add to Jpanel in reverse order so they show up the proper way
        for (int i = components.size() - 2; i >= 0; i--) {
            JComponent comp = components.get(i);
            crumbPanel.add(comp);
            
        }
        
        Dimension actual = crumbPanel.getSize();

        //
        // EMF: see VDSTv1.5_TechDesign_20100727.odt,
        // Breadcrumb Trail Improvements (1.5.1.14).
        // Scale the height of the breadCrumbSize dimension
        // so there is enough room for all the text.  
        // Note:
        // There are rare cases where specific ratios of pref width 
        // to actual width may result in a line being 'hidden' because 
        // there is not enough room.  If this occurs with frequency, simply
        // add a +1 to the required 'rows'.  
        //
        if( actual.width > 0){
        
        	Dimension pref = flowLayout.preferredLayoutSize(crumbPanel);
                
        	int rows = (int) Math.ceil((double)pref.width / (double)actual.width );
        	if( rows >= 1){
        		
        		breadCrumbSize.height = originalHeight * rows;
        		crumbPanel.setPreferredSize(breadCrumbSize);
                crumbPanel.setMaximumSize(breadCrumbSize);
                crumbPanel.setMinimumSize(breadCrumbSize);
        		
        	}
        }

        crumbPanel.revalidate();
        crumbPanel.repaint();
    }

    /**
     * Create the separator
     *
     * @return JLabel of the separator
     */
    private JComponent createCrumbSepComp() {
        JLabel sep = new JLabel(breadCrumbSeperator);
        sep.setForeground(FontColorUtils.getForegroundColor());
        sep.setFont(FontColorUtils.getSmallFont());
        return sep;
    }

    /**
     * Create a bread crumb trail button
     *
     * @param tg The group being represented by the button
     * @return The button created
     */
    private JComponent createBreadCrumb(ToolGroupChild tg) {
    	String name = tg.getName();
        JButton button = new JButton(
        		"<html>" +
        			"<div bgcolor='#" + bgColorString + "'>" +
        				"<u>" + name + "</u>" +
        			"</div>" +
        		"</html>");
        button.setName(name);
        button.setToolTipText("Back to " + name);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        button.setContentAreaFilled(false);
        button.setForeground(FontColorUtils.getForegroundColor());
        button.setFont(FontColorUtils.getSmallFont());
        
        button.addMouseListener(new ToolGroupChildCrumbMouseAdapter(this, tg));
        return button;
    }

    //------------------------------------------------------------------------
    // Customized Mouse Adapter for ToolGroupChild
    //------------------------------------------------------------------------
    private class ToolGroupChildCrumbMouseAdapter extends MouseAdapter {

        //ToolGroupChild tgc = null;
        Reference<ToolGroupChild> tgcRef = null;
        Reference<DrillDownToolbar> bctRef = null;

        public ToolGroupChildCrumbMouseAdapter(DrillDownToolbar bct, ToolGroupChild tgc) {
            this.tgcRef = new WeakReference<ToolGroupChild>(tgc);
            this.bctRef = new WeakReference<DrillDownToolbar>(bct);
        }

        public void mouseClicked(MouseEvent event) {
            
            if (navigationEnabled) {
            
                DrillDownToolbar bct = bctRef.get();
                ToolGroupChild tgc = tgcRef.get();
                if (bct != null && tgc != null) {
                    bct.setToolGroupActive(tgc);
                }
            }           
        }
    }
}
