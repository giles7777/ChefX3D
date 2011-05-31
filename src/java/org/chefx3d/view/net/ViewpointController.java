/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2007-2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.net;

// External Imports
import javax.swing.*;
import java.awt.*;

//Internal Imports
import org.chefx3d.model.*;
import org.chefx3d.view.View;

/**
 * Controller for changing view modes.  The user can be in master, slaved or
 * free mode.
 *
 * @author Alan Hudson
 * @version $Revision: 1.1 $
 */
public class ViewpointController extends JFrame {
    private JLabel mode;

    public ViewpointController(View[] views) {

        super("Viewpoint Control");
        JPanel buttonPanel = new JPanel(new GridLayout(2,3));
        JButton masterButton = new JButton(
            new ControlAction("Master",View.MODE_MASTER, views));

        JButton slavedButton = new JButton(
            new ControlAction("Slaved",View.MODE_SLAVED, views));

        JButton freeButton = new JButton(
            new ControlAction("Free",View.MODE_FREE_NAV, views));

        JLabel modeLabel = new JLabel("Mode");
        mode = new JLabel("Free");
        JLabel spacer = new JLabel();

        buttonPanel.add(modeLabel);
        buttonPanel.add(mode);
        buttonPanel.add(spacer);

        buttonPanel.add(masterButton);
        buttonPanel.add(slavedButton);
        buttonPanel.add(freeButton);

        Container content = getContentPane();
        content.add(buttonPanel, BorderLayout.SOUTH);


        pack();
        setVisible(true);
    }
}