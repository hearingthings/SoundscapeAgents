AnalysisChain { 
	//models a chain of analysis blocks - handles patching together, 
	//	encapsulates server messages
	//	
	var <>server, <>duration;
	
	var <>input, <>output; //[name, rate, numchannels, index]
	var soundInBus, filterOutBus, analysisOutBus;
	var logger, <>chainArray, <>logPaths;
	var <>group, <>target;
	var <>busses;
	var <>pathAllocator;
	var enumerated;
	var <>analysisData;
	var outputSpec;

	classvar <>logTypeToClassDict;

	*initClass {
		logTypeToClassDict = IdentityDictionary[
			\continuous 	-> AContinuousLogger,
			\discrete 	-> AContinuousLogger //TODO: in full implementation should be ADiscreteLogger
		];
	}
	*logTypeToClass { |type|
		^logTypeToClassDict[type];
	}

//CREATION	
	
	*new { |server, duration| 
		^super.newCopyArgs(server, duration)
	}

	*newChain{ |server, chain, duration|
		^super.new.server_(server).chainArray_(chain).duration_(duration).init;
	}
	
	*newWithSpecArray { |server, duration, specArray|
		 ^super.newCopyArgs(server, duration).initWithSpecArray(specArray);
	}
	
	
//INITIALIZATION
	init {
		enumerated = nil;
		busses = IdentityDictionary.new;
		if (pathAllocator.isNil) {
			pathAllocator = PathAllocator("tmp", "analysis", "wav");
		};
		if (try{chainArray.last.isLogBlock} == true) {
			"last was logger, no logger needed".postln;
		} {
			this.addLogToChain;
		};
	}
	
	initWithSpecArray{ |specArray|	
		this.chainFromSpecArray(specArray);
		this.init
	}
	
	//spec array for now is [className, synthDefName, ...args];
	chainFromSpecArray { |specArray|
		var logType, logClass;
		
		specArray = specArray.collect{ |spec, i|
			var newInstance;
			if (spec.notNil) {
				newInstance = spec[0].asSymbol.asClass.new(spec[1], spec[2..], this);
			} {
				nil
			};
		};
		
		chainArray = specArray.select{ |block| block.notNil };

	}
	
	addLogToChain {		
		logger = this.newLoggerForType( chainArray.last.logType );
		
		chainArray = chainArray.add(logger);		
	}

	newLoggerForType { |logType|
		var logClass = this.class.logTypeToClass(logType);
		logger = logClass.newDefault;
		^logger
	}

//POST-INITIALIZATION : ENUMERATION OF RESOURCES
	enumerateResources {
		this.enumerateSelf(server);
		this.enumerateNodeIDs(server);
		this.enumerateTargets(server);
		this.enumerateBusses(server);
		this.enumerateBuffers(server);
		this.enumeratePaths(server);
		enumerated = true;
	}

//server messages	
	allocBundle {
		var b = [];

		//allocSelfBundle //call alloc on self - allocate group
		b = b ++ this.allocSelfBundle(server);

		//allocChainBundle //call alloc on each chain element
		b = b ++ this.allocChainBundle(server);
		
		^b
	}
	
	runBundle {
		//synth messages for each block
		^this.runChainBundle;
	}
	
	finishBundle {
		//write messages for each block
		//finish messages for each block
		^this.finishChainBundle;
	}
	
	freeBundle {
		^this.freeChainBundle;
	}
	
	allocSelfBundle { |server|
		
		^[ ['/g_new', group, 1, target] ]
	}
	
	allocChainBundle {
		var b = [];
		chainArray.do{ |block|
			b = b ++ block.allocBundle;
		};
		^b
	}
	runChainBundle {
		var b = [];
		chainArray.do{ |block|
			b = b ++ block.runBundle;
		};
		^b	
	}
	finishChainBundle {
		var b = [];
		chainArray.do{ |block|
			b = b ++ block.finishBundle;
		};
		^b	
	}
	freeChainBundle {
		var b = [];
		chainArray.do{ |block|
			b = b ++ block.freeBundle;
		};
		^b	
	}

	asScore {
		var score, b;
		score = [];

		if (enumerated != true) {
			this.enumerateResources;
		};
		
		//happen at time = 0
		[\allocBundle, \runBundle].do{ |msg|
			b = this.perform(msg);
			if (b.size > 0) {
				b = b.insert(0, 0);
				score = score.add(b);	
			};
		};
		
		//happen at time = duration
		[\finishBundle, \freeBundle].do{ |msg|
			b = this.perform(msg);
			if (b.size > 0) {
				b = b.insert(0, duration);
				score = score.add(b);	
			};
		};
		score = score.add(
			[duration, ['/c_set', 0, 0]]
		);
		score = score.scoreCheck;
		^score;
	}
	
	render {
		var score = this.asScore;
		
		Score.recordNRTBlock(score, "testscore.osc", "testscore.aiff", options: server.options);
		"post render".postln;
		this.renderComplete;
		^this.makeAnalysisData;
		//do actions post-render like open and convert data, adjust sample rates, etc.
	}


	enumerateSelf { |server|
		if (group.isNil) {
			group = server.nextNodeID;
		};
		if (target.isNil) {
			target = 0;
		};		
	}

	enumerateBusses { |server|
		
		//CONNECT BUSSES
		//if first node has inputs
		if (chainArray[0].input.notNil and: this.input.notNil) {
			//connect chain input to node input
			chainArray[0].setInputIndex(this.inIndex);
		};
		
		chainArray[1..].do{ |block, prevBlockIndex|
			this.connectBlocks(chainArray[prevBlockIndex], block, server);
		};
		
		if (chainArray.last.output.notNil and: this.output.notNil) {
			chainArray.last.setOutputIndex(this.outIndex);		};
	}
	
	connectBlocks { |outBlock, inBlock, server|
			
			//connect nodes
			var rate;

			if (inBlock.inRate != outBlock.outRate) {
				("Block input and output rates do not match for"
				+ inBlock + "and" + outBlock).throw;
			};
			
			rate = inBlock.inRate;
			
			if (busses[rate].isNil)	 {
				//allocate new bus
				busses.put(
					rate,
					Bus.perform(rate, server, 1) //channels hardcoded to 1
				 ); //TODO: enforcing mono - need scheme for multi
			};
			
			//error checking with existing outputs
			if (outBlock.output.size > 3) {
				//make sure that busses match
				if (outBlock.output[3] != busses[outBlock.inRate].index) {
					"bus alignment error".throw;
				};
			} {
				outBlock.setOutputIndex(busses[rate].index);
			};
			
			if (inBlock.input.size > 3) {
				if (inBlock.input[3] != busses[inBlock.inRate].index) {
					"bus alighment error input".throw;
				};
			} {
				inBlock.setInputIndex(busses[rate].index);			};
			outBlock.outputBlock = inBlock;
			inBlock.inputBlock = outBlock;

	}
	
	enumerateBuffers { |server|
		chainArray.do{ |block|
			block.makeBufferInfo(server, duration);
		};
	}
	
	enumerateNodeIDs { |server|
		chainArray.do{ |block, blocki|
			block.nodeID = server.nextNodeID;
		};
	}
	
	enumerateTargets {
		chainArray[0].target = group ? 1;
		chainArray[0].addAction = 0;
		chainArray[1..].do{ |block, blocki|
			block.target = chainArray[ blocki ].nodeID;
			block.addAction = 3;
		};
	}
	
	

//ALLOCATION OF RESOURCES THAT HAVE BEEN ENUMERATED


	
	newSynthMsgBundle { |server, time|
		var bundle;
		bundle = chainArray.collect{ |block| block.newSynthMsgListForServer(server)  };
		bundle = [0, bundle];
		^bundle
	}
	
	newSynthMsgsArrayForServer { |server|
		var array;
		array = chainArray.collect{ |block| block.newSynthMsgListForServer(server)  };
		^array
	}	
	
	
	allocBuffersMsgBundle { |time, completion|
		chainArray.do{ |block|
			completion = block.allocBuffersMsgBundle(time, completion);
			//completion = [time, completion];
		};
		^completion
	}
	
	
	enumeratePaths { 
		chainArray.do{ |block|
			if (block.needsPaths) {
				block.setPaths(
					block.numPaths.collect{ pathAllocator.alloc }
				);	
			};	
		}
		^logPaths
	}
	
//	writeLogDataMsgBundle{ |time, pathArg|
//		var msg;
//		if (logPaths.isNil) { "write paths are nil, errors imminent".warn; }; 
//		msg = logger.writeMsg(pathArg);
//		msg.insert(0, time);
//		^msg
//	}
//	
//	writeLogMsg { |pathArg|
//		^logger.writeMsg(pathArg);
//	}
//	
//	writeCloseMsg {
//		^logger.writeCloseMsg
//	}
	
	freeMsgBundle { |time|
	
	}
	
	freeMsgList {
		var msg = chainArray.collect{ |block|
			block.freeMsgList;
		};
		^msg.flatten(1);
		
	}
	
	renderComplete{
		chainArray.do{ |block| //send renderComplete to chain
			try{ block.renderComplete };	
		};
	}
	
	outputSpec {
		var spec;
		chainArray.collect{ |block|
			var bs = block.desc.metadata[\outputSpec];
			if (bs.notNil) {
				^bs
			};
		}	
	}
	
	makeAnalysisData {
		analysisData = chainArray.select{ |block| block.isLogBlock };
		analysisData = analysisData.collect{ |log|
			//|type, name, params, path, duration, chain|
			AnalysisData.newForType(
				log.logType, 
				log.inputBlock.synthDefName,
				log.inputBlock.params,
				log.paths[0],
				duration,
				this
			);
		};
		^analysisData[0] //TODO: this is a bit of a hack...
	}
	
	//extracting data
	loadLogDataFromPaths {
		var dataEnv = logger.loadLogDataReturnEnv;
		^dataEnv;
	}
	
//	logDataAsEnv {
//		^dataEnv	
//	}
	
	

//non-essential for working, but good for interface in general
	asSpecArrays {
	
	}
	
	renderWithInputFile {}


	inName{ ^input[0] }
	inRate{ ^input[1] }
	inChannels { ^input[2] }
	inIndex {^input[3] }

	outName{ ^output[0] }
	outRate{ ^output[1] }
	outChannels { ^output[2] }
	outIndex { ^output[3] }

}


//AnalysisChain(nil, [ nil, [AFFTAnalysis, "ffttest", \fftSize, 1024, \rate, 100], nil ])