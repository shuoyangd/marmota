#! /usr/bin/python

import sys

if len(sys.argv) < 3:
	sys.stderr.write("usage: ./compare system1 system2")
else:
	if1 = open(sys.argv[1])
	if2 = open(sys.argv[2])
	for (score1, score2) in zip(if1, if2):
		score1 = float(score1.strip())
		score2 = float(score2.strip())
		if score1 > score2:
			comp = 1
		elif score1 < score2:
			comp = -1
		else:
			comp = 0
		sys.stdout.write(str(comp) + '\n')

