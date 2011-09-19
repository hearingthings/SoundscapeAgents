AnalysisRenderController {
	var <chains, <filePath, <inBlock, <sourceDuration, <sourceNChans, <>server, <score;
	
	var <logPaths, <renderPath;
	var logPathAllocator, renderPathAllocator;
	
	*newWithFileAndChains { |filePath, specsArray, server|
		^super.new.initWithChains(filePath, specsArray, server);
	}
	
	initWithChains { |filePathArg, specsArrayArg, serverArg|
		
		this.initVars;
		
		if (filePathArg.isNil) { "AnalysisRenderController needs file path".throw };
		if (specsArrayArg.isNil) { "AnalysisRenderController needs a specsArray".throw };
		
		filePath = filePathArg;
		
		server = serverArg ? Server("controllerServer", NetAddr("localhost", 57121));

		chains = specsArrayArg.collect{ |chainSpec, i|
			var chain = AnalysisChain(this, chainSpec);
			chain;
		};
		
		/*
		makeEmptyChains
		readFile
		initScoreforDurationWithServer  		//returns new Score
			chains.do(_.enumerateResources)
			synthBundles
				sourceSynthBundle
				chainSynthBundles
			bufferAllocBundles(synthBundles)
			(updateMsgs)
			writeMsgs
			freeMsgs
			endScoreMsg
		renderNRTWithScore(score)
		loadDataFromFiles					//returns data models, or just 1 env per chain?
		viewController.updateData			//array of Envs
		*/
		
	}
	
	initVars {
//		logPathAllocator = APathAllocator.newClearDir("tmplog/", "log" ++ this.hash, "sclog");
//		renderPathAllocator = APathAllocator.newClearDir("tmprend/", "render" ++ this.hash, "aiff");
		logPathAllocator = APathAllocator.newClearDir("tmplog/", "log", "sclog");
		renderPathAllocator = APathAllocator.newClearDir("tmprend/", "render", "aiff");

	}
	
	makeScore {
		var score = [], cBundle;
		this.readFileWithPath(filePath);
		cBundle = this.initScoreBundleAtTime(server, 0);
		score = score.add(cBundle);
		

		cBundle = this.logMsgBundleAtTime(this.sourceDuration);
		score = score.add(cBundle);

		cBundle = this.closeMsgBundleAtTime(this.sourceDuration);
		score = score.add(cBundle);

		cBundle = this.freeMsgBundleAtTime(this.sourceDuration);
		score = score.add(cBundle);
		
		cBundle = this.finishRenderBundleAtTime(this.sourceDuration);
		score = score.add(cBundle);
		^score;
		
	}	
	
	readFileWithPath { |filePath|
		//reads the file
		var file = SoundFile.openRead(filePath);
		sourceNChans = file.numChannels;
		sourceDuration = (file.numFrames / file.numChannels) / file.sampleRate;
		file.close;
		
		inBlock = AInputPlayBuf.newForServerWithChansAndPath(server, file.numChannels, filePath);
		inBlock.target = 0;

	}
	
	enumeratePaths { 
		this.enumerateLogPaths;
		this.enumerateRenderPaths;
	}
	
	initScoreBundleAtTime { |server, time=0|
		var oc; //oncomplete
		this.enumerateResources(server);
		oc = this.synthsBundleAtTime(time);
		oc = this.allocBuffersMsgBundle(time, oc);
		^oc
	}
	
	enumerateResources { |s|
		s = s ? server;
		this.chains.do{ |c| c.enumerateResources(s) };
		this.enumerateLogPaths;
		this.enumerateRenderPaths;
	}
	enumerateLogPaths {
		logPaths = chains.collect{ |chain|
			var path = logPathAllocator.alloc;
			chain.enumerateLogPaths(path);
		};
	}
	enumerateRenderPaths {
		renderPath =  renderPathAllocator.alloc;
	}
	
	enumerateFirstTargets { 
		chains.do{ |chain| chain.chainArray[0].target = 1 };
	}

	
	synthsBundleAtTime { |time|
		var msgArray = [];
		
		msgArray = msgArray.add(inBlock.newSynthMsgListForServer(server));
		chains.do{ |chain|
			msgArray = msgArray ++ chain.newSynthMsgsArrayForServer(server);
		}		
		
		^([time] ++ msgArray)		
	}
	
	allocBuffersMsgBundle { |time, completion|
		completion = inBlock.allocBuffersMsgBundle(time, completion);
		chains.do{ |chain| 
			completion = chain.allocBuffersMsgBundle(time, completion); 
//			completion = [time, completion];
		};
		^completion
	}
	
	logMsgBundleAtTime { |time|
		var msg = chains.collect{ |block| block.writeLogMsg };
		msg = msg.flatten(1);
		^([time] ++ msg)	
	}
	
	closeMsgBundleAtTime { |time|
		var msg = chains.collect{ |chain| chain.writeCloseMsg };
		msg = msg.flatten(1);
		^([time] ++ msg)	
	}
	
	freeMsgBundleAtTime { |time|
		var msg = chains.collect{ |block| block.freeMsgList };
		msg = msg.flatten(1);
		^([time] ++ msg)
	
	}
	
	finishRenderBundleAtTime { |time|
		^[time, [\c_set, 0, 0]];
	}

	duration { ^this.sourceDuration }
	
	
	//render support
	render {
		var o = ServerOptions.new.numOutputBusChannels = 2; // stereo output
		var rendOutput = Score.recordNRTBlock(score, "lastscore.osc", renderPath, options: o); // synthesize
		this.renderComplete(rendOutput);
	}
	
	renderComplete{ |renderOutput|
		var dataEnvs;
		chains.do{ |chain| 
			chain.loadLogDataFromPaths;
		};
		
		dataEnvs = chains.collect{ |chain, indexChain|
			chain.logDataAsEnv;
		};
		
		//will hand this off to the viewController, but for now, a stupid display
		this.displayEnvs(dataEnvs)
	}
	
	displayEnvs{ |envs|
		var u, w = Window("test", Rect(20, 20, 800, 800));
		
		var heightEach = 800 / envs.size;
		
		u = 4.collect{ |i|

			CompositeView(Rect(0, heightEach * i, w.view.bounds.width, heightEach));
		};
		u.do{ |v| Env.perc.plot(bounds: v.bounds, parent:v) };	
		w.front;
	
	}

}



APathAllocator  {
	classvar <>defaultBaseDir;
	var <>baseDir, <>prefix, <>extension;
	var <>allocator;
	
	*initClass { defaultBaseDir = "tmplog/" }
	
	*new{ |baseDir, prefix, extension| ^super.new.initWithBaseDirAndPrefix(baseDir, prefix, extension) }
	
	*newClearDir{ |baseDir, prefix, extension| ^super.new.initWithBaseDirAndPrefix(baseDir, prefix, extension).clearDir }
	
	initWithBaseDirAndPrefix { |baseDirArg, prefixArg, extensionArg|
		if (baseDirArg.last != $/) {baseDirArg = baseDirArg ++ "/"};
		baseDir = baseDirArg;
		prefix = prefixArg;
		extension = extensionArg;
		this.makeNewDirIfNeeded;
		
		allocator = RingNumberAllocator( this.getLastNumInDirForPrefix(baseDir, prefix), 100); //keep 100 files
	}
	
	alloc {
		var path = baseDir ++ prefix ++ "_" ++ allocator.alloc ++ "." ++ extension;
		^path;
	}
	
	
	makeNewDirIfNeeded {
		if (this.dirExists(baseDir).not) {
			this.makeDir(baseDir);
		};
	}
	
	getLastNumInDirForPrefix { |dir, prefix|
		var string = ("ls" + dir.asString).unixCmdGetStdOut;
		var ints = string.split(10.asAscii).collect{ |t|
			var split = t.split($.)[0];
			if (split.contains(prefix)) {
				split.split($_).last.asInteger
			} {
				0
			};
		};
		ints = ints.maxItem
		^(ints ? 0)	
	}
	
	makeDir { |dir|
		var cmd = "mkdir" + dir.asString;
		var result = cmd.unixCmd;
	}
	
	dirExists{ |dir| var res = ("ls" + dir.asString).unixCmdGetStdOut; ^(res.size > 0) }
	
	clearDir { |dir| ("rm" + baseDir ++ "*").unixCmd } 	
}







