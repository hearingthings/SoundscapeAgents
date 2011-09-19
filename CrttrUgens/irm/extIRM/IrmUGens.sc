BNIrm : UGen {
	*kr { |threshBool, playProb, prepPeriod, callPeriod, inhibitLow, inhibitHigh, restPeriod|
		^this.multiNew('control', threshBool, playProb, prepPeriod, callPeriod, inhibitLow, inhibitHigh, restPeriod)
	}
}

BNIrm2 : MultiOutUGen {
	*kr { |threshBool, playProb, prepPeriod, callPeriod, inhibitLow, inhibitHigh, restPeriod|
		^this.multiNew('control', threshBool, playProb, prepPeriod, callPeriod, inhibitLow, inhibitHigh, restPeriod)
	}
	
	init { arg ... theInputs;
		inputs = theInputs;
		^this.initOutputs(2, rate);
	}
}

BNIrmTrigState : UGen {
	*kr { |threshBool, playProb, prepPeriod, callPeriod, inhibitLow, inhibitHigh, restPeriod, trig=1, state = 0|
		^this.multiNew('control', threshBool, playProb, prepPeriod, callPeriod, inhibitLow, inhibitHigh, restPeriod, trig, state)
	}
}