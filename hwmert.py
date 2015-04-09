# MERT script for hw4 of EN.600.468
# 
# Author: Shuoyang Ding
# April, 2015 @ CLSP, Johns Hopkins University
# ---------------------------
# As of Apr 4th, 1e-2 grid [-0.3586388371759978, -0.4920710284648164, -0.6335926636820467] (-t -l -s order)
# As of Apr 5th, 1e-2 grid [-0.2961853104698642, -0.5004619760692802, -0.6293129249867515]
# As of Apr 5th, 1e-inf grid [-0.29765192126081513, -0.49973552264492066, -0.6285480091314667]
# As of Apr 6th, 1e-inf grid [-0.4600035522208132, -0.25618092649301605, -0.581800502932407] (-l -t -s order)
# As of Apr 7th, 1e-inf grid, wc feature [-1.2873896198430286, -1.1411404770373568, -0.9316588581835694, 1.321306674087566]
# As of Apr 7th, ie-inf grid, wc+oovc feature [-2.1175364133729553, -5.6003838940508, -3.1541990397132755, 1.9680361338028927, -14.004703396389772]
# As of Apr 9th, 1e-inf grid, wc+oovc feature [-1.287, -1.14, -0.932, 1.32, -3.5]

import os, sys, copy

def parameq(params, oldparams):
	if oldparams == None:
		return False
	else:
		for (param, oldparam) in zip(params, oldparams):
			if param != oldparam:
				return False
		return True

if __name__ == "__main__":
	params = [-1.0, -0.5, -0.5, -0.5, -0.5] 
	oldparams = None
	itern = 0
	# repeat until converge
	while not parameq(params, oldparams):
		sys.stderr.write("\niteration " + str(itern))
		oldparams = copy.copy(params)
		# for all parameters
		for i in range(0, len(params)):
			sys.stderr.write("\nparameter " + str(i))
			param = params[i]
			# threshold points
			T = []

			tf = open("data/dev+test.100best.+wc+wcd")
			rf = open("data/dev.ref")
			tl = tf.readline()
			rl = rf.readline()
			# for all sentence
			while rl != "":
				# scores for each translation
				sentfeats = []
				# for all translations
				for k in range(0, 100):
					# read scores from the k-best list
					featstrs = tl.strip().split(' ||| ')[2].split(' ')
					feats = []
					for featstr in featstrs:
						feats.append(float(featstr.split('=')[1]))
					sentfeats.append(feats)
					tl = tf.readline()
				# end for translation
				# find line l with steepest descent 
				# (actually a better definition here is to find l with smallest a,
				# so in this way no matter how the feature values are defined, the search always proceeds from smaller f to larger f.
				# no matter what, our goal is to find the upper envelop of all the lines)
				a = map(lambda x: x[i], sentfeats)
				b = map(lambda x: (sum(map(lambda y: y[0] * y[1], zip(params, x))) - params[i] * x[i]), sentfeats)
				li = a.index(min(a))
				amin = min(a)
				amax = max(a)
				
				last_tp = -1e200
				# upper envelop not drawn
				while a[li] != amax:
					intersecpts = []
					ai = a[li]
					bi = b[li]
					for k in range(0, 100):
						if ai - a[k] != 0:
							intersecpts.append((b[k] - bi) / (ai - a[k]))
						else:
							intersecpts.append(-1e200)
					last_tp = min(filter(lambda x: x > last_tp, intersecpts))
					T.append((last_tp, ai * last_tp + bi))
					li = intersecpts.index(last_tp)
				rl = rf.readline()
			# end for all sentence
			tf.close()
			rf.close()
			
			# compute current global score (i.e. error rate in MERT)
			os.popen("python rerankn -l " + str(params[0]) + " -t " + str(params[1]) + " -s " + str(params[2]) + "> english.out." + str(os.getpid()))
			bleustream = os.popen("python compute-bleu < english.out." + str(os.getpid()))
			cscore = float(bleustream.readline().strip())
			
			lastparams = copy.copy(params)
			tn = 0
			T = sorted(T, key = lambda t: t[0])
			last_t = -1e200
			for t in T:
				if tn % 100 == 0:
					sys.stderr.write(".")

				# if t[0] - last_t > 1e-3:
				if True:
					params[i] = t[0]
					last_t = t[0]
					os.popen("python rerankn -l " + str(params[0]) + " -t " + str(params[1]) + " -s " + str(params[2]) + "> english.out." + str(os.getpid()))
					bleustream = os.popen("python compute-bleu < english.out." + str(os.getpid()))
					nscore = float(bleustream.readline().strip())
					if nscore <= cscore:
						params = copy.copy(lastparams)
					else:
						sys.stderr.write("\nnew score " + str(nscore) + " | old score " + str(cscore) + " " + str(params))
						cscore = nscore
						lastparams = copy.copy(params)
				tn += 1
		itern += 1
	print params
