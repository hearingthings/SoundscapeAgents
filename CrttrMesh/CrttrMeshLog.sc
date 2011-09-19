
CrttrMeshLog { //a crttrmesh explicitly for nrt stuff. can be archived and read back.
	//server
	var <>mesh; //the mesh we are archiving
	var <>time; //the the overall time one should render
	var <>path; //the path - should be a directory
	
	var <pathAllocator, <archivePathAllocator, <recordNodes;
	var <>archive;
	
	var <renderComplete;
	var <>xListeners, <>yListeners;
	var <listenerSynths, <listenerOutputArray, <recordBuffers, <recordSynths, <listenerOutputPaths;
	var <>options;
	var <>soundfiles;
	
	*newFromMesh{|mesh, time|
		//copys mesh to self
	}
	
	*newFromPath{ |path| //path is archive file of self

	}
	
	
	*new{ |mesh, time, path = "tmp"| //like CrttrMesh new but with the args we need
		var l = super.new;
		l.mesh_(mesh);
		l.time_(time);
		l.path_(path);
		l = l.init;
		^l
	}

	init {
		//super.init;
		renderComplete = false;
		pathAllocator = PathAllocator.newClearDir(path, "meshLog", "wav");
		archivePathAllocator = PathAllocator.newClearDir(path, "CMFileArchive", "sc3archive");
	}
	
	
	//ADD "parasite" NODES to MESH THAT RECORD OUTPUT of all nodes
	
	recordSingleNode { |nodeToRecord|
		//look at outputs of node
		^nodeToRecord.outputs.collect{ |outArray|
			var logNode;
			logNode = CMWriteParamFiles(mesh, [0,0,0]);
			logNode.nodeToRecord_(nodeToRecord)
				.path_(pathAllocator.alloc)
				.rate_( this.rateConversion(outArray[1]) )
				.rateSymbol_( outArray[1] )
				.param_(outArray[0])
				.numChannels_(outArray[2])
				.length_(time)
				.busSpec_(outArray);
		};
		
		
		//returnes a node	
	}
	
	addRecordNodes {
		//only record nodes that have ouput
		recordNodes = mesh.nodes.select{ |node| node.outputs.notNil };
		
		//now we'll add nodes to record
		recordNodes = recordNodes.collect{ |node|
			node -> this.recordSingleNode(node)
		}
		^recordNodes	
	}
	
	rateConversion { |rate|
		var sampleRate = mesh.server.options.sampleRate ? 44100;
		^switch(rate,
			\audio, {sampleRate},
			\control, {sampleRate/mesh.server.options.blockSize}
		).value;
	}
	
	//REAL-TIME LOGGING
		//not done yet
	
	//RENDERING METHODS
	render {
		var score, archiveNodes;
		renderComplete = false;
		if (recordNodes.isNil) {
			this.addRecordNodes;	
		};
		score = mesh.asScore(time);
		Score.recordNRTBlock(score, pathAllocator.baseDir ++ "tmp.osc", pathAllocator.baseDir ++ "tmp.aiff", options: this.optionsForCurrentRender);
		
		archiveNodes = this.recordNodesToPlaybackNodes(recordNodes);
		archive = CMFileArchive.newWithMeshAndNodes( mesh, archiveNodes, archivePathAllocator.alloc);
		archive.write;
		
		renderComplete = true;

		"Render Complete".postln;		
	}
	
	
	recordNodesToPlaybackNodes { |recordNodes|
		//make playback nodes from recordNodes
		^recordNodes.collect{ |recNodesAcc|
			var node, params, pbnode;
			node = recNodesAcc.key;
			params = recNodesAcc.value;
			
			pbnode = params[0].class.readClass.perform(\basicNew);
			pbnode.paramList_(params);
			pbnode.ogNode_(node);			
			pbnode
		};
	}
	
	
	
	//MAKING MAPS
/*		
	renderWithXYListeners{|xListenerArg, yListenerArg|

	}
	
	partitionListeners { |size=16| //.wav is 256 channels
		//figure out the way that outputs need to be partitioned
		var tot, ends, segments;
		var r, n, l; 
		tot = xListeners * yListeners;
		
		r = tot;
		ends = [];
		
		while({r>0}, {
			r = r - size;
			n = r max: 0;
			ends = ends.add(tot - n);
		});

		
		l = 1;
		segments = ends.collect{ |end|
			end = [l, end];
			l = end[1] + 1;
			end;	
		};
		segments = segments - 1; //(adjust for array notation);
		
		^segments
	}
	
	makeListenerSynths {
		var xDist, yDist;
		var out = 0;
		
		listenerSynths = [];
		
		xDist = bounds.width / (xListeners-1);
		yDist = bounds.height / (yListeners-1);
		
		yListeners.do{ |yi| //row
			xListeners.do{ |xi| //column
				var xPos = xi * xDist;
				var yPos = yi * yDist;
				var monoList;
				[xPos, yPos].debug("positions");
				monoList = CNAmbiListenMono(this, [xPos, yPos, 0]); //add a mono listener at a point
				listenerSynths = listenerSynths.add(monoList);
			};
		};		
	}
	
	makeListenersBundle {
		var bundle = [];
		//LISTENING
		//first, we allocate listeners synths
		this.makeListenerSynths; //listenerSynths is now updated


			//make bundles for all listeners (add to bundle queue?)
		listenerSynths.do{ |listener|
			bundle = bundle ++ listener.makeBundle;
		};	
		
		^bundle
	}
	
	makeRecordBundle {
		var recordDefs;
		var bundle = []; //this is our bundle
		
		
		//RECORDING
		//first partition listeners
		listenerOutputArray = this.partitionListeners(16);
		
		//store recordSynthDefs for those outputs
		recordDefs = listenerOutputArray.collect{ |startEndArray|
			var numChans = ((startEndArray[1] - startEndArray[0]) + 1).asInteger;
			var name = "rbIn_" ++ numChans.asInteger.round(1);
			
//			if (SynthDescLib.global[name].isNil) {
			
				SynthDef(name, { |bufnum|
					var in, audio;
					in = Control.names([\in]).ir(Array.fill(numChans, {0}));
					audio = In.ar(in, 1);
					audio.debug("audio ugen");
					RecordBuf.ar(audio, bufnum);
				}).store;
//			};
			name; //return def name
		};
		
		//allocate buffers for those record synths
		recordBuffers = listenerOutputArray.collect{ |sea|
			var numChannels, numFrames, b;
			numChannels = (sea[1] - sea[0]) + 1;
			numFrames = time * 44100;
			
			b = Buffer.new(server, numFrames, numChannels);
		};
		
		//allocate record synths for those outputs
		recordSynths = recordDefs.collect{ |defName, i|
			var indexes, chans = [];
			var synth, msg;
			
			indexes = listenerOutputArray[i];
			for(indexes[0], indexes[1], { |li|
				chans = chans.add(
					listenerSynths[li].busses['out']
				);
			});
			
			synth = Synth.basicNew(defName, server);
			msg = synth.newMsg(groups['patch'], [\bufnum, recordBuffers[i].bufnum, \in, chans]); 
			[synth, msg]
		};		
		
		//stash some information about the mapping between listeners and buffers in the log variables?
		listenerOutputPaths = listenerOutputArray.collect{ |bufPath, i|
			bufPath = bufPath[0].asString ++ "_" ++ bufPath[1].asString;
			bufPath = path ++ "-" ++ bufPath;
			bufPath = bufPath ++ ".wav";
		};
		
		//make bundles for buffers and recordSynths
		recordBuffers.do{ |buf, i|
			var bufMsg = buf.allocMsg(recordSynths[i][1]);
			bundle = bundle.add(bufMsg);
		};

		
		^bundle
	}
	
	writeBuffersBundle {
		//loop through recordBuffers
		var b = [];
		recordBuffers.do{ |buf, i|
			var bufPath, writeMsg;
			//make path
			bufPath = listenerOutputPaths[i];

			//make message
			writeMsg = buf.writeMsg(bufPath, 'wav', 'float');
			b = b.add(writeMsg);
		};
		^b
	}



	doRender { |completionFunc|
		var score = [], n;
		
		//get options for current render
		
		
		
		//puke out all bundles, save to score
		//	make bundles
		n = this.makeAllBundle;
		n = n.insert(0, 0);
		score = score.add(n);
		
		n = this.bundleSafeWithTime( this.makeListenersBundle, 0);
		n.do{ |sc| score = score.add(sc) };
		
		n = this.makeRecordBundle;
		n = n.insert(0, 0);
		score = score.add(n);
		
			
		n = this.writeBuffersBundle; //TODO: should probably grab finish bundles from nodes
		n = n.insert(0, time);
		score = score.add(n);
		
		score = score.add([time, [\c_set, 0, 0]]); //finish message		
		score = Score(score);
		
//		score.clumpBundles(8192);

		score.recordNRT(path ++ ".osc", path ++ ".wav", nil, 44100, 'wav', 'float', server.options);
		
		^score;
			//recordNRT(list, oscFilePath, outputFilePath, inputFilePath, sampleRate, headerFormat, sampleFormat, options, completionString, duration)
		
		//write to file
		//wait while rendering is happening
		//completionFunc.value(meshLog)

	}
	

	
//	sourcePointsForFrame { |frame|
//		sources.collect{ |node| 
//			[ pos ] ++ node.dataAtFrame;
//		}
//	}

	initSoundfiles {
		soundfiles = listenerOutputPaths.collect{ |path|
			var f = SoundFile.openRead(path);
			f	
		};
	}

	listenerArrayForFrame { |frame|
		//
		var array = [];
		if (soundfiles.isNil) { this.initSoundfiles };
		
		soundfiles.do{ |sf|
			var data = FloatArray.newClear(sf.numChannels);
			sf.seek(frame, 0);
			sf.readData(data);
			array = array ++ data;
		};
		
		array = array.collect{ |data, i|
			var p;
			p = listenerSynths[i].pos;
			p = Point(p[0], p[1]);
			[ data, p ];	
		};
		
		^array
	}	
*/	

		
	optionsForCurrentRender {
		//new server options
		^this.class.options
		//TODO: relate numOutputBusChannels to actual number of outputs
	}

	*options {
		//new server options
		var options = ServerOptions.new;
		options.memSize = 2.pow(20);
		options.numAudioBusChannels = 2.pow(16);
		options.numOutputBusChannels = 1;
		options.maxNodes = 2.pow(16);
		options.numBuffers = 2.pow(14);
		options.numWireBufs = 1024;
		^options
	}
	
}

CMArchiveNoSpatial : CrttrMeshNode {
	
	spatialBundle	{ //you can't set the position of these nodes
		^nil
	}

}


CMWriteParamFiles : CMArchiveNoSpatial { //this writes a single parameter to a file
	var <>nodeToRecord, <>path, <>rate, <>rateSymbol, <>param, <>length, <>buffer, <>numChannels;
	var <>busSpec;
	
	*readClass { ^CMReadParamFiles }
	
	outputs { ^nil }
	sourceDef { ^nil }
	internalBusses { ^[ [\spatial, \control, 3] ] } //TODO: why do you have a spatial bus?
	
	allocFunc { 
		var bufFrames;
		if (rate.isNil or: length.isNil or: numChannels.isNil) { ("Must set vars: rate, length, numChannels" + this.class.asSymbol).throw };
		//allocate buffer for recording
		bufFrames = rate * length;
		buffer = Buffer.new(mesh.server, bufFrames, numChannels);
		^[buffer.allocMsg]
	}
	
	
	finishBundle {
		//write buffer to path	
		^[buffer.writeMsg(path, "wav", "float")]
	}
	
	listenToNodeBundle { |nodeIn|
		var args, synth;
		//listen to a single node
		if (nodeToRecord == nodeIn) {
			args = 
			[
				\in, nodeToRecord.busses[param].index,
				\bufnum, buffer.bufnum
			]; //this is the param to record
			synth = Synth.basicNew(this.defName, mesh.server);
			^[synth.newMsg(groups[\listener], args)];
		};
		^nil
	}
	
	freeFunc {
		^[buffer.freeMsg]
	}
	
	defName {
		var base;
		base = switch(rateSymbol.asSymbol,
			\audio, "cnLogParam_ar",
			\control, "cnLogParam_kr"
		).value;
		
		base = base ++ numChannels.asString;
		^base
	}
	
	*storeSynthDefs {
		(1..10).do{ |nc|
			var krName = "cnLogParam_kr" ++ nc; 					var arName = "cnLogParam_ar" ++ nc;
			
			SynthDef(krName, { |in, bufnum|
				var signal;
				signal = In.kr(in, nc);
				BufWr.kr(signal, bufnum, Phasor.kr(0, 1, 0, BufFrames.kr(bufnum)));
				
			}).store;
			
			SynthDef(arName, { |in, bufnum|
				var audio;
				audio = InFeedback.ar(in, nc);
				audio.poll(Impulse.ar(10), "audio");
				RecordBuf.ar(audio, bufnum, 0, 1, 0, 1); //TODO: use diskout later
				
			}).store;
		};
	}
	
			
}



CMReadParamFiles : CMArchiveNoSpatial {
	var <>paramList; //list of CMWriteParamFile
	var <paths, <buffers; //assumes that outputlist and paths are in same order
	var <>ogNode;
	
	*writeClass { ^CMWriteParamFiles }
	
	
	inputs {^nil}
	
	listenerBundle { ^nil } //not listening
	
	
	outputs {
		^this.outsFromParamList
	}
	
	outsFromParamList{
		^paramList.collect{ |writeParam|
			writeParam.busSpec
		};
	}
	
	sourceBundle {
		//build source synth from synthdef and args
		var sources, args, msg, b;
		
		isPlaying = true;
		
		b = [];
		
		paths = paramList.collect{ |w| w.path };
		paths.debug("paths");
		buffers = paths.collect{ |path|
			Buffer.new(mesh.server);
		};	
		sources = this.outputs.collect{ |outSpec, i|
			//we need one playback synth for each output
			var synth, args, defNameForChans;
			
			defNameForChans = this.defNameForChans(outSpec[1], outSpec[2]);
			
			synth = Synth.basicNew(defNameForChans, mesh.server);
			synths.put(outSpec[0], synth);
			args = [ 
				\bufnum, buffers[i].bufnum,
				\out, busses[ outSpec[0] ].index;
			];
			
			
			
			synth.newMsg(groups['source'], args);	
		};
				
		buffers.do{ |buffer, i|
			b = b.add(buffer.allocReadMsg(paths[i], completionMessage: sources[i]));		};

		b.debug("bundle from" + this.class);
		^b
	}
	
	freeFunc {
		^buffers.collect{ |buffer| buffer.freeMsg };
	}
	
	defNameForChans{ |rateSymbol, chans|
		var name;
		name = switch(rateSymbol,
			\audio, {this.class.baseDefName ++ "ar"},
			\control, {this.class.baseDefName ++ "kr"}
		).value;
		name = name ++ chans.asString;
		^name
	}
	
	*baseDefName{
		^"cnPlayBuf_"	
	}
	
	*storeSynthDefs {
	
		(1..10).do{ |nc|
			SynthDef(this.baseDefName ++ "ar" ++ nc, {|out, bufnum|
				var audio;
				audio = PlayBuf.ar(nc, bufnum);
			//	Amplitude.kr(audio).poll(Impulse.kr(1));
				Out.ar(out, audio);
			}).store;

			SynthDef(this.baseDefName ++ "kr" ++ nc, {|out, bufnum|
				var signal;
				signal = PlayBuf.kr(nc, bufnum);
				Out.kr(out, signal);
			}).store;

		};
	}
}
























