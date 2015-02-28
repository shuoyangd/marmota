package edu.jhu.marmota.lm;

import java.io.IOException;

/**
 * generic interface for implementing language model
 * @author shuoyang
 *
 */
public interface LM {

	/**
	 * load LM from dump file
	 * @param dir
	 * @return
	 * @throws IOException 
	 */
	public void load(String dir) throws IOException;
	
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
	 * @param word
	 * @return
	 */
	public double score(String...words);
}
