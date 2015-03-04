package edu.jhu.marmota.lm;

import java.io.IOException;

/**
 * generic interface for implementing language model
 * @author shuoyang
 *
 */
public interface LM {

	/**
	 * load lm from dump file
	 * @param dir
	 * @return the maximum n number of the ngram
	 * @throws IOException
	 */
	public int load(String dir) throws IOException;
	
	/**
	 * return the beginning state
	 * @return
	 */
	public String begin();
	
	/**
	 * score the wellness of a state followed by the end symbol 
	 * @param state
	 * @return
	 */
	public String end(String...words);
	
	/**
	 * score the wellness of a word sequence
	 * @param state
	 * @param babble
	 * @return
	 */
	public double score(String babble);
	
	/**
	 * get the n of grams the model looks for history
	 * @return
	 */
	public int ngram();
}
