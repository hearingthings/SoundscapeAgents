/* ==================================================================================== */
/* = NRT GA :: testing that everything syncs up when doing NRT and loading buffers... = */
/* ==================================================================================== */

/*
	TODO:
	- dynamicly figure out how many logger channels are needed
	- question: individual logger synths for things happening at different rates? this reduces data but makes code huge
	- create new instance of server to play with making bundles
	- use more built-in server methods for buffer allocation, etc
	- add scaling in analysis spec to log synth
	- cleanup analysis spec idea - too nested right now, needs to be a class
*/

//specify soundfile, start and end points...
	//this defines length of analysis

//which analysis ugens to use, what are its parameters, and what is its range of output. 
	//does it need its own buffer? if so, buffer params.

	//->run nrt analysis
	//->load logger data to arrays
	//->visualize those arrays in conjunction with the soundfile

//




(
var inputFile, inputPath = "/Users/bigd/projects/crickets/tagged_audio/melbourne_trams/bounces/dec23_2009.aiff";
var fileSegment = [50, 200];
var lengthInSeconds;

var diskInThresh = 300;

var ifb, ab, lastSynth;

var curAnalysis, aTypes, analysisRate;
var buffersToMake, ranges;

var bufferID = 1, channelNum = 30; //allocators

var logNumChans = 0, logRanges;

analysisRate = 50;
curAnalysis = [ \specCrestToBus1, \specPcileToBus1 ];

aTypes = IdentityDictionary[
	\specCrestToBus1 ->
		(	synthName: \specCrestToBus,
			range: [0, 1000],
			nChansOut: 1,
			params: [ \freqLo, 10, \freqHi, 1000],
			buffer: IdentityDictionary[ \numFrames -> 2048, \numChannels -> 1]
		),
	\specCrestToBus2 ->
		(	synthName: \specCrestToBus,
			range: [0, 1000],
			nChansOut: 1,
			params: [ \freqLo, 4000, \freqHi, 8000],
			buffer: IdentityDictionary[ \numFrames -> 2048, \numChannels -> 1]
		),
	\specPcileToBus1 ->
		(	synthName: \specPcileToBus,
			range: [20, 20000],
			nChansOut: 1,
			params: [ \fraction, 0.9],
			buffer: IdentityDictionary[ \numFrames -> 2048, \numChannels -> 1]
		),
		
	
];

buffersToMake = Array.newClear;
ranges = Array.newClear;


//read soundfile information?
lengthInSeconds = fileSegment[1] - fileSegment[0]; 


~bufferPostInc = {
	var thisID = bufferID;
	bufferID = bufferID + 1;
	thisID;
};

~channelPostInc = {
	var thisChannel = channelNum;
	channelNum = channelNum + 1;
	thisChannel
};

	
	//all analysis synthdefs have an input, which is the audio bus, and an output, which is the control bus

	SynthDef(\playBufInput, {|bufnum, out|
		var chain;
		chain = PlayBuf.ar(2, bufnum, 1, 0, 0, 1);
		Out.ar(out, Mix( chain ));
	}).store;

	SynthDef(\fakeInput, { |out=0|
		var chain;
		chain = SinOsc.kr(0.3).range(100, 2000);
	//	Out.kr(1, chain.linlin(0, 20000, 0, 1));
		Out.ar(1, K2A.ar(chain).linlin(0, 20000, 0, 1));
		Out.ar(0, SinOsc.ar(chain, 0, 1));
		Out.ar(0, WhiteNoise.ar(SinOsc.kr(1).range(0, 1)) );
	}).store;
	
	
	SynthDef(\freqLog, { |in= 0, out=30|
		var freq, hasFreq;
		in = In.ar(in, 1);
		#freq, hasFreq = Pitch.kr(in, execFreq: 400);
		Out.kr(out, freq);
	}).store;
	
	SynthDef(\fftBuffer_1mix1, { |in=0, bufnum, hop=0.5|
		var chain;
		chain = In.ar(in, 1);
		chain = FFT(bufnum, chain, hop: hop);
	}).store;
	
	
	SynthDef(\specCrestToBus, { |bufnum, hop=0.5, in=0, out=30, freqLo=20, freqHi=20000|
		var chain;
		chain = In.ar(in, 1);
		chain = FFT(bufnum, chain);
		chain = FFTCrest.kr(chain, freqLo, freqHi);
		chain.poll(Impulse.kr(10), "crest1");
	//	chain = SinOsc.ar(1).range(20,10000);
		Out.kr(out, chain);
	}).store;

	
	SynthDef(\specPcileToBus, { |bufnum, hop=0.5, in=0, out=30, fraction=0.9|
		var chain;
		chain = In.ar(in, 1);
		chain = FFT(bufnum, chain);
		chain = SpecPcile.kr(chain, fraction, 1);
		chain.poll(Impulse.kr(10), "pcile");
	//	chain = SinOsc.ar(1).range(20,10000);
		Out.kr(out, chain);
	}).store;	
	
	SynthDef(\log_1, {|rangesLo=#[0], rangesHi=#[600], in=30, bufnum, rate=100|
		in = In.kr(in, 1);
		Logger.kr(in.linlin(rangesLo, rangesHi, 0, 1), Impulse.kr(rate), bufnum);
	}).store;
	
	SynthDef(\log_2, {|rangesLo=#[0,0], rangesHi=#[20000,1], in=30, bufnum, rate=100|
		in = In.kr(in, 2);
		in.poll(Impulse.kr(10), "stuff");
		in = in.collect{ |tin, i|
			tin.linlin(rangesLo[i], rangesHi[i], 0, 1);
		};
		Logger.kr(in, Impulse.kr(rate), bufnum);
	}).store;
	
	
	//SYNTH BUNDLE
	
	//make input file bundles
	ifb = s.makeBundle(false, {
		lastSynth = Synth(\playBufInput, [\out, 0, \bufnum, 0]);
	});
	
	ab = s.makeBundle(false, {
	
		curAnalysis.do{ |aKey|
			var ta = aTypes[aKey], buf;
			if (ta[\buffer].notNil) {
				buf = ~bufferPostInc.value;
				buffersToMake = buffersToMake.add([ta[\buffer][\numFrames], ta[\buffer][\numChannels], buf]);
			};
			
			logRanges = logRanges.add( ta[\range] );
			logNumChans = logNumChans + ta[\nChansOut];
			
			
			lastSynth = Synth(ta[\synthName], [\bufnum, buf, \in, 0, \out, ~channelPostInc] ++ ta[\params], lastSynth, \addAfter);
			
		};
		
		~logName  = "log_" ++ logNumChans;
		logRanges = logRanges.flop;
		lastSynth = Synth(~logName, [
			\bufnum, ~logBuf = ~bufferPostInc.value, 
			\in, 30, 
			\rate, analysisRate,
			\rangesLo, logRanges[0],
			\rangesHi, logRanges[1],
			], target: lastSynth, addAction: \addAfter);

	});
	
	
	ab = ifb ++ ab;
	
	ab = ab.insert(0, 0);
	
	a = ab;
	//BUFFER ALLOCATION BUNDLES
	
	buffersToMake.do{ |bufPars|
		a = s.makeBundle(false, {
			Buffer.alloc(s, bufPars[0], bufPars[1], a, bufPars[2]); 
		});
		a = a.insert(0, 0);
	};

	
	a = s.makeBundle(false, {
		var buf = Buffer.new(s,  44100 * (fileSegment[1] - fileSegment[0]), 2, 0);
		buf.allocRead(inputPath, 44100 * fileSegment[0], 44100 * (fileSegment[1] - fileSegment[0]), a);
	});
	a = a.insert(0, 0);
	
	
	a = s.makeBundle(false, {
		~buf = Buffer.alloc(s, lengthInSeconds * analysisRate, logNumChans, completionMessage: { a }, bufnum: ~logBuf);
	});
	
	a = a.insert(0, 0);	
	
	
	~logTime = lengthInSeconds;
	
	
	b = s.makeBundle(false, {
		~buf.write("sounds/testlogg.aiff", "AIFF", "float");
	});
	b = b.insert(0, ~logTime);
	//[ [ /b_write, 10, sounds/testlogg.aiff, AIFF, float, -1, 0, 0, nil ] ] 
	
	c = s.makeBundle(false, {
		~buf.close;
//		~fftbuf.close;
	});
	
	c = c.insert(0, ~logTime);
	//[ [ /b_close, 10, nil ] ]
	
	d = [~logTime, [\c_set, 0, 0]];
	
	g = [
		a,
		b,
		c,
		d
	];
			
	o = ServerOptions.new.numOutputBusChannels = 2; // stereo output
	f = Score.recordNRTBlock(g, "scoreTests/help-oscFile.osc", "scoreTests/helpNRT.aiff", options: o); // synthesize
	"dude".postln;

	f = SoundFile.openRead("sounds/testlogg.aiff");
	e = FloatArray.newClear(f.numChannels * f.numFrames);
	f.readData(e);
	e = e.clump(f.numChannels).flop;
	f.close;
//	e[0].linlin(0, 1, 0, 1000).plot;
	e[1].linlin(0, 1, 0, 1000).plot;
		


);


	f = SoundFile.openRead("sounds/testlogg.aiff");
	d = FloatArray.newClear(f.numFrames);
	f.readData(d);
	d = d.linlin(0, 1, 0, 1000);
	f.close;
	d.plot;	
	

	

f = SoundFile.openRead("tmplog/log_6.sclog0");
e = FloatArray.newClear(f.numChannels * f.numFrames);
f.readData(e);
e = e.clump(f.numChannels).flop;
f.close;
e[0].plot;
e[0].plot(discrete: true);


//for multichannel
f = SoundFile.openRead("sounds/testlogg_2.aiff");
e = FloatArray.newClear(f.numChannels * f.numFrames);
f.readData(e);
e = e.clump(f.numChannels).flop;
f.close;
e[0].plot;