/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005 - 2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.property.awt;

// External Imports
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.*;

import javax.swing.*;

import org.w3c.dom.Node;

// Internal Imports
import org.chefx3d.model.*;
import org.chefx3d.property.PropertyEditor;
import org.chefx3d.property.PropertyTableCellFactory;
import org.chefx3d.tool.EntityBuilder;
import org.chefx3d.tool.Tool;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;
import org.chefx3d.util.FontColorUtils;
import org.chefx3d.view.ViewManager;
import org.j3d.util.I18nManager;

/**
 * A property editor.
 *
 * @author Alan Hudson
 * @version $Revision: 1.58 $
 */
public class DefaultPropertyEditor extends JScrollPane 
    implements
        PropertyEditor, 
        ModelListener, 
        PropertyStructureListener {

    /** The world model */
    private WorldModel model;

    /** The component used to center dialogs */
    protected Component frame;
 
    /** Manager of the views */
    private ViewManager viewManager;
         
    /** Are we in associateMode */
    private boolean associateMode;

    /** The ErrorReporter for messages */
    protected ErrorReporter errorReporter;

    /** The viewport used to wrap the editor panel */
    private JViewport viewPort;
    
    /** A tabbed panel for displayin vearious property sheets */
    private JTabbedPane tabbedPane;

    /** What factory to use to generate editors */
    private PropertyTableCellFactory editorCellFactory;
    
    private Set<String> tabsToShow;
    
    /** The internationalization manager */
    private I18nManager i18nMgr;
    
    /** Helper class that knows what is selected */
    private EntitySelectionHelper selectionHelper;

    /**
     * A default property editor sheet
     *
     * @param model The world model
     * @param parentFrame The frame to focus messages on
     */
    public DefaultPropertyEditor(WorldModel model, Component parentFrame) {

        this.model = model;
        this.frame = parentFrame;

        i18nMgr = I18nManager.getManager();
        
        associateMode = false;
        
        // TODO: decide how to allow the application programmer
        // to set this list
        tabsToShow = new HashSet<String>();
        tabsToShow.add(Entity.DEFAULT_ENTITY_PROPERTIES);
        //tabsToShow.add(SegmentEntity.SEGMENT_PROPERTY_SHEET);
        //tabsToShow.add(VertexEntity.VERTEX_PROPERTY_SHEET);
        
        // Create the default error reporter
        errorReporter = DefaultErrorReporter.getDefaultReporter();

        buildPropertyPanel();

        model.addModelListener(this);
        model.addPropertyStructureListener(this);        
      
        viewManager = ViewManager.getViewManager();
        viewManager.addView(this);
        
        selectionHelper = EntitySelectionHelper.getEntitySelectionHelper();
        
    }

    // ----------------------------------------------------------
    // Methods required by View
    // ----------------------------------------------------------
    
    /**
     * Meh.
     */
    public void shutdown(){
        // ignored
    }

    /**
     * Set the current tool.
     *
     * @param tool The tool
     */
    public void setTool(Tool tool) {
        // ignore
    }

    /**
     * Go into associate mode. The next mouse click will perform
     * a property update
     *
     * @param validTools A list of the valid tools. null string will be all
     *        valid. empty string will be none.
     * @param propertyGroup The grouping the property is a part of
     * @param propertyName The name of the property being associated
     */
    public void enableAssociateMode(
            String[] validTools, 
            String propertyGroup, 
            String propertyName) {
        associateMode = true;
    }

    /**
     * Exit associate mode.
     */
    public void disableAssociateMode() {
        associateMode = false;
    }

    /**
     * Get the viewID. This shall be unique per view on all systems.
     *
     * @return The unique view ID
     */
    public long getViewID() {
        // TODO: What to do here
        return -1;
    }

    /**
     * Control of the view has changed.
     *
     * @param newMode The new mode for this view
     */
    public void controlChanged(int newMode) {
        // ignore
    }

    /**
     * Set how helper objects are displayed.
     *
     * @param mode The mode
     */
    public void setHelperDisplayMode(int mode) {
        // ignore
    }

    /**
     * Get the component used to render this.
     *
     * @return The component
     */
    public JComponent getComponent() {
        return this;
    }

    /**
     * @return the entityBuilder
     */
    public EntityBuilder getEntityBuilder() {
        return null;
        // ignore
    }

    /**
     * @param entityBuilder the entityBuilder to set
     */
    public void setEntityBuilder(EntityBuilder entityBuilder) {
        // ignore
    }

    // ----------------------------------------------------------
    // Methods required by ModelListener
    // ----------------------------------------------------------

    /**
     * An entity was added.
     *
     * @param local Was this action initiated from the local UI
     * @param entity The unique entityID assigned by the view
     */
    public void entityAdded(boolean local, Entity entity) {
        // ignore
    }

    /**
     * An entity was removed.
     *
     * @param local Was this action initiated from the local UI
     * @param entity The entity to remove
     */
    public void entityRemoved(boolean local, Entity entity) {
        // ignore
    }
    
    /**
     * The master view has changed.
     *
     * @param local is the request local
     * @param viewID The view which is master
     */
    public void masterChanged(boolean local, long viewID) {
        // ignore
    }

    /**
     * User view information changed.
     *
     * @param pos The position of the user
     * @param rot The orientation of the user
     * @param fov The field of view changed(X3D Semantics)
     */
    public void viewChanged(boolean local, double[] pos, float[] rot, float fov) {
        // ignore
    }

    /**
     * The model has been reset.
     *
     * @param local Was this action initiated from the local UI
     */
    public void modelReset(boolean local) {
        updatePropertyPanel();
    }

    // ----------------------------------------------------------
    // Methods required by PropertyStructureListener
    // ----------------------------------------------------------

    /**
     * A property was added.
     *
     * @param local Was this a local change
     * @param entityID The entity which changed
     * @param propSheet The property sheet which has the property
     * @param propName The property which changed
     * @param propValue The complete value tree. It is ok to retain a reference to
     *        this.
     */
    public void propertyAdded(
            boolean local, 
            int entityID, 
            String propSheet,
            String propName, 
            Node propValue) {

        updatePropertyPanel();

    }

    /**
     * A property was removed.
     *
     * @param local Was this a local change
     * @param entityID The entity which changed
     * @param propertySheet The sheet that holds the property
     * @param propertyName The name of the property
     */
    public void propertyRemoved(
            boolean local, 
            int entityID,
            String propertySheet, 
            String propertyName) {

        updatePropertyPanel();

    }

    // ----------------------------------------------------------
    // Methods required by PropertyEditor
    // ----------------------------------------------------------

    /**
     * Register an error reporter with the CommonBrowser instance
     * so that any errors generated can be reported in a nice manner.
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        if(errorReporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    // ----------------------------------------------------------
    // Local Methods
    // ----------------------------------------------------------

    private void updatePropertyPanel() {
    
        Entity currentEntity = null;
        ArrayList<Entity> selected = selectionHelper.getSelectedList();
        
        // TODO: for now use the first entity found
        int len = selected.size();
        if (len > 0) {
            currentEntity = selected.get(0);
        }
        
        // now add content to the pane
        tabbedPane.removeAll();
        tabbedPane.revalidate();        

//System.out.println("DefaultPropertyEditor.updatePropertyPanel");
        
        if (currentEntity == null) 
            return;
        
        // get the list of sheets
        ArrayList<String> sheetList = 
            (ArrayList<String>)currentEntity.getPropertySheets();
//System.out.println("    sheetList: " + sheetList.size());
        
        // loop through list creating a tabbed panel for each
        for (int i = 0; i < sheetList.size(); i++) {
            
            String sheetName = sheetList.get(i);
//System.out.println("    tab: " + sheetName);
           
            if (tabsToShow.contains(sheetName)) {
                
                ArrayList<EntityProperty> properties = 
                    (ArrayList<EntityProperty>)currentEntity.getProperties(sheetName);         
                
                // create the property panel
                JPanel propertyPanel = new JPanel();                
                propertyPanel.setLayout(new BorderLayout());
                   
                // table model
                PropertyTableModel tableModel = new PropertyTableModel(model);
                
                // table
                PropertyTable propertyTable = new PropertyTable(model, frame);
                propertyTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);                
                propertyTable.setFont(FontColorUtils.getMediumFont());
                propertyTable.setModel(tableModel); 
                propertyTable.setEditorCellFactory(editorCellFactory);
                
                propertyPanel.add(propertyTable, BorderLayout.CENTER);
                
                Entity[] currentList = new Entity[] {currentEntity};
                
                tableModel.setEntity(currentList, properties.size());
                 
                for (int j = 0; j < properties.size(); j++) {
                    
                    EntityProperty prop = properties.get(j);
                    
                    // see if there is an i18n version of the name
                    String name = prop.propertyName;
                    String displayName;
                    try {
                        displayName = i18nMgr.getString(name);
                    } catch (Exception ex) {
                        displayName = name;
                    }
                    
                    propertyTable.setValueAt(displayName, j, 0);
                    propertyTable.setValueAt(prop, j, 1);    
                    
                    // initialize the property
                    if (prop.propertyValue instanceof AbstractProperty) {
                        ((AbstractProperty)prop.propertyValue).initialize(model, currentEntity);   
                    }
                    
                }
                tableModel.fireTableDataChanged();         
                tableModel.applyCommand = true;
     
                //Create the scroll pane and add the table to it.
                JScrollPane scrollPane = 
                    new JScrollPane(propertyTable);
                Dimension size = new Dimension(285, 600);
                scrollPane.setPreferredSize(size);
                scrollPane.setMaximumSize(size);
                scrollPane.setMinimumSize(size);
                
                //Add the scroll pane to this panel.
                propertyPanel.add(scrollPane, BorderLayout.CENTER);                        

                
                //Add the scroll pane to this panel.            
                tabbedPane.add(sheetName, propertyPanel);  
                
            }

        }                  

    }
    
    /**
     * Create the panel from the currently selected entity.
     *
     */
    private void buildPropertyPanel() {
      
        // get the view port to add content
        viewPort = this.getViewport();
        tabbedPane = new JTabbedPane();  
        
        // now add content to the pane
        viewPort.add(tabbedPane);
        viewPort.revalidate();        

    }

    /**
     * Returns the PropertyTableCellFactory that is currently assigned 
     * to the PropertyTable.  This factory defines what is rendered within
     * the JTable
     *  
     * @return the editorCellFactory
     */
    public PropertyTableCellFactory getEditorCellFactory() {
        return editorCellFactory;
    }

    /**
     * Sets the PropertyTableCellFactory that is currently assigned 
     * to the PropertyTable.  This factory defines what is rendered within
     * the JTable
     * 
     * @param editorCellFactory the editorCellFactory to set
     */
    public void setEditorCellFactory(PropertyTableCellFactory editorCellFactory) {
        this.editorCellFactory = editorCellFactory;
    }

}
