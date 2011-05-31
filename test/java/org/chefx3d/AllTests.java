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

package org.chefx3d;

//External Imports
import java.awt.EventQueue;
import junit.framework.TestSuite;
import junit.framework.Test;
import junit.framework.TestResult;
import junit.framework.TestFailure;
import java.util.Enumeration;

// Internal Imports
import org.chefx3d.model.*;

/**
 * Create a TestSuite to run all tests
 * 
 * @author Russell Dodds
 * @version $Revision: 1.1 $
 */
public class AllTests {
 
    /**
     * Run all the tests defined
     *
     */
    public AllTests() {
        
        TestFailure error;     
        TestResult results = new TestResult();
        
        // Execute the Tests
        suite().run(results);
        
        // Print the run statistics
        printHeader("Test Results", true);
        System.out.println("Errors:     " + results.errorCount());        
        System.out.println("Failures:   " + results.failureCount());
        System.out.println("");
        
        // Print out errors
        if (results.errorCount() > 0) {
            printHeader("Errors", false);
            for (Enumeration errors = results.errors() ; errors.hasMoreElements() ;) {
                error = (TestFailure) errors.nextElement();            
                System.out.println("Test:       " + error.failedTest().toString());
                System.out.println("Message:    " + error.exceptionMessage());
                System.out.println("Failure Trace: ");
                System.out.println(error.trace());
                System.out.println("");
           }          
        }

        // Print out failures
        if (results.failureCount() > 0) {
            printHeader("Failures", false);
            for (Enumeration failures = results.failures() ; failures.hasMoreElements() ;) {
                error = (TestFailure) failures.nextElement();
                System.out.println("Test:       " + error.failedTest().toString());
                System.out.println("Message:    " + error.exceptionMessage());
                System.out.println("Failure Trace: ");
                System.out.println(error.trace());
                System.out.println("");
           }
        }
 
    }
    
    /**
     * Define the tests to run
     * 
     * @return The suite of tests to run
     */
    static public Test suite() {
        
        // Create the suite
        TestSuite suite = new TestSuite();
        
        // add the Model TestCases
        Test modelTests = AllModelTests.suite();
        suite.addTest(modelTests);        
          
        return suite;
        
    }
 
    /**
     * A helper method to print a command line header
     * 
     * @param message The message/title to print
     * @param spacers Add line breaker above and below message
     */
    private void printHeader(String message, boolean spacers) {
        System.out.println("");
        System.out.println("************************************************************");
        if (spacers) System.out.println("*");
        System.out.println("*       " + message.toUpperCase());
        if (spacers) System.out.println("*");
        System.out.println("************************************************************");
        System.out.println("");
    }
    
    /**
     * Execute the class
     * 
     * @param args
     */
    public static void main(String args[]) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                AllTests tests = new AllTests();
            }
        });
    }

}