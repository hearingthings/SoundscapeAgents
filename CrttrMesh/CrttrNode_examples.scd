s.reboot;
c = CrttrMesh.new(20@20, s);

SynthDef("sin", { |test=1, ta=200, out| Out.ar(out, SinOsc.ar(SinOsc.ar(test, 0, ta), 0, 1)) }).store 

m = CNAmbiSynth(c, [1,1,0], "sin")
n = CNAmbiSynth(c, [1,1,0], "sin")
o = CNAmbiSynth(c, [1,1,0], "sin")

p = CNStereoDecoder(c, [4, 4, 0]);

u = c.makeAllBundle
s.listSendBundle(0, u)
s.queryAllNodes;


//v = c.fromNodePerspectiveBundle(m, 0);
v = c.setNodeAsOutputBundle(p, 0);
s.listSendBundle(0, v);

p.setPosition([0, 0, 0])

p.synths['spatial'].set(\x, 1, \y, 1)

u = c.setNodeAsOutputBundle(n, 1);



b = [];
b = b ++ c.makeBundle;
b = b ++ m.makeBundle;
b = b ++ n.makeBundle;


s.listSendBundle(0, c.makeBundle);
s.listSendBundle(0, o.makeBundle);

//debugging individual bundles
b = n.makeBundle;
b.size; //13
s.listSendMsg(b[0])
s.listSendMsg(b[1])
s.listSendMsg( b[2])
s.listSendMsg( b[3])
s.listSendMsg( b[4])
s.listSendMsg( b[5])
s.listSendMsg( b[6]); //duplicate node id
s.listSendMsg( b[7])
s.listSendMsg( b[8])
s.listSendMsg( b[9])
s.listSendMsg( b[10])
s.listSendMsg( b[11])
s.listSendMsg( b[12])

b[8]

s.queryAllNodes

n.groupArray;
n.makeBundle.postcs;
n.listenToNodeBundle(m);
s.listSendBundle(0, n.makeBundle)
n.dump
s.queryAllNodes

n.synths