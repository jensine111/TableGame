package GroupX;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import players.PlayerConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Sushi Go MCTS agent. Built from the allowed players.basicMCTS.BasicMCTSPlayer (TAG, GAIGResearch),
 * copied into this package so the submission depends on nothing outside groupX except TAG's public APIs.
 * The search itself lives in {@link SushiTreeNode}; see {@link SushiMCTSParams} for the ablation switches.
 */
public class SushiMCTSPlayer extends AbstractPlayer {

    // wall-clock duration of each search decision, read by the experiment harness
    long lastDecisionNanos;
    final List<Long> decisionNanos = new java.util.ArrayList<>();
    int fallbacks;  // searches that threw and fell back to a random legal action

    public SushiMCTSPlayer() {
        this(new SushiMCTSParams());
    }

    public SushiMCTSPlayer(long seed) {
        this(seededParams(seed));
    }

    public SushiMCTSPlayer(SushiMCTSParams params) {
        super(params, "SushiMCTS");
        rnd = new Random(params.getRandomSeed());
    }

    private static SushiMCTSParams seededParams(long seed) {
        SushiMCTSParams p = new SushiMCTSParams();
        p.setRandomSeed(seed);
        return p;
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        long start = System.nanoTime();
        try {
            SushiMCTSParams params = getParameters();
            int k = Math.max(1, params.nDeterminisations);
            if (k == 1) {
                // BasicMCTS behaviour: one tree on the observation TAG gave us
                SushiTreeNode root = new SushiTreeNode(this, null, gameState, rnd);
                root.mctsSearch(start, params.budget, params.breakMS);
                return root.bestAction();
            }
            // improvement C: k trees, each on a fresh redeterminisation of what we cannot see (copy(playerID)
            // keeps our own hand and any hands we have already seen, and resamples the rest), each given an
            // equal slice of the budget; the root visit counts are summed to choose the move
            Map<AbstractAction, Double> votes = new HashMap<>();
            // for a time budget the breakMS safety margin is taken once off the whole decision, not per slice
            boolean timed = params.budgetType == PlayerConstants.BUDGET_TIME;
            double slice = (double) (timed ? params.budget - params.breakMS : params.budget) / k;
            for (int i = 0; i < k; i++) {
                AbstractGameState det = gameState.copy(getPlayerID());
                SushiTreeNode root = new SushiTreeNode(this, null, det, rnd);
                // slices follow a fixed schedule from the decision start, so an overrun in one slice is taken
                // out of the next rather than extending the decision
                long sliceStart = start + (long) (i * slice * 1e6);
                double sliceBudget = slice;
                if (timed) {
                    long now = System.nanoTime();
                    sliceBudget = slice - (now - sliceStart) / 1e6;
                    sliceStart = now;
                }
                root.mctsSearch(sliceStart, Math.max(sliceBudget, 0.0), 0);
                root.addRootVisits(votes);
            }
            AbstractAction best = null;
            double bestV = -1;
            for (Map.Entry<AbstractAction, Double> e : votes.entrySet()) {
                double v = e.getValue() + rnd.nextDouble() * 1e-3;  // random tie-break
                if (v > bestV) {
                    bestV = v;
                    best = e.getKey();
                }
            }
            if (best == null)
                throw new AssertionError("No root action visited in any determinisation");
            return best;
        } catch (RuntimeException | AssertionError e) {
            // fail safe: report the problem loudly but still return a legal move so the game continues
            System.err.println("SushiMCTS search failed, falling back to random legal action: " + e);
            e.printStackTrace();
            fallbacks++;
            return actions.get(rnd.nextInt(actions.size()));
        } finally {
            lastDecisionNanos = System.nanoTime() - start;
            decisionNanos.add(lastDecisionNanos);
        }
    }

    public long getLastDecisionNanos() {
        return lastDecisionNanos;
    }

    public int getFallbacks() {
        return fallbacks;
    }

    public List<Long> getDecisionNanos() {
        return decisionNanos;
    }

    @Override
    public SushiMCTSParams getParameters() {
        return (SushiMCTSParams) parameters;
    }

    @Override
    public SushiMCTSPlayer copy() {
        SushiMCTSPlayer retValue = new SushiMCTSPlayer((SushiMCTSParams) parameters.copy());
        retValue.setName(toString());
        return retValue;
    }
}
