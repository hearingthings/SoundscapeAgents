CrttrMeshNode {
	var <>mesh;
	var <pos; //(Array[x, y, z])
	var <>sourceDef; //synthdef name for source
	var <>sourceArgs;
	var <isPlaying, <isListening;
	var <numInputChannels, <numOutputChannels; //mono by default
	var <busses; //(identity dictionary of busses)
	var <group; //(head group)
	var <groups, <>groupArray; //(identity dictionary of groups ... name -> group)
	var <>synths; //(identity dictionary of all running synths, each a flat array)
	
	var allocFunc; //(array of funcs that each return an osc message a an array)

	var <>defs; //synthdefs to use

	//CREATION	
	*new{ |mesh, pos, sourceDef, sourceArgs|
		^super.newCopyArgs(mesh, pos, sourceDef, sourceArgs).init	
	}
	
	*basicNew {
		^super.new
	}
		
	*initClass {
		StartUp.add({this.storeSynthDefs};); 
		this.allSubclasses.do{ |sclass|
			try{ StartUp.add({sclass.perform(\storeSynthDefs)};) };
		};
	}
	
	copyToMesh { |newMesh|
		var copy = this.copy;
		copy.mesh = newMesh;
		^copy.init;	
	}

	
	
	//initialize resources
	init {
		//error check args
	//	if (pos.isNil) { (this.class.name.asString + ": pos argument must be specified").throw };
		
		//init vars
		//initialize number of channels
		isListening = isPlaying = false;
		numInputChannels = numOutputChannels = 1; //TODO: don't hardcode
		synths = IdentityDictionary.new;

		this.initVars;
		
		//initialize busses
		this.initBusses;
		this.initGroups;
		this.initDefs;
				
		//add me to the mesh
		mesh.addNode(this);
	}

	
	defaultInputs { ^[ [\in, \audio, 1] ] }
	
	defaultOutputs {
		//[\name, rate, numchannels
		^[ [\out, \audio, 1], [\spatial, \control, 3] ]
	}
	
	defaultInternalBusses { ^nil }
	
	inputs { ^this.defaultInputs } //subclass can use this to make more busses: [name, rate, numChans]

	outputs { ^this.defaultOutputs }
	
	internalBusses { ^this.defaultInternalBusses }
	
	initVars { } //any additional variables - used by subclasses
	
	initBusses {
		//builds dictionary of busses
		//calls this.busses to get more bus info from subclasses
		var busArray, b;
		
		busArray = [];
		[this.inputs, this.outputs, this.internalBusses].do{ |ba|
			if (ba.notNil) {
				busArray = busArray ++ ba;	
			}
		};

		busses = IdentityDictionary.new;
		busArray.do{ |busSpec|
			var bus = Bus.alloc(busSpec[1], mesh.server, busSpec[2]);
			busses.put(busSpec[0], bus);
		};
	}
	
	initGroups { //builds dictionary of groups, allocates nodes
		var g;
		groupArray = [ //[name, target, addAction]
			[\head, mesh.asTargetForNode, \addToHead],
			[\spatial, \head, \addToHead],
			[\listener, \spatial, \addAfter],
			[\source, \listener, \addAfter]
		];
		g = this.addGroups;
		if (g.class == Array) {
			groupArray = groupArray ++ g;
		};
		groups = IdentityDictionary.new;
		groupArray = groupArray.collect{ |ga|
			var newGroup = Group.basicNew(mesh.server); //allocate a new group number
			groups.put(ga[0], newGroup); //store in the identity dictionary
			ga = ga.add(newGroup); //add to the groupArray for use in allocation
			ga
		};

	}
	addGroups { ^nil }
	
	initDefs { //not implemented yet
		var defArray, s;
	}
	addDefs { ^nil }
	
	//real-time interface
	play { 
		if (isPlaying or: isListening) {
			this.group.run(true);
		} {
			mesh.server.listSendBundle(0, this.makeBundle);
		};
	}
	
	pause { this.group.run(false); }
	stop { this.free; }
	free { 
		//mesh.server.listSendBundle(0, this.freeBundle); //TODO: freeBundle
		this.groups['head'].free; 
		//stopListeningToNodeBundle for all that are listening
		this.freeFunc;
	}

	//message interface - used publicly for NRT, privately for realtime
	makeBundle {
		var b = [];
		b = b ++ this.allocBundle;	
		b = b ++ this.spatialBundle;
		b = b ++ this.listenerBundle;
		b = b ++ this.sourceBundle;
		^b
	}
	
	
	allocBundle { //inits groups and other resources
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
			if(target.class == Symbol) {target = groups[target]};
			g.newMsg(target, ga[2]);
		};
	}
	
	allocFunc { ^nil}
	
	spatialBundle { //initializes the spatial bus synth
		//this is standard behavior, so it is fine to put in the superclass
		var msg, synth;
		var spatialArgs = [
			\controlBus, busses['spatial'].index,
			\x, pos[0],
			\y, pos[1],
			\z, pos[2]
		];
		
		synth = Synth.basicNew("crttrSpatialBus", mesh.server);
		synths.put('spatial', synth); //store synth
		
		msg = synth.newMsg(groups['spatial'], spatialArgs);
		^[msg]			
	}
	
	listenerBundle { //builds array of listeners for each node already present
		var b = [];
		isListening = true;
		b = b ++ this.listenToNodeArrayBundle(mesh.nodes);
		b = b ++ this.additionalListenerBundle;
		^b
	}
	additionalListenerBundle { ^nil } //for subclasses
	
	listenToNodeArrayBundle { |nodeArray|
		var otherNodes, b = [];
		otherNodes = nodeArray.select{ |node| node.hash != this.hash };
		otherNodes = otherNodes.select{ |node| node.isPlaying }; //only listen to nodes that are playing
		otherNodes.debug("otherNodes");
		otherNodes.do{ |node| b = b ++ this.listenToNodeBundle(node); }
		^b
	}
	
	listenToNodeBundle { |node|
		^nil

		//dictates how this node listens to other nodes. called by self and other nodes
		 //must be implemented by each subclass
		//called by other crttrnodes, each node is responsible for the bundles of all its listeners
		//if I don't want to listen, then my listenToNodeBundle for a particular node will return nil
		//if I want a special behavior for nodes with a particular name, then I will return a special synth
		//the node that wants to be listened to is responsible for sending all bundles!!
		
			//do I want to listen to you?
			//if yes, 
				//add a synth to my encoders with the key being the object hash
				//return a bundle that will be sent by the other node when it decides to start playing
			//if no
				//return nil
	}
	
	stopListeningToNodeBundle{ |node|
		this.subclassResponsibility(thisMethod);
	}
	

	sourceBundle { //builds source - no source if def is nil
		var b = [];
		if (sourceDef.notNil) {
			isPlaying = true;
			b = b ++ this.sourceSynthBundle;
			b = b ++ this.listenToMeArrayBundle(mesh.nodes);
			^b
		} {
			^nil
		}
	}
	
	sourceSynthBundle {
		//build source synth from synthdef and args
		var source, args, msg;
		source = Synth.basicNew(sourceDef, mesh.server);
		synths.put('source', source);
		
		args = [
			\in, 	busses['in'],
			\out, 	busses['out'],
			\spatialBus, busses['spatial']
			] ++ sourceArgs;		
		msg = source.newMsg(groups['source'], args);
		^[msg]
	}
	
	listenToMeArrayBundle { |nodeArray|
		var b = [];
		//filter out node array so it is all nodes but me
		nodeArray = nodeArray.select{ |node| node.hash != this.hash };
		nodeArray = nodeArray.select{ |node| node.isListening };

		nodeArray.do{ |node| 
			b = b ++ this.listenToMeBundle(node) 
		};
		^b
	}
	
	listenToMeBundle { |node|
		^node.listenToNodeBundle(this);
	}
	
	finishBundle {
		^nil
	}
	
	//freeing
	freeBundle {
		var b = [];
		b = b ++ this.groups['head'].freeMsg; 
		//stopListeningToNodeBundle for all that are listening
		b = b ++ this.freeFunc; //i.e. free buffers
		^b
	}	
	freeFunc { ^nil } //for subclasses
	
	//REALTIME CONTROL
	setPosition { |array|
		pos = array;
		//TODO: for realtime, need to update the spatial bus
	}
	
	
	addToMesh { |m|
		mesh = m;	
	}
	

	//Synths
	*storeSynthDefs {
		SynthDef("crttrSpatialBus", { |x, y, z, controlBus|
			var array = [x, y, z];
			array = Lag.kr(array, 0.01); 
			Out.kr(controlBus, [x, y, z]);
		}).store;		
	}

}

CNAmbiSynth : CrttrMeshNode {
	
	internalBusses {

		^[
			['ambi', \audio, 4],
		]
	}

	addGroups {
		^[
			['encoder', 'listener', \addToHead],
			['decoder', 'listener', \addToTail]
		]
	}

//specify what synths to use
	
	additionalListenerBundle {
		^this.decoderBundle;
	}
	
	listenToNodeBundle { |node|
		var synth, msg;
		if (node.hash == this.hash) { "can't listen to self!".throw; };
		
		#synth, msg = this.listenToNodeSynthAndMsg(node);
		
		//store synth in synths dictionary (TODO: this should be in superclass)
		if (synths[\encoders].isNil) { synths[\encoders] = IdentityDictionary.new };
		synths[\encoders].put(node.hash, synth);
	
		//return bundle
		^[msg]
	}
	
	listenToNodeSynthAndMsg { |node|
		var listenSynth, listenMsg, listenArgs;
		
		listenSynth = Synth.basicNew("crttrBFEncode1", mesh.server);
		listenArgs = [
			\audioIn, node.busses['out'],
			\sourcePosBus, node.busses['spatial'], 
			\listenerPosBus, busses['spatial'],
			\ambiOut, busses['ambi'], 
			\maxDelay, mesh.maxDelay
			];
		listenMsg = listenSynth.newMsg(groups['encoder'], listenArgs);
		^[listenSynth, listenMsg]
	}
	
	stopListeningToNodeBundle { |node|
		var listenSynth = synths['encoders'].at(node.hash);
		if (listenSynth.notNil) {
			synths['encoders'].removeAt(node.hash);
			^[listenSynth.freeMsg];
		} {
			^nil
		};
	}
	
	decoderBundle {
		var synth, args, msg; 
		args = [
			\ambiIn, busses['ambi'],
			\decAudioOut, busses['in'],
			\azimuth, 0,
			\elevation, 0
			];
		synth = Synth.basicNew("wChanOut", mesh.server);
		synths.put('decoder', synth);
		msg = synth.newMsg(groups['decoder'], args);
		^[msg]
	}
	
	*storeSynthDefs {
		SynthDef("crttrBFEncode1", { |audioIn, sourcePosBus, listenerPosBus, ambiOut, maxDelay, speedOfSound=344|
			var audio, sourcePos, listenerPos, listenerPosRel, r, delayTime, ambiChans;
			
			audio = InFeedback.ar(audioIn);

			//POSITION
			sourcePos = In.kr(sourcePosBus, 3);
			listenerPos = In.kr(listenerPosBus, 3);
			listenerPosRel = sourcePos - listenerPos;
			
			r = (listenerPosRel[0].squared + listenerPosRel[1].squared).sqrt;
			delayTime = r/speedOfSound;
			
			//DELAY
			audio = DelayN.ar(audio, maxDelay, delayTime);
			
			//TODO: filtering based on distance
			

			ambiChans = BFEncode2.ar(audio, listenerPosRel[0], listenerPosRel[1], 0);
			
			Out.ar(ambiOut, ambiChans); //sum to the ambisonic bus for this node
			
		}).store;
		
		SynthDef("wChanOut", { |ambiIn, decAudioOut=0|
			var w,x,y,z, audio;
			#w,x,y,z = In.ar(ambiIn, 4);
			audio = w;
			Out.ar(decAudioOut, audio);
		}).store;
	}

}

CNStereoDecoder : CNAmbiSynth {
	
	initVars {
		numInputChannels = 2;
		numOutputChannels = 2;
	}	
	
	decoderBundle {
		var synth, args, msg; 
		args = [
			\ambiIn, busses['ambi'],
			\decAudioOut, busses['out']
			] ++ args;
		synth = Synth.basicNew("stereoDecoder", mesh.server);
		synths.put('decoder', synth);
		msg = synth.newMsg(groups['decoder'], args);
		^[msg]
	}
	
	sourceBundle { ^nil }
	
	*storeSynthDefs {
		"stored stereodecoder".postln;
		SynthDef("stereoDecoder", { |ambiIn, decAudioOut=0|
			var w,x,y,z, audio;
			#w,x,y,z = In.ar(ambiIn, 4);
			audio = BFDecode1.ar(w, x, y, z, [-0.5pi, 0.5pi], [0, 0]);
			Out.ar(decAudioOut, audio);
		}).store;	
	}

}

CNAmbiListenMono : CNAmbiSynth { //mostly used for mapping
		
	decoderBundle {
		var synth, args, msg; 
		args = [
			\ambiIn, busses['ambi'],
			\decAudioOut, busses['out']
			] ++ args;
		synth = Synth.basicNew("monoDecoder", mesh.server);
		synths.put('decoder', synth);
		msg = synth.newMsg(groups['decoder'], args);
		^[msg]
	}
	
	sourceBundle { ^nil }
	
	outputs { ^nil }
	
	*storeSynthDefs {
		SynthDef("monoDecoder", { |ambiIn, decAudioOut=0|
			var w,x,y,z, audio;
			#w,x,y,z = In.ar(ambiIn, 4);
	//		audio = BFDecode1.ar(w, x, y, z, [-0.5pi, 0.5pi], [0, 0]);
			Out.ar(decAudioOut, w); //just the w channel for now
		}).store;	
	}
}



CNSoundFile : CrttrMeshNode {
	var <>path, <buffer;
	
	initVars {
		sourceDef = "cnPlayBuf_1";
	}
	
	listenerBundle { ^nil }
	
	sourceSynthBundle {
		//build source synth from synthdef and args
		var source, args, msg, bmsg;
		
		buffer = Buffer.new(mesh.server);
		
		source = Synth.basicNew(sourceDef, mesh.server);
		synths.put('source', source);
		
		args = [
			\in, 	busses['in'],
			\out, 	busses['out'],
			\spatialBus, busses['spatial'],
			\bufnum, buffer.bufnum
			] ++ sourceArgs;		
		msg = source.newMsg(groups['source'], args);

		bmsg = buffer.allocReadMsg(path,  completionMessage: msg)

		^[bmsg]
	}
	
	freeFunc {
		^buffer.freeMsg
	}
	
	*storeSynthDefs {
	
		SynthDef("cnPlayBuf_1", { |out, bufnum|
			var audio;
			audio = PlayBuf.ar(1, bufnum, 1, 0, 0, 0, doneAction: 0);
			Out.ar(out, audio);
		}).store;
	}
}


CNPhaseIn : CNAmbiSynth {
		
	outputs {
		^[ 
			[\out, \audio, 1],
			[\phaseInState, \control, 1],
			[\phaseInQ, \control, 1]
		]
	}
	
		
}




//useage of CNSoundFile
//c = CNSoundFile(mesh, pos).path_(sfPath);
//


CNIrmLogger : CNAmbiSynth { //works with CrttrMeshLog
	//want to send audio out one channel and data out another channel
	//the data will later be visualized
	//GOAL: use with many different IRMs without writing two synthdefs
	//FORNOW: Synthdef has its own record buffer 
	var logPath;
	
	//initialize log buffer

	//send logbuffer to synthdef

	finishBundle {
		//write logpath		
	}
	
	
	*storeSynthDef {
		
	
	}

}

