Outputs!
3 outputs - #trig, score, state
these can mean different things for the different ugens


PhaseIn1
 |threshBool, callRestPeriod, maxTimeInhibited|

PhaseInIrm
|threshBool=1, callPeriod = 1, restPeriod = 1, inhibitMax = 1, inhibitDist = 1, playProb=1, restMode=0|

inhibitDist - determines what power to multiply the random number by. values minus one will result in (1 - rand)

restMode 0 = fixed rest time
	1 = rest time takes a random walk. resetting this to 0 will reset the random walk.