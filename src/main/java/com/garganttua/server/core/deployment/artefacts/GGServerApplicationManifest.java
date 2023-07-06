package com.garganttua.server.core.deployment.artefacts;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.update4j.FileMetadata;

import com.garganttua.server.core.exceptions.GGServerApplicationException;

import lombok.Getter;

public class GGServerApplicationManifest {

	public static final String PEGASUS_MANIFEST_EXTENSION = "pem";
	
//	@JsonIgnore
	private String path;
	@Getter
	private String fileName;
	private String fileExtension;
	@Getter
	private File file;
	
	public GGServerApplicationManifest(File file) throws GGServerApplicationException {
		this.file = file;
		if (file == null || !file.exists()) {
			throw new GGServerApplicationException("Manifest file is null or does not exist");
		}
		
		this.fileName = file.getName();
		this.fileExtension = this.fileName.split("\\.")[this.fileName.split("\\.").length-1];
		
		if( !this.fileExtension.equals(GGServerApplicationManifest.PEGASUS_MANIFEST_EXTENSION) ) {
			throw new GGServerApplicationException("Manifest file "+this.file.getPath().toString()+" is not a valid file. Should be with extension "+GGServerApplicationManifest.PEGASUS_MANIFEST_EXTENSION);
		}
	}
	
	public GGServerApplicationManifest(String string) throws GGServerApplicationException {
		this(new File(string));
		this.path = string;
	}

	public GGServerApplicationManifest(Path path) throws GGServerApplicationException {
		this(path.toFile());
		this.path = path.toString();
	}

	public URI getPath() {
		return URI.create("file:///"+path);
	}

	public String getPathStr() {
		return this.path;
	}

	public static boolean isManifest(File f) {
		if( f.getPath().toString().endsWith(GGServerApplicationManifest.PEGASUS_MANIFEST_EXTENSION) ) {
			return true;
		}
		return false;
	}
	
	public List<GGServerApplicationArtefact> getArtefacts() throws IOException {
		List<GGServerApplicationArtefact> artefacts = new ArrayList<GGServerApplicationArtefact>();
		org.update4j.Configuration config = org.update4j.Configuration.read(Files.newBufferedReader(Paths.get(this.getPathStr())));
		List<FileMetadata> files = config.getFiles();
		
		files.forEach(f -> {
			
			String path = f.getPath().toString();
			String filename = f.getPath().getFileName().toString();
			
			GGServerApplicationArtefact art = new GGServerApplicationArtefact(filename, path);
			artefacts.add(art);
			
		});
		
		return artefacts;
	}


}
