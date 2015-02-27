package edu.jhu.marmota.decoder;

public interface AbstractDecoder {
	
	/**
	 * the initialization should not rely on external information
	 */
	public void init();
	
	/**
	 * Default decoding -- give the input and return the best output
	 * 
	 * @param input
	 * @return
	 */
	public String decode(String input);
}
