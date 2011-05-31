/*****************************************************************************
 *                        Yumetech, Inc Copyright (c) 2007
 *                               Java Source
 *
 * This source is licensed under the BSD license.
 * Please read docs/BSD.txt for the text of the license.
 *
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.actions.awt;

// External imports
import java.awt.Frame;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.chefx3d.model.Command;
import org.chefx3d.model.CommandController;
import org.chefx3d.model.CommandListener;
import org.chefx3d.view.ViewManager;
import org.j3d.util.I18nManager;


// Local imports
// None

/**
 * An action that can be used to exit the system.
 * <p>
 *
 * <b>Internationalisation Resource Names</b>
 * <ul>
 * <li>title: The name that appears on the action when no icon given</li>
 * <li>description: The short description to go with the action (eg tooltip)</li>
 * </ul>
 *
 * @author Alan Hudson, Eric Fickenscher
 * @version $Revision: 1.9 $
 */
public class ExitAction extends AbstractAction implements CommandListener {
	
	/** The base name for internationalisation text pertinent to this class */
    private static final String I18N_BASE = "org.chefx3d.actions.awt.ExitAction.";
        
    /** The base frame over which the warning box will appear */
    private Frame frame;
    
    /** Title for the dialogue warning box */
    private String title;
    
    /** Text of the dialogue warning box */
    private String message;
    
    /** Singleton instance */
    private static ExitAction exitAction;
    
    /** Has the scene been saved?  FALSE if commands have recently occured */
    private boolean sceneHasBeenSaved;
        
    /**
     * Create an instance of the action class.
     */
    public static ExitAction getInstance(CommandController controller, 
    									 Frame frame){
    	if( exitAction == null)
    		exitAction = new ExitAction(controller, frame);
    	return exitAction;
    }
    
    /**
     * Private constructor.
     * @param controller The Command Controller, manages the commands 
     * in/out of the data model
     * @param frameParam
     */
    private ExitAction(	CommandController controller,
    					Frame frameParam) {

        controller.addCommandHistoryListener(this);
        frame = frameParam;
        sceneHasBeenSaved = false;
        I18nManager intl_mgr = I18nManager.getManager();

        KeyStroke acc_key = KeyStroke.getKeyStroke(KeyEvent.VK_Q,
                                                   KeyEvent.CTRL_MASK);

        putValue(Action.NAME, intl_mgr.getString(I18N_BASE + "title"));
        putValue(SHORT_DESCRIPTION, intl_mgr.getString(I18N_BASE + "description"));
        putValue(ACCELERATOR_KEY, acc_key);
        putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_X));
        
        title = intl_mgr.getString(I18N_BASE + "warning.title");
        message = intl_mgr.getString(I18N_BASE + "warning.text");
    }
    

    //  -----------------------------------------------------------------------
    //  Methods defined by CommandListener
    //  -----------------------------------------------------------------------
    
    /**
     * A command was successfully executed
     * 
     * @param cmd The command
     */
    public void commandExecuted(Command cmd){
    	sceneHasBeenSaved = false;
    }

    /**
     * A command was not successfully executed
     * 
     * @param cmd The command
     */
    public void commandFailed(Command cmd){
        // ignored.
    }

    /**
     * A command was successfully undone
     * 
     * @param cmd The command
     */
    public void commandUndone(Command cmd){
    	// ignored.
    }

    /**
     * A command was successfully redone
     * 
     * @param cmd The command
     */
    public void commandRedone(Command cmd){
    	// ignored
    }

    /**
     * The command stack was cleared
     */
    public void commandCleared(){
    	// ignored
    }
    
    //  -----------------------------------------------------------------------
    //  Local Methods
    //  -----------------------------------------------------------------------

    /**
     * A Save has occured, it is safe to exit without a warning.
     */
    public void sceneHasBeenSaved(){
    	sceneHasBeenSaved = true;
    }
    
    /**
     * An action has been performed.
     *
     * @param evt The event that caused this method to be called.
     */
    public void actionPerformed(ActionEvent evt) {
    	
    	int result = 0;

		// check to make sure this is what the user wants            
        if (!sceneHasBeenSaved) {            
            
            result = JOptionPane.showConfirmDialog(
                    frame, 
                    message,
                    title, 
                    JOptionPane.YES_NO_OPTION, 
                    JOptionPane.QUESTION_MESSAGE);  
        }
        
        if (result == 0){
        	ViewManager vM = ViewManager.getViewManager();
        	vM.shutdown();
        	System.exit(0);
        }
    }
}
