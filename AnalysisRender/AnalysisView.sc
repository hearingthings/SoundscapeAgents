AnalysisView {
	var <>data, rect;
	var <>window, <>zoom, <>sf, <>fft;
	var <>zoomView, <>sfView, <>fftView, <>dataContainer;
	var <>dataViews; // [ [containerview, userview1, userview2, ...] ]
	
	var <>dataWidthPct, <>dataHeight, <>dataPad;
	
	//
	
	*initClass { this.initSynths }
	
	*new { |data, rect|
		^super.newCopyArgs(data, rect).init
	}
	
	show {
		this.makeAll;
	}
	
	init {
		dataWidthPct = 0.8;
		dataHeight = 100;
		dataPad = 10;
		data.timeIn = 0;
		data.timeOut = data.duration;
		data.timeGrain = (data.timeOut - data.timeIn / rect.width);
	}
	
	makeAll {
		this.makeWindow;
		this.drawZoom(window);
		this.drawSf(window);
//		this.drawFFT(window);
		this.makeDataContainer(window);
		this.drawAllData(dataContainer);
		
		window.front;
	}
	
	makeWindow {
		window = Window.new("analysisView", rect);
		window.view.decorator = FlowLayout(window.view.bounds);
		^window
	}
	
	dec {
		^window.view.decorator;	
	}
	contentWidth {
		^this.dec.innerBounds.width;	
	}
	dataWidth {
		^(this.contentWidth * dataWidthPct)
	}
	statsWidth {
		^(this.contentWidth * (1 - dataWidthPct))	
	}
	
	drawZoom { |parent| //parent is usually the window
		//make zoomView (container)
		zoomView = CompositeView(window.postln, Rect(0, 0, this.contentWidth, 20).postln);


		//drawZoom
			//zoomslider updates timein, timeout, and time grains
		
		zoom = RangeSlider(
			zoomView, 
			Rect(this.statsWidth, 0, this.dataWidth, zoomView.bounds.height)
		);
		zoom.action_{ |slider|
			[slider.lo, slider.hi].postln;	
		};
		zoom.lo = 0;
		zoom.hi = 1;
		
	}
	
	drawSf { |parent|
		var f, synth, buf, startPos, duration;
		//make soundfile container
		sfView = CompositeView(parent.postln, Rect(0, 0, this.contentWidth, 70).postln);
		
		sf = SoundFileView(
			sfView, 
			Rect(this.statsWidth, 0, this.dataWidth, sfView.bounds.height)
		);
		
		buf = Buffer.read(Server.default, data.path);
		synth = (instrument: \playbufLoop, bufnum: buf.bufnum);	
		

				
		f = SoundFile.new;
		f.openRead(data.path);
		sf.soundfile = f;
		sf.read(0, f.numFrames);
		
		sf.keyDownAction = { |char, mod, unicode, keycode|

			[char, mod, unicode, keycode].postln;
			if (keycode == 32) { //we have a spacebar
				if (synth['isPlaying'] == true) {
					synth.free;
				} {
					startPos = sf.selectionStart(0);
					if (startPos.isNil) {startPos = 0};
					duration = sf.selectionDuration(0);
					if (duration.isNil) {duration = f.duration};
					duration.debug("duration of playback");
					synth.put(\startPos, startPos);
					synth.put(\duration, duration);
					synth.play;
				};
			};
		};		
		
		//soundfile has time in and duration
			//needs to be updated from drawzoom
	}
	drawFFT { |parent|
		//make fft container
		
		//draw fft
			//needs to be updated from zoom
	}
	
	makeDataContainer{ |parent|
		var contHeight;
		
		//container height
		contHeight = dataHeight * data.size * dataPad;
		
		dataContainer = CompositeView(parent,
			Rect(0, 0, this.contentWidth, contHeight)
		);
	
	}
	
	drawAllData { |container|
		//
		dataViews = data.collect{ |dat, i|
			var v;
			var x, y;
			y = i * (dataHeight + dataPad);
			v = AnalysisDataView(
				container,
				Rect(0, y, this.contentWidth, dataHeight),
				dat,
				dataWidthPct
			);
			v.draw;
		};
	}
	
	drawDataIn { |data, parent|
		
	}
	
	*initSynths {
		SynthDef(\playbufLoop, { |bufnum, startPos, duration|
			var chain;
			chain = PlayBuf.ar(1,bufnum, 1, Impulse.ar(1/duration), startPos, 1);
			Out.ar(0, chain.dup);
		}).store;
		
	}
	
}

AnalysisDataView {
	var <>parent;
	var <>bounds;
	var <>data; //data we are displaying
	var <>dataWidthPct;
	var <>cont, <>statsCont, <>dataCont;
	var <>statsViews, <>dataView, <>axisView; //views for statistics
	var <>spec;
	
	*new { |parent, bounds, data, dataWidthPct|
		^super.newCopyArgs(parent, bounds.asRect, data, dataWidthPct).init;
	}
	
	init {}
	
	draw { 
		this.drawContainers;
		this.drawStats(statsCont);
		this.drawData(dataCont);
	}
	
	drawContainers { 
		//make overall container
		cont = CompositeView(parent, bounds);
		//make stats container
		statsCont	 = CompositeView(
			cont,
		 	Rect(0, 0, this.statsWidth, cont.bounds.height)
		 );
//		statsCont.background_(Color.white);

		//make data container
		 dataCont = CompositeView(
		 	cont,
		 	Rect(this.statsWidth, 0, this.dataWidth, cont.bounds.height)
		 );
		 
		 dataCont.background_(Color.white);



		//TODO: make axis container, make axis view
				
	}
	
	drawStats { |par|
		var name, params, plist;
		
		name = StaticText(
			par,
			Rect(0, 0, par.bounds.width * (2/3), 20)
		);
		name.string_(data.name);
		
		params = ListView(
			par,
			Rect(0, 20, par.bounds.width * (2/3), par.bounds.height-25)
		);
		plist = [];
		data.params.keysValuesDo{ |k, v|
			plist = plist.add([k, v].asString);	
		};
		params.items = plist;
		params.action = {};
		
		//make stats view
			//text fields with data name
			//mouse over gives params? drop-down with params
	}
	
	drawData { |par|
		//
		var dataView = 
			UserView(
				par,
				Rect(0, 0, par.bounds.width, par.bounds.height)
			);
		dataView.drawFunc = { |view|
			var array = data.dataForView;
			var max = array.maxItem;
			var min = array.minItem;
			var startPoint;
			var dataToPoint = { |p, i|
				var curTime = i * data.timeGrain;
				var curX = (curTime/data.displayTime) * view.bounds.width;
				var curY = p.linlin(min, max, view.bounds.height, 0);
				curX@curY;
			};
						
			startPoint = dataToPoint.value(array[0], 0);
			
			Pen.color = Color.black;
			
			Pen.moveTo(startPoint);
			
			array[1..].do{ |point, in|
				in = in + 1;
				point = dataToPoint.value(point, in);
				Pen.lineTo(point);
			};
			Pen.stroke;
			
			//now fill in for shading
		};	
		
	}
	
	refresh {}
	
	drawAxis {
		//not implemented yet	
	}
	
	contentWidth {
		^bounds.width;	
	}
	dataWidth {
		^(this.contentWidth * dataWidthPct)
	}
	statsWidth {
		^(this.contentWidth * (1 - dataWidthPct))	
	}	
}
