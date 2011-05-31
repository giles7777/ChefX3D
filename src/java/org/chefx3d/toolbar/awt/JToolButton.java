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
import javax.swing.JToggleButton;

// Internal Imports

/**
 * Extends JToggleButton for tracking loaded state.   Side 
 * pockets the enable state until the icons and data has 
 * been completely loaded.
 * 
 * @author Russell Dodds
 * @version $Revision: 1.2 $
 */
class JToolButton extends JToggleButton {
    
    /** Should the be active once its loaded */
    private boolean active;
    
    /** Is the item's data loaded */
    private boolean isLoaded;
    
    /** TRUE if this button represents a ToolGroup,
     * FALSE if this button represents a single tool   */
    private boolean isToolGroup;
    
    /**
     * Create a basic JToggleButton
     */
    public JToolButton() {
        super();
        init();       
    }

    /**
     * Create a basic JToggleButton with the text set
     * 
     * @param text Text to set
     */
    public JToolButton(String text, boolean isGroup) {
        super(text);
        
        isToolGroup = isGroup;
        init();
    }

    // ----------------------------------------------------------
    // Overridden Methods
    // ----------------------------------------------------------

    /**
     * Enable/disable button.  Only does so if the item's data 
     * has been loaded.  Otherwise it sets the active state.
     * 
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        
        if (isLoaded) {
            super.setEnabled(enabled);
        } else {
            active = enabled;
        }
        
    }
    
    // ----------------------------------------------------------
    // Local Methods
    // ---------------------------------------------------------- 
    
    private void init() {
    	isLoaded = false;
        active = false;        
    }
    
    /**
     * Is the item's data loaded
     * 
     * @return True if loaded, false otherwise
     */
    public boolean isLoaded() {
        return isLoaded;
    }
    
    /**
     * Sets the item's data loaded state.  The calls the setEnabled
     * 
     * @param isLoaded
     */
    public void setLoaded(boolean isLoaded) {
        this.isLoaded = isLoaded;        
        setEnabled(active);        
    }
    
    /**
     * @return TRUE if this button represents a ToolGroup,
     * FALSE if this button represents a single tool.
     */
    public boolean isToolGroup(){
    	return isToolGroup;
    }

}
