package GroupE;

import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.AbstractGameState;
import players.basicMCTS.BasicMCTSPlayer; // 🎯 Flat folder path structure
import players.basicMCTS.BasicMCTSParams; // 🎯 Flat folder path structure
import java.util.List;

/**
 * Custom Sushi Go Agent inheriting directly from BasicMCTSPlayer.
 * Reflects the precise players.basicMCTS folder layout.
 */
public class SushiGoAgent extends BasicMCTSPlayer {

    // Default constructor required by the framework
    public SushiGoAgent() {
        this(new BasicMCTSParams());
    }

    // Parameterized constructor using the correct local BasicMCTSParams class
    public SushiGoAgent(BasicMCTSParams params) {
        super(params);

        // Inject your custom Sushi Go heuristic directly into the tree search parameters
        params.heuristic = new SushiGoHeuristic();
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> actions) {
        // Runs the Basic MCTS search algorithm augmented with your Group E heuristic
        return super._getAction(gameState, actions);
    }

    @Override
    public String toString() {
        return "Group E BasicMCTS Sushi Go Agent";
    }
}
