///CrttrMeshLog examples


//HOW TO RECORD THE ARCHIVE
SynthDef("sin", { |test=1, ta=200, out| Out.ar(out, SinOsc.ar(1000, IRand(0, 2pi), 1)) }).store;

(	
	v = Server.new("nrt", s.addr, options: CrttrMeshLog.options);
	c = CrttrMesh(20@20, v);
	l = CrttrMeshLog(c, 100, "tmpp"); //mesh, time, directory
	
	2.do{ CNAmbiSynth(c, [10.rand,10.rand,0], "sin"); };
	
	//m = CNAmbiSynth(c, [10,10,0], "sin");
	//n = CNAmbiSynth(c, [0,0,0], "sin");
	
	l.render //renders
)

l.renderComplete; //should be true

//HOW TO LISTEN TO THE ARCHIVE
f = l.archive.meshFromArchive(s) //creates a mesh with CMReadParamFiles - sound output and position
f.nodes;

n = CNStereoDecoder(f, [10, 10, 0]); //make a decoder to listen

g = f.makeAllBundle;
s.listSendBundle(0, g)
f.setNodeAsOutput(n, 0); //YOU SHOULD HEAR SOMETHING!!!


//how to record the archive:
	//make an analysis chain for a particular node (class - CMAnalysisChain - takes a node as input)
	//run the analysis 







s.queryAllNodes(true)
s.scope(8, 16)
l.recordNodes[1].value[0].path

s.queryAllNodes(true)

f.nodes
f.nodes[0].buffers[0].getn(0, 1000, { |v| h = v })
h.plot
f.server;






(


)


f = SoundFile.new;
f.openRead(c.nodes[3].path);
f.numChannels
a = FloatArray.newClear(f.numFrames*f.numChannels);
f.readData(a);
a.plot(numChannels: 3)


a = a.clump(3)
a = a.flop
a.plot


c.recordSingleNode(m);
c.addRecordNodes;

l.recordNodes

l.recordNodes[0][1].param
b = c.recordNodes[0][1].allocBundle


f = c.doRender;

c
b = c.asScore

c.nodes[0].busses

c.nodes[4].defName

Task{

	g = CrttrMeshGui(c);
	g.draw;

	i = 0;

	loop{
		g.setCurrentFrame(i);
		g.refresh;
		(1/60).wait;
		i = i + 1;
	}
}.play(AppClock);


//why is this bastard crashing at over 11by11 grids?

///CrttrMeshLog examples

SynthDef("sin", { |test=1, ta=200, out| Out.ar(out, SinOsc.ar(1000, IRand(0, 2pi), 1)) }).store;
(
v = Server.new("nrt", s.addr, options: CrttrMeshLog.options);
c = CrttrMeshLog.new(20@20, v, "test", 5, 20, 20);

m = CNAmbiSynth(c, [10,10,0], "sin");
n = CNAmbiSynth(c, [0,0,0], "sin");
)

f = c.doRender; //crash fucking city

//puke out all bundles, save to score
//	make bundles
t = c.makeAllBundle;
u = c.makeListenersBundle;
v = c.makeRecordBundle;
w = c.writeBuffersBundle; //TODO: should probably grab finish bundles from nodes

//see te post results for each line
t.size;
t.msgSize;
u.size;
a = u.bundleSize;
v.size;
v.bundleSize;
w.size;
w.bundleSize;


Task{

	g = CrttrMeshGui(c);
	g.draw;

	i = 0;

	loop{
		g.setCurrentFrame(i);
		g.refresh;
		(1/60).wait;
		i = i + 5;
	}
}.play(AppClock);
