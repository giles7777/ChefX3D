/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2006
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.property;

// Standard Imports

// Application specific imports
import org.chefx3d.view.View;
import org.chefx3d.util.ErrorReporter;

/**
 * A property editor.
 * 
 * @author Alan Hudson
 * @version $Revision: 1.7 $
 */
public interface PropertyEditor extends View {
    
    /**
     * Register an error reporter with the PropertyEditor instance
     * so that any errors generated can be reported in a nice manner.
     * @param reporter The new ErrorReporter to use.
     */
    public void setErrorReporter(ErrorReporter reporter);
    
}
