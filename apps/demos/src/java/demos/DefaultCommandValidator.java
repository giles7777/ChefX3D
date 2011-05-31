
package demos;

import java.util.ArrayList;

import org.chefx3d.model.Command;
import org.chefx3d.model.CommandValidator;
import org.chefx3d.model.Entity;

import org.chefx3d.view.awt.av3d.EntityCollisionManager;
import org.chefx3d.view.awt.av3d.RuleCollisionChecker;

public class DefaultCommandValidator implements CommandValidator {
	
	/** Collision checker */
	private RuleCollisionChecker collisionChecker;
	
	/**
	 * Constructor
	 */
	public DefaultCommandValidator() {
		collisionChecker = EntityCollisionManager.getInstance();
		if (collisionChecker == null) {
			throw new NullPointerException("RuleCollisionChecker must be non-null");
		}
	}
	
	/**
	 * Return whether this command passes the validation process.
	 * 
	 * @param command The command to check.
	 */
	public boolean validate(Command command) {
		boolean isValid = false;
		ArrayList<Entity> results = collisionChecker.submitCommand(command);
		if (results == null) {
			isValid = true;
		}
		//System.out.println("validate: "+ command +": "+ isValid);
		return(isValid);
	}
}
