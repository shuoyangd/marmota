# A sentence-level evaluation script for marmota
# 
# Author: Shuoyang Ding
# March, 2015 @ CLSP, Johns Hopkins University
# ---------------------------
# implementation reference:
# [meteor] is implemented with reference of http://mt-class.org/jhu/hw3.html
# [bleu] is implemented with reference of http://acl2014.org/acl2014/W14-33/pdf/W14-3346.pdf

import argparse
import sys
import os
import math

argparser = argparse.ArgumentParser("sentence-level evaluation script for marmota")
argparser.add_argument("--metric", "-m", action='store', type=str, required=True, help="evaluation metric, currently: [meteor]")
argparser.add_argument("--input", "-i", action='store', type=str, required=True, help="input file for evaluation (or, system output)")
argparser.add_argument("--reference", "-r", action='store', type=str, required=True, help="reference file")
argparser.add_argument("--meteoralpha", action='store', type=float, default=0.5, help="hyper parameter for meteor (default=0.5)")
argparser.add_argument("--bleusmoothing", action='store', type=int, default=7, help="smoothing techniques [1-7] for bleu, described in (Chen et al. 2014), with 0 means no smoothing (default=7)")
argparser.add_argument("--bleun", action='store', type=int, default=3, help="the maximum ngram examined when calculating bleu (default=3)")
argparser.add_argument("--bleusmoothingeps", action='store', type=float, default=0.1, help="hyper parameter for bleu smoothing method 1 (deault=0.1)")
argparser.add_argument("--bleusmoothingk", action='store', type=float, default=5.0, help="hyper parameter for bleu smoothing method 4 (deault=5.0)")
argparser.add_argument("--bleusmoothingalpha", action='store', type=float, default=5.0, help="hyper parameter for bleu smoothing method 6 (deault=5.0)")

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

def getngrams(toks, ngram):
	if len(toks) <= ngram:
		# for ngram that is longer than the sentence length, fall back to the whole sentence
		# (is that the right definition?)
		return [tuple(toks)]
	else:
		res = []
		for i in range(0, len(toks) - ngram):
			res += tuple(toks[i: i + ngram])
		return res

def bleu(hsent, esent, maxgram, smoothing):
	if smoothing == 5 or smoothing == 7:
		maxgram += 1
	h = hsent.split(' ')
	e = esent.split(' ')
	hgrams = [[]]
	egrams = [[]]
	pn = []
	for i in range(1, maxgram + 1):
		hgrams.append(getngrams(h, i))
		egrams.append(getngrams(e, i))
	invcnt = 1 # variable for smoothing 3
	mn = sum(1 for hgram in hgrams[1] if hgram in egrams[1]) # special processing for smoothing 5
	if mn == 0 and smoothing == 7: # special processing for smoothing 7
		# calculate mn use smoothing of 4
		# the original version is (len(h), which may cause problem when len(h) = 1)
		invcnt *= (args.bleusmoothingk / math.log(1 + len(h)))
		pn.append((1.0 / invcnt) / float(len(hgrams[i])))
	mnp = [mn + 1.0] # variable for smoothing 5 and 7
	if smoothing == 5 or smoothing == 7:
		maxgram -= 1
	for i in range(1, maxgram + 1):
		if smoothing == 0:
			if len(hgrams[i]) == 0:
				return 0.0
			else:
				mn = sum(1 for hgram in hgrams[i] if hgram in egrams[i])
				pn.append(float(mn) / float(len(hgrams[i])))
		elif smoothing == 1:
			mn = sum(1 for hgram in hgrams[i] if hgram in egrams[i])
			if mn == 0:
				pn.append(args.bleusmoothingeps)
			else:
				pn.append(float(mn) / float(len(hgrams[i])))
		elif smoothing == 2:
			mn = sum(1 for hgram in hgrams[i] if hgram in egrams[i])
			if i == 1 and mn != 0:
				pn.append(float(mn) / float(len(hgrams[i])))
			elif i == 1:
				# the paper does not address what to do for this case (mn == 0)
				# after some experiment it seems better to backoff to smoothing method 1
				pn.append(args.bleusmoothingeps)
			else:
				pn.append(float(mn + 1.0) / float(len(hgrams[i]) + 1.0))
		elif smoothing == 3:
			mn = sum(1 for hgram in hgrams[i] if hgram in egrams[i])
			if mn == 0:
				invcnt *= 2
				pn.append((1.0 / invcnt) / float(len(hgrams[i])))
			else:
				pn.append(float(mn) / float(len(hgrams[i])))
		elif smoothing == 4:
			mn = sum(1 for hgram in hgrams[i] if hgram in egrams[i])
			if mn == 0:
				# the original version is (len(h), which may cause problem when len(h) = 1)
				invcnt *= (args.bleusmoothingk / math.log(1 + len(h)))
				pn.append((1.0 / invcnt) / float(len(hgrams[i])))
			else:
				pn.append(float(mn) / float(len(hgrams[i])))
		elif smoothing == 5:
			mnplus1 = sum(1 for hgram in hgrams[i + 1] if hgram in egrams[i + 1])
			mnp.append((mnp[i - 1] + mn + mnplus1) / 3)
			mn = mnplus1
			pn.append(float(mnp[i]) / float(len(hgrams[i])))
		elif smoothing == 6:
			mn = sum(1 for hgram in hgrams[i] if hgram in egrams[i])
			if i == 1 or i == 2:
				pn.append(float(mn) / float(len(hgrams[i])))
			else:
				# p1 is stored in pn[0], so on so forth
				if pn[i - 3] == 0:
					return 0.0
				else:
					psmooth = pn[i - 2] * pn[i - 2] / pn[i - 3]
					pn.append((mn + psmooth * args.bleusmoothingalpha) / (len(hgrams[i]) + args.bleusmoothingalpha))
		elif smoothing == 7:
			mnplus1 = sum(1 for hgram in hgrams[i + 1] if hgram in egrams[i + 1])
			mnp.append((mnp[i - 1] + mn + mnplus1) / 3)
			mn = mnplus1
			pn.append(float(mnp[i]) / float(len(hgrams[i])))
	p = math.pow(reduce(lambda x, y: x * y, pn), 1.0 / maxgram)
	bp = min(1.0, math.exp(1 - float(len(e)) / float(len(h))))
	return p * bp

if __name__ == "__main__":
	hfile = open(args.input)
	rfile = open(args.reference)
	
	if os.stat(args.input).st_size != os.stat(args.input).st_size:
		sys.stderr.write("different file length between input(" + str(len(hfile)) + ") and reference(" + str(len(rfile)) + ")!")
		sys.exit(1)

	for (hline, rline) in zip(hfile, rfile):
		if args.metric == "meteor":
			sys.stdout.write(str(meteor(hline.strip(), rline.strip(), args.meteoralpha)) + "\n")
		if args.metric == "bleu":
			sys.stdout.write(str(bleu(hline.strip(), rline.strip(), args.bleun, args.bleusmoothing)) + "\n")
