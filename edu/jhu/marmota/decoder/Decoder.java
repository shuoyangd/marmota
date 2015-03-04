package edu.jhu.marmota.decoder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import fig.basic.LogInfo;
import fig.basic.Option;
import fig.exec.Execution;

public class Decoder implements Runnable {
	@Option(name="algo", required=true, gloss="currently support [stack]")
	public String algo;
	
	@Option(name="f", required=true, gloss="file for foreign sentences")
	public String ffile;
	
	@Option(name="e", required=true, gloss="file for output translation")
	public String efile;
	
	@Option(name="pt", required=true, gloss="file for phrase table")
	public String ptfile;
	
	@Option(name="lm", required=true, gloss="file for language model")
	public String lmfile;
	
	@Option(name="maxStackSize", gloss="maximum stack size for the stack decoder (default:100)")
	public int maxStackSize = 100;
	
	@Option(name="distortionLimit", gloss="distortion limit of the decoding process (default: 5)")
	public int distortionLimit = 5;
	
	@Option(name="maxPhraseLength", gloss="maximum phrase length allowed for decoding process (default: 20)")
	public int maxPhraseLength = 20;
	
	@Override
	public void run() {
		if (algo.equals("stack")) {
			stackDecode();
		}
		else {
			System.err.println("model " + algo + " is not supported yet.");
		}
	}
	
	public void stackDecode() {
		try {
			NaiveStackDecoder decoder = new NaiveStackDecoder(ptfile, lmfile, maxStackSize, distortionLimit, maxPhraseLength);
			LogInfo.begin_track("initialization");
			decoder.init();
			LogInfo.end_track();
			
			LogInfo.begin_track("decoding");
			BufferedReader fin = new BufferedReader(new FileReader(new File(ffile)));
			BufferedWriter eout = new BufferedWriter(new FileWriter(new File(efile)));
			String line = fin.readLine();
			int linen = 0;
			while (line != null) {
				if (linen % 1 == 0) {
					System.err.print(".");
				}
				String output = decoder.decode(line);
				System.out.println(String.valueOf(linen) + output);
				eout.write(output + "\n");
				line = fin.readLine();
				linen++;
			}
			fin.close();
			eout.close();
			LogInfo.end_track();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static public void main(String[] args) {
		Decoder decoder = new Decoder();
		Execution.run(args, decoder);
	}
}
