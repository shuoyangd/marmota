package edu.jhu.marmota.aligner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import edu.jhu.marmota.util.PairCounter;
import edu.jhu.marmota.util.Triple;
import fig.basic.LogInfo;
import fig.basic.Pair;

/**
 * HMM word alignment model, as described in (Vogel et al. 1996).
 * 
 * @author shuoyang
 *
 */
public class HMM {
	PairCounter<String> t;
	PairCounter<Integer> a;
	PairCounter<String> lexSample;
	PairCounter<Integer> stateSample;
	
	public HMM(PairCounter<String> t, String ffile) {
		this.t = t;
		
		a = new PairCounter<Integer>(false, false);
		lexSample = new PairCounter<String>(false, true);
		stateSample = new PairCounter<Integer>(false, true);

		LogInfo.begin_track("initializing");
		try {
			BufferedReader fin = new BufferedReader(new FileReader(new File(
					ffile)));
			int linen = 0;
			while (true) {
				if (linen % 1000 == 0) {
					System.err.print(".");
				}
				
				String fline = fin.readLine();

				if (fline == null) {
					break;
				}
				
				String[] ftoks = fline.split(" ");
//				Random rand = new Random();
				for (int i = 0; i < ftoks.length; i++) {
					for (int iprev = 0; iprev < ftoks.length; iprev++) {
//						if (a.count(new Pair<Integer, Integer>(i, iprev)) == 0.0) {
//							a.increment(new Pair<Integer, Integer>(i, iprev),
//									rand.nextDouble());
//						}
						if (a.count(new Pair<Integer, Integer>(i, iprev)) == 0.0) {
							a.increment(new Pair<Integer, Integer>(i, iprev),
									1.0 / ftoks.length);
						}
					}
//					if (a.count(new Pair<Integer, Integer>(i, -1)) == 0.0) {
//						a.increment(new Pair<Integer, Integer>(i, -1),
//								rand.nextDouble());
//					}
					if (a.count(new Pair<Integer, Integer>(i, -1)) == 0.0) {
						a.increment(new Pair<Integer, Integer>(i, -1),
								1.0 / ftoks.length);
					}
				}
				linen++;
			}
			LogInfo.end_track();
			fin.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public HMM(String efile, String ffile) {
		t = new PairCounter<String>(false, false);
		a = new PairCounter<Integer>(false, false);
		lexSample = new PairCounter<String>(false, true);
		stateSample = new PairCounter<Integer>(false, true);

		LogInfo.begin_track("initializing");
		try {
			BufferedReader ein = new BufferedReader(new FileReader(new File(
					efile)));
			BufferedReader fin = new BufferedReader(new FileReader(new File(
					ffile)));
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
				Random rand = new Random();
				for (int j = 0; j < etoks.length; j++) {
					for (int i = 0; i < ftoks.length; i++) {
						t.increment(
								new Pair<String, String>(etoks[j], ftoks[i]),
								rand.nextDouble());
					}
				}
				for (int i = 0; i < ftoks.length; i++) {
					for (int iprev = 0; iprev < ftoks.length; iprev++) {
//						if (a.count(new Pair<Integer, Integer>(i, iprev)) == 0.0) {
//							a.increment(new Pair<Integer, Integer>(i, iprev),
//									rand.nextDouble());
//						}
						if (a.count(new Pair<Integer, Integer>(i, iprev)) == 0.0) {
							a.increment(new Pair<Integer, Integer>(i, iprev),
									1.0 / ftoks.length);
						}
					}
//					if (a.count(new Pair<Integer, Integer>(i, -1)) == 0.0) {
//						a.increment(new Pair<Integer, Integer>(i, -1),
//								rand.nextDouble());
//					}
					if (a.count(new Pair<Integer, Integer>(i, -1)) == 0.0) {
						a.increment(new Pair<Integer, Integer>(i, -1),
								1.0 / ftoks.length);
					}
				}
				linen++;
			}
			LogInfo.end_track();
			ein.close();
			fin.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void e_step(String[] es, String[] fs) {
		// for the first word
		double z = 0;
		Map<Integer, Double> q0 = new HashMap<Integer, Double>();
		Map<Integer, Double> p_iprev = new HashMap<Integer, Double>();
		for (int i = 0; i < fs.length; i++) {
			Pair<String, String> wp = new Pair<String, String>(es[0], fs[i]);
			Pair<Integer, Integer> ip = new Pair<Integer, Integer>(i, -1);
			z += (t.count(wp) * a.count(ip));
			q0.put(i, t.count(wp) * a.count(ip));
		}

		for (Integer i : q0.keySet()) {
			Pair<String, String> wp = new Pair<String, String>(es[0], fs[i]);
			Pair<Integer, Integer> ip = new Pair<Integer, Integer>(i, -1);
			lexSample.increment(wp, q0.get(i) / z);
			stateSample.increment(ip, q0.get(i) / z);
			p_iprev.put(i, q0.get(i) / z);
		}
		q0 = null;

		for (int j = 1; j < es.length; j++) {
			// z for lexSample
			double z1 = 0;
			Map<Triple<Integer, Integer, Integer>, Double> q = new HashMap<Triple<Integer, Integer, Integer>, Double>();
			for (int i = 0; i < fs.length; i++) {
				// z for stateSample
				double z2 = 0;
				for (int iprev = 0; iprev < fs.length; iprev++) {
					Pair<String, String> wp = new Pair<String, String>(es[j],
							fs[i]);
					Pair<Integer, Integer> ip = new Pair<Integer, Integer>(i,
							iprev);
					Triple<Integer, Integer, Integer> key = new Triple<Integer, Integer, Integer>(
							i, iprev, j);
					z1 += (t.count(wp) * a.count(ip) * p_iprev.get(iprev));
					z2 += (t.count(wp) * a.count(ip) * p_iprev.get(iprev));
					q.put(key, t.count(wp) * a.count(ip) * p_iprev.get(iprev));
				}

				// accumulate sample for state transition
				for (int iprev = 0; iprev < fs.length; iprev++) {
					Pair<Integer, Integer> ip = new Pair<Integer, Integer>(i,
							iprev);
					Triple<Integer, Integer, Integer> key = new Triple<Integer, Integer, Integer>(
							i, iprev, j);
					stateSample.increment(ip, q.get(key) / z2);
				}
			}

			// accumulate sample for lex transition
			for (Triple<Integer, Integer, Integer> key : q.keySet()) {
				Pair<String, String> wp = new Pair<String, String>(
						es[key.getThird()], fs[key.getFirst()]);
				if (q.get(key) != null) {
					lexSample.increment(wp, q.get(key) / z1);
				}
			}
		}
	}

	public void m_step() {
		for (Pair<String, String> key : lexSample.keys()) {
			t.set(key, lexSample.count(key) / lexSample.county(key.getSecond()));
		}
		for (Pair<Integer, Integer> key : stateSample.keys()) {
			a.set(key,
					stateSample.count(key)
							/ stateSample.county(key.getSecond()));
		}
		t.marginalize();
		a.marginalize();
	}

	public void clear_sample() {
		lexSample.clear();
		stateSample.clear();
	}

	public double get_t(String ew, String fw) {
		Pair<String, String> wp = new Pair<String, String>(ew, fw);
		return t.count(wp);
	}

	public double get_a(int i, int iprev) {
		Pair<Integer, Integer> ip = new Pair<Integer, Integer>(i, iprev);
		return a.count(ip);
	}
}
