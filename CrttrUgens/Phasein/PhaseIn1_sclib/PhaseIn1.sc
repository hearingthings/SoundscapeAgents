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

PhaseInIrm2 : PhaseInUgen { 
	*kr { |threshBool=1, callThreshBool=1, callPeriod = 1, restPeriod = 1, inhibitCallMax = 1, inhibitRestMax=1, inhibitDist = 1, playProb=1, restMode=0, callInhibitMode=0, callInhibitWeight=1|
		
		^this.multiNew('control', 
			threshBool, callThreshBool, callPeriod, restPeriod, 
			inhibitCallMax, inhibitRestMax, inhibitDist, playProb, restMode, callInhibitMode, callInhibitWeight 
		)
	}
	
	init { arg ... theInputs;
		inputs = theInputs
		^this.initOutputs(4, rate)
	}
}