/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005 - 2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.view.awt;

//External Imports
import java.util.Map;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.lang.reflect.Constructor;

//Internal Imports
import org.chefx3d.model.*;
import org.chefx3d.view.View;
import org.chefx3d.view.ViewFactory;
import org.chefx3d.model.WorldModel;
import org.chefx3d.util.DefaultErrorReporter;
import org.chefx3d.util.ErrorReporter;

/**
 * Factory for creating Views
 *
 * @author Alan Hudson
 * @version $Revision: 1.21 $
 */
public class AWTViewFactory implements ViewFactory {

    /** The ErrorReporter for messages */
    private ErrorReporter errorReporter;


    /**
     * A new AWTViewFactory
     *
     */
    public AWTViewFactory() {
        errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

    /**
     * Create a view.
     *
     * @return The View
     */
    public View createView(WorldModel model, int type) {
        return createView(model, type, null);
    }

    /**
     * Create a view.
     *
     * @param type The type of view to create
     * @param params View specific parameters.
     * @return The View
     */
    public View createView(WorldModel model, int type, Map params) {

        String imageDir = null;
        String initialWorld = null;
        String pickingType = null;
        String showRuler = "false";
        CommandController controller = null;
        float[] backgroundColor = new float[]{0,0,0};
        
        if (params != null) {
            imageDir = (String) params.get(PARAM_IMAGES_DIRECTORY);
            initialWorld = (String) params.get(PARAM_INITIAL_WORLD);
            pickingType = (String) params.get(PARAM_PICKING_TYPE);
            showRuler = (String) params.get(PARAM_SHOW_RULER);
            controller = (CommandController) params.get(PARAM_COMMAND_CONTROLLER);
            backgroundColor = (float[]) params.get(PARAM_BACKGROUND_COLOR);
        }

        switch(type) {
            case PICTURE_VIEW:
                return null;
            case OPENMAP_VIEW:
                errorReporter.messageReport("OpenMap View not implemented yet");
                return null;
            case TOP_X3D_VIEW:
                errorReporter.messageReport("3D Top View not implemented yet");
                return null;
            case LEFT_X3D_VIEW:
                errorReporter.messageReport("3D Left View not implemented yet");
                return null;
            case RIGHT_X3D_VIEW:
                errorReporter.messageReport("3D Right View not implemented yet");
                return null;
            case TOP_2D_VIEW:

                boolean b = false;
                try {
                    b = Boolean.parseBoolean(showRuler);
                } catch (Exception ex) {
                }

                final Object[] viewParams = new Object[] {
                    model,
                    imageDir,
                    b};

                View ret_val = (View)AccessController.doPrivileged(
                    new PrivilegedAction( ) {
                        public Object run( ) {
                            try {
                                Class viewClass = Class.forName("org.chefx3d.view.awt.gt2d.GT2DView");

                                Constructor[] consts = viewClass.getConstructors();

                                for (int i = 0; i < consts.length; i++) {
                                    Object[] paramTypes = consts[i].getParameterTypes( );
                                    if( paramTypes.length == viewParams.length ) {
                                        return (View)consts[i].newInstance(viewParams);
                                    }
                                }

                                return null;
                            } catch(Exception e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                    }
                );

                return ret_val;
                


            case PERSPECTIVE_X3D_VIEW:
                Watcher3DView.PickingType pickingTypeValue = Watcher3DView.PickingType.NONE;

                if(pickingType != null && pickingType.toLowerCase().equals("primitive")){
                    pickingTypeValue = Watcher3DView.PickingType.PRIMITIVE;
                } else if (pickingType != null && pickingType.toLowerCase().equals("elevation")) {
                    pickingTypeValue = Watcher3DView.PickingType.ELEVATION;
                }

                return new Watcher3DView(model, initialWorld, pickingTypeValue);
            case TOP_3D_VIEW:
            	

                final Object[] viewParameters = new Object[] {
                    model,
                    controller
					};
            	
            	  View returnValue = (View)AccessController.doPrivileged(
                          new PrivilegedAction( ) {
                              public Object run( ) {
                                  try {
                                      Class viewClass = Class.forName("org.chefx3d.view.awt.av3d.AV3DView");

                                      Constructor[] consts = viewClass.getConstructors();

                                      for (int i = 0; i < consts.length; i++) {
                                          Object[] paramTypes = consts[i].getParameterTypes( );
                                          if( paramTypes.length == viewParameters.length ) {
                                              return (View)consts[i].newInstance(viewParameters);
                                          }
                                      }

                                      return null;
                                  } catch(Exception e) {
                                      e.printStackTrace();
                                      return null;
                                  }
                              }
                          }
                      );

                      return returnValue;

            	
            	
            default:
                IllegalArgumentException e = new IllegalArgumentException("Unsupported view type: " + type);
                errorReporter.errorReport(e.getMessage(), e);
                throw e;
        }

    }

    /**
     * Register an error reporter with the command instance
     * so that any errors generated can be reported in a nice manner.
     *
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter) {
        errorReporter = reporter;

        if(errorReporter == null)
            errorReporter = DefaultErrorReporter.getDefaultReporter();
    }

}
