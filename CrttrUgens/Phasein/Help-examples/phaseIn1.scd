SynthDef("phaseInTest", { |inCh=0, threshClampTime = 10, sustain=0.5, dur=1, thresh=0.1|
	var trig, score, state;
	var in;
//	var thresh;
	var threshBool;
	var playTrig;
	var env;
	var chain;
	var sidetone;

	sidetone = LocalIn.ar(1);
	
	in = SoundIn.ar(inCh);
	
	in = FFTCancel.ar(in, sidetone, 0.01);
//	in = Gate.ar(in, 1 - Trig1.ar(playTrig, sustain)); //one form of cancelling - don't listen
	

//	in = RunningSum.rms(in);
//	in = A2K.kr(in);
//	thresh = Ramp.kr(in, threshClampTime);

	in = Amplitude.kr(in, 0.1, 0.1);
//	thresh = LagUD.kr(in, threshClampTime, 0.01);
	 
	
	threshBool = in < thresh;
	
	#trig, score, state = PhaseIn1.kr(threshBool, dur, 10);
	
	env = EnvGen.kr(Env.perc, timeScale: sustain, gate: trig);
	chain = SinOsc.ar(1000, 0, env);

	LocalOut.ar(chain);

	
	thresh = [in, thresh, threshBool];
	thresh = thresh ++ [score, state/2];
	thresh = K2A.ar(thresh);
	Out.ar(0, [chain] ++ thresh);
	
}).store;

v = Synth("phaseInTest");

s.recChannels = 6;
s.options.hardwareBufferSize = 64;
s.options.blockSize = 1;

s.reboot