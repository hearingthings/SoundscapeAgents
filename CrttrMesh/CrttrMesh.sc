//TODO:
//look into output patching for multi-speakers
//
//methods to add:
//hasOutput <- boolean, does the mesh have an output node
//

CrttrMesh {
	var <bounds;
	var <server;
	var <nodes;
	var <groups, <>groupArray;
	var <maxDelay;
	var <speedOfSound;
//	var pendingBundles;
	var <outputs; //[[node, hardwareChan, numChans, repatchSynth]]  
	var <others;

	*new { |bounds, server|
		^super.newCopyArgs(bounds, server).init
	}
	
	*initClass {
		StartUp.add({this.storeSynthDefs};); 
		this.allSubclasses.do{ |sclass|
			try{ StartUp.add({sclass.perform(\storeSynthDefs)};) };
		};
	}
	
	init {
		//check errors
		
		//initialize variables
		bounds = bounds.asRect;
		speedOfSound = 344;
		this.setMaxDelay;
		
		this.initGroups;
	}
	
	setMaxDelay {
		maxDelay = (bounds.width.squared + bounds.height.squared).sqrt / speedOfSound; 
		maxDelay = maxDelay + 1;
	}
	speedOfSound_{ |newSos|
		speedOfSound = newSos;
		this.setMaxDelay;
		//TODO: propogate this to all running synths
	}
	
	//initialize
	initGroups {
		var g;
		groupArray = [
			[\head, server.defaultGroup, \addToHead],
			[\node, \head, \addToHead],
			[\patch, \node, \addAfter]
		];
		
		g = this.addGroups;
		if (g.class == Array) {
			groupArray = groupArray ++ g;
		};
		groups = IdentityDictionary.new;
		groupArray = groupArray.collect{ |ga|
			var newGroup = Group.basicNew(server); //allocate a new group number
			groups.put(ga[0], newGroup); //store in the identity dictionary
			ga = ga.add(newGroup); //add to the groupArray for use in allocation
			ga
		};
	}
	addGroups {^nil}
	
	setNodeAsOutput{ |node, hardwareChan| //listen to node on hardware out - mix with others?
		
		server.listSendBundle(0, this.setNodeAsOutputBundle(node, hardwareChan));
	}
	
	setNodeAsOutputBundle { |node, hardwareChan|
		var repatchSynth, outputArray, numChans;
		
		if (hardwareChan.isNil) { (this.class + thisMethod.name + "hardwareChan cannot be nil").throw; };
		
		numChans = node.numOutputChannels;
		repatchSynth = Synth.basicNew("arRepatch_" ++ numChans, server);

		outputArray = 		//[node, hardwareOut, numChans, synth]
			[node, hardwareChan, numChans, repatchSynth];
			
		outputs = outputs.add(outputArray);
		
		^[repatchSynth.newMsg(groups[\patch], [\in, node.busses[\out], \out, hardwareChan])]	}


	removeNodeAsOutput { |node|
		server.listSendBundle(0, this.removeNodeAsOutputBundle(node));
	}
	
	removeNodeAsOutputBundle { |node|
		//search through output array for node at element 0
		//make bundle
		var thisOut;
		thisOut = outputs.select{ |ar| ar[0] == node };
		
		if (thisOut.size == 0) {
			(this.class + thisMethod.name + "no node found to remove").warn;
		} {
			thisOut = thisOut[0];
			^[thisOut[3].freeMsg]
		};		
	}

	fromNodePerspectiveBundle { |node, hardwareChan| 
		//TODO: should add appropriate listener node for our hardware setup, have that node read from the other node's spatial bus, make that new node a "dependent" of the node we want to listen to so that it is removed when the other node is removed, and it does not listen to the node we are a dependent of
		var repatchSynth, outputArray, numChans;
		
		if (hardwareChan.isNil) { (this.class + thisMethod.name + "hardwareChan cannot be nil").throw; };
		
		numChans = node.numOutputChannels;
		repatchSynth = Synth.basicNew("arRepatch_" ++ numChans, server);

		outputArray = 		//[node, hardwareOut, numChans, synth]
			[node, hardwareChan, numChans, repatchSynth];
			
		outputs = outputs.add(outputArray);
		
		^[repatchSynth.newMsg(groups[\patch], [\in, node.busses[\in], \out, hardwareChan])]	}	

	
	//adding, removing nodes
	asTargetForNode {
		^groups[\node].asTarget
	}

	addNode { |node|
		nodes = nodes.add(node);
	}
	
	removeNode { |node|
		nodes.remove(node); //TODO: removeNode needs to do more?
	}

	addOther { |other|
		others = others.add(other); //others are things that respond to makebundle, runbundle, etc
		others.debug("others");
	}

	//realtime?
	play { 
		//TODO: switch based on whether or not we are playing
		//if(playing.not) {
		server.listSendBundle(server.latency, this.makeAllBundle)
		// } {
		// unpause server nodes
		// }
	}
	pause { //send message to head group to pause 
		}
	stop { //send message to head group to st
		}
	free {}

	makeBundle {
		var b = [];
		b = b ++ this.allocBundle;
		^b
	}

	makeAllBundle {
		var b = [];
		b = b ++ this.makeBundle;
		nodes.do{ |node| b = b ++ node.makeBundle };

			//TODO: the two lines below are a hack - need unified interface for soundscapeAgents
		others.do{ |other| b = b ++ other.allocBundle }; 
		others.do{ |other| b = b ++ other.runBundle };

		^b
	}

	//nrt, bundle
	allocBundle { //allocs groups and other resources
		var b = [];
		b = b ++ this.allocGroupsBundle; //secondary groups
		b = b ++ this.allocFunc;			//allocate any other server resources
		^b
	}

	allocGroupsBundle {
		if (groupArray.isNil) {"initGroups must be called before allocGroupsBundle".throw};
		^groupArray.collect{ |ga|
			var g = ga.last;
			var target = ga[1];
			target.debug("target");
			if(target.class == Symbol) {target = groups[target]};
			g.newMsg(target, ga[2]);
		};
	}
	
	allocFunc { ^nil }
	
	listenToNode {
	
	}
	
	hasOutput {
		if (outputs.notNil) {
			
		};	
	}
	
	
	//NRT STUFF, can also be used for rt
	
	asScore { |duration|
		var score = [], n;
		
		n = this.makeAllBundle;
		if (n.size > 0) {
			n = n.insert(0, 0);
			score = score.add(n);
		};
		
		n = this.finishBundle;
		if (n.size > 0) {
			n = n.insert(0, duration);
			score = score.add(n);
		};
		
//		n = this.freeBundle;
//		if (n.size > 0) {
//			n = n.insert(0, duration);
//			score = score.add(n);
//		};


		//finish it all off		
		score = score.add([duration, [\c_set, 0, 0]]); //finish message		
		score = score.scoreCheck;
		
		^score;
	}

	finishBundle {
		var n = [];
		
		nodes.do{ |node| 
			n = n ++ node.finishBundle;
		};
		others.do{ |other| n = n ++ other.finishBundle };
		
		^n		
		
	}
	
	freeBundle {
		var n = [];
		
		nodes.do{ |node| 
			n = n ++ node.freeBundle;
		};
		
		^n				
	}
	
	render { |path, duration, soundFileParams|
		//TODO: question of output paths
		//s = this.asScore;	
		//s.render
	}
	
	
	//these all update pendingBundles
//	addSource { |node|
//		var listeningNodes, b = [];
//		listeningNodes = nodes.select{ |n| n.isListening };
//		listeningNodes.do{ |n|
//			b = b ++ n.listenToNodeBundle(node);
//		};
//		pendingBundles
//	}
//	
//	removeSource { |node|
//		var listeningNodes, b = [];
//		listeningNodes = nodes.select {|n| n.isListening };
//		listeningNodes.do{ |n|
//			//todo - remove nodes
//		}
//	}
//	
//	addBundleToPending { 
//	
//	}
//	
//	//flushes pendingBundles and returns result
//	updateBundles {
//	
//	}
	
	*storeSynthDefs {
		(1..16).do{ |i|
			SynthDef("arRepatch_" ++ i, { |in, out|
				var audio = In.ar(in, i);
				Out.ar(out, audio);
			}).store;
		};
	}

}


+ Array {
	
	scoreCheck {
		var badSize = false, hasNil = false;
		this.do{ |scoreArr|
			//size check
			if (scoreArr.size < 2) {
				badSize = true;
			};

			//make sure there are no nils
			scoreArr.do{ |el| if (el.isNil) {hasNil = true} };
			
			scoreArr[1..].do{ |el|
				if (el.class != Array) {
					"possible non-array".warn;
				};
			};
		};
		if (badSize) {"Score has time with no bundle".throw};
		if (hasNil) {"Score has a nil as a time or bundle".throw};

		//do a size check	
		
		^this.scoreSizeCheck

	}
	
	scoreSizeCheck { |limit = 8192|
		var new, needNew=false;
		
		this.do{ |scoreArr|
			var bundle = scoreArr[1..];
			if (bundle.bundleSizeSafe > limit) {
				needNew = true;	
			};
		};
		
		if (needNew) { //if we're over the size limit for one or more score entries
			new = []; //allocate new array for new score
			this.do{ |scoreArr|
				var time, bundle;
				bundle = scoreArr[1..];
				time = scoreArr[0];
				bundle.bundleSafeWithTime(time).do{ |scoreEntry|
					new = new.add(scoreEntry);	
				};
			};
			^new
		};
		//else we return self, as we did not need a new array
		
	}
	
	bundleSafeWithTime { |time| //for osc bundles
		
		if (this.bundleSizeSafe > 8192) {

			^this.clumpBundles.collect{ |b| //clump the osc messages (not the time, so item[1..])
				b.insert(0, time);
			};
		} {
			^[this.insert(0, time)];	
		}		
	}
		
	bundleSizeSafe {
		var r = 0;
		
		this.do{ |b, i|
			r = r + b.msgSize;

		};
		
		^r		
		
	}
	
}



+ Score {
	clumpBundles { |size=8192| //udp size limit
		var arr = [], changed=false;
		
		score.do{ |item, i|
			if (item.bundleSize > 8192) {
				(i + "had to change bundle size").postln;
				changed = true;
				item[1..].clumpBundles.do{ |bundle| //clump the osc messages (not the time, so item[1..])
					bundle.postln;
					arr = arr.add(bundle.insert(0, item[0]));
				};
				score.remove(item); //remove old item from the list
			};
		};
		if (changed) {arr.do{ |sc| this.add(sc)}; this.sort};
	}
}