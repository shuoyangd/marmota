package edu.jhu.clsp.marmota.aligner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

import edu.jhu.clsp.marmota.util.JointCounter;
import fig.basic.Pair;

/**
 * IBM Model 1, as described in (Brown et al. 1993).
 * 
 * @author shuoyang
 *
 */
public class IBM1 {
	// the pair always take the form (e, f)
	private JointCounter<String> sample;
	private JointCounter<String> param;
	
	// zero initialization
	public IBM1 () {
		sample = new JointCounter<String>();
		param = new JointCounter<String>();
	}
	
	// random initialization
	public IBM1 (String efile, String ffile) {
		sample = new JointCounter<String>();
		param = new JointCounter<String>();
		
		try {
			BufferedReader ein = new BufferedReader(new FileReader(new File(efile)));
			BufferedReader fin = new BufferedReader(new FileReader(new File(ffile)));
			while (true) {
				String eline = ein.readLine();
				String fline = fin.readLine();
				
				if (eline == null || fline == null) {
					break;
				}
				
				String[] etoks = eline.split(" ");
				String[] ftoks = fline.split(" ");
				Random rand = new Random();
				for (String etok: etoks) {
					for (String ftok: ftoks) {
						param.increment(new Pair<String, String>(etok, ftok), rand.nextDouble());
					}
				}
			}
			ein.close();
			fin.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void e_step(String[] es, String[] fs) {
		for (String fw: fs) {
			for (String ew: es) {
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
}
