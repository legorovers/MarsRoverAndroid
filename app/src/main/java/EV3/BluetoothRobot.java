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

import android.graphics.Color;
import android.graphics.PointF;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import ail.mas.MAS;
import ail.semantics.AILAgent;
import ail.syntax.Action;
import ail.syntax.DefaultAILStructure;
import ail.syntax.Intention;
import ail.syntax.Literal;
import ail.syntax.Plan;
import ail.syntax.Deed;
import ail.syntax.Event;
import ail.syntax.Guard;
import ail.util.AILexception;
import eass.semantics.EASSAgent;

/**
 * Thread that manages the connection to the EV3, interactions between the abstraction and reasoning
 * engines and manages sets of beliefs and actions etc.
 *
 * Created by joecollenette on 02/07/2015.
 */
public class BluetoothRobot implements Runnable
{
    // Abstraction and reasoning engines and environment for the reasoning engine.
    // Though this class also performs  some of the functions of the abstraction engine (see updateBeliefs() method).
    private Robot abstraction_engine;
    private EASSAgent reasoningengine;
    EASSEV3Environment env = new EASSEV3Environment();

    // Modes and status for managing connection and behaviour
	public enum ConnectStatus {CONNECTED, DISCONNECTED, CONNECTING, DISCONNECTING}
    private ConnectStatus status = ConnectStatus.DISCONNECTED;
    public enum RobotMode {MANUAL, AVOID, WATER, LINE}
    private RobotMode mode;

    // BDI concepts
	public enum BeliefStates
	{
		OBSTACLE(0),
		WATER(1),
		PATH(2);

		static BeliefStates[] a = BeliefStates.values();

		int value;
		BeliefStates(int i)
		{
			value = i;
		}

		public int toInt()
		{
			return value;
		}

		public static BeliefStates fromInt(int i)
		{
			for (int j = 0; j < a.length; j++)
			{
				if (a[j].toInt() == i)
				{
					return a[j];
				}
			}
			return OBSTACLE;
		}

        public Literal toLiteral() {
            if (value == 0) {
                return new Literal("obstacle");
            } else if (value == 1) {
                return new Literal("water");
            } else {
                return new Literal("path");
            }
        }

	}

	//Implements cloneable so when access to the arraylist is outside of this thread
	//there will be no clashes during the belief update.
    // Contains both the beliefs (OBSTACLE, PATH, etc) and the  sensor data.
	public static class BeliefSet implements Cloneable
	{
		public float distance;
		public int colour;
		public ArrayList<BeliefStates> states = new ArrayList<BeliefStates>();

		@Override
		public BeliefSet clone()
		{
			BeliefSet b = new BeliefSet();
			b.distance = distance;
			b.colour = colour;
			b.states = (ArrayList<BeliefStates>)states.clone();
			return b;
		}
	}
    private BeliefSet state;
    private BeliefSet stateCopy;

    public enum Goal
    {
        NONE(0),
        OBSTACLE(1),
        PATH(2),
        WATER(3);

        static Goal[] a = Goal.values();

        int value;
        Goal(int i)
        {
            value = i;
        }

        public int toInt()
        {
            return value;
        }

        public static Goal fromInt(int i)
        {
            for (int j = 0; j < a.length; j++)
            {
                if (a[j].toInt() == i)
                {
                    return a[j];
                }
            }
            return OBSTACLE;
        }

        public Literal toLiteral() {
            if (value == 0) {
                return new Literal("no_goal");
            } else if (value == 1) {
                return new Literal("obstacle");
            } else if (value == 2){
                return new Literal("path");
            } else {
                return new Literal("water");
            }
        }

    }

    public static class GoalSet implements Cloneable
    {
        public ArrayList<BeliefStates>  goals = new ArrayList<BeliefStates>();

        @Override
        public GoalSet clone()
        {
            GoalSet g = new GoalSet();
            g.goals = (ArrayList<BeliefStates>)goals.clone();
            return g;
        }
    }
    private GoalSet goals;

    public enum robotEvent
    {
        BOBSTACLE(0),
        BWATER(1),
        BPATH(2),
        GOBSTACLE(3),
        GWATER(4),
        GPATH(5);

        static robotEvent[] a = robotEvent.values();

        int value;
        robotEvent(int i)
        {
            value = i;
        }

        public int toInt()
        {
            return value;
        }

        public static robotEvent fromInt(int i)
        {
            for (int j = 0; j < a.length; j++)
            {
                if (a[j].toInt() == i)
                {
                    return a[j];
                }
            }
            return BOBSTACLE;
        }

        public Event toEvent() {
            if (value == 0) {
                return new Event(DefaultAILStructure.AILAddition, DefaultAILStructure.AILBel, new Literal("obstacle"));
            } else if (value == 1) {
                return new Event(DefaultAILStructure.AILAddition, DefaultAILStructure.AILBel, new Literal("water"));
            } else if (value == 2){
                return new Event(DefaultAILStructure.AILAddition, DefaultAILStructure.AILBel, new Literal("path"));
            } else if (value == 3) {
                return new Event(DefaultAILStructure.AILAddition, new ail.syntax.Goal("obstacle"));
            } else if (value == 4) {
                return new Event(DefaultAILStructure.AILAddition, new ail.syntax.Goal("water"));
            } else  {
                return new Event(DefaultAILStructure.AILAddition, new ail.syntax.Goal("path"));
            }
        }

    }

    public enum RobotAction
    {
        NOTHING(0), //Index must match with string array rule-options in strings.xml
        STOP(1),
        FORWARD(2),
        FORWARD_A_BIT(3),
        BACKWARD(4),
        BACKWARD_A_BIT(5),
        LEFT(6),
        LEFT_A_BIT(7),
        LEFT_A_LOT(8),
        ARC_LEFT(9),
        RIGHT(10),
        RIGHT_A_BIT(11),
        RIGHT_A_LOT(12),
        ARC_RIGHT(13),
        ACHIEVE_OBSTACLE(14),
        ACHIEVE_PATH(15),
        ACHIEVE_WATER(16);


        static RobotAction[] a = RobotAction.values();

        int value;
        RobotAction(int i)
        {
            value = i;
        }

        public int toInt()
        {
            return value;
        }

        public static RobotAction fromInt(int i)
        {

            for (int j = 0; j < a.length; j++)
            {
                if (a[j].toInt() == i)
                {
                    return a[j];
                }
            }
            return NOTHING;
        }

        public Deed toAILAction() {
            if (value == 0) {
                return new Deed(new Action("nothing"));
            } else if (value == 1) {
                return new Deed(new Action("stop"));
            } else if (value == 2) {
                return new Deed(new Action("forward"));
            } else if (value == 3) {
                return new Deed(new Action("forward_a_bit"));
            } else if (value == 4) {
                return new Deed(new Action("backward"));
            } else if (value == 5) {
                return new Deed(new Action("backward_a_bit"));
            } else if (value == 6) {
                return new Deed(new Action("left"));
            } else if (value == 7) {
                return new Deed(new Action("left_a_bit"));
            } else if (value == 8) {
                return new Deed(new Action("left_a_lot"));
            } else if (value == 9) {
                return new Deed(new Action("forward_left"));
            } else if (value == 10) {
                return new Deed(new Action("right"));
            } else if (value == 11) {
                return new Deed(new Action("right_a_bit"));
            } else if (value == 12) {
                return new Deed(new Action("right_a_lot"));
            } else if (value == 13) {
                return new Deed(new Action("forward_right"));
            } else if (value == 14) {
                return new Deed(new ail.syntax.Goal("obstacle"));
            } else if (value == 15) {
                return new Deed(new ail.syntax.Goal("path"));
            } else if (value == 16) {
                return new Deed(new ail.syntax.Goal("water"));
            } else {
                    return new Deed(new Action("stop"));
            }
        }
    }
    private LinkedBlockingDeque<Pair<RobotAction, Long>> actions;

    public static class RobotRule
	{
		private boolean on;
		private robotEvent type;
		private RobotAction[] actions;
		private int onAppeared;

        private Plan agentPlan;

        @Override
        public boolean equals(Object o) {
            if (o instanceof RobotRule) {
                RobotRule r = (RobotRule) o;
                if (type != r.getType()) {
                    return false;
                }

                if (onAppeared != r.getOnAppeared()) {
                    return false;
                }

                if (on != r.getEnabled()) {
                    return false;
                }

                int i = 0;
                for (RobotAction a : actions) {
                    if (a != r.getAction(i)) {
                        return false;
                    }
                    i++;
                }
                return true;
            }
            return false;
        }

		public RobotRule()
		{
			on = false;
			type = robotEvent.BOBSTACLE;
			onAppeared = 0;
			actions = new RobotAction[]{RobotAction.NOTHING, RobotAction.NOTHING, RobotAction.NOTHING};
		}
		
		public RobotRule(boolean _on, robotEvent _type, int appeared, RobotAction action1, RobotAction action2, RobotAction action3)
		{
			type = _type;
			on = _on;
			onAppeared = appeared;
			actions = new RobotAction[]{action1, action2, action3};
		}

		public boolean getEnabled()
		{
			return on;
		}

		public int getOnAppeared()
		{
			return onAppeared;
		}

		public RobotAction getAction(int pos)
		{
			return actions[pos];
		}

		public robotEvent getType()
		{
			return type;
		}

        public void setPlan(Plan p) {
            agentPlan = p;
        }

        public Plan getPlan() {
            return agentPlan;
        }
	}
    private RobotRule[] rules;

    private Exception generatedException;
	private String btAddress;
    private boolean running;
	
	//private boolean obstacleChanged;
	// private boolean pathChanged;
	//private boolean waterChanged;

    // Used to track status in Find Water routine.
    private boolean pathFound;
    private Random rTurn = new Random();

    // Default settings for sensor thresholds that generate beliefs;
	private float objectDetected = 0.4f;
	private int blackMax = 50;
	private int waterMin = 50;
	private int waterMax = 100;
	private int waterRMax = 50;
	private int waterGMax = 50;

    // Controls for Navigation.
	private int speed = 10;
	private long delay = 0;
	private long untilAction = 0;

    // Constructor
    public BluetoothRobot()
    {
        actions = new LinkedBlockingDeque<Pair<RobotAction, Long>>();

        rules = new RobotRule[]{
                new RobotRule(),
                new RobotRule(),
                new RobotRule(),
                new RobotRule()
        };

        state = new BeliefSet();
        abstraction_engine = new Robot();
        mode = RobotMode.MANUAL;
        running = false;


        MAS mas = new MAS();
        mas.setEnv(env);
        try {
            reasoningengine = new EASSAgent("robot");
        } catch (AILexception aiLexception) {
            aiLexception.printStackTrace();
        }
        mas.addAg(reasoningengine);
        env.addAgent(reasoningengine);

        env.addRobot("robot", abstraction_engine);
    }

    @Override
    public void run()
    {
        try
        {
            //Connect to robot
            generatedException = null;
            status = ConnectStatus.CONNECTING;
            abstraction_engine.connectToRobot(btAddress);
            status = ConnectStatus.CONNECTED;
            float disInput;
            int curSpeed = speed;
            //While connected or no signal to disconnect
            while (status == ConnectStatus.CONNECTED)
            {
                // Perform 10 reasoning cycles in the reasoning engine.
                // NB. Actions performed by the reasoning cycle are passed directly
                // to the abstraction engine by the environment.
                 for (int i = 0; i < 10; i++) {
                    reasoningengine.reason();
                }

                // Get new information from sensors.
                if (abstraction_engine.isHome_edition()) {
                    disInput = abstraction_engine.getirSensor().getSample();
                    disInput = disInput/100;
                } else {
                    disInput = abstraction_engine.getusSensor().getSample();
                }
                float[] rgb = abstraction_engine.getRGBSensor().getRGBSample();

                // Update Beliefs
                state.colour = Color.rgb((int)(rgb[0] * 850), (int)(rgb[1] * 1026), (int)(rgb[2] * 1815));
                state.distance = disInput;
                updateBeliefs(disInput, state.colour);

                //Create newest copy of beliefs
                stateCopy = state.clone();

                // Update robot speed if necessary
                if (curSpeed != speed)
                {
                    abstraction_engine.setTravelSpeed(speed);
                    curSpeed = speed;
                }

                // Do something.
                switch(mode)
                {
                    case MANUAL:
                        doAction();
                        break;
                    case LINE:
                        doLine();
                        break;
                    case AVOID:
                        doAvoid();
                        break;
                    case WATER:
                        doWater();
                        break;
                }
            }

            //Clear beliefs before disconnect
            state.states.clear();
            state.colour = 0;
            state.distance = 0;

            disconnect();

        }
        catch (Exception e)
        {
            status = ConnectStatus.DISCONNECTING;
            if (abstraction_engine != null && abstraction_engine.isConnected())
            {
                abstraction_engine.close();
            }
            status = ConnectStatus.DISCONNECTED;
            generatedException = e;
        }
    }

    // Update both the belief set here (used for displaying information to the user) and
    // the beliefs in the Reasoning Engine.
	private void updateBeliefs(float distance, int colour)
	{
		boolean curObs = state.states.contains(BeliefStates.OBSTACLE);
		if (Float.compare(distance, objectDetected) < 0)
		{
			if (!state.states.contains(BeliefStates.OBSTACLE))
			{
				state.states.add(BeliefStates.OBSTACLE);
                env.addSharedBelief("robot", new Literal("obstacle"));
			}
		}
		else
		{
			if (state.states.contains(BeliefStates.OBSTACLE))
			{
                state.states.remove(BeliefStates.OBSTACLE);
                env.removeSharedBelief("robot", new Literal("obstacle"));
			}

		}

		int red = Color.red(colour);
		int blue = Color.blue(colour);
		int green = Color.green(colour);

		boolean curPath = state.states.contains(BeliefStates.PATH);
		if ((red < blackMax) && (blue < blackMax) && (green < blackMax))
		{
			if (!state.states.contains(BeliefStates.PATH))
			{
				state.states.add(BeliefStates.PATH);
                env.addSharedBelief("robot", new Literal("path"));
			}
		}
		else
		{
			if (state.states.contains(BeliefStates.PATH))
			{
				state.states.remove(BeliefStates.PATH);
                env.removeSharedBelief("robot", new Literal("path"));
			}
		}

		boolean curWater = state.states.contains(BeliefStates.WATER);
		if (((blue > green) && (blue > red)) && (blue > waterMin) && ((red < waterRMax) && (blue < waterMax) && (green < waterGMax)))
		{
			if (!state.states.contains(BeliefStates.WATER))
			{
				state.states.add(BeliefStates.WATER);
                env.addSharedBelief("robot", new Literal("water"));
			}
		}
		else
		{
			if (state.states.contains(BeliefStates.WATER))
			{
				state.states.remove(BeliefStates.WATER);
                env.removeSharedBelief("robot", new Literal("water"));
			}
		}

	}


    // Navigation Panel Support
	private void doAction()
	{
		if (actions.peek() != null)
		{
			Pair<RobotAction, Long> actionPair = actions.peek();
			if ((actionPair.second + delay > SystemClock.uptimeMillis() && status != ConnectStatus.DISCONNECTING))
			{
				untilAction = (actionPair.second + delay) - SystemClock.uptimeMillis();
			}
			else
			{
				actionPair = actions.poll();
				untilAction = 0;
				if (status == ConnectStatus.CONNECTED)
				{
					switch (actionPair.first)
					{
						case FORWARD:
							abstraction_engine.forward();
							break;
						case FORWARD_A_BIT:
							abstraction_engine.short_forward();
							break;
						case STOP:
							abstraction_engine.stop();
							break;
						case BACKWARD_A_BIT:
							abstraction_engine.short_backward();
							break;
						case BACKWARD:
							abstraction_engine.backward();
							break;
						case LEFT:
							abstraction_engine.left();
							break;
						case LEFT_A_BIT:
							abstraction_engine.very_short_left();
							break;
						case RIGHT:
							abstraction_engine.right();
							break;
						case RIGHT_A_BIT:
							abstraction_engine.very_short_right();
							break;
					}
				}
			}
		}
	}

    public void addAction(RobotAction action, boolean mustprocess)
    {
        if (actions.isEmpty() || mustprocess == true) {
            actions.add(new Pair<RobotAction, Long>(action, SystemClock.uptimeMillis()));
        }
    }

    public long getTimeToAction()
    {
        return untilAction;
    }

    public void updateManual(long delayMills, int _speed)
    {
        delay = delayMills;
        speed = _speed;
    }

    // Task Panel Options
	private void doAvoid()
	{
		if (running)
		{
			if (state.states.contains(BeliefStates.OBSTACLE))
			{
                if (! abstraction_engine.isTurning()) {
                    abstraction_engine.left();
                }
			}
			else
			{
                if (! abstraction_engine.isMovingStraight()) {
                    abstraction_engine.forward();
                }
			}
		}
		else
		{
			abstraction_engine.stop();
		}
	}

	private void doLine()
	{
		if (running)
		{
			if (!state.states.contains(BeliefStates.PATH))
			{
				abstraction_engine.forward_right();
			}
			else
			{
				abstraction_engine.forward_left();
			}
		}
		else
		{
			abstraction_engine.stop();
		}
	}

	private void doWater()
	{
		if (running)
		{
			if (!state.states.contains(BeliefStates.WATER))
			{
				if (!pathFound && state.states.contains(BeliefStates.PATH))
				{
					pathFound = true;
				}
				else if (pathFound)
				{
					doLine();
				}
				else if (!state.states.contains(BeliefStates.OBSTACLE))
				{
					abstraction_engine.forward();
				}
				else
				{
					abstraction_engine.turn(45 + rTurn.nextInt(135));
				}
			}
			else
			{
				// running = false;
				pathFound = false;
				abstraction_engine.stop();
			}
		}
		else
		{
			pathFound = false;
			abstraction_engine.stop();
		}
	}

    //public ArrayList<Literal> getBeliefs() {
    //    return reasoningengine.getBB().getAll();
    //}

    // Support for Rules Panel.
    public void addPlan(RobotRule r) {
        Plan p = new Plan();
        if (r.getOnAppeared() == 0) {
            p.setTrigger(r.getType().toEvent());
        } else {
            Event trigger = r.getType().toEvent();
            trigger.setTrigType(Event.AILDeletion);
            p.setTrigger(trigger);
        }

        p.setContextSingle(new Guard(), 3);
        ArrayList<Deed> deeds = new ArrayList<Deed>();
        deeds.add(r.getAction(2).toAILAction());
        deeds.add(r.getAction(1).toAILAction());
        deeds.add(r.getAction(0).toAILAction());
        p.setBody(deeds);

        ArrayList<Deed> prefix = new ArrayList<Deed>();
        prefix.add(new Deed(Deed.Dnpy));

        p.setPrefix(prefix);

        try {
            reasoningengine.addPlan(p);
            System.err.println(reasoningengine.getPL());
        } catch (Exception e) {

        }

        r.setPlan(p);

    }

    public void removePlan(RobotRule r) {
        reasoningengine.removePlan(r.getPlan());
    }

    public RobotRule[] getAllRules()
    {
        return rules;
    }

    public void changedRule(int pos, RobotRule rule)
    {
        if (! (rules[pos].equals(rule))) {
            RobotRule oldrule = rules[pos];
            if (oldrule.getEnabled()) {
                removePlan(oldrule);
            }
            rules[pos] = rule;
            if (rule.getEnabled()) {
                addPlan(rule);
            }
        }
    }

    public void setGoal(Goal goal) {
        if (goal != Goal.NONE) {
            Intention i = new Intention(new ail.syntax.Goal(goal.toLiteral().getFunctor()), AILAgent.refertoself());
            reasoningengine.getIntentions().add(i);
        }
    }

    // Connect Panel Support
    public void setChanged(boolean education_set) {
        abstraction_engine.setChanged(education_set);
    }

    public void setBTAddress(String address)
    {
        btAddress = address;
    }

    public ConnectStatus connectionStatus()
    {
        return status;
    }

    public boolean isConnected() {
        return (status == ConnectStatus.CONNECTED);
    }

    public void disconnect()
    {
        status = ConnectStatus.DISCONNECTING;
        setRunning(false);
        close();
        status = ConnectStatus.DISCONNECTED;
    }

    public void setDisconnecting() {
        if (status == ConnectStatus.CONNECTED) {
            status = ConnectStatus.DISCONNECTING;
        }
    }

    public void close()
    {
        if (abstraction_engine != null && abstraction_engine.isConnected())
        {
            abstraction_engine.close();
        }
    }

    // NB. Not agent messages but messages generated while trying to connect to robot
    public String getMessages()
    {
        if (abstraction_engine != null)
        {
            return abstraction_engine.getMessages();
        }
        else
        {
            return "No Current Messages";
        }
    }

    // Settings Panel Support
    public void changeSettings(float objectRange, int blackMaximum, int waterMinimum, int waterMaximum, int waterRMaximum, int waterGMaximum)
    {
        objectDetected = objectRange;
        blackMax = blackMaximum;
        waterMin = waterMinimum;
        waterMax = waterMaximum;
        waterRMax = waterRMaximum;
        waterGMax = waterGMaximum;
        //waterLightRange = new PointF(waterLower, waterUpper);
        //pathLight = pathRange;
    }

    // Set up and general setters and getters.
    public EASSAgent getReasoningEngine() {
        return reasoningengine;
    }

    public Exception getGeneratedException()
    {
        return generatedException;
    }

	public BeliefSet getBeliefSet()
	{
		return stateCopy;
	}

    public GoalSet getGoals() {return goals;}

	public void setMode(RobotMode _mode)
	{
		running = false;
		pathFound = false;
		mode = _mode;
	}

	public void setRunning(boolean _running)
	{
		actions.clear();
		running = _running;
	}

	public boolean getRunning()
	{
		return running;
	}

	//public int getColourFound()
	//{
	//	return state.colour;
	//}
}
