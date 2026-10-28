package GroupX;

import players.basicMCTS.BasicMCTSParams;
//import players.mcts.MCTSParams;

/**
 * Parameter container for the Sushi Go! hybrid MCTS player.
 * Extends the allowed base framework parameter module and adds switchable toggles
 * for ablation testing and progressive bias weighting coefficients.
 */
public class SushiMCTSParams extends BasicMCTSParams {

    // Original Group X Ablation Modules
    public boolean useModuleA = true;
    public boolean useModuleB = true;
    public boolean useModuleC = true;

    // Integrated Group E Parameters
    public boolean useGroupEHeuristics = true;
    public double groupEHeuristicWeight = 0.5;

    @Override
    public void init() {
        super.init();
        // Set standard baseline budget constraints to safe defaults if not provided by JSON
        if (this.budget <= 0) {
            this.budget = 100; // default 100ms budget limit
        }
        if (this.breakMS <= 0) {
            this.breakMS = 10; // 10ms safety buffer boundary margin
        }
    }

    @Override
    public SushiMCTSParams copy() {
        SushiMCTSParams cp = new SushiMCTSParams();
        cp.useModuleA = this.useModuleA;
        cp.useModuleB = this.useModuleB;
        cp.useModuleC = this.useModuleC;
        cp.useGroupEHeuristics = this.useGroupEHeuristics;
        cp.groupEHeuristicWeight = this.groupEHeuristicWeight;

        // Copy standard MCTS fields to the new instance
        cp.budget = this.budget;
        cp.budgetType = this.budgetType;
        cp.breakMS = this.breakMS;
        cp.rolloutLength = this.rolloutLength;
        cp.K = this.K;
        return cp;
    }
}
