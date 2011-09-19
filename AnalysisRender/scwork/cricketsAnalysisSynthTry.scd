//here is an analysis synth
//

SynthDef(\analyzeCrickets, { 
	|bufnum, bufnum1,inar0, crestFreqLo = 3000, crestFreqHigh = 10000, pitchFreqLo=3000, pitchFreqHi = 10000,
		argCrestOn, arg| 

	var crest, slope, in, freq, hasFreq;
	
	in = In.ar(inar0, 1);
	
	crest = FFT(bufnum, in);
	crest = FFTCrest(slope, crestFreqLo, crestFreqHigh);
	slope = FFT(bufnum1, in);
	slope = FFTSlope(slope);
	
	#freq, hasFreq = Pitch.kr(in, pitchFreqLo, pitchFreqLo, pitchFreqHi);
	
}).store;

//various filter ideas::

slopeLow
	
	
	