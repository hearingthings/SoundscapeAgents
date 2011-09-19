+ Score {
//blocks thread execution until the render is done
	*recordNRTBlock { arg list, oscFilePath, outputFilePath, inputFilePath, sampleRate = 44100,
		headerFormat = "AIFF", sampleFormat = "int16", options, completionString="", duration = nil;
		^this.new(list).recordNRTBlock(oscFilePath, outputFilePath, inputFilePath, sampleRate,
		headerFormat, sampleFormat, options, completionString, duration);
	}
	
	recordNRTBlock { arg oscFilePath, outputFilePath, inputFilePath, sampleRate = 44100, headerFormat =
		"AIFF", sampleFormat = "int16", options, completionString="", duration = nil;
		this.writeOSCFile(oscFilePath, 0, duration);
		^unixCmdGetStdOut(program + " -N" + oscFilePath + (inputFilePath ? "_") + "\""++outputFilePath++"\""
		 	+ sampleRate + headerFormat + sampleFormat +
			(options ? Score.options).asOptionsString
			+ completionString);
	}


}