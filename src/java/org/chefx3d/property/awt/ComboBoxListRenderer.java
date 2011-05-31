/*****************************************************************************
 *                        Copyright Yumetech, Inc (c) 2005
 *                               Java Source
 *
 * This source is licensed under the GNU LGPL v2.1
 * Please read http://www.gnu.org/copyleft/lgpl.html for more information
 *
 * This software comes with the standard NO WARRANTY disclaimer for any
 * purpose. Use it at your own risk. If there's a problem you get to fix it.
 *
 ****************************************************************************/

package org.chefx3d.property.awt;

//External Imports
import javax.swing.*;
import javax.swing.border.*;

import java.awt.Color;
import java.awt.Component;
import java.util.StringTokenizer;

//Internal Imports
import org.chefx3d.model.ListProperty;
import org.chefx3d.util.FontColorUtils;

/**
 * A Renderer for the drop down box. It displays the text in the list as JLabels
 * with images
 * 
 * @author Russell Dodds
 * @version $Revision: 1.10 $
 */
class ComboBoxListRenderer extends JLabel 
    implements ListCellRenderer {
    
	private static final int MAX_CHAR_PER_LINE = 16;
	
    /** The current list of possible items */
    private ListProperty listProperty;
       	
    private String bgColorString;
    /**
     * Construct the JLable with certain text and image positioning
     */
    public ComboBoxListRenderer(ListProperty list) {
        
        listProperty = list;
        
        setOpaque(true);
        setVerticalTextPosition(JLabel.BOTTOM);
        setHorizontalTextPosition(JLabel.CENTER);
        setHorizontalAlignment(JLabel.CENTER);
        
        Border padding = 
            BorderFactory.createEmptyBorder(4, 2, 2, 2);
        setBorder(padding);  
        
        Color backgroundColor = FontColorUtils.getBackgroundColor();
        
        setFont(FontColorUtils.getMediumFont());
        setBackground(backgroundColor);
        setForeground(FontColorUtils.getForegroundColor());
  
        // convert the color to be used in the HTML labels
        bgColorString = 
        	Integer.toHexString(backgroundColor.getRGB() & 0x00ffffff);

    }

    /**
     * This method finds the image and text corresponding
     * to the selected value and returns the label, set up
     * to display the text and image.
     */
    public Component getListCellRendererComponent(
                                       JList list,
                                       Object value,
                                       int index,
                                       boolean isSelected,
                                       boolean cellHasFocus) {
                
        // handle this bad case
        if (index < 0) {
            index = (Integer)listProperty.getValue();
        }
        
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }

        String[] texts = listProperty.getLabels();

        /*
         * Set the icon and text.  If icon was null, say so.
         * Label length is limited to make sure all of the characters
         * are displayed and word wrapping is forced with html
         * line breaks.
         */
        String tmpText = texts[index];
		StringTokenizer st = new StringTokenizer(tmpText, " ");
		String token = "";
		String currentLine = "";
		String labelText = 
			"<HTML><P ALIGN=\"CENTER\" bgcolor=\"#" + bgColorString + "\">";
		
		while(st.hasMoreTokens()){

			token = st.nextToken();
			
			if((currentLine.length() + 1 + token.length()) > MAX_CHAR_PER_LINE){
				
				labelText = labelText + currentLine + "<BR>";
				currentLine = token;
				
			} else {
				
				currentLine = currentLine + " " + token;
			}
			
			if(!st.hasMoreTokens()){
				
				labelText = labelText + currentLine;
			}

		}
		
		labelText = labelText + "</P></HTML>";
		
		ImageIcon[] icons = listProperty.getValidImages();
		if (index < icons.length) {        
		    ImageIcon icon = listProperty.getValidImages()[index];       
		    setIcon(icon);
		}
		
        setText(labelText);
        setFont(FontColorUtils.getMediumFont()); 
        setBackground(FontColorUtils.getBackgroundColor());
        setForeground(FontColorUtils.getForegroundColor());
    
        return this;
        
    }

}
