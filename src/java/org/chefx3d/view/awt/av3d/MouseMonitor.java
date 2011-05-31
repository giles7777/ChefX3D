
package org.chefx3d.view.awt.av3d;

import java.awt.Component;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


class MouseMonitor extends MouseAdapter {
	
	private LocationLayerManager location;
	
	MouseMonitor(Component cmp, LocationLayerManager location) {
		this.location = location;
		cmp.addMouseListener(this);
	}
	
	public void mouseEntered(MouseEvent me) {
		location.mouseEntered(me);
//System.out.println("mouseEntered");
	}
	
	public void mouseExited(MouseEvent me) {
		location.mouseExited(me);
//System.out.println("mouseExited");
	}
}
