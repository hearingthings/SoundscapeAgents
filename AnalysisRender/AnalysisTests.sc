//AAnalysis tests

TestAnalysisChain : UnitTest {
	var myController, a, s;
	
	setUp {
		var myController = AnalysisRenderController.newWithFileAndChains(
		"sounds/a11wlk01-44_1.aiff",
			[
				[ 
					[AFilter, \afilterLPF_1, \lowpassFreq, 2000],
					[AFFTAnalysis, \afftSpecCrestToBus]
				]
			]
		);
		
		a = myController[0];
		s = Server.new("test", NetAddr("localhost", 57121));
	}
	
	tearDown {
		s = nil;
	}
	
	newWithRandSpec{
		var spec = [ 
			[AFilter, \afilterLPF_1, \lowpassFreq, 2000],
			[AFFTAnalysis, \afftSpecCrestToBus]
		];	
		spec
	}	
	
	test_allocBuffers {
		
	}
	
	test_busAllocate {
		a.enumerateBusses(s);
	}

}



TestAnalysisRenderController : UnitTest {

	 newRandController {
		var a = AnalysisRenderController.newWithFileAndChains(
		"sounds/a11wlk01-44_1.aiff",
			[
				[ 
					[AFilter, \afilterLPF_1, \lowpassFreq, 2000],
					[AFFTAnalysis, \afftSpecCrestToBus]
				]
			]
		);
		^a
	}

	test_newWithFileAndChains_returnsCorrectClass {
		//assert things about the new controller below
		var a = this.newRandController;
		this.assert(a.class == AnalysisRenderController, "method returns correct class");
				
	}
	
	test_bufferAlloc {
		var a = this.newRandController, b;
		a.readFileWithPath("sounds/a11wlk01-44_1.aiff");
		a.enumerateResources;
		b = a.synthsBundleAtTime(0);
		a.allocBuffersMsgBundle(0, b);	
	}
}


TestAFFTAnalysis : UnitTest {
	var a, name, s;
	
	setUp {
		name = \affttestarglist;
		SynthDef(name, { |bufnum=0, rate=12, in=0, out=30|
			var chain;
			chain = FFT(bufnum, In.ar(in, 1));
			chain = FFTCrest.kr(bufnum);
			Out.kr(out, chain);
		}).store;
		
		s = Server.new("test", NetAddr("localhost", 57121));
		
		a = AFFTAnalysis.new(name);
	}
	
	tearDown {
		a = nil;
		s = nil;
	}
	
	test_setParams {
		
		a.params = [\fftSize, 100, \dude, 1];
		
		this.assert(a.params.at(\fftSize).isNil, "params should strip out FFT size");
		this.assert(a.params.at(\rate).isNil, "set params does not retain old parameters");
	}
	
	test_arglistIsCcorrect {
		var fftSize = 512;
		a = AFFTAnalysis.new(name, [\fftSize, fftSize]);
	}
	
	test_makeBufferReturnsCorrectSize {
		var fftSize = 512;
		a = AFFTAnalysis.new(name, [\fftSize, fftSize]);
		a.makeBufferInfo(s);
		this.assert(a.buffers[0].numFrames == (fftSize * 2), "makeBuffersFFT analysis buffer has correct number of frames")
	}
}

TestAInputPlayBuf : UnitTest {

	
	
	test_newForServerWithChansAndFrames {
		10.do{ |numChans|
			var a = AInputPlayBuf.newForServerWithChansAndPath(Server.default, numChans, "sounds/a11wlk01.aiff");
			this.assert(a.buffers.size == 1, "only one buffer allocated");
			this.assert(a.buffers[0].numChannels == numChans, "correct amount of frames allocated");
		}
	}
}	
	
	
TestAnalysisBlock : UnitTest {

	test_paramsInitNoDoubling {
		var a, synthDef, paramsList, paramsArgList, paramsNames, paramsValues, checkFunction;
		var name = \testparamsInitNoDoubling;
		
		var hasExtraNames = false, containsAllNames = true, hasCorrectValues=true;
		
		paramsList = [\freq, 200, \mul, 0.3, \add, 0.1, \fm, 1000.1];
		paramsArgList = paramsList[0..3];
		
		paramsNames = paramsList.clump(2).flop[0];
		paramsValues = paramsList.clump(2).flop[1]; 
		
		synthDef = SynthDef(name, {
			|freq=2000, mul= -1, add=0.1, fm=1000.1|
			Out.ar(0, SinOsc.ar(freq, fm, mul, add));
		});
		

		
		synthDef.store;
		a = AnalysisBlock(name, paramsArgList);
		
		
		a.params.keysValuesDo{ |key, value|
			if (paramsNames.includes(key).not) {hasExtraNames = true};
			if (paramsValues[ paramsNames.indexOf(key) ].round(0.01) != value.round(0.01)) {hasCorrectValues = false};
//			[paramsValues[ paramsNames.indexOf(key) ].round(0.01), value.round(0.01)].debug("argVal, parVal");
		};
		
		this.assert(hasExtraNames == false, "params list does not have extra keys");
		this.assert(hasCorrectValues, "params list has correct values");
		
		a.params.keysDo{ |key| paramsNames.remove(key) };
		
		this.assert(paramsNames.size == 0, "params list contains all names");
	}
	
	
	
}
	
	
	