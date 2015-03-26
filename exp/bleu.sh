#! /bin/bash
# 0 Accuracy = 0.491552
# 1 Accuracy = 0.506805
# 2 Accuracy = 0.507861
# 3 Accuracy = 0.507040
# 4 Accuracy = 0.506610
# 5 Accuracy = 0.507470
# 6 Accuracy = 0.504459
# 7 Accuracy = 0.507548
# as of 03/14/15
# 
# fixed a bug in getngram in eval.py
# 0 Accuracy = 0.495033
# 1 Accuracy = 0.511577
# 2 Accuracy = 0.511890
# 3 Accuracy = 0.511773
# 4 Accuracy = 0.510873
# 5 Accuracy = 0.511538
# 6 Accuracy = 0.508761
# 7 Accuracy = 0.511538
# as of 03/25/15

# sentence-bleu in moses
#   Accuracy = 0.507314

for s in 0 1 2 3 4 5 6 7
do
	python src/eval.py -m bleu -i data/hyp1 -r data/ref --bleun 5 --bleusmoothing $s > res1
	python src/eval.py -m bleu -i data/hyp2 -r data/ref --bleun 5 --bleusmoothing $s > res2
	python src/compare.py res1 res2 > comp
	./compare-with-human-evaluation < comp
done
