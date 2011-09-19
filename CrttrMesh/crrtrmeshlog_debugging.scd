///CrttrMeshLog examples

SynthDef("sin", { |test=1, ta=200, out| Out.ar(out, SinOsc.ar(1000, 0, SinOsc.kr(1).range(0, 1))) }).store;

v = Server.new("nrt", s.addr, options: CrttrMeshLog.options);


s.options.numAudioBusChannels = 512;
s.reboot
v = s;
c = CrttrMeshLog.new(20@20, v, "test", 5, 10, 10);

m = CNAmbiSynth(c, [10,10,0], "sin");

f = c.doRender;

n = c.makeAllBundle;
s.listSendBundle(0.2, n);


//check busses
b = c.nodes.select{ |n| n.isPlaying };
b.do{ |node| 
	node.busses['out'].debug; 
//	node.busses['out'].get{ |v| v.debug("outval") } 
};

//scope the bus that prints out to see if audio is flowing
s.scope

n = c.makeListenersBundle;
s.listSendBundle(0.2, n); 

g = c.listenerSynths.collect{ |synth| synth.busses['out'] }
s.scope(1, g[1].index )


n = c.makeRecordBundle;
n = n.insert(0, 0);
score = score.add(n);
		




Task{
	5.wait;
	g = CrttrMeshGui(c);
	g.draw;

	i = 0;

	loop{
		g.setCurrentFrame(i);
		g.refresh;
		(1/60).wait;
		i = i + 100;
	}
}.play(AppClock);
