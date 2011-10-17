var ntests = 5;
var ambTime=0.1, onTime = 0.5;

var rmsValues, uc = Condition.new(false);



o.remove; o = OSCresponder(nil, 'bpfRMS', { 
	|t, r, msg| 
	var id = msg[2];
	var val = msg[3];

	msg.postln;
	rmsValues = msg[3..4];
	uc.test = true;
	uc.signal;
}).add;


s.latency = 0.0;

{
	
	5.wait;

~amps = ntests.collect{	
	(-70, -69..-20).collect{ |amp|
		var pre, on, post;
		//record pre secs no speaker
		uc.test = false;
		v.set(\amp, 0);
		0.01.wait;
		v.set(\t_trig, 1, \timeToMeasure, ambTime);
		uc.wait;
		pre = rmsValues;
		
		uc.test = false;
		v.set(\amp, amp.dbamp, \t_trig, 1, \timeToMeasure, onTime);
		uc.wait;
		on = rmsValues;
		
		0.1.wait; //wait for reverb to die off
		
		uc.test = false;
		v.set(\amp, 0);
		0.01.wait;
		v.set(\t_trig, 1, \timeToMeasure, ambTime);
		uc.wait;
		post = rmsValues;
		
		0.1.wait;
		
		(pre: pre, on: on, post: post, ampdb: amp)	
			
			//record rms value of IN
		//record on secs speaker on with amp
			//record rms value of IN and SYNTH
		//record post secs no speaker
			//record rms value of IN
	
		//store preRMS, onRMS, synthRMS, amp, postRMS	
		
		
	};
};
}.fork






SynthDef("noiseMeasure", { |freq=1000, rq=0.3, amp=0, inCh=0, outCh=0, inOutLatency=0.004, timeToMeasure=1, rmsSampWindow=40, t_trig|
	var inChain, outChain;
	var inRMS, outRMS;
	
	var takeMean, mean, sendVal;
	var numSampsForMean;
	
	//INPUT
	inChain = SoundIn.ar(inCh);
	inChain = BPF.ar(inChain, freq, rq);

	//OUTPUT
//	outChain = WhiteNoise.ar(amp);
//	outChain = BPF.ar(outChain, freq, rq);

	SinOsc.ar(freq, 0, 0.2);
	

	//MEASUREMENTS
	inRMS = RunningSum.rms(inChain, rmsSampWindow);
	outRMS = RunningSum.rms(outChain, rmsSampWindow);

	//TIME CORRECTION	
	outRMS = DelayN.ar(outRMS, inOutLatency, inOutLatency);
	takeMean = TDelay.kr(t_trig, inOutLatency);

	//MEAN VALUES
	#mean, sendVal = MeanForTime.ar([inRMS, outRMS], timeToMeasure, takeMean);	
	//SEND VALUE

//	mean.poll(sendVal);	
	SendReply.ar(sendVal, 'bpfRMS', mean, 0);
	
	Out.ar(outCh, outChain);

}).store;

v = Synth("noiseMeasure")



v.set(\t_trig, 1, \amp, 0);

s.options.hardwareBufferSize = 64;
s.options.blockSize = 1
s.reboot;