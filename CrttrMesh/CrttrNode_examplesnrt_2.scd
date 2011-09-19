
~numX = 20;
~numY = 20;

o = Server.default.options;
o.memSize = 2.pow(16);
o.numAudioBusChannels = 2.pow(13);
o.numOutputBusChannels = ~numX * ~numY;

o.maxNodes = 2.pow(16);
o.numBuffers = 2.pow(12);

v = Server.new("nrt", NetAddr("127.0.0.1", 57111), o, 1);


c = CrttrMesh(20@20, v);


//add some nodes
10.do {  CNAmbiSynth(c, [20.rand,20.rand,0], "irm_supershort", [\freq, 300.rand + 300]); }
p = CNStereoDecoder(c, [4, 4, 0]);


//get some bundles
u = c.makeAllBundle;

q = c.setNodeAsOutputBundle(p, 0);


//



//now we can send the bundles




h = v.makeBundle(false, {
	c.make;
});
//	h = h.insert(0, 0);

i = v.makeBundle(false, {
	n = CrleNode(c);
	n.listenAndPlayAtPoint(15, 15, 0, "irm_supershort");
	
	m = CrleNode(c, s);
	m.listenAndPlayAtPoint(5, 5, 0, "irm_supershort");

	3.do{ 
		var oh; 
		oh = CrleNode(c); 
		oh.listenAndPlayAtPoint(
			c.rect.width.asFloat.rand, 
			c.rect.height.asFloat.rand, 
			0, 
			"irm_supershort"
		)
	};

});
//	i = i.insert(0, 0);

j = v.makeBundle(false, {
	c.makeListenerGrid(~numX, ~numY);
});
