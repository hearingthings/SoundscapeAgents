PhaseIn1 : MultiOutUGen {
	*kr { |threshBool, callRestPeriod, maxTimeInhibited|
		^this.multiNew('control', threshBool, callRestPeriod, maxTimeInhibited)	
	}
	
	init { arg ... theInputs;
		inputs = theInputs
		^this.initOutputs(2, rate)
	}
}