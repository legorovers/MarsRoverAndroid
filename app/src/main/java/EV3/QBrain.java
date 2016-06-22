package EV3;

/**
 * Created by louiseadennis on 07/05/2016.  Stolen from the internet http://www.informit.com/articles/article.aspx?p=26423
 */
public class QBrain {
    public static float LEARN_RATE = 0.35f; // the learning rate constant
    public float[] EXPLORE_RATES; // the rate it tries out random actions
    public static float EXPLORE_RATE_DECREASE = 0.9f;
    public static int number_of_actions = 3;

    float [][] Q; // table of Q-values (action,state)

    byte i = -1; // last state
    byte a = -1; // last action
    byte a1 = 0; // action with highest q-value

    float r = 0; // Last reward

    QUtil util;

    public QBrain(byte actions, byte states, QUtil util) {
        Q = new float[actions][states]; // table of Q-values for action-states
        EXPLORE_RATES = new float[states];
        for (int j = 0; j<states; j++) {
            EXPLORE_RATES[j] = 1f;
        }
        this.util = util;
        util.setRandomValues(Q);
    }

    /**
     * Returns an action given a set of environment percepts (e).
     */
    public byte getAction(float[] e) {

        byte j = util.getState(e); // convert e to a state id number.

        if(i>=0) {
            r = util.getRewardValue(e);
            a1 = getMaxAction(j);
            Q[a][i] = Q[a][i] + LEARN_RATE * (r + Q[a1][j] - Q[a][i]);
        }

        i = j;

        float rand = (float)Math.random();
        if(rand > EXPLORE_RATES[j]) {
            a = getMaxAction(j);
            System.err.println("Current maximum is " + a);
        } else {
            a = (byte)(Math.random() * number_of_actions);
            // josx.platform.rcx.Sound.beepSequence();
        }
        EXPLORE_RATES[j] = EXPLORE_RATES[j] * EXPLORE_RATE_DECREASE;
        return a;
    }

    // find the largest Q-value for a given state (j), and return action
    public byte getMaxAction(byte state) {
        float max = -1000;
        byte action = 0;

        for(byte a=0;a<Q.length;++a) {
            if(Q[a][state] > max) {
                max = Q[a][state];
                action = a;
            }
        }
        return action;
    }
}
