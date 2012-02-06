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
	
	
PhaseInIrm2
 |threshBool=1, callThreshBool=1, callPeriod = 1, restPeriod = 1, inhibitCallMax = 1, inhibitRestMax, inhibitDist = 1, playProb=1, restMode=0, callInhibitMode=0, callInhibitWeight=1|
 
 
 threshBool : 1 means "go ahead", 0 means "inhibit"
 callThreshBool : 1 means "go ahead", 0 means "inhibit"
 
 callInhibitMode: 0 means stop calling, 1 means keep calling but update score
 
SynthDef("pii2", { |tb=1, ctb=1|
	 var trig, citrig, score, state;
	 
	 #trig, citrig, score, state = PhaseInIrm2.kr(tb, ctb, 0.5, 0.5, callInhibitMode: 1, callInhibitWeight:100);
	 
	 score.poll;
	 
	 Out.ar(0, SinOsc.ar(400, 0, trig * 0.2));
	 
 }).store;
 
 v = Synth("pii2");
 v.set(\ctb, 0)
 s.reboot;