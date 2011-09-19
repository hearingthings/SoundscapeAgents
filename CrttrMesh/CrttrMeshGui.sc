
CrttrMeshGui {
	var <log;
	var parent, container, mapcontainer, pointsview, mapview, timeline;
	var dbToPoint;
	var <>currentFrame, <>pwidth;
	

	*new{ | ...args |
		^super.newCopyArgs(*args).init;
	}
	
	init{ 
		if (parent.isNil) {parent = Window("crttrmeshgui", Rect(200, 200, 800, 800))};
		pwidth = 5; 
		dbToPoint= DBToPoint(-90, 0, 5, 40); 
		
	}
	
	draw {
		this.drawContainerViews(parent.bounds.moveToPoint(0@0));
		this.drawMap;
		parent.front;
		^parent	
	}

	refresh {
		mapview.refresh;	
	}
	
	drawContainerViews { |bounds|
		container = CompositeView(parent, bounds);
		mapcontainer = CompositeView(container, bounds);
		
	}
	
	drawMap {
		mapview = UserView(parent, parent.bounds.moveToPoint(0@0));
		mapview.drawFunc = { |v|
			var b = v.bounds.insetBy(20, 20);
			Pen.use{
				Pen.color_(Color.black);
				currentFrame.do{ |ampPoint|
					var p, pos;
					p = dbToPoint.mapAmp( ampPoint[0].abs );

					p = p.asRect;
					
					pos = ampPoint[1].transform(log.bounds.width@log.bounds.height, b.width@b.height);
					p = p.moveToPoint(pos);
					
					p.debug("oval");
					Pen.fillOval(p);
				}
			};
		};
	}
	
	drawPointOverlayView {
		
	}
	
	drawTimelineView {
		
	}

	
	drawBackgroundAtFrame { 
		
	}
	
	renderMPG { |inFrame, outFrame, sfFPS|
		
	}
	
	setCurrentFrame { |frameIndex|
		currentFrame	= log.listenerArrayForFrame(frameIndex);
	} 	
	
}

DBToPoint {
	var <>lo, <>hi, <>minWidth, <>maxWidth;
	
	*new { | ...args |	
		^super.newCopyArgs(*args).init;
	}
	
	init {
		
	}
	
	mapAmp { |amp|
		var db = amp.ampdb;
		^this.mapDB(db);		
	}
	
	mapDB { |db|
		var w;
		w = db.linlin(lo, hi, minWidth, maxWidth);
		
		^Point(w, w);
	}
	
}

+ Point {
	transform { |oldBounds, newBounds|
		var scale, new;
		scale = newBounds.asArray / oldBounds.asArray;
		new = this.asArray * scale;
		^Point.new( *new )
		
	}	
	
}
