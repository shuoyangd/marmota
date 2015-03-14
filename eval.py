import argparse
import sys
import os

argparser = argparse.ArgumentParser("sentence-level evaluation script for marmota")
argparser.add_argument("--metric", "-m", action='store', type=str, required=True, help="evaluation metric, currently: [meteor]")
argparser.add_argument("--input", "-i", action='store', type=str, required=True, help="input file for evaluation (or, system output)")
argparser.add_argument("--reference", "-r", action='store', type=str, required=True, help="reference file")
argparser.add_argument("--meteoralpha", action='store', type=float, default=0.5, help="hyper parameter for meteor (default=0.5)")

args = argparser.parse_args()

def meteor(hsent, esent, a):
	h = hsent.split(' ')
	e = esent.split(' ')
	ch = len(set(h)) # |h|
	ce = len(set(e)) # |e|
	ci = sum(1 for w in h if w in e)
	p = float(ci) / float(ch)
	r = float(ci) / float(ce)
	if p == 0.0 and r == 0.0:
		return 0.0
	else:
		return (p * r) / ((1 - a) * r + a * p)

if __name__ == "__main__":
	hfile = open(args.input)
	rfile = open(args.reference)
	
	if os.stat(args.input).st_size != os.stat(args.input).st_size:
		sys.stderr.write("different file length between input(" + str(len(hfile)) + ") and reference(" + str(len(rfile)) + ")!")
		sys.exit(1)

	for (hline, rline) in zip(hfile, rfile):
		if args.metric == "meteor":
			sys.stdout.write(str(meteor(hline.strip(), rline.strip(), args.meteoralpha)) + "\n")			
