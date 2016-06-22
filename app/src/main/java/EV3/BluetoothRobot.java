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
import ail.syntax.Action;
import ail.syntax.Literal;
import ail.syntax.Plan;
import ail.syntax.Deed;
import ail.syntax.Event;
import ail.syntax.Guard;
import ail.util.AILexception;
import eass.semantics.EASSAgent;

/**
 * Thread that manages the connection to the EV3, also manages Beliefs, Rules and actions
 *
 * Created by joecollenette on 02/07/2015.
 */
public class BluetoothRobot implements Runnable
{
	public enum ConnectStatus {CONNECTED, DISCONNECTED, CONNECTING, DISCONNECTING}

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

	public enum RobotMode {MANUAL, AVOID, WATER, LINE}

	public enum RobotAction
	{
		NOTHING(0), //Index must match with string array rule-options in strings.xml
		FORWARD(1),
		FORWARD_A_BIT(2),
		BACKWARD(3),
		BACKWARD_A_BIT(4),
		LEFT(5),
		LEFT_A_BIT(6),
		RIGHT(7),
		RIGHT_A_BIT(8),
		STOP(9),
		ARC_LEFT(10),
		ARC_RIGHT(11);


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

        public Action toAILAction() {
            if (value == 0) {
                return new Action("nothing");
            } else if (value == 1) {
                return new Action("forward");
            } else if (value == 2) {
                return new Action("forward_a_bit");
            } else if (value == 3) {
                return new Action("backward");
            } else if (value == 4) {
                return new Action("backward_a_bit");
            } else if (value == 5) {
                return new Action("left");
            } else if (value == 6) {
                return new Action("left_a_bit");
            } else if (value == 7) {
                return new Action("right");
            } else if (value == 8) {
				return new Action("right_a_bit");
			} else if (value == 10) {
				return new Action("forward_left");
            } else if (value == 11) {
				return new Action("forward_right");
			} else {
                return new Action("stop");
            }
        }
	}

	//Implements cloneable so when access to the arraylist is outside of this thread
	//there will be no clashes during the belief update.
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

	public static class RobotRule
	{
		private boolean on;
		private BeliefStates type;
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
			type = BeliefStates.OBSTACLE;
			onAppeared = 0;
			actions = new RobotAction[]{RobotAction.NOTHING, RobotAction.NOTHING, RobotAction.NOTHING};
		}
		
		public RobotRule(boolean _on, BeliefStates _type, int appeared, RobotAction action1, RobotAction action2, RobotAction action3)
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

		public BeliefStates getType()
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

    private Robot abstraction_engine;
    private Exception generatedException;
	private String btAddress;
	private LinkedBlockingDeque<Pair<RobotAction, Long>> actions;
	private ConnectStatus status = ConnectStatus.DISCONNECTED;

	private RobotRule[] rules;
	private RobotMode mode;
	private boolean running;
	
	private BeliefSet state;
	private BeliefSet stateCopy;
	private boolean obstacleChanged;
	private boolean pathChanged;
	private boolean waterChanged;
	private boolean pathFound;

	private float objectDetected = 0.4f;
	private int blackMax = 50;
	private int waterMin = 50;
	private int waterMax = 100;
	private int waterNBMax = 50;
	private Random rTurn = new Random();
	//private float pathLight = 0.09f;
	//private PointF waterLightRange = new PointF(0.06f, 0.09f);
	private int speed = 10;
	private long delay = 0;
	private long untilAction = 0;

    EASSEV3Environment env = new EASSEV3Environment();
    EASSAgent reasoningengine;

    // private int beliefchangecounter = 0;

	private void updateBeliefs(float distance, int colour)
	{
		boolean curObs = state.states.contains(BeliefStates.OBSTACLE);
		if (Float.compare(distance, objectDetected) < 0)
		{
			if (!state.states.contains(BeliefStates.OBSTACLE))
			{
				state.states.add(BeliefStates.OBSTACLE);
                env.addSharedBelief("robot", new Literal("obstacle"));
                // beliefchangecounter = 0;
			}
		}
		else
		{
			if (state.states.contains(BeliefStates.OBSTACLE))
			{
              //   if (beliefchangecounter > 2) {
                    state.states.remove(BeliefStates.OBSTACLE);
                    env.removeSharedBelief("robot", new Literal("obstacle"));
              //      beliefchangecounter = 0;
              //  } else {
             //       beliefchangecounter++;
             //   }
			}

		}
		obstacleChanged = curObs != state.states.contains(BeliefStates.OBSTACLE);
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
		pathChanged = curPath != state.states.contains(BeliefStates.PATH);

		boolean curWater = state.states.contains(BeliefStates.WATER);
		if (((blue > green) && (blue > red)) && (blue > waterMin) && ((red < waterNBMax) && (blue < waterMax) && (green < waterNBMax)))
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
		waterChanged = curWater != state.states.contains(BeliefStates.WATER);
		// System.err.println("current beliefs are: " + env.getPercepts("robot", false));

	}

	/* private void checkRules()
	{
		for (int i = 0; i < rules.length; i++)
		{
			RobotRule rule = rules[i];
			boolean doActions = false;
			if (rule.getEnabled())
			{
				boolean onAppeared = (rule.getOnAppeared() == 0);
				switch (rule.getType())
				{
					case WATER:
						doActions = waterChanged && (onAppeared == state.states.contains(BeliefStates.WATER));
						break;
					case OBSTACLE:
						doActions = obstacleChanged && (onAppeared == state.states.contains(BeliefStates.OBSTACLE));
						break;
					case PATH:
						doActions = pathChanged && (onAppeared == state.states.contains(BeliefStates.OBSTACLE));
						break;
				}
				if (doActions)
				{
					//Go backwards so actions are put in order
					for (int j = rule.actions.length - 1; j >= 0; j--)
					{
						//Add action to the front of the queue with the delay removed so that the action happens immediately
						actions.addFirst(new Pair<RobotAction, Long>(rule.getAction(j), SystemClock.uptimeMillis() - delay));
					}
				}
			}
		}
	} */

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
							abstraction_engine.short_left();
							break;
						case RIGHT:
							abstraction_engine.right();
							break;
						case RIGHT_A_BIT:
							abstraction_engine.short_right();
							break;
					}
				}
			}
		}
	}

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

	byte learning_actions = 3;
	byte states  = 64;
	QUtil qutil;
	QBrain qbrain;
	private boolean initialise_learning = true;
	private void doLearning() {
		if (running) {
			float[] e = new float[4];
			if (initialise_learning) {
				initialise_learning = false;
				qutil = new QUtil(states);
				qbrain = new QBrain(learning_actions, states, qutil);
			}

			getEnvironmentVals(e);
			byte a = qbrain.getAction(e);

			switch (a) {
				case 0:
					abstraction_engine.short_forward();
                    break;
				case 1:
					abstraction_engine.short_forward_left();
                    break;
				case 2:
					abstraction_engine.short_forward_right();
                    break;
			}
		} else {
			initialise_learning = true;
		}
	}

	public void getEnvironmentVals(float[] e) {
		float rgb_center = abstraction_engine.getRGBSensor().getRGBSample()[0]*850;
		float rgb_left = abstraction_engine.getLeftRGBSensor().getRGBSample()[0]*850;
		float rgb_right = abstraction_engine.getRightRGBSensor().getRGBSample()[0]*850;

		if (state.states.contains(BeliefStates.PATH)) {
			e[0] = 0;
		} else {
			e[0] = 1;
		}

		if (rgb_left < blackMax) {
			e[1] = 0;
		} else {
			e[1] = 1;
		}

		if (rgb_right < blackMax) {
			e[2] = 0;
		} else {
			e[2] = 1;
		}

		e[3] = rgb_center;
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
				running = false;
				abstraction_engine.stop();
			}
		}
		else
		{
			abstraction_engine.stop();
		}
	}

    public ArrayList<Literal> getBeliefs() {
        return reasoningengine.getBB().getAll();
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
                // env.eachrun();
                for (int i = 0; i < 10; i++) {
                    reasoningengine.reason();
                    // System.err.println(reasoningengine);
                }

				if (abstraction_engine.isHome_edition()) {
					disInput = abstraction_engine.getirSensor().getSample();
                    disInput = disInput/100;
				} else {
					disInput = abstraction_engine.getusSensor().getSample();
				}
				float[] rgb = abstraction_engine.getRGBSensor().getRGBSample();
				//Log.w("Colour Values", "R - " + (int) (rgb[0] * 850) + " G - " + (int) (rgb[1] * 1026) + " B - " + (int) (rgb[2] * 1815));
				state.colour = Color.rgb((int)(rgb[0] * 850), (int)(rgb[1] * 1026), (int)(rgb[2] * 1815));
				state.distance = disInput;
				updateBeliefs(disInput, state.colour);
				//Create newest copy of beliefs
				stateCopy = state.clone();
				if (curSpeed != speed)
				{
					abstraction_engine.setTravelSpeed(speed);
					curSpeed = speed;
				}

				switch(mode)
				{
					case MANUAL:
						// checkRules();
						doAction();
						break;
					case LINE:
						doLine();
						break;
					case AVOID:
						doAvoid();
						break;
					case WATER:
						doLearning();
						break;
				}
			}
			//Clear beliefs before disconnect
			state.states.clear();
			state.colour = 0;
			state.distance = 0;
			close();
			status = ConnectStatus.DISCONNECTED;

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

    public boolean isConnected() {
        return (status == ConnectStatus.CONNECTED);
    }

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

        // EASSAgent fake_abstractionengine = new EASSagent

        env.addRobot("robot", abstraction_engine);
	}

	public void addAction(RobotAction action, boolean mustprocess)
	{
		if (actions.isEmpty() || mustprocess == true) {
			actions.add(new Pair<RobotAction, Long>(action, SystemClock.uptimeMillis()));
		}
	}

    public EASSAgent getReasoningEngine() {
        return reasoningengine;
    }

	public RobotRule[] getAllRules()
	{
		return rules;
	}

    public Exception getGeneratedException()
    {
        return generatedException;
    }

	public void setBTAddress(String address)
	{
		btAddress = address;
	}

	public ConnectStatus connectionStatus()
	{
		return status;
	}

	public void disconnect()
	{
        status = ConnectStatus.DISCONNECTING;
        setRunning(false);
		close();
	}

	public void close()
	{
		if (abstraction_engine != null && abstraction_engine.isConnected())
		{
			abstraction_engine.close();
		}
	}

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

	public void changeSettings(float objectRange, int blackMaximum, int waterMinimum, int waterMaximum, int waterNBMaximum)
	{
		objectDetected = objectRange;
		blackMax = blackMaximum;
		waterMin = waterMinimum;
		waterMax = waterMaximum;
		waterNBMax = waterNBMaximum;
		//waterLightRange = new PointF(waterLower, waterUpper);
		//pathLight = pathRange;
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

	public BeliefSet getBeliefSet()
	{
		return stateCopy;
	}

	public void setMode(RobotMode _mode)
	{
		running = false;
		pathFound = false;
		mode = _mode;
	}

	public long getTimeToAction()
	{
		return untilAction;
	}

	public void setRunning(boolean _running)
	{
		actions.clear();
		running = _running;
	}

	public void updateManual(long delayMills, int _speed)
	{
		delay = delayMills;
		speed = _speed;
	}

	public boolean getRunning()
	{
		return running;
	}

	public int getColourFound()
	{
		return state.colour;
	}

    public void addPlan(RobotRule r) {
        Plan p = new Plan();
        if (r.getOnAppeared() == 0) {
            p.setTrigger(new Event(Event.AILAddition, Event.AILBel, r.getType().toLiteral()));
        } else {
            p.setTrigger(new Event(Event.AILDeletion, Event.AILBel, r.getType().toLiteral()));
        }

        p.setContextSingle(new Guard(), 3);
        ArrayList<Deed> deeds = new ArrayList<Deed>();
        deeds.add(new Deed(r.getAction(2).toAILAction()));
        deeds.add(new Deed(r.getAction(1).toAILAction()));
        deeds.add(new Deed(r.getAction(0).toAILAction()));
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

    public void setChanged(boolean education_set) {
        abstraction_engine.setChanged(education_set);
    }
}
