PhaseInUgen : MultiOutUGen {
	init { arg ... theInputs;
		inputs = theInputs
		^this.initOutputs(3, rate)
	}
}	


PhaseIn1 : PhaseInUgen {
	*kr { |threshBool, callRestPeriod, maxTimeInhibited|
		^this.multiNew('control', threshBool, callRestPeriod, maxTimeInhibited)	
	}
	
}

PhaseInIrm : PhaseInUgen { 
	
	*kr { |threshBool=1, callPeriod = 1, restPeriod = 1, inhibitMax = 1, inhibitDist = 1, playProb=1, restMode=0|
		
		^this.multiNew('control', 
			threshBool, callPeriod, restPeriod, 
			inhibitMax, inhibitDist, playProb, restMode
		)
	}
}