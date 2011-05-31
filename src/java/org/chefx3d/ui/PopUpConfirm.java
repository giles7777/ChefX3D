/*****************************************************************************
 *                        Web3d.org Copyright (c) 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.ui;

// External import
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import org.chefx3d.util.FontColorUtils;
import org.j3d.util.I18nManager;

// Local import

/**
 * Confirmation message box that show short messages to the user with Ok and
 * cancel buttons to control future action. Confirmation box is non-modal.
 * 
 * @author Ben Yarger
 * @version $Revision: 1.8 $
 */
public class PopUpConfirm extends JOptionPane {

	/** The class instance */
	private static PopUpConfirm popUpConfirm = new PopUpConfirm();
	
	/** TextArea where message is displayed */
	protected JTextArea messageText;
	
	/** Scroll pane holding the text */
	protected JScrollPane scrollPane;
	
	/** Parent Component used to set location relative to */
	protected Component parentWidget;
		
	/** protected const for preferred JTextArea width */
	protected static final int TEXT_PREFERRED_WIDTH = 300;
	
	/** protected const for minimum JTextArea width */
	protected static final int TEXT_MINIMUM_WIDTH = 300;
	
	/** protected const for preferred JTextArea height */
	protected static final int TEXT_PREFERRED_HEIGHT = 150;
	
	/** Protected const for minimum JTextArea height */
	protected static final int TEXT_MINIMUM_HEIGHT = 20;
	
	protected static final Font DEFAULT_FONT = 
        new Font("Tahoma", Font.PLAIN, 10);

	/** Translation utility */
	I18nManager intl_mgr;
	
	/** Ready status message */
	private static final String TITLE_PROP = 
		"org.chefx3d.messaging.PopUpConfirm.title";
		
	/** OK button text */
	private static final String OK_BUTTON_PROP = 
		"org.chefx3d.messaging.PopUpConfirm.okButton";
	
	/** Yes button text */
	private static final String YES_BUTTON_PROP = 
		"org.chefx3d.messaging.PopUpConfirm.yesButton";
	
    /** No button text */
    private static final String NO_BUTTON_PROP = 
        "org.chefx3d.messaging.PopUpConfirm.noButton";
    
    /** Higher level user override, prevents messages from being displayed */
    private static boolean displayPopUpConfirm = true;
    
	/** Flag for confirm or cancel response */
	private boolean confirmed;
    
	/** Custom button names */
    protected String[] buttonNames;
    
    private int buttonCount;
    
    /**
	 * Constructor
	 */
	private PopUpConfirm(){
		
		parentWidget = null;
		intl_mgr = I18nManager.getManager();
		init();
	}
	
	/**
	 * Instance accessor
	 * 
	 * @return PopUpConfirm instance
	 */
	public static PopUpConfirm getInstance(){
		return popUpConfirm;
	}
	
	//---------------------------------------------------------------
    // Local Methods 
    //---------------------------------------------------------------

	   /**
     * Set the parent component used as a location position reference for 
     * the dialog box. If null, dialog will be placed in top left corner
     * of screen.
     * 
     * @param comp Component
     */
    public void setParentComponent(Component comp){
        parentWidget = comp;
    }
    
    /**
     * Initialize the GUI widgets
     */
    public void init(){
        
        // Setup dialog        
        BorderLayout layout = new BorderLayout();
        layout.setHgap(8);
        layout.setVgap(10);
        
        setLayout(layout);
                        
        // Text
        messageText = new JTextArea();
        messageText.setEditable(false);
        messageText.setWrapStyleWord(true);
        messageText.setLineWrap(true);
        messageText.setBackground(SystemColor.control);
        messageText.setFont(FontColorUtils.getMediumFont());
        
        scrollPane = new JScrollPane(messageText);
        scrollPane.setHorizontalScrollBarPolicy(
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        scrollPane.setMinimumSize(
                new Dimension(TEXT_MINIMUM_WIDTH, TEXT_MINIMUM_HEIGHT));
        
        scrollPane.setPreferredSize(
                new Dimension(TEXT_PREFERRED_WIDTH, TEXT_PREFERRED_HEIGHT));
        
        scrollPane.setBorder(new EmptyBorder(5, 5, 5, 5));

        buttonCount = 2;
        	
	    buttonNames = new String[3];
	    
        // OK button
        buttonNames[0] = intl_mgr.getString(OK_BUTTON_PROP);;

        // Yes button
        buttonNames[1] = intl_mgr.getString(YES_BUTTON_PROP);
        
        // No button
        buttonNames[2] =  intl_mgr.getString(NO_BUTTON_PROP);

    }
    
    /**
     * Allows the parent widget to be temporarily changed for this call.
     * If the parent widget should always be set to the one passed in, use
     * setParentComponent().
     * 
     * @param msg String message to display
     * @param parentWidget Component to use as parent for this message
     * @return True if yes/ok, false otherwise
     */
    public boolean showMessage(String msg, Component parentWidget){
    	
    	Component originalParent = this.parentWidget;
    	this.parentWidget = parentWidget;
    	
    	boolean result = showMessage(msg);
    	
    	this.parentWidget = originalParent;
    	
    	return result;
    }
    
    /**
     * Shows the confirmation dialog with the new message and ok, cancel 
     * buttons. Returns with true if user confirmed, or false if user canceled.
     * 
     * @param msg String to display
     * @return True if confirmed, false if canceled.
     */
    public boolean showMessage(String msg){
        
        if(!displayPopUpConfirm)
            return confirmed;

        messageText.setText(msg);
        String title = intl_mgr.getString(TITLE_PROP);

        String[] buttonText;
        if (buttonCount == 1) {
        	buttonText = new String[] {buttonNames[0]};
        } else {
        	buttonText = new String[] {buttonNames[1], buttonNames[2]};
        }
        
        int answer = JOptionPane.showOptionDialog(
        		parentWidget,
            	scrollPane,
            	title,
            	buttonCount,
            	JOptionPane.PLAIN_MESSAGE,
            	null,
            	buttonText,
            	buttonText[0]);

        if(answer == JOptionPane.YES_OPTION){
        	confirmed = true;
        } else {
        	confirmed = false;
        }
        
        return confirmed;
    }
    
    /**
     * A flag set to say if the pop up should be displayed or not
     * @param state boolean True display messages, false does not
     */
    public void setDisplayPopUp(boolean state) {
        displayPopUpConfirm = state;
    }
    
    //TODO: This should be removed when we have this running on proper thread
    public void setConfirmedFlag(boolean state) {
        confirmed = state;
    }
    
    /**
     * 1 will display only an OK button.
     * 2 will display Save and Cancel buttons.
     * 
     * @param buttonCount
     */
    public void setButtonCount(int buttonCount) {
    	this.buttonCount = buttonCount;
    }

}
