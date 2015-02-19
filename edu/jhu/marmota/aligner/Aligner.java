package edu.jhu.marmota.aligner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fig.basic.LogInfo;
import fig.basic.Option;
import fig.basic.Pair;
import fig.exec.Execution;

public class Aligner implements Runnable {
	@Option(name = "model", required = true, gloss = "currently support ibm1")
	public String model;

	@Option(name = "f", required = true, gloss = "foreign file")
	public String ffile;

	@Option(name = "e", required = true, gloss = "english file")
	public String efile;

	@Option(name = "a", required = true, gloss = "alignment file")
	public String afile;

	@Option(name = "maxiter", gloss = "setting the max iteration of em algorithm (DEFAULT = 100)")
	public int maxiter = 100;

	@Option(name = "threshold", gloss = "forget about this for now (DEFAULT = 1e-4)")
	public double threshold = 1e-4;
	
	@Override
	public void run() {
		if (model.equalsIgnoreCase("ibm1")) {
			IBM1 ibm1 = new IBM1(efile, ffile);
			IBM1aligner(ibm1);
		}
		else if (model.equalsIgnoreCase("hmm")) {
			HMM hmm = new HMM(efile, ffile);
			HMMaligner(hmm);
		}
		else if (model.equalsIgnoreCase("ibmhmm")) {
			IBM1 ibm1 = new IBM1(efile, ffile);
			IBM1aligner(ibm1);
			HMM hmm = new HMM(ibm1.get_param(), ffile);
			HMMaligner(hmm);
		}
		else {
			System.out.println(model + " is not supported yet.");
		}
	}
	
	public void IBM1aligner(IBM1 ibm1) {
		try {
			LogInfo.begin_track("training");
			// em training
			for (int iter = 0; iter < maxiter; iter++) {
				LogInfo.log("iteration " + String.valueOf(iter));
				// e-step
				BufferedReader ein = new BufferedReader(new FileReader(new File(efile)));
				BufferedReader fin = new BufferedReader(new FileReader(new File(ffile)));
				int linen = 0;
				while (true) {
					if (linen % 1000 == 0) {
						System.err.print(".");
					}
					
					String eline = ein.readLine();
					String fline = fin.readLine();

					if (eline == null || fline == null) {
						break;
					}

					ibm1.e_step(eline.split(" "), fline.split(" "));
					linen ++;
				}
				ein.close();
				fin.close();

				// m-step
				ibm1.m_step();
				ibm1.clear_sample();
			}
			LogInfo.end_track();
			
			// generating alignment
			LogInfo.begin_track("aligning");
			BufferedReader ein = new BufferedReader(new FileReader(new File(efile)));
			BufferedReader fin = new BufferedReader(new FileReader(new File(ffile)));
			BufferedWriter aout = new BufferedWriter(new FileWriter(new File(afile)));
			int linen = 0;
			while (true) {
				if (linen % 1000 == 0) {
					System.err.print(".");
				}
				
				String eline = ein.readLine();
				String fline = fin.readLine();
				
				if (eline == null || fline == null) {
					break;
				}
				
				String[] etoks = eline.split(" ");
				String[] ftoks = fline.split(" ");
				List<Pair<Integer, Integer>> wps = new ArrayList<Pair<Integer, Integer>>();
				for (int j = 0; j < etoks.length; j++) {
					// select the most possible foreign word for each english word
					int fi = 0;
					double max = 0.0;
					for (int i = 0; i < ftoks.length; i++) {
						if (ibm1.get_param(etoks[j], ftoks[i]) > max) {
							fi = i;
							max = ibm1.get_param(etoks[j], ftoks[i]);
						}
					}
					wps.add(new Pair<Integer, Integer>(fi, j));
				}
				String aline = "";
				for (Pair<Integer, Integer> wp: wps) {
					aline += (String.valueOf(wp.getFirst()) + "-" + String.valueOf(wp.getSecond()) + " ");
				}
				aline = aline.substring(0, aline.length() - 1);
				aout.write(aline + "\n");
				linen ++;
			}
			ein.close();
			fin.close();
			aout.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void HMMaligner(HMM hmm) {
		try {
			LogInfo.begin_track("training");
			// em training
			for (int iter = 0; iter < maxiter; iter++) {
				LogInfo.log("iteration " + String.valueOf(iter));
				LogInfo.log("e-step" + String.valueOf(iter));
				// e-step
				BufferedReader ein = new BufferedReader(new FileReader(new File(efile)));
				BufferedReader fin = new BufferedReader(new FileReader(new File(ffile)));
				int linen = 0;
				while (true) {
					if (linen % 1000 == 0) {
						System.err.print(".");
					}
					
					String eline = ein.readLine();
					String fline = fin.readLine();

					if (eline == null || fline == null) {
						break;
					}

					hmm.e_step(eline.split(" "), fline.split(" "));
					linen ++;
				}
				ein.close();
				fin.close();

				// m-step
				LogInfo.log("m-step" + String.valueOf(iter));
				hmm.m_step();
				hmm.clear_sample();
			}
			LogInfo.end_track();
			
			// generating alignment
			LogInfo.begin_track("aligning");
			BufferedReader ein = new BufferedReader(new FileReader(new File(efile)));
			BufferedReader fin = new BufferedReader(new FileReader(new File(ffile)));
			BufferedWriter aout = new BufferedWriter(new FileWriter(new File(afile)));
			int linen = 0;
			while (true) {
				if (linen % 1000 == 0) {
					System.err.print(".");
				}
				
				String eline = ein.readLine();
				String fline = fin.readLine();
				
				if (eline == null || fline == null) {
					break;
				}
				
				String[] etoks = eline.split(" ");
				String[] ftoks = fline.split(" ");
				
				// viterbi decoding
				List<Pair<Integer, Integer>> wps = new ArrayList<Pair<Integer, Integer>>();
				Map<Pair<Integer, Integer>, Double> mu = new HashMap<Pair<Integer, Integer>, Double>();
				Map<Pair<Integer, Integer>, Integer> bp = new HashMap<Pair<Integer, Integer>, Integer>();
				
				// for j = 0
				for (int i = 0; i < ftoks.length; i++) {
					double p = hmm.get_t(etoks[0], ftoks[i]) * hmm.get_a(i, -1);
					if (mu.get(new Pair<Integer, Integer>(0, i)) == null
							|| p > mu.get(new Pair<Integer, Integer>(0, i))) {
						mu.put(new Pair<Integer, Integer>(0, i), p);
					}
				}
				
				// for j = 1 to len - 1
				for (int j = 1; j < etoks.length; j++) {
					for (int i = 0; i < ftoks.length; i++) {
						for (int iprev = 0; iprev < ftoks.length; iprev++) {
							double p = hmm.get_t(etoks[j], ftoks[i]) * hmm.get_a(i, iprev);
							double bestmu = mu.get(new Pair<Integer, Integer>(j - 1, iprev)) * p;
							if (mu.get(new Pair<Integer, Integer>(j, i)) == null
									|| bestmu > mu.get(new Pair<Integer, Integer>(j, i))) {
								mu.put(new Pair<Integer, Integer>(j, i), bestmu);
								bp.put(new Pair<Integer, Integer>(j, i), iprev);
							}
						}
					}
				}
				
				// deciding the alignment of the last word
				double bestmu = 0.0;
				int inext = 0;
				for (int i = 0; i < ftoks.length; i++) {
					if (mu.get(new Pair<Integer, Integer>(etoks.length - 1, i)) > bestmu) {
						bestmu = mu.get(new Pair<Integer, Integer>(etoks.length - 1, i));
						inext = i;
					}
				}
				wps.add(new Pair<Integer, Integer>(inext, etoks.length - 1));
				
				// follow the backpointer
				for (int j = etoks.length - 1; j >= 1; j--) {
					int iprev = bp.get(new Pair<Integer, Integer>(j, inext));
					wps.add(new Pair<Integer, Integer>(iprev, j - 1));
					inext = iprev;
				}
				
				String aline = "";
				for (Pair<Integer, Integer> wp: wps) {
					aline += (String.valueOf(wp.getFirst()) + "-" + String.valueOf(wp.getSecond()) + " ");
				}
				aline = aline.substring(0, aline.length() - 1);
				aout.write(aline + "\n");
				linen ++;
			}
			ein.close();
			fin.close();
			aout.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		Aligner aligner = new Aligner();
		Execution.run(args, aligner);
	}
}
