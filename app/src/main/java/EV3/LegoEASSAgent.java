package EV3;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ail.syntax.Deed;
import ail.syntax.Goal;
import ail.syntax.GoalBase;
import ail.syntax.Intention;
import ail.syntax.Unifiable;
import eass.semantics.EASSAgent;
import ail.util.AILexception;

/**
 * Created by louiseadennis on 27/06/2017.
 */

public class LegoEASSAgent extends EASSAgent {

    public LegoEASSAgent(String agname) throws AILexception {
        super(agname);
    }

    public synchronized void do_reason() {
        super.reason();
    }

    public synchronized Iterator<Goal> getPrintGoals() {
        ArrayList gs = new ArrayList();
        Iterator var3 = this.gbmap.values().iterator();

        while(var3.hasNext()) {
            GoalBase g = (GoalBase)var3.next();
            gs.addAll(g.getAll());
        }


        Intention i = getIntention();
        if (i != null && ! i.empty() && i.hdE().isStart()) {
            if (i.hdD().getContent() instanceof Goal) {
                Goal gl = (Goal) i.hdD().getContent();
                gs.add(gl.clone());
             }
        }

        List<Intention> is = getIntentions();
        for (Intention in: is) {
            if (!in.empty() && in.hdE().isStart()) {
                if (in.hdD().getContent() instanceof Goal) {
                    Goal gl = (Goal) in.hdD().getContent();
                    gs.add(gl);
                }
            }
        }

        // Log.i("infoTag", toString());
        return gs.iterator();
    }

    public synchronized void removeAllGoals() {
        Iterator var3 = this.gbmap.values().iterator();

        while(var3.hasNext()) {
            GoalBase g = (GoalBase)var3.next();
            for (Goal goal: g.getAll()) {
                removeGoal(goal);
            }
        }

        Intention i = getIntention();
        if (i!= null && !i.empty() && i.hdE().referstoGoal()) {
            setIntention(null);
        } else if (i!= null && !i.empty() && i.hdE().isStart()) {
            if (i.hdD().getContent() instanceof Goal) {
                setIntention(null);
            }
        }

        List<Intention> is = getIntentions();
        for (Intention in: is) {
            if (!in.empty() && in.hdE().referstoGoal()) {
                is.remove(in);
            } else if (!in.empty() && in.hdE().isStart()) {
                if (in.hdD().getContent() instanceof Goal) {
                    is.remove(in);
                }
            }
        }

    }
}
