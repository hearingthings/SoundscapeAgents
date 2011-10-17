//various input cancelling schemes

FFTCancel {
	*ar{ |input, toCancel, latency, bufsize=2048|
		var chain1, chain2, chainOut, outrms;
//		input = HPF.ar(input, 20); //remove DC
		chain1 = FFT(LocalBuf(bufsize), input);
		toCancel = DelayN.ar(toCancel, latency, latency);
		chain2 = FFT(LocalBuf(bufsize), toCancel);	
		chainOut = PV_MagSubtract(chain1, chain2, 1);
	
		chainOut = IFFT(chainOut);
//		chainOut = HPF.ar(chainOut, 20); //further remove LF artifacts
		^chainOut
	}
}


BRFCancel {
	*ar{ |input, freq, rq, latency|
		var chain;
		chain = DelayN.ar(input, latency, latency);
		chain = BRF.ar(chain, freq, rq);
		^chain	
	}
}