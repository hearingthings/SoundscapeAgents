TenmaMeter {
	
	
	*adcToDB { |volts|	
		^((0.1069 * volts) + 0.5211);			
	}
	
}