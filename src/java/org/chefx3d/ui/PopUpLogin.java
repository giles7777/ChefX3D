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

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import org.chefx3d.util.FontColorUtils;
import org.j3d.util.I18nManager;


/**
 * Message box that show short messages to the user with a check box 
 * option to never show that message again. Message box is modal.
 * 
 * @author Ben Yarger
 * @version $Revision: 1.5 $
 */
public class PopUpLogin extends JDialog implements ActionListener{
	
    /** The register action */
    public static final String REG_ACTION = "REGISTER";
    
    /** The OK action */
    public static final String OK_ACTION = "0K";
    
    /** The cancel action */
    public static final String CANCEL_ACTION = "CANCEL";

	/** Static class instance */
	private static PopUpLogin popUpLogin = new PopUpLogin();
	
	/** TextArea where message is displayed */
	protected JTextArea messageText;
	
	/** Panel that holds the checkbox and ok button */
	protected JPanel buttonPanel;
	
	/** Register button to open registration page */
    protected JButton registerButton;

	/** Ok button to close dialog */
	protected JButton okButton;

	/** Ok button to close dialog */
    protected JButton cancelButton;
    
    /** Field to capture the username */
    private JTextField userField;
    
    /** Field to capture the password */
    private JPasswordField passField;

    /** Contains the text of the button that was pressed */
    private String action;
	
	/** Parent Component used to set location relative to */
	protected Component parentWidget;
		
	/** protected const for preferred JTextArea width */
	protected static final int TEXT_PREFERRED_WIDTH = 300;
	
	/** protected const for minimum JTextArea width */
	protected static final int TEXT_MINIMUM_WIDTH = 300;
	
	/** protected const for preferred JTextArea height */
	protected static final int TEXT_PREFERRED_HEIGHT = 50;
	
	/** Protected const for minimum JTextArea height */
	protected static final int TEXT_MINIMUM_HEIGHT = 20;
	
	/** Translation utility */
	private I18nManager intl_mgr;
	
	/** Ready status message */
	private static final String I18N_ROOT = "org.chefx3d.messaging.PopUpLogin.";
		
    private String registrationURL;
    
    /**
	 * Constructor
	 */
	private PopUpLogin(){
		
		parentWidget = null;
		intl_mgr = I18nManager.getManager();
		
        registrationURL = intl_mgr.getString(I18N_ROOT + "regLink");
		
		init();
	}
	
	/**
	 * Instance accessor
	 * 
	 * @return PopUpMessage instance
	 */
	public static PopUpLogin getInstance(){
		return popUpLogin;
	}
	
	//---------------------------------------------------------------
	// Methods required by ActionListener
	//---------------------------------------------------------------
	
	/**
	 * Listen for events from dialog widget interaction 
	 */
	public void actionPerformed(ActionEvent evt) {
	    
	    String command = evt.getActionCommand();
	    if (command.equals(REG_ACTION)) {	
	        // TODO: move this back to use Utilties.openURL once we fix FileLoader
	        openUrl(registrationURL);
	    } else {
	        action = cancelButton.getActionCommand();
	        Object o = evt.getSource();
	        if (o instanceof JButton) {
	            action = ((JButton) o).getActionCommand();
	        }
	        setVisible(false);	        
	    }

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
    protected void init(){
        
        // Setup dialog
        setModal(true);
        String title = intl_mgr.getString(I18N_ROOT + "title");
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
        
        scrollPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        // Add JLabel to the Dialog
        add(scrollPane, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(3, 2));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Username and password fields and labels
        String name = intl_mgr.getString(I18N_ROOT + "usernameLabel");
        JLabel userLabel = new JLabel(name);   
        userLabel.setFont(FontColorUtils.getMediumFont());
        
        name = intl_mgr.getString(I18N_ROOT + "passwordLabel");
        JLabel passLabel = new JLabel(name);
        passLabel.setFont(FontColorUtils.getMediumFont());
        
        userField = new JTextField(10);
        userField.setFont(FontColorUtils.getMediumFont());
        
        passField = new JPasswordField(10);
        passField.setFont(FontColorUtils.getMediumFont());
        
        // Add all the components.
        inputPanel.add(userLabel);
        inputPanel.add(userField);
                
        inputPanel.add(passLabel);
        inputPanel.add(passField);
                
        // OK button
        String okButtonText = intl_mgr.getString(I18N_ROOT + "okButton");
        okButton = new JButton(okButtonText);
        okButton.setSize(new Dimension(32, 24));
        okButton.setAlignmentY(JButton.RIGHT_ALIGNMENT);
        okButton.setActionCommand(OK_ACTION);
        okButton.addActionListener(this);
        okButton.setFont(FontColorUtils.getMediumFont());
        
        inputPanel.add(okButton);

        // Cancel button
        String cancelButtonText = intl_mgr.getString(I18N_ROOT + "cancelButton");
        cancelButton = new JButton(cancelButtonText);
        cancelButton.setSize(new Dimension(32, 24));
        cancelButton.setAlignmentY(JButton.RIGHT_ALIGNMENT);
        cancelButton.setActionCommand(CANCEL_ACTION);
        cancelButton.addActionListener(this);
        cancelButton.setFont(FontColorUtils.getMediumFont());
        
        inputPanel.add(cancelButton);

        // Add the button panel to the dialog
        add(inputPanel, BorderLayout.CENTER);

        if (registrationURL != null) {
            name = intl_mgr.getString(I18N_ROOT + "regButton");
            registerButton = new JButton("<html><u>" + name + "</u></html>");
            registerButton.setActionCommand(REG_ACTION);
            registerButton.addActionListener(this);
            registerButton.setMargin(new Insets(2, 2, 2, 2));
            registerButton.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            registerButton.setContentAreaFilled(false);
            registerButton.setForeground(Color.BLUE);
            registerButton.setFont(FontColorUtils.getMediumFont());
            add(registerButton, BorderLayout.SOUTH);   
        }

    }
    
    /**
     * Shows the dialog with a default message
     */
    public void showLogin() {
        
        String msg = intl_mgr.getString(I18N_ROOT + "message");
        showLogin(msg);
        
    }

    /**
     * Shows the dialog with the new message
     * 
     * @param msg String to display
     */
    public void showLogin(String msg){
                
        messageText.setText(msg);
        
        pack();
        
        if(parentWidget != null){
            setLocationRelativeTo(parentWidget);
        }
        
        setVisible(true);
        
    }
    
    /**
     * 
     * @return String[username, password]
     */
    public String[] getLoginInfo() {
        
        // get the values
        String username = userField.getText();
        char[] passVal = passField.getPassword();
                
        // process the password
        StringBuilder passBuf = new StringBuilder();
        for (int i = 0; i < passVal.length; i++) {
            passBuf.append(passVal[i]);
        }
        
        String password = passBuf.toString().trim();
        
        // return the result
        return new String[] {username, password};

    }
    
    /**
     * Return the string value of the button that was pressed.  Either "login" or "cancel."
     * 
     * @return
     */
    public String getAction() {
        return action;
    }

    /**
     * Opens a web page in a default web browser.
     *
     * <p>
     *
     * Since we're running our code on Java 5, we don't have a direct way to access launch a
     * default web browser from Java application.  In Java 6, however, by using Desktop class
     * you can get an access to a default web browser.
     *
     * The source of this code is pulled from:
     * <a href="http://www.centerkey.com/java/browser/">http://www.centerkey.com/java/browser/</a>
     *
     * @param url URL of the web page
     */
    private void openUrl(final String url) {
        
        AccessController.doPrivileged(
            new PrivilegedAction() {
                public Object run() {
                    
                    String osName = System.getProperty("os.name");
                    try {

                        if (osName.startsWith("Mac OS")) {

                            Class fileMgr = Class.forName("com.apple.eio.FileManager");

                            Method openURL = fileMgr.getDeclaredMethod("openURL", new Class[] {String.class});

                            openURL.invoke(null, new Object[] {url});
                        }
                        else if (osName.startsWith("Windows")) {

                            Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                        }
                        else { //assume Unix or Linux

                            String[] browsers = {"firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };

                            String browser = null;

                            for (int count = 0; count < browsers.length && browser == null; count++)
                            {
                                if (Runtime.getRuntime().exec( new String[] {"which", browsers[count]}).waitFor() == 0) {
                                    browser = browsers[count];
                                }
                            }

                            if (browser == null) {
                                throw new Exception("Could not find web browser");
                            }
                            else {
                                Runtime.getRuntime().exec(new String[] {browser, url});
                            }
                        }
                    }
                    catch (Exception e) {

                        JOptionPane.showMessageDialog(null, "Error attempting to launch web browser" + ":\n" + e.getLocalizedMessage());                  
                        return false;
                        
                    }
                    
                    return true;
           
                }
                
            }
            
        );

    }

}
