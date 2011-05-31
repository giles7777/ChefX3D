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

package org.chefx3d.util;


// external imports
import java.util.HashMap;

// internal imports
/**
 *
 * @author Eric Fickenscher
 * @version $Revision: 1.2 $
 */
public class ArgumentParser {

	/** An enumeration of primative object types */
	public static enum ArgumentType {
		STRING, INT, FLOAT, DOUBLE, BOOLEAN
	};

	/** Map of application parameters and their associated Object type*/
    private static HashMap<String, ArgumentType> paramObjects;

	/**
	 * Constructor.  Initialize the hash map of parameter-object pairings.
	 */
	public ArgumentParser() {
		paramObjects = new HashMap<String, ArgumentType>();
	}

	/**
	 *
	 * @param parameter the string name of the parameter.  This is the
	 * key value for the HashMap parameObjects.
	 * @param argType the ArgumentType, or type of object to which the
	 * argument is mapped.  IE: If argType equals ArgumentType.FLOAT,
	 * we know the argument should be parsed as a float.
	 */
	public void add(String parameter, ArgumentType argType){
		paramObjects.put(parameter, argType);
	}

	/**
	 * Parse the string array of application parameters.
	 * If the values are of the appropriate type we will
	 * add them to the ApplicationParams.<p>
	 * Assumes the application parameters are of the form
	 * "parameterName=parameterValue".
	 * @param args the string array of application parameters to parse.
	 */
	public void parse (String[] args){

		int index;
		String parameterName = "";
		String parameterValue = "";

		for( int i = 0; i < args.length; i++){

		    // first look to see if the arg is -open or -print
		    if (args[i].equals("-open") || args[i].equals("-print")) {
		        
		        parameterName =  args[i].replace("-", "");
		        parameterValue = args[++i];
		        ApplicationParams.put(parameterName, parameterValue);

		    } else {
		    
    			index = args[i].indexOf("=");
    
    			try {
    				// the name of the parameter is the string to the
    				// left of the equals sign
    				parameterName = args[i].substring(0, index);
    
    				// the object to which the parameter's value parses
    				// is found in the paramObjects map
    				ArgumentType argType = paramObjects.get(parameterName);
    
    				// the value of the parameter is found in the string to
    				// the right of the equals sign
    				parameterValue = args[i].substring(index + 1);
    
    				// we switch on the object type to smartly parse the value
    				switch(argType) {
    
    					case STRING:
    
    						ApplicationParams.put(parameterName, parameterValue);
    						break;
    
    					case INT:
    
    						ApplicationParams.put(parameterName,
    								Integer.parseInt(parameterValue));
    						break;
    
    					case FLOAT:
    
    						ApplicationParams.put(parameterName,
    								Float.parseFloat(parameterValue));
    						break;
    
    					case DOUBLE:
    
    						ApplicationParams.put(parameterName,
    								Double.parseDouble(parameterValue));
    						break;
    
    					case BOOLEAN:
    
    						ApplicationParams.put(parameterName,
    								Boolean.parseBoolean(parameterValue));
    						break;
    
    					default:
    				}
    				System.out.println(args[i]);
    
    			} catch(NullPointerException npe){
    				System.out.println("argument " +
    					parameterName + " not found.");
    			} catch(NumberFormatException nfe){
    				System.out.println("unable to parse " + parameterValue +
    					" as value for argument " + parameterName);
    			} catch( StringIndexOutOfBoundsException sioobe){
    				System.out.println("argument \"" + args[i] + "\" must be of " +
    					"the form \"parameterName=parameterValue\"");
    			}
		    }
		}
	}
}
