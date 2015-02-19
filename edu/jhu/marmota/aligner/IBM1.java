package edu.jhu.marmota.aligner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import edu.jhu.marmota.util.PairCounter;
import fig.basic.LogInfo;
import fig.basic.Pair;

/**
 * IBM Model 1, as described in (Brown et al. 1993).
 * 
 * @author shuoyang
 *
 */
public class IBM1 {
	// the pair always take the form (e, f)
	private PairCounter<String> sample;
	private PairCounter<String> param;
	
	// random initialization
	public IBM1 (String efile, String ffile) {
		sample = new PairCounter<String>(false, true);
		param = new PairCounter<String>(true, false);
		
		LogInfo.begin_track("initializing");
		try {
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
				
				String[] etoks = eline.split(" ");
				String[] ftoks = fline.split(" ");
//				Random rand = new Random();
				for (String etok: etoks) {
					for (String ftok: ftoks) {
//						param.increment(new Pair<String, String>(etok, ftok), rand.nextDouble());
						param.increment(new Pair<String, String>(etok, ftok), 1.0 / ftoks.length);
					}
				}
				linen ++;
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
		for (String ew: es) {
			for (String fw: fs) {
				Pair<String, String> wp = new Pair<String, String>(ew, fw);
				double t = param.count(wp);
				double s = param.countx(ew);
				sample.increment(wp, t / s);
			}
		}
	}
	
	public void m_step() {
		for (Pair<String, String> wp: param.keys()) {
			param.set(wp, sample.count(wp) / sample.county(wp.getSecond()));
		}
		param.marginalize();
	}
	
	public void clear_sample() {
		sample.clear();
	}
	
	public double get_param(String ew, String fw) {
		Pair<String, String> wp = new Pair<String, String>(ew, fw);
		return param.count(wp);
	}
	
	public PairCounter<String> get_param() {
		return param;
	}
}
