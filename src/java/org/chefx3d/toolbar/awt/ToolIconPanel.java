/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2006
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

//External Imports
import java.awt.LayoutManager;
import java.lang.ref.Reference;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import javax.swing.JPanel;

//Local Imports

/**
 * Raw JPanel implementation for the purpose of providing catalog filter access
 * to button objects.
 *
 * @author Ben Yarger
 * @version $Revision: 1.5 $
 */
public class ToolIconPanel extends JPanel {

    /** Map from the toolID to the corresponding button */
    protected HashMap<String, JToolButton> toolIdToButtonMap;
    
    /** An ordered list of the buttons by name */
    protected List<String> orderedButtonList;
	
	/**
	 * Creates a new JPanel with a double buffer and a flow layout.
	 */
	public ToolIconPanel(){
		
	}
	
	/**
	 * Creates a new JPanel with FlowLayout and the specified buffering strategy.
	 * 
	 * @param isDoubleBuffered boolean
	 */
	public ToolIconPanel(boolean isDoubleBuffered){
		super(isDoubleBuffered);
	}

	/**
	 * Create a new buffered JPanel with the specified layout manager
	 * 
	 * @param layout LayoutManager
	 */
    public ToolIconPanel(LayoutManager layout){
    	super(layout);
    }

    /**
     * Creates a new JPanel with the specified layout manager and buffering 
     * strategy.
     * 
     * @param layout LayoutManager
     * @param isDoubleBuffered boolean
     */
	public ToolIconPanel(LayoutManager layout, boolean isDoubleBuffered){
		super(layout, isDoubleBuffered);
	}
	
	/**
	 * Lookup the button by name
	 * 
	 * @param name String name
	 * @return JToolButton
	 */
	public JToolButton getButton(String id){
		
		JToolButton button = toolIdToButtonMap.get(id);
		return button;
		
	}
}
