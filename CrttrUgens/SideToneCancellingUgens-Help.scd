b = Buffer.read(s, "sounds/a11wlk01.wav")

SynthDef("magsub", { |inCh=0|
	
	var chain1, chain2, chainOut, outrms;
	
	chain1 = FFT(LocalBuf(2048), SoundIn.ar(inCh));
	
	chain2 = PlayBuf.ar(1, b, loop: 1);
	Out.ar(0, chain2);
	chain2 = DelayN.ar(chain2, 0.01);
	chain2 = FFT(LocalBuf(2048), chain2);	
	chainOut = PV_MagSubtract(chain1, chain2, 1);

	chainOut = IFFT(chainOut);
	chainOut = HPF.ar(chainOut, 40);
//	Out.ar(1, chainOut);
	outrms = RunningSum.rms(chainOut, 200);
	Out.ar(2, [chainOut, SoundIn.ar(inCh), outrms]);	
	
}).store;

SynthDef("magsub2", {
	var in, chain, inAmp;
	
	in = SoundIn.ar(0);
	
	chain = SinOsc.ar(200, SinOsc.ar(20, 0, 200), SinOsc.ar(20, 0, 0.5)); //complex sound
	
	inAmp = FFTCancel.ar(in, chain, 0.03);
	
	Out.ar(0, chain);
	Out.ar(2, [inAmp, RunningSum.rms(inAmp, 4000)]); 
	
}).store;

s.recChannels = 4;

Synth("magsub2");


s.options.hardwareBufferSize = 64
s.options.blockSize = 1;


(	//with phasein1
	SynthDef("phaseInTest", { |inCh=0, threshClampTime = 10, sustain=0.5, dur=1, thresh=0.1|
		var trig, score, state;
		var in, inCancel, amp;
	//	var thresh;
		var threshBool;
		var playTrig;
		var env;
		var chain;
		var sidetone;
	
		sidetone = LocalIn.ar(1);
		
		inCancel = SoundIn.ar(inCh);
	
		inCancel = FFTCancel.ar(inCancel, sidetone, 0.03, 2048);
	//	in = Gate.ar(in, 1 - Trig1.ar(playTrig, sustain)); //one form of cancelling - don't listen
		
	
	//	in = RunningSum.rms(in);
	//	in = A2K.kr(in);
	//	thresh = Ramp.kr(in, threshClampTime);
	
		amp = Amplitude.kr(inCancel, 0.1, 0.1);
	//	thresh = LagUD.kr(in, threshClampTime, 0.01);
		 
		
		threshBool = amp < thresh;
		
		#trig, score, state = PhaseIn1.kr(threshBool, dur, 10);
		
		env = EnvGen.kr(Env.perc, timeScale: sustain, gate: trig);
		chain = SinOsc.ar(1000, 0, env);
	
		LocalOut.ar(chain);
	
		
		thresh = [amp, thresh, threshBool];
		thresh = thresh ++ [score, state/2];
		thresh = K2A.ar(thresh);
		Out.ar(0, [chain] ++ [inCancel] ++ thresh);
		
	}).store;
	
	v = Synth("phaseInTest");
	
	s.recChannels = 7;
	s.options.hardwareBufferSize = 64;
	s.options.blockSize = 1;
	
	s.reboot
)