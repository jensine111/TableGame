package GroupE;

import core.interfaces.IStateHeuristic;
import core.AbstractGameState;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;
import utilities.Utils;
import core.components.Deck;
import java.util.List;

public class SushiGoHeuristic implements IStateHeuristic {

    @Override
    public double evaluateState(AbstractGameState gameState, int playerId) {
        SGGameState state = (SGGameState) gameState;

        if (!state.isNotTerminal()) {
            return state.getPlayerResults()[playerId].value;
        }

        double score = 0.0;

        // 1. Baseline permanent scores from prior rounds
        score += state.getGameScore(playerId) * 1.0;

        // 2. Safely count played cards for this round/game using the player's deck
        int myPuddings = 0;
        int mySashimi = 0;
        int myDumplings = 0;

        // Fetch all player decks without arguments, then index manually by playerId
        List<Deck<SGCard>> allPlayerDecks = state.getPlayedCards();

        if (allPlayerDecks != null && playerId < allPlayerDecks.size()) {
            Deck<SGCard> playedDeck = allPlayerDecks.get(playerId);

            if (playedDeck != null) {
                for (SGCard card : playedDeck.getComponents()) {
                    if (card.type == SGCard.SGCardType.Pudding) myPuddings++;
                    else if (card.type == SGCard.SGCardType.Sashimi) mySashimi++;
                    else if (card.type == SGCard.SGCardType.Dumpling) myDumplings++;
                }
            }
        }

        // 3. Apply Heuristic Weighting Rules
        // Pudding tracking
        score += myPuddings * 2.0;

        // Sashimi evaluation
        if (mySashimi == 1) {
            score += 0.5;
        } else if (mySashimi == 2) {
            score += 2.0;
        } else if (mySashimi >= 3) {
            score += 10.0 * (mySashimi / 3);
        }

        // Dumpling evaluation
        if (myDumplings == 1) {
            score += 1.0;
        } else if (myDumplings == 2) {
            score += 3.0;
        } else if (myDumplings == 3) {
            score += 6.0;
        } else if (myDumplings == 4) {
            score += 10.0;
        } else if (myDumplings >= 5) {
            score += 15.0;
        }

        return score;
    }
}