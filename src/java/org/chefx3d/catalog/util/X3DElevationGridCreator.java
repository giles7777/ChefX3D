/*****************************************************************************
 *                        Yumetech, Inc Copyright (c) 2007
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

// External imports

package org.chefx3d.catalog.util;

// External Imports
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import org.j3d.geom.terrain.HeightDataCreator;

// Local imports
// none

/**
 * Utility for creating elevation grids
 *
 * @author Russell Dodds
 * @version $Revision: 1.1 $
 */
public class X3DElevationGridCreator {
	
	/** Usage message with command line options */
	private static final String USAGE =
		"Usage: X3DElevationGridCreator [options] \n" +
		"  -help                 Print out this messaage to the stdout\n" +
		"  -image filename       Specify the source image file to use, either relative or absolute. \n" +
		"  -output filename      Specify the resulting filename to produce, either relative or absolute. \n";
		
	/** The maximum height */
	public static final int MAX_HEIGHT = 100;
	
	/**
	 * Constructor
	 */
	public X3DElevationGridCreator() {
		
	}
	
	//----------------------------------------------------------
	// Local Methods 
	//----------------------------------------------------------
	
	/** 
	 * Parse the arg and initialize, internal parameters, create a new instance
	 */
	public static void main( String[] args ) {
		
		String input = null;
        String output = null;
		
		// first, sort out a help request, the processing mode and
		// the option strings
		for (int i = 0; i < args.length; i++) {
			String argument = args[i];
			if (argument.startsWith( "-" )) {
				try {
					if (argument.equals("-help")) {
						System.out.println(USAGE);
						return;
                    } else if ( argument.equals("-image")) {
                        input = args[i+1];
                    } else if (argument.equals("-output")) {
                        output = args[i+1];
					} else if (argument.startsWith("-")) {
						System.out.println("Unknown argument: " + argument);
					}
				} catch (Exception e) {
					// this would be an IndexOutOfBounds
				}
			}
		}
        
        // check the input and out files
        if ((input == null) || (output == null)) {
            System.out.println("You must specify both a source and destination file, exiting");
            return;
        }
		
        File srcFile = new File(input);
        if (!srcFile.exists()) {
            System.out.println("Invalid source file specified: "+ srcFile +" does not exist");
            return;
        }
        
        if (!output.endsWith(".x3d")) {
            output += ".x3d";
        }
        		
        X3DElevationGridCreator creator = new X3DElevationGridCreator();	
        creator.process(srcFile, output);	

	}
    
    /**
     * Do the batch mode processing
     *
     * @param dir The directory to begin processing at
     */
    private void process(File srcFile, String destFile) {

        BufferedImage bufImage;
        FileWriter writer;
        
        // Create a BufferedImage
        try {
            bufImage = ImageIO.read(srcFile);          
        } catch (Exception e) {
            System.out.println("Parsing image file failed!");
            return;
        }

        // Create the output file
        try {
            File output = new File(destFile);
            writer = new FileWriter(output);
        } catch (Exception e) {
            System.out.println("Creating output file writer failed!");
            return;
        }
        
        HeightDataCreator heightConverter = new HeightDataCreator(-MAX_HEIGHT, MAX_HEIGHT);
        float [][] terrain = heightConverter.createHeightField(bufImage);
  
        int width = bufImage.getWidth();
        int height = bufImage.getHeight();

        try {

            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    writer.append(terrain[i][j] + " ");
                }
                writer.append("\n");
            }
            
        } catch (Exception e) {
            
            System.out.println("Writing failed!");
            return;
            
        } finally {
            
            try {
                writer.close();
            } catch (Exception e) {
  
            }
            
        }      

    }
    
}

