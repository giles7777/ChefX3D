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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import org.chefx3d.util.FontColorUtils;
import org.j3d.util.I18nManager;

/**
 * Message box that show short messages to the user with a check box 
 * option to never show that message again. Message box is modal.
 * 
 * @author Ben Yarger
 * @version $Revision: 1.3 $
 */
public class PopUpMessage extends JDialog implements ActionListener{
	
	/** Static class instance */
	private static PopUpMessage popUpMessage = new PopUpMessage();

	/** Check box - when set does not show message again */
	protected JCheckBox showAgain;
	
	/** TextArea where message is displayed */
	protected JTextArea messageText;
	
	/** Panel that holds the checkbox and ok button */
	protected JPanel buttonPanel;
	
	/** Ok button to close dialog */
	protected JButton okButton;
	
	/** Map of statements and the boolean display value */
	protected HashMap<String, Boolean> showAgainMap;
	
	/** Parent Component used to set location relative to */
	protected Component parentWidget;
	
	/** Higher level user override, prevents messages from being displayed */
	private boolean blockPopUps;
	
    /** Prevents messages from being displayed and stacks them together */
    private boolean storePopUps;
	
	
	/** protected const for preferred JTextArea width */
	protected static final int TEXT_PREFERRED_WIDTH = 300;
	
	/** protected const for minimum JTextArea width */
	protected static final int TEXT_MINIMUM_WIDTH = 300;
	
	/** protected const for preferred JTextArea height */
	protected static final int TEXT_PREFERRED_HEIGHT = 150;
	
	/** Protected const for minimum JTextArea height */
	protected static final int TEXT_MINIMUM_HEIGHT = 30;
	
	/** Translation utility */
	I18nManager intl_mgr;
	
	/** Ready status message */
	private static final String TITLE_PROP = 
		"org.chefx3d.messaging.PopUpMessage.title";
	
	/** Do not show again check box text */
	private static final String DO_NOT_SHOW_PROP = 
		"org.chefx3d.messaging.PopUpMessage.doNotShow";
	
	/** OK button text */
	private static final String OK_BUTTON_PROP = 
		"org.chefx3d.messaging.PopUpMessage.okButton";
	
	/** Build up the list of messages */
	private StringBuilder compoundMessage;
		
	/**
	 * Constructor
	 */
	private PopUpMessage(){
		
		showAgainMap = new HashMap<String, Boolean>();
		parentWidget = null;
		blockPopUps = false;
		storePopUps = false;
		
		intl_mgr = I18nManager.getManager();
		init();
	}
	
	/**
	 * Instance accessor
	 * 
	 * @return PopUpMessage instance
	 */
	public static PopUpMessage getInstance(){
		return popUpMessage;
	}
	
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
	protected void init(){
		
		// Setup dialog
		setModal(false);
		String title = intl_mgr.getString(TITLE_PROP);
		setTitle(title);
		setResizable(false);
		
		BorderLayout layout = new BorderLayout();
		layout.setHgap(8);
		layout.setVgap(10);
		
		setLayout(layout);
		
		compoundMessage = new StringBuilder();
		
		// Checkbox
		String checkBoxMessage = intl_mgr.getString(DO_NOT_SHOW_PROP);
	    showAgain = new JCheckBox(checkBoxMessage);
	    showAgain.setAlignmentY(JCheckBox.CENTER_ALIGNMENT);
	    showAgain.setFont(FontColorUtils.getMediumFont());
	    
	    // Text
	    messageText = new JTextArea();
	    messageText.setEditable(false);
	    messageText.setWrapStyleWord(true);
	    messageText.setLineWrap(true);
	    messageText.setBackground(SystemColor.control);
        messageText.setFont(FontColorUtils.getMediumFont());
	    
	    JScrollPane scrollPane = new JScrollPane(messageText);
	    scrollPane.setHorizontalScrollBarPolicy(
	    		ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	    
	    scrollPane.setMinimumSize(
	    		new Dimension(TEXT_MINIMUM_WIDTH, TEXT_MINIMUM_HEIGHT));
	    
	    scrollPane.setPreferredSize(
	    		new Dimension(TEXT_PREFERRED_WIDTH, TEXT_PREFERRED_HEIGHT));
	    
	    scrollPane.setBorder(new EmptyBorder(10, 10, 10 , 5));
	    
	    // Add JLabel to the Dialog
	    add(scrollPane, BorderLayout.CENTER);
	    
	    // Checkbox panel
	    buttonPanel = new JPanel();
	    buttonPanel.add(showAgain, BorderLayout.WEST);
	    
	    // Close button
	    String okButtonText = intl_mgr.getString(OK_BUTTON_PROP);
	    okButton = new JButton(okButtonText);
	    okButton.setSize(new Dimension(32, 24));
	    okButton.setAlignmentY(JButton.RIGHT_ALIGNMENT);
	    okButton.addActionListener(this);
	    okButton.setFont(FontColorUtils.getMediumFont());
	    
	    buttonPanel.add(okButton, BorderLayout.EAST);
	    
	    // Add the button panel to the dialog
	    add(buttonPanel, BorderLayout.SOUTH);

	}
	
	/**
	 * Shows the dialog with the new message
	 * 
	 * @param msg String to display
	 */
	public void showMessage(String msg){
		
		// If high level user override set, just return
		if(blockPopUps){
			return;
		}
		
		if (storePopUps) {
		    
            compoundMessage.append("\n");		    
		    compoundMessage.append(msg);
		    
		} else {
		    
		    if(showAgainMap.get(msg) == null ||
		            showAgainMap.get(msg) == true){
            
		        showAgain.setVisible(true);
		        showAgain.setSelected(false);
		        messageText.setText(msg);           
		        messageText.setCaretPosition(0);
            
		        pack();
            
		        if(parentWidget != null){
		            setLocationRelativeTo(parentWidget);
		        }
            
		        setVisible(true);
		    }

		}
		
	}
	
	/**
	 * Show the compound message
	 */
	public void showCompoundMessage() {
	    if (compoundMessage.length() > 0) {
	        showAgain.setVisible(false);
	        messageText.setText(compoundMessage.toString());           
	        messageText.setCaretPosition(0);
	        compoundMessage = new StringBuilder();

            pack();
            
            if(parentWidget != null){
                setLocationRelativeTo(parentWidget);
            }
        
            setVisible(true);
	    }
	}
	
	/**
	 * Set the override state to true or false. True prevents messages from 
	 * being displayed.
	 * 
	 * @param state boolean True block messages, false allow
	 */
	public void setMessageOverride(boolean state){
		blockPopUps = state;
	}
	

	//---------------------------------------------------------------
	// Methods required by ActionListener
	//---------------------------------------------------------------
	
	/*
	 * Listen for events from dialog widget interaction 
	 */
	public void actionPerformed(ActionEvent evt) {
		
		if(evt.getSource() == okButton){
			
			if(showAgain.isSelected()){
				showAgainMap.put(messageText.getText(), false);
			}
			
			setVisible(false);
		}

	}

    /**
     * @return the storePopUps
     */
    public boolean isStorePopUps() {
        return storePopUps;
    }

    /**
     * @param storePopUps the storePopUps to set
     */
    public void setStorePopUps(boolean storePopUps) {
        this.storePopUps = storePopUps;
    }
}
