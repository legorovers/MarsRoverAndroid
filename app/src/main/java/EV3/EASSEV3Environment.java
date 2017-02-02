// ----------------------------------------------------------------------------
// Copyright (C) 2015 Strategic Facilities Technology Council 
//
// This file is part of the Engineering Autonomous Space Software (EASS) Library.
// 
// The EASS Library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 3 of the License, or (at your option) any later version.
// 
// The EASS Library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public
// License along with the EASS Library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
// 
// To contact the authors:
// http://www.csc.liv.ac.uk/~lad
//
//----------------------------------------------------------------------------
package EV3;

import java.util.HashMap;
import java.util.Set;

import ail.syntax.Action;
import ail.syntax.Predicate;
import ail.syntax.Unifier;
import ail.util.AILexception;
import eass.mas.DefaultEASSEnvironment;

/**
 * Default environment class for an EV3 robot.
 * @author louiseadennis
 *
 */
public class EASSEV3Environment extends DefaultEASSEnvironment {
	boolean connectingtobricks = true;
	String defaultaddress = "10.0.1.1";
	HashMap<String, LegoRobot> robots = new HashMap<String, LegoRobot>();
	// Used to signal if there have been connection errors in setting up the robot.
	public boolean error = false;

	/**
	 * Constructor 
	 *
	 */
	public EASSEV3Environment() {
		super();
	}
		
	
	/**
	 * Close the connection to the robot.
	 * @param rname
	 */
	public void  close(String rname) {
		setDone(true);
		LegoRobot r = getRobot(rname);
		synchronized(r) {
			if (r != null) {
				r.close();
			}
		}
	}
	
	/**
	 * Add and abstraction engine.
	 * @param s
	 */
	public void addAbstractionEngine(String s, String foragent) {
		super.addAbstractionEngine(s, foragent);
		try {
			LegoRobot robot = createRobot(foragent);
			robots.put(foragent, robot);
		} catch (Exception e) {
			error = true;
		}
	}
	
	/**
	 * Add a robot to the environment.
	 * @param name
	 * @param r
	 */
	public void addRobot(String name, LegoRobot r) {
		robots.put(name, r);
	}
	
	/**
	 * Create an encapsulation for a robot.
	 * @param agent
	 * @return
	 */
	public LegoRobot createRobot(String agent) throws Exception
	{
		try {
			BasicRobot robot = new BasicRobot();
			robot.connect(defaultaddress);
			return robot;
		} catch (Exception e) {
			System.err.println(e.getMessage());
			throw e;
		}
	}
		
	/*
	 * (non-Javadoc)
	 * @see eass.mas.DefaultEASSEnvironment#eachrun()
	 */
	@Override
	public void eachrun() {
		for (LegoRobot r: robots.values()) {
			synchronized(r) {
				r.addPercepts(this);
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see ail.mas.DefaultEnvironment#cleanup()
	 */
	@Override
	public void cleanup() {
		for (LegoRobot r: robots.values()) {
			synchronized(r) {
				r.close();
			}
		}
	}


	/**
	 * Get the robot with this name from the environment.
	 * @param name
	 * @return
	 */
	public LegoRobot getRobot(String name) {
		return robots.get(name);
	}
	
	/*
	 * (non-Javadoc)
	 * @see ail.others.DefaultEnvironment#executeAction(java.lang.String, ail.syntax.Action)
	 */
	@Override
	public Unifier executeAction(String agName, Action act) throws AILexception {
	   
		Unifier u = new Unifier();
		/* if (act.getFunctor().equals("shutdown")) {
			stopRunning();
		} else {
			u = super.executeAction(agName, act);
		} */
		if (act.getFunctor().equals("forward")) {
			((Robot) robots.get(agName)).forward();
		} else if (act.getFunctor().equals("forward_a_bit")) {
			((Robot) robots.get(agName)).short_forward();
		} else if (act.getFunctor().equals("backward")) {
			((Robot) robots.get(agName)).backward();
		} else if (act.getFunctor().equals("backward_a_bit")) {
			((Robot) robots.get(agName)).short_backward();
		} else if (act.getFunctor().equals("left")) {
			((Robot) robots.get(agName)).left();
		} else if (act.getFunctor().equals("left_a_bit")) {
			((Robot) robots.get(agName)).very_short_left();
		} else if (act.getFunctor().equals("right")) {
			((Robot) robots.get(agName)).right();
		} else if (act.getFunctor().equals("right_a_bit")) {
			((Robot) robots.get(agName)).very_short_right();
		} else if (act.getFunctor().equals("stop")){
			((Robot) robots.get(agName)).stop();
		} else if (act.getFunctor().equals("forward_right")) {
			((Robot) robots.get(agName)).forward_right();
		} else if (act.getFunctor().equals("forward_left")) {
			((Robot) robots.get(agName)).forward_left();
		} else if (act.getFunctor().equals("right_a_lot")) {
			((Robot) robots.get(agName)).short_right();
		} else if (act.getFunctor().equals("left_a_lot")) {
			((Robot) robots.get(agName)).short_left();
		}

		return u;
	  }
		
	
	/*
	 * (non-Javadoc)
	 * @see ail.mas.DefaultEnvironment#done()
	 */
	@Override
	public boolean done() {
		return super.done;
	}
	
}


