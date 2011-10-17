CrttrMeshMap {
	//maps multiple parameters	
	
}

CrttrMeshMapParam {
	//maps a single quality of the mesh
	var <x, <y; //number of points in 2D
	var <>mesh;
	var <>path;
	var <>paramName; //name of parameter mapped
	var <>analysisSynth;
	var <>time;
	var <>dataFiles; 
	var <>points; //points that have been mapped
	var <>pointsToDataIndex; // [ [ point, AnalysisData ], [point, AD] ... ]
	var <>interpType; //lin or exp - for interpolating between points graphically
	
	var pathAllocator;
	var <renderComplete;
	
	//should add some statistical stuff such as mean, stdev, pcile?.
	
	*newGrid { |x, y, mesh, path, paramName, analysisSynth, time|
		^super.newCopyArgs(x, y, mesh, path, paramName, analysisSynth, time).init
	}
	
	init {
		mesh = mesh.copy;
		//setup path allocator
		pathAllocator = PathAllocator.new(path, paramName, "wav");	
	}	
	
	makeListenerAndChain { |xPos, yPos, asynth|
		//copy proto chain array
		var newChain, input;
		var synth = asynth.copy;
		var chainBus;
		
		var decoder;

		//make a decoder node at x and y
			//add to mesh		
		decoder = CNAmbiListenMono(mesh, [xPos, yPos, 0]);
		
		chainBus = decoder.busses['out'].index;
		
		input = AInputBusMono.new(mesh.server, chainBus);

		//add input bus synth to proto chain that reads decoder bus
			//target group should be the 'patch' group
		newChain = [input, synth];
		newChain = AnalysisChain.newChain(mesh.server, newChain, time);
		newChain.pathAllocator_(pathAllocator);
		newChain.target = mesh.groups['patch'];
		newChain.enumerateResources;
		
		mesh.addOther(newChain); //add other to mesh so that it will run in parallel
		pointsToDataIndex = pointsToDataIndex.add( [xPos@yPos, newChain] );
	}
	
	render {
		var score, archiveNodes;
		renderComplete = false;
		score = mesh.asScore(time);
		
		//just record the score
		Score.recordNRTBlock(score, pathAllocator.baseDir ++ "tmp.osc", pathAllocator.baseDir ++ "tmp.aiff", options: this.optionsForCurrentRender);
				
		pointsToDataIndex = pointsToDataIndex.collect{ |ptd|
			ptd.put(1, ptd[1].makeAnalysisData);	
		};
				
		renderComplete = true;

		"Render Complete".postln;		
	}
	
	
	
	matrixAtTime { 
		//	atTimeNoEnv
	}
	
	matrixForFrame { |frame|
		
		//returns matrix of listeners for a particular frame	
	}
	
	valueAtPoint { |xy|
		//interpolate the value at a particular point
	}
		
		
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