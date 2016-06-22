package EV3;

/**
 * Created by louiseadennis on 07/05/2016.
 */
public class QUtil {
    byte[][] state;
    byte[] previous_e;
    float previous_reward = -1f;
    // byte []action;

    public QUtil(byte states) {
        state = new byte[states][6]; // 64 unique states, 6 unique percepts
       // action = new byte[actions]; // 4 unique actions
       // initAction(action);
        previous_e = new byte[3];

        previous_e[0] = 0;
        previous_e[1] = 0;
        previous_e[2] = 0;

        initState(state);
    }


    // float previousLightValue = -1;
    // int repeating = 0; // counts if light value is changing

    public float getRewardValue(float[] e) {

        float reward = 0;
        // float lightValue = e[3];

        // If the light value is "black" or decreased, give a good reward:
        if (e[0] < 0.5) {
            reward = 4;
            if (e[1] > 0.5) {
                reward += 1;
            }

            if (e[2] > 0.5) {
                reward += 1;
            }
        } else {
            reward = -1;
            if (e[1] < 0.5 || e[2] < 0.5) {
                reward += 2;
            }
        }


        // If it has had the same light value for >5 times, penalize:
        // if(previousLightValue == lightValue) {
        //     ++repeating;
        // } else
        //    repeating = 0;
        // if(repeating > 5)
        //    reward -= 2;

        // previousLightValue = lightValue;

        // if bumper pressed, give bad reward
        //if(e[1] == 1||e[2] == 1)
        //    reward -= 2;
        float value = 2*reward - previous_reward;
        previous_reward = reward;

        return value;
    }

    /** Given an array of environment variables (sensor readings) and
     * a table of states this method returns a state ID number (assuming
     * there are 2x2x2 combinations).
     */
    public byte getState(float [] in){
        byte count = 0;

        byte[] e = new byte[6];
        if (in[0] < 0.5) { e[0] = 0;}
        else e[0] = 1;
        if (in[1] < 0.5) { e[1] = 0;}
        else e[1] = 1;
        if (in[2] < 0.5) { e[2] = 0;}
        else e[2] = 1;

        e[3] = previous_e[0];
        e[4] = previous_e[1];
        e[5] = previous_e[2];

        previous_e[0] = e[0];
        previous_e[1] = e[1];
        previous_e[2] = e[2];


        for(byte i=0;i<2;++i)
            for(byte j=0;j<2;++j)
                for(byte k=0;k<2;++k)
                    for(byte pi=0; pi<2; ++pi)
                        for(byte pj=0;pj<2; ++pj)
                            for(byte pk=0;pk<2;++pk) {
                                if (e[0] == state[count][0])
                                    if (e[1] == state[count][1])
                                        if (e[2] == state[count][2])
                                            if (e[3] == state[count][3])
                                                if (e[4] == state[count][4])
                                                    if (e[5] == state[count][5])
                                                        return count;
                                ++count;
                            }

        return -1;
    }

    /** Given an action id (1-4), it converts it to an array representing what
     * the 2 motor ports should do. A and C only!
     */
   // public void getCommands(byte a, byte[] commands) {
        //PCUtil.outPutTable(action);
 //       commands[0] = action[a][0];
  //      commands[1] = action[a][1];
   // }
//
    /**
     * Sets random numbers for a given table of floats.
     * Note: It doesn't return a variable because an array
     * is an object, so changes take place within Q.
     */
    public static void setRandomValues(float [][] Q) {
        for(byte i=0;i<Q.length;++i) {
            for(byte j=0;j<Q[0].length;++j) {
                Q[i][j] = (float)Math.random();
            }
        }
    }

    /** Initializes a 24x4 array with all the combinations the sensors
     * can provide for this robot, assuming there are 3x2x2x2 combinations.
     */
    public static void initState(byte [][] state) {
        byte count = 0;
        for(byte i=0;i<2;++i)
            for(byte j=0;j<2;++j)
                for(byte k=0;k<2;++k)
                    for(byte pi=0; pi<2; ++pi)
                        for(byte pj=0;pj<2; ++pj)
                            for(byte pk=0;pk<2;++pk) {
                                state[count][0] = i;
                                state[count][1] = j;
                                state[count][2] = k;
                                state[count][3] = pi;
                                state[count][4] = pj;
                                state[count][5] = pk;
                                ++count;
                            }
    }

    /** Initializes a 4x2 array with all the states the motors
     * can provide for this robot, assuming there are 3x3 combinations.
     * i.e. each of 2 motors can be in forward, reverse, and stop states.
     */
    //public static void initAction(byte [] state) {
  //      byte count = 0;
  //      for(byte i=0;i<4;++i)
  //          for(byte j=0;j<3;++j) {
  //              state[count] = i;
   //             state[count][1] = j;
  //              ++count;
  //          }
  //  }

}
