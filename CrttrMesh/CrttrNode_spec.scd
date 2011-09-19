CrttrNode

plays with a CrttrMesh
has a spatial position
can listen to other nodes
has state (isListening, isPlaying)
can traverse array of other nodes in the mesh
can listen to other nodes

vars:
mesh
numInChannels, numOutChannels (ints, used by the mesh for recording)
isPlaying, isListening
busses (identity dictionary of busses - spatial and audio out)
group (head group)
groups (identity dictionary of groups ... name -> group)
synths (identity dictionary of all running synths, each a flat array)
pos (Array[x, y, z])

allocFuncs (array of funcs that each return an osc message a an array)
	//init happens when class is instantiated, alloc happens when it gets put on the server
freeFuncs (array of funcs that each return an osc message...)


funcs:
*new(mesh, pos, sourceDef, args)
play
pase
stop
free
makeBundle //makes all
	initBundle //inits groups and other resources
		initGroup //head group
		groupsBundle
	spatialBundle
	listenerBundle
		listenToNodeArrayBundle(otherNode)
	sourceBundle
		synthBundle
		otherNodesBundle(otherNodeArray)


free

addToAlloc { for subclasses, buffers etc. returns an osc message for bundling }
addToFree { cleans up any addtoinit functions }

listenToNodeArrayBundle { |nodeArray| //call listenToNodeBundle for all other nodes except self }

listenToNodeBundle(otherNode) //must be implemented by each subclass
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

stopListeningToNodeBundle(otherNode) //must be implemented by each subclass that listens to nodes


archive

numListenerChannels
numOutputChannels

setPosition(pos) { 

//set type messages
listener_(type, args) //TODO: not sure yet
	//find new listener type, rebuild tree with new funcs
	
synth_(defName, args)
	//free old synth if present
	//make new synth bundle
	//send new synth bundle
	//isPlaying -> notify others

get(\name, val) //not implemented

//recording support



subclasses:

CNAmbiSynth : CrttrNode
	busses { 
		[
			['ambiBus', \audio, 4], 
			['ambiOut', \audio, numInputChannels]			[ 'audioOut', \audio, numOutputChannels]
		];
	}
	
	synths {
		decoder = "decodeBF_" ++ numInputChans;
	
		var id = IdentityDictionary.new;
		var decoder = "decodeBF_" ++ numInputChannels;
		id.put('decoder', decoder);
		^id
	}
	
	
	listenToNodeBundle(otherNode)
		//gets synth out bus
		//gets spatial bus
		//add to head of listener group
	listenBundle {
		listenToNodeArrayBundle
		decoderBundle
	}
	decoderBundle { //make decoder, add to tail of listener group }

//CNAmbiIRM (records state to buffer)
	
CNAmbiStereo : CNAmbiSynth //listens stereo
	//isPlaying = false
	//numChannels = 2
	init {
		this.numOutputChannels = 2;
	}
	synths {
		numOutputChannels = 2;
		defs['decoderDef'] = "BFBinauralDecode";
		defs['decoderArgs'] = [\anything, anything];
	}
	groups {
		[
			[\encoder, \listener, \addToHead],
			[\decoder, \listener, \addToTail]
		]
	}
	
	//decoder synth is "BFBinauralDecode"

	//crttrMesh can then listen to this node's position on the hardware outs
	//
}


CNAmbiMono : CNAmbiSynth //we'll use these as listeners

CNSoundFile : CrttrMeshLog //plays soundfile at point
	path_(path)
	b = CNSoundFile(c, [20, 15, 0]).path_("sounds/a11wlk01.wav");


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
	

}




CrttrMesh
	vars:
	bounds
	server
	nodes
	group (head group)
	nodeGroup
	repatchGroup
	outputs //[ [node, hardwareOut, numChans], [node, hardwareOut, numChans] ] 

	funcs:
//realtime/render control of output
	setNodeAsOutput(node, overWrite=false) //listen to node on hardware out - mix with others?
		repatchBundle
	repatchBundle
	outputs //array of [node, hardwareOut, numChans] //what is this useful for?
	
//	groups (sub-groups of the head group, can be nil, but can be used for re-patching)
//ALL RENDERING ISSUES
	render(path)
		prep server options for ouputs, etc.
		makeAllBundle
		render to path
	
	renderLog(path)
		path is logFile
		make new crttrmeshlog
		write crttrmeshlog
		
		
		
	
	renderSourcesAsFiles (used for nrt)
		all nodes that isPlaying should be set as output, overWrite false
	playSourcesFromFiles (used for nrt)
	recordFromNode(node) //this records from a single node's perspective
		setNodeAsOutput
		renderWithPath
	recordMultipleNodes
	
	
		
	renderWithPath(path, time) //writes files
	
	
	asTargetForNode{ ^nodeGroup }

//what should be logged
	nodes
	paths
	time

CrttrMeshLog : CrttrMesh //a crttrmesh explicitly for nrt stuff. can be archived and read back.
	var path; //the path - can be a directory
	var time; //the the overall time one should render
	var xListeners, yListeners
	
	*newFromMesh(mesh, time)
		//copys mesh to self

	*newFromPath(path)
		//archive file of self
	
	*new(bounds, server, path, time) //like CrttrMesh new but with the args we need
	
		
	renderWithXYListeners(xListenerArg, yListenerArg)

	addListenersToMesh
		//synths - listeners

	makeListeners
		//assign groups of outputs
		//allocate record synths for those outputs
		//allocate buffers for those record synths
		//stash some information about the mapping between listeners and buffers in the log variables
		//make bundles for buffers
		//make bundles for all listeners (add to bundle queue?)
		
	optionsForCurrentRender
		^options

	doRender
		//get options for current render
		//make a server for current render?
		//create score
		//make all initial bundles + add to score
			//apply any changes over time + add
		//do all finish bundles + add to score
		//finish score
		//render score
	

	


	
//wishlist
	meshes can be overlaid
	meshes can have selective cross-talk

CrttrMeshGui
	//works for realtime operation
	new(CrttrMesh)
	showNodesAsPoints

CrttrMeshLogGui	
	//works to display archived data
	


other CrttrLib classes:
CrttrComposite, CrttrCall, etc.

BeastMulch Compatability:

Check out BMAbstractAudioSource and BMAbstractAudioChainElement
methods for now:
	group



///SEPTEMBER UPDATE

//following syntax?


//REALTIME
c = CrttrMesh(s, 100@100); //big field

//c.play; //could put play up here aka nodeproxy

100.do{ |i| CrttrHessFrog(c, 100.rand@100.rand) };
n = CrttrAmbiListen(c, x, y, z, (speakerSetup: \pod));
c.setNodeAsOuput(n, 0);

g = c.rtgui; //gui where you can see locations and stats

l = c.log; //start recording

c.play;

//do stuff
//respond to gui input

c.stop; //stops logs as well
g.close;

m = l.makeMeshMap(20, 20, [\dbRMS, \flux, \L10]); //numxListeners, numYListeners, keys to map

g = m.gui;
m.renderMPG(path, frameRate, secsPerFrame, timeIn, timeOut,  keyToRender: \dbRMS); 

//get statistics
m.avgDB 
m.avgFlux

//
//NRT
//

c = CrttrMesh(100@100, s); //big field

//c.play; //could put play up here aka nodeproxy

100.do{ |i| CrttrHessFrog(c, 100.rand@100.rand) };

n = CrttrAmbiListen(c, x, y, z, (speakerSetup: \pod));
c.setNodeAsOuput(n, 0);

l = c.log("path/to/directory"); //get logging in place. l is a CrttrMeshLog

//TWO OPTIONS for render

//1 - render from the log
l.render(time); // -> s = c.asScore(time); s.renderNRT(server, format);
	//renders NRT, records all node outputs
	//TODO: records the output channels as an .aiff file

//2 - render from the mesh
c.render(path, time); //
	//set up recording of outputs
	//c.asScore(time)
	//does not record the log variables???


//play back render in real-time
if (l.playMesh.hasOutput.not) {
	n = CrttrAmbiListen(c, x, y, z, (speakerSetup: \pod));
	l.playMesh.setNodeAsOutput(n, 0);
};

l.playMesh.play; //does the playing back - the node that is set as output will 


m = l.makeMeshMap(20, 20, [\dbRMS, \flux, \L10]); //numxListeners, numYListeners, keys to map
	//make a new playmesh
	//add listeners to that playmesh
	//render that new playmesh
	
g = m.gui;
m.renderMPG(path, frameRate, secsPerFrame, timeIn, timeOut,  keyToRender: \dbRMS); 

//get statistics
m.avgDB 
m.avgFlux


