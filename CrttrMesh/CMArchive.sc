
CMFileArchive {
	//archive where all parameters are read into files

	var <archivedMesh, <nodes, <path;
	var path;
	
	*fromPath { |path|
		//
		^Object.readArchive(path);
	}
	
	
	*newWithMeshAndNodes { |archivedMesh, nodes, path|
		^super.newCopyArgs(archivedMesh, nodes, path);
	}
	
	
	
	
	meshFromArchive { |server|
		//returns a new crttr mesh from 
		var mesh;
		if (server.isNil) { server = archivedMesh.server };
		mesh = CrttrMesh(archivedMesh.bounds, server);
		
		nodes.do{ |node|
			node.value.copyToMesh(mesh);	
		};
		
		^mesh
	}	
	
	write {
		
		this.writeArchive(path);	
	}
	

	
}



CMScoreArchive {
	//archive where params are as an osc "score"
}

CMArchive {
	//simply archives the mesh
}
