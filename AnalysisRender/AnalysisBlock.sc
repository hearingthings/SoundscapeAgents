AnalysisBlock { //abstract class, wraps a synthdef with rate and bus information
	var <synthDefName, <>params, <server;

	var <desc;
	var <input, <output; //bus specs - [controlName, rate, nChannels, busIndex]
	var <>buffers, <>busses;
	var <>inputBlock, <>outputBlock; //the blocks that I take input from and send ouput to
//	var <>group;
	var <>synth;
	
	var <>nodeID, <>target, <>addAction; //how should I be added to the 

	var defaultSynthDefName;	
	classvar defaultSynthDefName;
	
//	classvar <>synthDescLib;
	*initClass{
		//initialize synths
	}
	needsPaths {^false }
	isLogBlock { ^false }

	
	*new { |synthDefName, params, server|
		synthDefName = synthDefName.asSymbol; //crucial for looking up in synthdesclib
//		if (server.isNil) { server = chain.server };
		
		^super.newCopyArgs(synthDefName, params, server).init;
	}
	
	*newFromArray{ |array, server|
		var synthDefName, params;
		synthDefName = array[0];
		params = array[1..];
		^super.newCopyArgs(synthDefName, params, server);
	}
	
	*newClear { ^super.new }
		
	
	init {
		
	//	super.init;
		
		this.initVars;
		
		//do we have a valid name either in the synthDefNameArg or as a defaultSynthDefName?
		if (synthDefName.notNil) {
			this.setSynthDefVars
		} {
			"Warning, no synthdef name set for AnalysisBlock".warn;
		};

		if (params.isNil) { params = [] };
		this.params_(this.completeParamsFromSubset(params));
		this.setInputOutputFromDesc;
	}
	
	setSynthDefVars{ 
		var inputUgens, outputData;
		desc = SynthDescLib.global.at(synthDefName);
		if (desc.isNil) { ("synthDefName" + synthDefName + "not found").throw };
		params = this.completeParamsFromSubset(params);
	}		
	
	
	completeParamsFromSubset { |subsetOfParams|
		var numControls = desc.controls.size;
		var remainingNames = desc.controls.collect(_.name);

		var paramsDict = IdentityDictionary.new;
		
		if (subsetOfParams.class == Array) {subsetOfParams = subsetOfParams.asArgsDict};
		if (subsetOfParams.isNil) { subsetOfParams = IdentityDictionary.new };		

		subsetOfParams.keysValuesDo{ |name, val|
			paramsDict.put(name, val);
		};


		desc.controls.do{ |controlName|
			var symbol = controlName.name.asSymbol;
			if (paramsDict.at(symbol).isNil) {
				paramsDict.put(symbol, controlName.defaultValue);
			}
		};
	
		^paramsDict
	}
	
	
	initVars {
		buffers = Array.newClear;
		busses = IdentityDictionary.new;
		this.initSubclassVars; 
	}
	
	initSubclassVars {} //will be subclassed
	

	setInputOutputFromDesc {
		var m = desc.metadata;
		if (m.outputName.notNil) {
			output = [ m.outputName, m.outputRate, m.outputChannels];
		};
		if (m.inputName.notNil) {
			input = [ m.inputName, m.inputRate, m.inputChannels];
		};
		//TODO: more sophisticated checking of metadata, automatic generation of input/output metadata?
	}

	inBus { //returns a bus object
		var inKey = input[0];
		//need to make sure the bus is allocated
		^busses[inKey]
	}
	outBus {
		var outKey = output[0];
		//need to make sure the bus is allocated
		^busses[outKey]
	}
	
	setOutputIndex { |index|
		if (output.size > 3) {
			output = output.put(3, index);	
		} {
			output = output.add(index);
		};
		this.busUpdateParams;
	}
	
	setInputIndex { |index|
		if (input.size > 3) {
			input = input.put(3, index);
		} {
			input = input.add(index);	
		};	
		this.busUpdateParams;
	}
	
	inName{ ^input[0] }
	inRate{ ^input[1] }
	inChannels { ^input[2] }
	inIndex {^input[3] }

	outName{ ^output[0] }
	outRate{ ^output[1] }
	outChannels { ^output[2] }
	outIndex { ^output[3] }
	
	busUpdateParams {
		if (input.notNil) { params.put( this.inName, this.inIndex ); };
		if (output.notNil) {params.put( this.outName, this.outIndex); };
	}
		
	//unused
	connectInputsToBlock { |block| //this assumes that they are the same rate
		var output = block.output;
		var inputName, spec;		
		//find the name of the input control
		inputName = this.input[0];
		
		spec = output.copy;
		spec.replaceAt(0, inputName);
		^spec
	}
	
	//unused
	allocBusReturnSpec { |busSpec| //we'll need to allocate output busses if they don't exist
		var rate = busSpec[1];
		var bus, allocator;
		
		//could put if(busses[busSpec[0]].isNil) { //so that we don't allocate a bus twice
		if(busses[busSpec[0]].notNil) { "Bus already allocated".warn;}; //instead we'll just warn if bus already exists 
		bus = switch(rate,
			\audio, {Bus.audio(server, busSpec[2])},
			\control, {Bus.control(server, busSpec[2])}
		).value;
		
		bus.debug("this is the inbus");
		
		busses.put(busSpec[0], bus);
		
		if (busSpec.size == 4) {
			busSpec.put(3, bus.index);	
		} {
			busSpec = busSpec.add(bus.index);	
		};
		
		^busSpec
	}
	
	
	allocBundle {
		^this.allocBuffersBundle;	 //usually for allocating buffers
	}
	
	runBundle { |server|
		var b = [];
		b = b ++ this.makeSynthBundle(server);
		^b	
	}
	
	finishBundle {
		^nil //usually for writing files	
	}
	
	freeBundle { |server|
		var b = [];
		b = b ++ this.freeSynthAndBuffers(server);
		^b
	}
	
	
	makeSynthBundle { |server| //assumes node has been connected
		var paramsArray;
		
		//make sure inputs are reflected in params
		this.busUpdateParams;

		//plug in buffer args if there are any
		if (buffers.size > 0) {
			buffers.do{ 
				|buffer, i| 
				var bufArgName = if (i > 0) {"bufnum" ++ i} {"bufnum"};
				params.put(bufArgName.asSymbol, buffer.bufnum);
			};
		};
		
		//make array of params
		paramsArray = params.asArgsArray;
				
		
		if (target.isNil) {target = 1; "target was nil".warn;};
		if (addAction.isNil) {addAction = 0; "addAction was nil".warn;};
		if (nodeID.isNil) { nodeID = server.nextNodeID; };
		
		
		synth = Synth.basicNew(synthDefName, server, nodeID);
		
		^[synth.newMsg(target, paramsArray, addAction)];
		
		//anything else need to be plugged in?
			//not yet
			
		//make new server message
		//^(['/s_new', synthDefName, nodeID, addAction, target] ++ paramsArray)		
		
	}
	
	
	
	
	makeBufferInfo { |server, duration| 
		
	}

	allocBuffersBundle { |completionMsg|
		var b = [];
		buffers.do{ |buf, i|
			if (buf.class != Buffer) 
				{ ("buffers ivar should hold only buffer objects, currently a" + buf.class.asString).throw };
			b = b.add(buf.allocMsg(completionMsg));
		};
		^b;
	}

	freeSynthAndBuffers {
		var msg = [];
		if (buffers.size > 0) {
			buffers.do{ |buf, i|
				msg = msg.add(buf.freeMsg);
			};
		};
		if (nodeID.notNil) {
			msg = msg.add( ["/n_free", nodeID] );
		};
		^msg
	}

}

//INPUT SOURCE
AInputPlayBuf : AnalysisBlock {
	classvar synthDefNameBase;

	var <>path, duration;

	*initClass { synthDefNameBase = "playBufOut_"; this.initSynths }
	
	*newWithPath { |server, path| ^super.newClear.path_(path).init;}
	
	init {
		var file, numChannels;

		file = SoundFile.new;
		file.openRead(path);
		numChannels = file.numChannels;
		file.close;
		
		
		synthDefName = this.defNameForFileChannels(numChannels);
//		buffers = [ Buffer(server) ];
		super.init
	}
	
	duration {
		var file;
		if (duration.isNil) {
			file = SoundFile.new;
			file.openRead(path);
			duration = file.duration;
			file.close;
			^duration
		} {
			^duration
		};
	}
	
	makeBufferInfo { |server, duration|
		buffers = [ Buffer(server) ];
		^buffers
	}
	
	allocBuffersBundle { |completionMsg|
		var b = [], msg;
//		buffers.do{ |buf, i|
//			if (buf.class != Buffer) 
//				{ ("buffers ivar should hold only buffer objects, currently a" + buf.class.asString).throw };
//			msg = buf.allocReadMsg(path, 0, -1, completionMsg); //<- difference to AnalysisBlock
//			b = b.add(msg);
//		};
		path.debug("path");
		msg = buffers[0].allocReadMsg(path, 0, -1, completionMsg); //<- difference to AnalysisBlock
		^[msg];
	}
	
	defNameForFileChannels { |numChannels|
		^switch(numChannels,
			1, \playBufOut_1,
			2, \playBufOut_2
		).value;
	}
	
	*initSynths {
		//for now, output is only mono, even for the stereo version
		SynthDef(\playBufOut_1, { |bufnum, outar0=0|
			var chain;
			chain = PlayBuf.ar(1, bufnum);
			Out.ar(outar0, chain);
		}, metadata: (outputName: \outar0, outputRate: \audio, outputChannels: 1) ).store;
		
		SynthDef(\playBufOut_2, { |bufnum, outar0=0|
			var chain;
			chain = PlayBuf.ar(2, bufnum);
			chain = Mix(chain);
			Out.ar(outar0, chain);
		}, metadata: (outputName: \outar0, outputRate: \audio, outputChannels: 1) ).store;
		
		
		//should add diskin for super long files	
	}
	
}

AInputBusMono : AnalysisBlock {
	
	var <>inputBus;

	*initClass { this.initSynths }
	
	*new { |server, inputBus|
		^super.newClear.initWithArgs(server, inputBus);
	}
	
	initWithArgs { |sA, iA|
		server = sA; inputBus = iA;
		
		synthDefName = \inOutMono;

		super.init;
		
		params.put(this.inName, inputBus);
	}
	
	*initSynths {
		SynthDef(\inOutMono, { |inar0=0, outar0=0|
			var chain;
			chain = In.ar(inar0, 1);
			Out.ar(outar0, chain);
		}, metadata: (
			inputName: \inar0, inputRate: \audio, inputChannels: 1,
			outputName: \outar0, outputRate: \audio, outputChannels: 1
		) ).store;	
		
	}	
	
}


//ANALYSIS BLOCKS

//ANALYSIS!! - the synths that actually generate data
AAnalysis : AnalysisBlock {

	//TODO: make sure all subclasses have a valid maxTrigsPerSecond
	numFramesForDuration { |duration|
		^(duration * this.rateHZ;).ceil;
	}
	
	rateHZ {
		^switch(this.outRate,
			\audio, {server.options.sampleRate},
			\control, {server.options.blockSize / server.options.sampleRate}
		).value;	
	}

	logType {
		if (desc.metadata.logType.isNil) { "AAnalysis synthdef does not have logType metadata".throw; };
		^desc.metadata.logType;
	}

}

AFFTAnalysis : AAnalysis {
	
	*initClass {this.initSynths; } 

	fftSize {
		^(params[\fftSize] ? desc.metadata[\fftSize] ? nil);
	}		

//	maxTrigsPerSecond {
//		var hop;
//		params[\hop].debug("max trigs params hop");
//		hop = params[\hop] ? 0.5;
//		^( server.sampleRate / (this.fftSize * hop) );
//	}

//commented out 9/7/11 because we don't need that	
//	makeBufferInfo { |server|
//
//		buffers = [
//			Buffer(server, this.fftSize * 2, 1);
//		];
//		
//	}
	
	
	*initSynths {

		SynthDef('SpecFlatness', { |fftSize=1024, hop=0.5, inar0=0, outkr0=30|
			var chain, trig, bufnum;
			bufnum = LocalBuf(fftSize*2, 1);
			chain = In.ar(inar0, 1);
			chain = FFT(bufnum, chain, hop, 1);
			//trig = chain > -1; //these may trigger everytime a new fft buffer is available
			//trig = Impulse.kr( 44100 / ((BufFrames.ir(bufnum)/2) * hop) ); //this estimates fft rate
			chain = SpecFlatness.kr(chain);
			Out.kr(outkr0, chain);
		}, metadata: (logType: \continuous, interp: \lin, inputName: \inar0,inputRate: \audio,inputChannels: 1,outputName: \outkr0,outputRate: \control, outputChannels: 1)
		).store;

		SynthDef('SpecCentroid', { |fftSize=1024, hop=0.5, inar0=0, outkr0=30|
			var chain, trig, bufnum;
			bufnum = LocalBuf(fftSize*2, 1);
			chain = In.ar(inar0, 1);
			chain = FFT(bufnum, chain, hop, 1);
			//trig = chain > -1; //these may trigger everytime a new fft buffer is available
			//trig = Impulse.kr( 44100 / ((BufFrames.ir(bufnum)/2) * hop) ); //this estimates fft rate
			chain = SpecCentroid.kr(chain);
			Out.kr(outkr0, chain);
		}, metadata: (logType: \continuous, interp: \lin, inputName: \inar0,inputRate: \audio,inputChannels: 1,outputName: \outkr0,outputRate: \control, outputChannels: 1)
		).store;

		SynthDef('FFTFlux', { |fftSize=1024, hop=0.5, inar0=0, outkr0=30, normalize=1|
			var chain, trig, bufnum;
			bufnum = LocalBuf(fftSize*2, 1);
			chain = In.ar(inar0, 1);
			chain = FFT(bufnum, chain, hop, 1);
			//trig = chain > -1; //these may trigger everytime a new fft buffer is available
			//trig = Impulse.kr( 44100 / ((BufFrames.ir(bufnum)/2) * hop) ); //this estimates fft rate
			chain = FFTFlux.kr(chain, normalize);
			Out.kr(outkr0, chain);
		}, metadata: (logType: \continuous, interp: \lin, inputName: \inar0,inputRate: \audio,inputChannels: 1,outputName: \outkr0,outputRate: \control, outputChannels: 1)
		).store;

		SynthDef('FFTFluxPos', { |fftSize=1024, hop=0.5, inar0=0, outkr0=30, normalize=1|
			var chain, trig, bufnum;
			bufnum = LocalBuf(fftSize*2, 1);
			chain = In.ar(inar0, 1);
			chain = FFT(bufnum, chain, hop, 1);
			//trig = chain > -1; //these may trigger everytime a new fft buffer is available
			//trig = Impulse.kr( 44100 / ((BufFrames.ir(bufnum)/2) * hop) ); //this estimates fft rate
			chain = FFTFluxPos.kr(chain, normalize);
			Out.kr(outkr0, chain);
		}, metadata: (logType: \continuous, interp: \lin, inputName: \inar0,inputRate: \audio,inputChannels: 1,outputName: \outkr0,outputRate: \control, outputChannels: 1)
		).store;
		
		SynthDef('SpecCrest', { |fftSize=1024, hop=0.5, inar0=0, outkr0=30, freqLo=20, freqHi=20000|
			var chain, trig, bufnum;
			bufnum = LocalBuf(fftSize*2, 1);
			chain = In.ar(inar0, 1);
			chain = FFT(bufnum, chain, hop, 1);
			//trig = chain > -1; //these may trigger everytime a new fft buffer is available
			//trig = Impulse.kr( 44100 / ((BufFrames.ir(bufnum)/2) * hop) ); //this estimates fft rate
			chain = FFTCrest.kr(chain, freqLo, freqHi);
			Out.kr(outkr0, chain);
		}, metadata: (
			logType: \continuous, 
			interp: \lin, 
			inputName: \inar0,
			inputRate: \audio,
			inputChannels: 1,
			outputName: \outkr0, 
			outputRate: \control,
			outputChannels: 1
		)
		).store;
	
		
		SynthDef('SpecPcile', { |fftSize=512, hop=0.5, inar0=0, outkr0=30, fraction=0.9|
			var chain, bufnum;
			bufnum = LocalBuf(fftSize*2, 1);
			chain = In.ar(inar0, 1);
			chain = FFT(bufnum, chain, hop, 1); 
			chain = SpecPcile.kr(chain, fraction, 1);
		//	chain = SinOsc.ar(1).range(20,10000);
			Out.kr(outkr0, chain);
		}, metadata: (
			logType: \continuous, 
			interp: \lin,
			inputName: \inar0,
			inputRate: \audio,
			inputChannels: 1, 
			outputName: \outkr0, 
			outputRate: \control,
			outputChannels: 1,
			outputSpec: \freq //an instance of controlspec
		) ).store;
	}
	
}

APitchAnalysis : AAnalysis {
	
	maxTrigsPerSecond {
		^params[\execFreq];
	}

}

ALevelsAnalysis : AAnalysis {
	*initClass{ this.initSynths }

	*initSynths {
		SynthDef(\ampDB, { |inar0=0, outkr0=30, attackTime = 0.01, releaseTime = 0.01|
			var chain, bufnum;
			chain = In.ar(inar0, 1);
			chain = Amplitude.kr(chain, attackTime, releaseTime);
			chain = chain.ampdb;
			Out.kr(outkr0, chain);
		}, metadata: (
			logType: \continuous, 
			interp: \lin,
			inputName: \inar0,
			inputRate: \audio,
			inputChannels: 1, 
			outputName: \outkr0, 
			outputRate: \control,
			outputChannels: 1,
			outputSpec: \db //an instance of controlspec
		) ).store;	


		SynthDef(\rmsDB, { |inar0=0, outkr0=30, windowSize = 40|
			var chain, bufnum;
			chain = In.ar(inar0, 1);
			chain = RunningSum.rms(chain, windowSize);
			chain = A2K.kr(chain);
			chain.poll(Impulse.kr(10), "rms");
			chain = chain.ampdb;
			Out.kr(outkr0, chain);
		}, metadata: (
			logType: \continuous, 
			interp: \lin,
			inputName: \inar0,
			inputRate: \audio,
			inputChannels: 1, 
			outputName: \outkr0, 
			outputRate: \control,
			outputChannels: 1,
			outputSpec: \db //an instance of controlspec
		) ).store;
	}	
}





//LOGGERS - these write the analysis data to buffers

/*
	TODO:
	- estimate number of logs needed
	
*/

ALogger : AnalysisBlock {
	var <>data, <>paths; //paths correspond to the paths of the buffers
	
	isLogBlock { ^true }
	needsPaths { ^true }
	*initClass {this.initSynths }	
	
	*newDefault { 
		//need to make params?
		^super.new(this.defaultSynthDef)
	}
	
	finishBundle {
		var b = [];
		buffers.do{ |buffer, i|
			b = b.add(buffer.writeMsg(paths[i], "wav", "float"));
		};
		^b
	}
	
	numPaths { ^buffers.size }
	
	setPaths { |pathsArg|
		paths = pathsArg;
		//could make sure that pathsArg and buffers are same size
	}
	
	*initSynths {
		SynthDef(\logTrigWithDuration, { |bufnum, bufnum1, bufnum2, inkr0=30, durGrain=1000|
			var duration, trig, delayedTrig, value, ntrigs;
			
			#value, trig = In.kr(inkr0, 2);
			delayedTrig =  DelayN.ar(trig, SampleDur.ir, SampleDur.ir) ;
			
			
			trig.poll(trig, "trigged", bufnum2);
			
			Logger.kr(value, trig, bufnum);
			
			duration = PulseCount.ar(Impulse.ar(durGrain), delayedTrig);
			duration = duration / durGrain;
			
			duration = A2K.kr(duration);
			
			ntrigs = PulseCount.kr(trig);
			
	//		(ntrigs > (BufFrames.ir(bufnum) - 1)).poll(trig, "ntrigs bool");
			
			Logger.kr(duration, trig * (ntrigs < BufFrames.ir(bufnum1) + 1), bufnum1);
			
		//	Logger.kr( 1, ntrigs > (BufFrames.ir(bufnum) - 1), bufnum2); //this gives a true/false if the buffer is full or not
		
		//	Logger.kr( ntrigs.linlin(0, BufFrames.ir(bufnum), 0, 1), delayedTrig, bufnum2, trig);

			Logger.kr( ntrigs, delayedTrig, bufnum2, trig);
			
		}).store;
	}	
}


AContinuousLogger : ALogger {
	var <sampleRate;
	logType { ^\continuous }
	*initClass { this.initSynths }
	*defaultSynthDef { ^"aContLogger_kr"  }
	defaultSynthDef { ^this.class.defaultSynthDef }

	makeBufferInfo { |server, duration|
		var numFrames;
		sampleRate = (server.options.sampleRate/server.options.blockSize);
		numFrames = duration * sampleRate;
		numFrames = numFrames.ceil.asInteger; //always sample a little faster
		buffers = [
			Buffer(server, numFrames, 1),
		];	
	}

	renderComplete {
		//write correct sample rate onto file
		paths.do{ |path, i|
			var file, data, nf, nc, success;
			file = SoundFile.new;
			file.openRead(path);
			nf = file.numFrames;
			nc = file.numChannels;
			data = FloatArray.newClear(nf* nc);
			file.readData(data);
			file.close;
			
			file = SoundFile.new;
			success = file.openWrite(path);
			file.numChannels = nc;
			file.sampleRate = sampleRate.ceil;
			file.writeData(data);
			file.close;
			
			("wrote file with new samplerate" + sampleRate + success).postln
		};
	}


//this gets handled by the analysis data object
//	loadDataEnv {
//		//TODO: load data from buffer and return it
//		^paths.collect{ |path, i|
//			var file, data, nf, nc;
//			var env, times;
//			file = SoundFile.new;
//			file.openRead(path);
//			nf = file.numFrames;
//			nc = file.numChannels;
//			data = FloatArray.newClear(nf* nc);
//			file.readData(data);
//			file.close;
//			
//			times = FloatArray.fill(data.size-1, { sampleRate });
//			env = Env(data, times)
//			
//			^env
//			
//		};		
//			
//		
//	}

	
	*initSynths {
//		SynthDef("aContLogger", { |bufnum, inkr0, rate| //this is variable rate
//			var chain, next, step, done;
//			
//			chain = SinOsc.ar(ControlRate.ir/100);
//			
//			next = Impulse.ar(ControlRate.ir * 3);
//		//	step = Stepper.ar(next, 0, -1, BufFrames.kr(bufnum) + 1, 1);
//			step = Phasor.ar(0, rate/SampleRate.ir, 0, BufFrames.kr(bufnum) + ControlRate.ir);
//			done = BufWr.ar(chain, bufnum, step, 0);
//			done = Done.kr(done);
//			//could send a trig here
//			FreeSelf.kr(done);
//			
//		}).send(s);		

		SynthDef("aContLogger_kr", { |bufnum, inkr0, outkr0|
			var chain, step, done;
			chain = In.kr(inkr0, 1);
			step = Phasor.kr(0, 1, 0, BufFrames.kr(bufnum));
			done = BufWr.kr(chain, bufnum, step, 0);
			done = Done.kr(done);
			//could send a trig here
			FreeSelf.kr(done);
			Out.kr(outkr0, chain); //pass output along
		}, metadata: (
			inputName: \inkr0,
			inputChannels: 1,
			inputRate: \control,
			outputName: \outkr0, 
			outputRate: \control,
			outputChannels: 1
		)
		).store;
	}
	
}




//ADiscreteLogger : AnalysisBlock {
////	classvar <>ioRate;
//	var <>defaultSynthDefName;
//	
//	var <>paths, <data;
//	var <>estimatedRate;
//	
//	*initClass{ 
////		ioRate = \control;
//		this.initSynths;
//	}
//	
//	*newForChain{ |chain|
//		^super.newClear.initWithChain(chain);
//	}
//	
//	initWithChain { |chainArg|
//		this.initDefaultSynthName;
//		defaultSynthDefName.debug("dsdn");
//		this.init(defaultSynthDefName, [], chainArg);
//	}
//		
//	initDefaultSynthName {
//		defaultSynthDefName = \logTrigWithDuration;
//	}
//		
//	loadData {
//		data = paths.collect{ |path, i|
//			var f, d;
//			f = SoundFile.openRead(path);
//			d = FloatArray.newClear(f.numChannels * f.numFrames);
//			f.readData(d);
//
//			if (f.numChannels > 1) { //more than one channel?
//				d = d.clump(f.numChannels).flop;	//data must be de-interleaved
//			};
//			f.close;
//			
//			d //return the array
//		};
//	}
//	
//	
//	loadLogDataReturnEnv {
//		this.loadData;
//		this.makeEnvFromData;
//	
//	}
//	
//	makeEnvFromData {
//		^this.subclassResponsibility(thisMethod);
//	}
//
//	rawAnalysisData {
//		if (data.isNil) {this.loadData};
//		^data[0]
//	}
//	
//	rawTimeData {
//		if (data.isNil) { this.loadData };
//		^data[1]
//	}	
//	
//	rawNumFramesLogged {
//		if (data.isNil) {this.loadData};
//		^data[2]
//	}
//	
//	enumeratePaths { |basePath|
//		paths = buffers.collect{ |buf, ibuf|
//			basePath ++ ibuf.asString;
//		};
//		^paths
//	}
//	
//	writeMsg { |pathArg|
//		var msg;
//		if (paths.isNil) {this.enumeratePaths(pathArg)};
//		if (paths.isNil) {"paths is nil, errors imminent".warn};
//		msg = buffers.collect{ |buf, ibuf| 
//			buf.writeMsg(paths[ibuf], "AIFF", "float");
//		};
//		^msg
//	}
//	
//	writeCloseMsg {
//		^buffers.collect{ |buf| buf.closeMsg};
//	}
//
//}

//on a trigger from the analysis UGen, log the incoming value on the data bus, and the time between triggers on the time bus
//ADiscreteLogger : ALogger { 	
//	
//	
//
//	prMakeBufferInfo { |server, numFrames|
//		buffers = [
//			Buffer(server, numFrames, 1),
//			Buffer(server, numFrames, 1),
//			Buffer(server, 1, 1)
//		];
//	}
//	
//}


//PRE-ANALYSIS FILTERS
AFilter : AnalysisBlock {

	*initClass{ 
		this.initSynths;
	}

	*initSynths {
		SynthDef(\afilterLPF_1, { |inar0, outar0, lowpassFreq = 200|
			var chain;
			chain = In.ar(inar0, 1);
			chain = LPF.ar(chain, lowpassFreq);
			Out.ar(outar0, chain);
		}).store;
	}

}

+ AFFTAnalysis {

}

		



+ Array {

	asArgsDict {
		var id = IdentityDictionary.new;
		this.clump(2).do{ |k, v|
			id.put(k, v);
		}	
	}
	
}

+ IdentityDictionary {
	asArgsArray {
		var ar = Array.newClear;
		this.keysValuesDo{ |key, val|
			ar = ar ++ [key, val];	
		};
		^ar
	}
}
