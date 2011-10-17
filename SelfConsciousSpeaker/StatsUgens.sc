MeanForTime : UGen
{
	*ar { |in, time, trig|
		var inL, timeTrig, endTrig;
		var numChans = in.size;
		if (numChans == 0) {numChans = 1};
		
		trig = T2A.ar(trig);
		timeTrig = Trig1.ar(trig, time + ControlDur.ir);
	
		inL = LocalIn.ar(numChans);
		inL = inL * timeTrig;
		inL = in + inL;
		LocalOut.ar(inL);
			
		inL = inL / ControlRate.ir;
		inL = inL / time;
		
		endTrig = TDelay.ar(trig, time);
		^[inL, endTrig];
	}	

	*kr {|in, time, trig|
		var inL, timeTrig;

		timeTrig = Trig1.kr(trig, time + ControlDur.ir);
	
		inL = LocalIn.kr(1);
		inL = inL * timeTrig;
		inL = in + inL;
		LocalOut.kr(inL);
			
		inL = inL / ControlRate.ir;
		inL = in / time;
		^inL;		
	}
	
}
