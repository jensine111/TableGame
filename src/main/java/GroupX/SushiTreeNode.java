package GroupX;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.basicMCTS.BasicMCTSPlayer;
import players.basicMCTS.BasicTreeNode;

import java.util.Random;

/**
 * Custom Search Tree Node for Sushi Go!.
 * Inherits directly from the framework's allowed basic tree node template.
 * Injects domain-specific heuristics as an active progressive bias term during selection loops.
 */
public class SushiTreeNode extends BasicTreeNode {

    private final SushiMCTSPlayer player;
    private final SushiMCTSParams params;
    private final SushiHeuristics heuristicsProcessor;
    private final Random rnd;
    private final AbstractAction action;

    /**
     * Root Node Constructor. Called directly by SushiMCTSPlayer loop.
     */
    public SushiTreeNode(SushiMCTSPlayer player, AbstractGameState state, Random rnd) {
        super(null, state, null);
        this.player = player;
        this.params = player.getParameters();
        this.heuristicsProcessor = new SushiHeuristics();
        this.rnd = rnd;

        if (state != null) {
            // Populate framework action registers for exploration tracking
            this.actions = player.getForwardModel().computeAvailableActions(state);
        }
    }

    /**
     * Internal Child Node Constructor. Used during expansion phases.
     */
    protected SushiTreeNode(SushiTreeNode parent, AbstractAction action, AbstractGameState state) {
        super(parent, state, action);
        this.player = parent.player;
        this.params = parent.params;
        this.heuristicsProcessor = parent.heuristicsProcessor;
        this.rnd = parent.rnd;

        if (state != null) {
            this.actions = player.getForwardModel().computeAvailableActions(state);
        }
    }

    /**
     * Executes iterations of selection, expansion, simulation, and backpropagation
     * within the allocated time/iteration budget limits.
     */
    public void mctsSearch(long startTimeNanos, double budget, double breakMS) {
        while (hasRemainingBudget(startTimeNanos, budget, breakMS)) {
            // Phase 1 & 2: Selection and Expansion (Tree Policy)
            SushiTreeNode selectedNode = selectOrExpand();

            // Phase 3: Rollout Simulation
            double[] simulationRewards = selectedNode.executeRollout();

            // Phase 4: Backpropagation up the tree
            selectedNode.backpropagate(simulationRewards);
        }
    }

    private boolean hasRemainingBudget(long startNanos, double budget, double breakMS) {
        if (params.budgetType == core.players.PlayerConstants.BUDGET_ITERATIONS) {
            return this.nVisits < budget;
        }
        double elapsedMS = (System.nanoTime() - startNanos) / 1e6;
        return elapsedMS < (budget - breakMS);
    }

    private SushiTreeNode selectOrExpand() {
        SushiTreeNode current = this;
        while (!current.state.isTerminal()) {
            if (current.children == null || current.children.length < current.actions.size()) {
                return current.expand();
            } else {
                current = current.applySelectionPolicy();
            }
        }
        return current;
    }

    private SushiTreeNode expand() {
        int childIndex = (this.children == null) ? 0 : this.children.length;
        AbstractAction nextAction = this.actions.get(childIndex);

        AbstractGameState nextState = this.state.copy();
        player.getForwardModel().next(nextState, nextAction);

        SushiTreeNode childNode = new SushiTreeNode(this, nextAction, nextState);

        // Append new branch to framework basic node tracking arrays safely
        BasicTreeNode[] updatedChildren = new BasicTreeNode[childIndex + 1];
        if (this.children != null) {
            System.arraycopy(this.children, 0, updatedChildren, 0, this.children.length);
        }
        updatedChildren[childIndex] = childNode;
        this.children = updatedChildren;

        return childNode;
    }

    private double[] executeRollout() {
        AbstractGameState rolloutState = this.state.copy();
        int currentDepth = 0;

        // Bounded rollout depth loop using standard basic random move patterns
        while (!rolloutState.isTerminal() && currentDepth < params.rolloutLength) {
            java.util.List<AbstractAction> moves = player.getForwardModel().computeAvailableActions(rolloutState);
            AbstractAction chosenMove = moves.get(rnd.nextInt(moves.size()));
            player.getForwardModel().next(rolloutState, chosenMove);
            currentDepth++;
        }

        int totalPlayers = rolloutState.getNPlayers();
        double[] endgameRewards = new double[totalPlayers];
        for (int i = 0; i < totalPlayers; i++) {
            endgameRewards[i] = rolloutState.getScore(i);
        }
        return endgameRewards;
    }

    private void backpropagate(double[] rewards) {
        // Recursively or iteratively pass values backwards up the nodes to the root.
        // Adjust array properties cleanly to map framework data parameters.
        SushiTreeNode runner = this;
        while (runner != null) {
            runner.nVisits++;
            for (int p = 0; i < rewards.length; p++) {
                runner.totValue[p] += rewards[p];
            }
            // Trace execution vertically back to root node space
            runner = (SushiTreeNode) runner.parent;
        }
    }

    /**
     * Intercepts standard basic selection equations to include Group E domain heuristics
     * as an active Progressive Bias term.
     */
    private SushiTreeNode applySelectionPolicy() {
        SushiTreeNode bestChildNode = null;
        double bestChildScore = -Double.MAX_VALUE;

        for (BasicTreeNode frameworkChild : this.children) {
            SushiTreeNode child = (SushiTreeNode) frameworkChild;

            // Extract baseline abstract framework UCB mathematical boundaries
            double baseScore = child.getUCB1();

            // =========================================================================
            // CORE UPGRADE: PROGRESSIVE HEURISTIC BIAS INJECTION LOOP
            // =========================================================================
            if (params.useGroupEHeuristics) {
                double heuristicsVal = heuristicsProcessor.evaluateState(child.state, this.state.getCurrentPlayer(), params);

                // Progressive Bias: Highly effective on low sample sizes,
                // decays smoothly as direct visit trials accumulate.
                double earlyExplorationBias = (params.groupEHeuristicWeight * heuristicsVal) / (child.nVisits + 1);
                baseScore += earlyExplorationBias;
            }
            // =========================================================================

            if (baseScore > bestChildScore) {
                bestChildScore = baseScore;
                bestChildNode = child;
            }
        }
        return bestChildNode;
    }

    /**
     * Returns the optimal action chosen by the search tree to play in the live match.
     */
    public AbstractAction getBestAction() {
        SushiTreeNode eliteChild = null;
        double maximumVisits = -1;

        if (this.children == null) return null;

        for (BasicTreeNode frameworkChild : this.children) {
            if (frameworkChild.nVisits > maximumVisits) {
                maximumVisits = frameworkChild.nVisits;
                eliteChild = (SushiTreeNode) frameworkChild;
            }
        }
        return eliteChild != null ? eliteChild.action : null;
    }

    /**
     * Accumulates visit frequencies to support Multi-Tree Voter patterns.
     */
    public void poolVisitsToVotes(java.util.Map<AbstractAction, Double> voteRegistry) {
        if (this.children == null) return;

        for (BasicTreeNode frameworkChild : this.children) {
            SushiTreeNode child = (SushiTreeNode) frameworkChild;
            if (child.action != null) {
                double accumulatedWeight = voteRegistry.getOrDefault(child.action, 0.0);
                voteRegistry.put(child.action, accumulatedWeight + child.nVisits);
            }
        }
    }
}


