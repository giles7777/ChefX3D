/*****************************************************************************
 *                        Web3d.org Copyright (c) 2010
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

// External imports
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

// Local imports
// none

/**
 * Message box for fatal error messages..
 * 
 * @author Rex Melton
 * @version $Revision: 1.2 $
 */
public class PopUpFatalError extends JDialog implements ActionListener {
	
	/** TextArea where message is displayed */
	private JTextArea messageText;
	
	/** Panel that holds the checkbox and ok button */
	private JPanel buttonPanel;
	
	/** Ok button to close dialog */
	private JButton okButton;
	
	/** protected const for preferred JTextArea width */
	private static final int TEXT_PREFERRED_WIDTH = 300;
	
	/** protected const for minimum JTextArea width */
	private static final int TEXT_MINIMUM_WIDTH = 300;
	
	/** protected const for preferred JTextArea height */
	private static final int TEXT_PREFERRED_HEIGHT = 150;
	
	/** Protected const for minimum JTextArea height */
	private static final int TEXT_MINIMUM_HEIGHT = 30;
	
	/** Ready status message */
	private static final String TITLE_PROP = 
		"org.chefx3d.messaging.PopUpFatalError.title";
	
	/** OK button text */
	private static final String OK_BUTTON_PROP = 
		"org.chefx3d.messaging.PopUpFatalError.okButton";
	
	/** Translation utility */
	private I18nManager intl_mgr;
	
	/**
	 * Constructor
	 * 
	 * @param msg The String to display
	 */
	public PopUpFatalError(String msg) {
		
		intl_mgr = I18nManager.getManager();
		init();
		showMessage(msg);
	}
	
	//---------------------------------------------------------------
	// Methods required by ActionListener
	//---------------------------------------------------------------
	
	/*
	 * Listen for events from dialog widget interaction 
	 */
	public void actionPerformed(ActionEvent ae) {
		
		Object src = ae.getSource();
		if (src == okButton) {
			
			setVisible(false);
			dispose();
		}
	}
	
	//---------------------------------------------------------------
	// Local Methods
	//---------------------------------------------------------------
	
	/**
	 * Initialize the GUI widgets
	 */
	private void init() {
		
		// Setup dialog
		setModal(false);
		String title = intl_mgr.getString(TITLE_PROP);
		setTitle(title);
		setResizable(false);
		
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
	 * @param msg The String to display
	 */
	private void showMessage(String msg){
		
		messageText.setText(msg);           
		messageText.setCaretPosition(0);
		
		pack();
		
		Dimension screenSize = getToolkit().getScreenSize();
		Dimension dialogSize = getSize();
		setLocation(
			(screenSize.width - dialogSize.width) / 2, 
			(screenSize.height - dialogSize.width) / 2);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		setVisible(true);
	}
}
