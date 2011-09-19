AnalysisDataGroup {
	var <>array;
	var <>path;
	var <timeIn, <timeOut, <timeGrain;
	var duration;

	*new { |array|
		^super.newCopyArgs(array);
	}

	duration {
		var maxdur;
		if (duration.isNil) {
			
			"dur was nil".postln;
			maxdur = this.collect{ |dat| dat.duration };
			maxdur.debug("durations");
			maxdur = maxdur.maxItem;
			maxdur.debug("max durations");
			^maxdur		
		} {
			duration.debug("duration was not nil");
			^duration		
		};	
	}
	
	duration_{ |indur| indur.debug("indur"); }
	
	timeIn_{ |ti|
		timeIn = ti;
		this.do{ |dat| dat.timeIn = timeIn };
	}
	timeOut_{ |to|
		timeOut = to;
		this.do{ |dat| dat.timeOut = timeOut };
	}
	timeGrain_{ |to|
		timeGrain = to;
		this.do{ |dat| dat.timeGrain = timeGrain };
	}
	
	do { |func|
		^this.array.do(func);
	}
	collect { |func|
		^this.array.collect(func);
	}


}


AnalysisData {
	classvar subclassTypes;
	var <>name;
	var <>params, <>path, duration, <>chain;
	var <nStreams;
	
	var <>timeIn, <>timeOut, <>timeGrain;
	
	*initClass { 
		subclassTypes = IdentityDictionary.new;
		this.subclasses.do{ |c|
			subclassTypes.put(c.dataType, c);
		};
	}
	
	*dataType { ^nil }
	
	*newForType { |type, name, params, path, duration, chain|
		var classToMake = this.subclassForType(type);
		^classToMake.new(name, params, path, duration, chain);
	}
	
	*new { |name, params, path, duration, chain|
		^super.newCopyArgs(name, params, path, duration, chain).init;
	}
	
	*subclassForType { |type|
		^subclassTypes[type];
	}
	
	init {

	}
	
	displayTime {
		^(timeOut - timeIn);	
	}

}

ADataCont : AnalysisData { //continuously triggered analysis data
	var <sampleRate;
	var <env;
	*dataType { ^\continuous }
	
	init {
		sampleRate = chain.chainArray.last.sampleRate;
	}
	
	makeEnv {
		var levels, times;
		levels = this.returnFloatArrayData;
		times = FloatArray.fill(levels.size-1, { 1/sampleRate });		env = Env(levels, times);
		^env
	}
	
	returnFloatArrayData {
			var file, data, nf, nc;
			var env, times;
			file = SoundFile.new;
			file.openRead(path);
			nf = file.numFrames;
			nc = file.numChannels;
			data = FloatArray.newClear(nf* nc);
			file.readData(data);
			file.close;

			^data		
	}
	
	
	at { |time|
		if (env.isNil) {this.makeEnv};
		^env.at(time);
	}
	
	dataForView {
		var numPoints = (timeOut - timeIn) / timeGrain;
		var d;
		
		numPoints.debug("numpoints");
		numPoints = numPoints.ceil.asInteger;
		d = Array.fill(numPoints, { |i|
			this.at(timeIn + (i * timeGrain));
		});
		
		^d
	}
	
	duration {
		if (duration.notNil) {
			"sub1".postln;
			^duration	
		} {
			if (env.notNil) {

			"sub2".postln;
				^env.times.sum;
			} {
			"sub3".postln;
				this.makeEnv;
				^env.times.sum;
			}
		};	
	}
	
	duration_{ |indur|
		indur.debug("in dur in sub");
	}	
	
}


ADataDisc : AnalysisData { //discretely triggered analysis data
	*dataType { ^\discrete }
	
}