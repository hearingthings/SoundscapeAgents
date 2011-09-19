CrttrMeshMap2D {
	//maps a single quality of the mesh
	var <>mesh;
	var <>paramName; //name of parameter mapped
	var <>dataFiles; 
	var <>points; //points that have been mapped
	var <>pointsToFileIndex; // [ [point, 
	var <>interpType; //lin or exp - for interpolating between points
	
	//should add some statistical stuff such as mean, stdev, etc.
	
	matrixAtTime { 
		//convert time to frames
		//find nearest frame in time OR interpolate between two frames
	}
	
	matrixForFrame { |frame|
		
		//returns matrix of listeners for a particular frame	
	}
	
	valueAtPoint { |xy|
		//interpolate the value at a particular point
	}
		
	
}