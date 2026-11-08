package nikedemos.markovnames.generators;

import nikedemos.markovnames.MarkovDictionary;

public class MarkovOldNorse extends MarkovGenerator {

	public MarkovOldNorse(int seqlen) {
		markov = new MarkovDictionary("old_norse_bothgenders.txt", seqlen);
		name = "markov.oldNorse";
	}

	@Override
	public String fetch(int gender) { return markov.generateWord(); }

}
