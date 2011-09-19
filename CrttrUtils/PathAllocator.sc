
//PathAllocator takes care of allocating consecutive paths in a directory

//p = PathAllocator("tmp", "text", "txt");

PathAllocator  {
	classvar <>defaultBaseDir;
	var <>baseDir, <>prefix, <>extension;
	var <>allocator;
	var <>maxFiles;
	
	*initClass { defaultBaseDir = "tmp/" }
	
	*new{ |baseDir, prefix, extension| ^super.new.initWithBaseDirAndPrefix(baseDir, prefix, extension) }	
	*newClearDir{ |baseDir, prefix, extension| ^super.new.initWithBaseDirAndPrefix(baseDir, prefix, extension).clearDir }
	
	initWithBaseDirAndPrefix { |baseDirArg, prefixArg, extensionArg|
		if (baseDirArg.last != $/) {baseDirArg = baseDirArg ++ "/"};
		baseDir = baseDirArg;
		prefix = prefixArg;
		extension = extensionArg;
		this.makeNewDirIfNeeded;
		
		allocator = RingNumberAllocator( this.getLastNumInDirForPrefix(baseDir, prefix), 2.pow(16)); //TODO: use stacknumberallocator and return nil when we're at the end, or wrap around?
	}
	
	alloc {
		var path, allocNum;
		allocNum = allocator.alloc;
		if (allocNum.notNil) {
			path = baseDir ++ prefix ++ "_" ++ allocNum ++ "." ++ extension; //TODO: make the formatting modular
		} {
			path = nil;	
		}
		^path;
	}
	
	
	makeNewDirIfNeeded {
		if (this.dirExists(baseDir).not) {
			this.makeDir(baseDir);
		};
	}
	
	getLastNumInDirForPrefix { |dir, prefix| //TODO: make formatting modular - search for various prefixes
		var string = ("ls" + dir.asString).unixCmdGetStdOut;
		var ints = string.split(10.asAscii).collect{ |t|
			var split = t.split($.)[0];
			if (split.contains(prefix)) {
				split.split($_).last.asInteger
			} {
				0
			};
		};
		ints = ints.maxItem
		^(ints ? 0)	
	}
	
	makeDir { |dir|
		var cmd = "mkdir" + dir.asString;
		var result = cmd.unixCmd;
	}
	
	dirExists{ |dir| var res = ("ls" + dir.asString).unixCmdGetStdOut; ^(res.size > 0) }
	
	clearDir { |dir| ("rm" + baseDir ++ "*").unixCmd } 	
}







