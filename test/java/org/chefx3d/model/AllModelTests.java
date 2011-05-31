/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005-2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.model;

//External Imports
import junit.framework.TestSuite;
import junit.framework.Test;

// Internal Imports

/**
 * Create a TestSuite to run all tests
 *
 * @author Russell Dodds
 * @version $Revision: 1.2 $
 */
public class AllModelTests {

    static public Test suite() {

        // Create the suite
        TestSuite suite = new TestSuite();
        suite.setName("org.chefx3d.model.AllModelTests");

        // add the TestCases
        suite.addTestSuite(TestLocationWorldModel.class);
        suite.addTestSuite(TestEntityWorldModel.class);
        suite.addTestSuite(TestImportExport.class);
        //suite.addTestSuite(TestFenceEntityWorldModel.class);

        return suite;
    }

}