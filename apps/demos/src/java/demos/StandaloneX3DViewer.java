/*****************************************************************************
 *                        Yumetech, Inc Copyright (c) 2006
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

package demos;

import java.util.*;
import java.awt.*;
import java.io.*;
import javax.swing.*;

import org.web3d.x3d.sai.*;

import org.xj3d.sai.Xj3DBrowser;

/**
 * A standalone X3D viewer. This uses Xj3D to view X3D files.
 * 
 * @author Alan Hudson
 * @version $Revision: 1.1 $
 */
public class StandaloneX3DViewer extends JFrame {

    /** version id */
    private static final long serialVersionUID = 1L;

    /** The X3D browser */
    private ExternalBrowser x3dBrowser;

    /** The current scene */
    private X3DScene mainScene;

    /** The X3DComponent in use */
    private X3DComponent x3dComp;

    /** The content pane */
    private Container contentPane;

    @SuppressWarnings("unchecked")
    public StandaloneX3DViewer() {
        super("Xj3D Viewer");

        // Setup browser parameters
        HashMap requestedParameters = new HashMap();
        requestedParameters.put("Xj3D_FPSShown", Boolean.TRUE);
        requestedParameters.put("Xj3D_LocationShown", Boolean.FALSE);
        requestedParameters.put("Xj3D_LocationPosition", "top");
        requestedParameters.put("Xj3D_LocationReadOnly", Boolean.FALSE);
        requestedParameters.put("Xj3D_OpenButtonShown", Boolean.FALSE);
        requestedParameters.put("Xj3D_ReloadButtonShown", Boolean.FALSE);
        requestedParameters.put("Xj3D_ShowConsole", Boolean.FALSE);
        requestedParameters.put("Xj3D_StatusBarShown", Boolean.TRUE);

        init(requestedParameters);

        setSize(800, 600);
        setVisible(true);
    }

    private void init(HashMap params) {
        // Create an SAI component
        x3dComp = BrowserFactory.createX3DComponent(params);

        // Add the component to the UI
        JComponent x3dPanel = (JComponent) x3dComp.getImplementation();

        contentPane = getContentPane();
        contentPane.add(x3dPanel);

        // Get an external browser
        x3dBrowser = x3dComp.getBrowser();

        ((Xj3DBrowser) x3dBrowser).setMinimumFrameInterval(40);
    }

    /**
     * Return the X3D component in use.
     * 
     * @return The component
     */
    public X3DComponent getX3DComponent() {
        return x3dComp;
    }

    /**
     * Load a new scene. This will replace the currently loaded scene.
     */
    public void load(String strURL) {
        String baseFileURL = null;

        try {
            baseFileURL = (new File(strURL)).toURL().toString();
        } catch (Exception ex) {
            ex.printStackTrace();

            return;
        }

        mainScene = x3dBrowser.createX3DFromURL(new String[] { baseFileURL });
        x3dBrowser.replaceWorld(mainScene);
    }
}
