/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2009
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.awt.av3d;

// External Imports
import java.awt.Component;
import java.awt.Frame;

import org.chefx3d.model.CommandController;
import org.chefx3d.model.WorldModel;
import org.chefx3d.view.awt.scenemanager.SceneManagerObserver;
import org.j3d.aviatrix3d.Scene;

/**
 * Complex data type that holds references to everything needed to create thumbnails.  Passing
 * this object to a ThumbnailPanel is enough to generate an array of thumbnail Images from
 * the data held by the state of this object.
 *
 * @author Christopher Shankland
 * @version $Revision: 1.4 $
 */
public class ThumbnailData {
    private Frame owner;
    private Component comp;
    private Scene scene;
    private SceneManagerObserver mgmtObserver;
    private CommandController controller;
    private WorldModel model;

    /**
     * Constructor.
     *
     * @param frame
     * @param comp
     * @param scene
     * @param mgmtObserver
     * @param controller
     */
    public ThumbnailData(Frame frame, Component comp, Scene scene, SceneManagerObserver mgmtObserver, CommandController controller, WorldModel model) {
        this.owner = frame;
        this.comp = comp;
        this.scene = scene;
        this.mgmtObserver = mgmtObserver;
        this.controller = controller;
        this.model = model;
    }

    public void setComp(Component comp) {
        this.comp = comp;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public void setController(CommandController controller) {
        this.controller = controller;
    }

    public void setSceneManagerObserver(SceneManagerObserver mgmtObserver) {
        this.mgmtObserver = mgmtObserver;
    }

    public void setOwner(Frame frame) {
        this.owner = frame;
    }

    public void setModel(WorldModel model) {
        this.model = model;
    }

    public Component getComp() {
        return comp;
    }

    public Scene getScene() {
        return scene;
    }

    public CommandController getController() {
        return controller;
    }

    public SceneManagerObserver getSceneManagerObserver() {
        return mgmtObserver;
    }

    public Frame getOwner() {
        return owner;
    }

    public WorldModel getModel() {
        return model;
    }
}
