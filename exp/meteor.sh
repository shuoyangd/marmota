#! /bin/bash
# best on 0.3775 -- 0.506336 accuracy

for a in 0.37 0.3725 0.375 0.3775 0.38 0.3825 0.385
do
	python src/eval.py -m meteor -i data/hyp1 -r data/ref --meteoralpha $a > res1
	python src/eval.py -m meteor -i data/hyp2 -r data/ref --meteoralpha $a > res2
	python src/compare.py res1 res2 > comp
	./compare-with-human-evaluation < comp
done
