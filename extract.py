# feature extractor for the classifier-based evaluation method in (Song et al., 2011)
# 
# Author: Shuoyang Ding
# March, 2015 @ CLSP, Johns Hopkins University
# ---------------------------
# as of March 25, 2014: does not extract feature 15-17 in the original paper

import sys, collections
from eval import getngrams

def extract(sl, rl):
	features = {}
	st = sl.split(' ')
	rt = rl.split(' ')
	sw = []
	sp = []
	for t in st:
		w_p = t.split('_')
		sw.append(w_p[0])
		if len(w_p) > 1:
			sp.append(w_p[1])
	rw = []
	rp = []
	for t in rt:
		w_p = t.split('_')
		rw.append(w_p[0])
		if len(w_p) > 1:
			rp.append(w_p[1])
	sump = 0.0
	for n in range(1, 5):
		sgrams = getngrams(sw, n)
		rgrams = getngrams(rw, n)
		ch = len(set(sw)) # |h|
	        ce = len(set(rw)) # |e|
		ci = len(set(sw) & set(rw))
		p = float(ci) / float(ch)
		r = float(ci) / float(ce)
		f = 2 * p * r / (p + r) if p + r != 0 else 0
		features[n] = p
		features[n + 4] = r
		features[n + 8] = f
		sump += p

		ch = len(set(sp)) # |h|
		ce = len(set(rp)) # |e|
		ci = len(set(sp) & set(rp))
		if ch > 0 and ce > 0:
			p = float(ci) / float(ch)
			r = float(ci) / float(ce)
			f = 2 * p * r / (p + r) if p + r != 0 else 0
			features[n + 17] = p
			features[n + 21] = r
			features[n + 25] = f

		ch = len(set(st))
		ci = len(set(st) & set(rt))
		p = float(ci) / float(ch)
		features[n + 29] = p

	features[13] = sump / 4.0
	features[14] = len(sw)
	return features
	
def feats2str(feats):
	line = ""
	for k in sorted(feats.keys()):
		line += str(k)
		line += ":"
		line += str(feats[k])
		line += " "
	line = line[0:len(line) - 1]
	return line

if __name__ == "__main__":
	if len(sys.argv) < 3:
		sys.stderr.write("usage: ./extract.py sys ref\n")
		sys.exit(1)

	sf = open(sys.argv[1])
	rf = open(sys.argv[2])
	for (sl, rl) in zip(sf, rf):
		feats = extract(sl, rl)
		sys.stdout.write(feats2str(feats) + "\n")
	sf.close()
	rf.close()

