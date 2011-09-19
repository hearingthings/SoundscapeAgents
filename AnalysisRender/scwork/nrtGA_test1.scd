/* ==================================================================================== */
/* = NRT GA :: testing that everything syncs up when doing NRT and loading buffers... = */
/* ==================================================================================== */

(
SynthDef(\fakeInput, { |out=0|
	var chain;
	chain = SinOsc.kr(0.7).range(100, 2000);
	Out.kr(1, chain.linlin(0, 20000, 0, 1));
	Out.ar(1, K2A.ar(chain).linlin(0, 20000, 0, 1));
	Out.ar(0, SinOsc.ar(chain, 0, 0.2));
}).store;


SynthDef(\freqLog, { |in= 0, out=30|
	var freq, hasFreq;
	in = In.ar(in, 1);
	#freq, hasFreq = Pitch.kr(in, execFreq: 400);
	Out.kr(out, freq);
}).store;

SynthDef(\log_1, {|rangesLo=#[0], rangesHi=#[20000], in=30, bufnum, logRate=100|
	in = In.kr(in, 1);
	Logger.kr(in.linlin(rangesLo, rangesHi, 0, 1), Impulse.kr(logRate), bufnum);
}).store;


g = s.makeBundle(false, { ( id:(2..5)).group.play; });

~bufnum = 1;

~newSynths = s.makeBundle(false, {
	x = Synth(\fakeInput, [\out, 0]);
	y = Synth(\freqLog, [\in, 0], target: x, addAction: \addAfter);
	z= Synth(\log_1, [\bufnum, ~bufnum, \in, 1], target: y, addAction: \addAfter);	
});
~newSynths.insert(0, 0); //times

a = s.makeBundle(false, {
	~buf = Buffer.alloc(s, 100, bufnum: ~bufnum, completionMessage: { ~newSynths });
});
//[ [ /b_alloc, 10, 100, 1, nil ] ]
a = a.insert(0, 0);

b = s.makeBundle(false, {
	~buf.write("sounds/testlogg.aiff", "AIFF", "float");
});
b = b.insert(0, 1);
//[ [ /b_write, 10, sounds/testlogg.aiff, AIFF, float, -1, 0, 0, nil ] ] 

c = s.makeBundle(false, {
	~buf.close;
});
c = c.insert(0, 1);
//[ [ /b_close, 10, nil ] ]

d = [1, [\c_set, 0, 0]];

g = [
	a,
	b,
	c,
	d
];
		
o = ServerOptions.new.numOutputBusChannels = 2; // stereo output
Score.recordNRT(g, "scoreTests/help-oscFile.osc", "scoreTests/helpNRT.aiff", options: o); // synthesize

);


f = SoundFile.openRead("sounds/testlogg.aiff");
d = FloatArray.newClear(f.numFrames);
f.readData(d);
d = d.linlin(0, 1, 0, 20000);
f.close;
d.plot;

f = SoundFile.openRead("scoreTests/helpNRT.aiff");
e = FloatArray.newClear(f.numChannels * f.numFrames);
f.readData(e);
e = e.clump(f.numChannels).flop;
f.close;
e[1].linlin(0, 1, 0, 20000).plot;
e[0].plot;


//for multichannel
f = SoundFile.openRead("sounds/testlogg_2.aiff");
e = FloatArray.newClear(f.numChannels * f.numFrames);
f.readData(e);
e = e.clump(f.numChannels).flop;
f.close;
e[0].plot;