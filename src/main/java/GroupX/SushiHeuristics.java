package GroupX;

import core.AbstractGameState;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;

/**
 * Heuristic scoring engine for Sushi Go!.
 * Converts Group E's complex card prioritization thresholds into bounded values
 * suitable for guiding tree lookaheads without distorting UCB bounds.
 */
public class SushiHeuristics {

    /**
     * Evaluates a given game state from the perspective of a specific player.
     * @return a normalized score between 0.0 (worst) and 1.0 (best).
     */
    public double evaluateState(AbstractGameState gameState, int playerId, SushiMCTSParams params) {
        if (!(gameState instanceof SGGameState)) {
            return 0.0;
        }

        SGGameState state = (SGGameState) gameState;
        double heuristicScore = 0.0;

        // Execute Group X's baseline module tracking first if active
        if (params.useModuleA) {
            heuristicScore += (state.getScore(playerId) * 0.01);
        }

        // Deep Integration of Group E's Game Mechanics Rules
        if (params.useGroupEHeuristics) {
            double comboValue = 0.0;
            int turnsLeftInRound = state.getTurnsLeftInRound();

            // 1. Porting Group E's Sashimi Progression Model (3 cards = 10 points)
            int sashimiCount = state.getPlayedCards(playerId).getCount(SGCard.SASHIMI);
            if (sashimiCount == 1 && turnsLeftInRound >= 3) {
                comboValue += 0.3; // Encourages starting a set early in the round
            } else if (sashimiCount == 2 && turnsLeftInRound >= 1) {
                comboValue += 0.9; // Extremely high priority to complete the set before the round ends
            }

            // 2. Porting Group E's Dumpling Scale Tracker (Exponential growth: 1, 3, 6, 10, 15)
            int dumplingCount = state.getPlayedCards(playerId).getCount(SGCard.DUMPLING);
            if (dumplingCount > 0 && dumplingCount < 5 && turnsLeftInRound >= (5 - dumplingCount)) {
                comboValue += (dumplingCount * 0.15); // Drives drafting subsequent dumplings
            }

            // 3. Porting Group E's Pudding End-Game Strategy (+6 for most, -6 for least)
            int puddingCount = state.getPuddingCount(playerId);
            int currentRound = state.getRoundCounter(); // Rounds 1, 2, or 3
            if (currentRound == 3) {
                comboValue += (puddingCount * 0.25); // Critical focus in the final round to avoid penalties
            } else {
                comboValue += (puddingCount * 0.05); // Subtle background hoarding in early rounds
            }

            heuristicScore += comboValue;
        }

        // Guarantee strict normalization to secure UCB mathematical convergence stability
        return Math.min(1.0, Math.max(0.0, heuristicScore));
    }
}
