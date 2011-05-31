/*
Copyright (c) 2005-2006 Yumetech.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer
      in the documentation and/or other materials provided with the
      distribution.
    * Neither the names of the Naval Postgraduate School (NPS)
      Modeling Virtual Environments and Simulation (MOVES) Institute
      (http://www.nps.edu and http://www.MovesInstitute.org)
      nor the names of its contributors may be used to endorse or
      promote products derived from this software without specific
      prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
*/

package org.chefx3d.actions.awt;

// External imports
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import java.io.*;
import java.util.HashMap;

import org.j3d.util.I18nManager;

// Local imports
import org.chefx3d.model.*;
import org.chefx3d.view.*;

import org.chefx3d.view.awt.AWTViewFactory;

/**
 * An action that can be used to launch the 3D viewer.
 * <p>
 *
 * <b>Internationalisation Resource Names</b>
 * <p>
 * <ul>
 * <li>title: The name that appears on the action when no icon given</li>
 * <li>description: The short description to go with the action (eg tooltip)</li>
 * <li>frameTitle: If an external frame is started with a standalone window,
 *     this is the title of the frame</li>
 * </ul>
 *
 * @author Alan Hudson
 * @version $Revision: 1.6 $
 */
public class View3DAction extends AbstractAction implements WindowListener {

    /** Name of the property to get the action name */
    private static final String DESCRIPTION_PROP =
        "org.chefx3d.actions.awt.View3DAction.description";

    /** Name of the property to get the action name */
    private static final String TITLE_PROP =
        "org.chefx3d.actions.awt.View3DAction.title";

    /** Name of the property to get the action name */
    private static final String FRAME_TITLE_PROP =
        "org.chefx3d.actions.awt.View3DAction.frameTitle";

    /** The world model */
    private WorldModel model;

    /** The view manager */
    private ViewManager viewManager;

    /** The view */
    private ViewX3D view;

    /** The external viewer */
    private JFrame externalViewer;

    /** The device to display on */
    private GraphicsDevice device;

    /** The intialWorld to load */
    private String intialWorld;

    /** The image directory */
    private String imageDirectory;

    /**
     * Create an instance of the action class.
     *
     * @param iconOnly True if you want to display the icon only, and no text
     *    labels
     * @param icon The icon to use. Null if none
     * @param model The world model
     * @param vmanager The view manager
     * @param device The device to display on
     * @param intialWorld The intialWorld to load
     * @param imageDirectory The image directory
     */
    public View3DAction(boolean iconOnly, Icon icon, WorldModel model,
        ViewManager vmanager, GraphicsDevice device, String intialWorld,
        String imageDirectory) {

        I18nManager intl_mgr = I18nManager.getManager();

        if (icon != null)
            putValue(Action.SMALL_ICON, icon);

        if (!iconOnly)
            putValue(Action.NAME, intl_mgr.getString(TITLE_PROP));

        this.model = model;
        viewManager = vmanager;
        this.device = device;
        this.intialWorld = intialWorld; //  build/catalog/InitialWorld.x3dv
        this.imageDirectory = imageDirectory; //  build/images

        KeyStroke acc_key = KeyStroke.getKeyStroke(KeyEvent.VK_V,
                                                   KeyEvent.ALT_MASK);

        putValue(ACCELERATOR_KEY, acc_key);
        putValue(MNEMONIC_KEY, new Integer(KeyEvent.VK_V));

        putValue(SHORT_DESCRIPTION, intl_mgr.getString(DESCRIPTION_PROP));
    }

    //----------------------------------------------------------
    // Methods required by the ActionListener interface
    //----------------------------------------------------------

    /**
     * An action has been performed.
     *
     * @param evt The event that caused this method to be called.
     */
    public void actionPerformed(ActionEvent evt) {
        show();
    }

    //---------------------------------------------------------------
    // Methods defined by WindowListener
    //---------------------------------------------------------------

    /**
     * Ignored
     */
    public void windowActivated(WindowEvent evt) {
    }

    /**
     * Ignored
     */
    public void windowClosed(WindowEvent evt) {
    }

    /**
     * Exit the application
     *
     * @param evt The event that caused this method to be called.
     */
    public void windowClosing(WindowEvent evt) {
        close();
    }

    /**
     * Ignored
     */
    public void windowDeactivated(WindowEvent evt) {
    }

    /**
     * Ignored
     */
    public void windowDeiconified(WindowEvent evt) {
    }

    /**
     * Ignored
     */
    public void windowIconified(WindowEvent evt) {
    }

    /**
     * When the window is opened, start everything up.
     */
    public void windowOpened(WindowEvent evt) {
    }

    //----------------------------------------------------------
    // Local methods
    //----------------------------------------------------------
    public void show() {

        if (view == null) {
            HashMap params = new HashMap();
            params.put(ViewFactory.PARAM_INITIAL_WORLD, intialWorld);
            params.put(ViewFactory.PARAM_IMAGES_DIRECTORY, imageDirectory);
            AWTViewFactory view_fac = new AWTViewFactory();
            view = (ViewX3D) view_fac.createView(model,
                                 ViewFactory.PERSPECTIVE_X3D_VIEW,
                                 params);

            I18nManager intl_mgr = I18nManager.getManager();
            String title = intl_mgr.getString(FRAME_TITLE_PROP);
            externalViewer = new JFrame(title, device.getDefaultConfiguration());

            externalViewer.setSize(1024,768);
            Container cp = externalViewer.getContentPane();
            cp.add((JComponent)view.getComponent(), BorderLayout.CENTER);
            externalViewer.setVisible(true);

            externalViewer.addWindowListener(this);

            viewManager.addView(view);
            // TODO: Need to have the view catchup with the model
        } else {
            externalViewer.setVisible(true);
        }
    }

    /**
     * Close this window.
     */
    public void close() {
        if (view != null) {
            view.shutdown();

            viewManager.removeView(view);
            view = null;
            externalViewer.setVisible(false);
            externalViewer = null;
        }
    }

    /**
     * Return the view.
     *
     * @return the view. If not instantiated, null is returned.
     */
    public ViewX3D getView() {
        return(view);
    }
}
