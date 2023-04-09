package com.gtech.pegasus.core.deployment.artefacts;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.update4j.FileMetadata;

import com.gtech.pegasus.core.exceptions.PGApplicationException;

import lombok.Getter;

public class PGApplicationManifest {

	public static final String PEGASUS_MANIFEST_EXTENSION = "pem";
	
//	@JsonIgnore
	private String path;
	@Getter
	private String fileName;
	private String fileExtension;
	@Getter
	private File file;
	
	public PGApplicationManifest(File file) throws PGApplicationException {
		this.file = file;
		if (file == null || !file.exists()) {
			throw new PGApplicationException("Manifest file is null or does not exist");
		}
		
		this.fileName = file.getName();
		this.fileExtension = this.fileName.split("\\.")[this.fileName.split("\\.").length-1];
		
		if( !this.fileExtension.equals(PGApplicationManifest.PEGASUS_MANIFEST_EXTENSION) ) {
			throw new PGApplicationException("Manifest file "+this.file.getPath().toString()+" is not a valid file. Should be with extension "+PGApplicationManifest.PEGASUS_MANIFEST_EXTENSION);
		}
	}
	
	public PGApplicationManifest(String string) throws PGApplicationException {
		this(new File(string));
		this.path = string;
	}

	public PGApplicationManifest(Path path) throws PGApplicationException {
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
		if( f.getPath().toString().endsWith(PGApplicationManifest.PEGASUS_MANIFEST_EXTENSION) ) {
			return true;
		}
		return false;
	}
	
	public List<PGApplicationArtefact> getArtefacts() throws IOException {
		List<PGApplicationArtefact> artefacts = new ArrayList<PGApplicationArtefact>();
		org.update4j.Configuration config = org.update4j.Configuration.read(Files.newBufferedReader(Paths.get(this.getPathStr())));
		List<FileMetadata> files = config.getFiles();
		
		files.forEach(f -> {
			
			String path = f.getPath().toString();
			String filename = f.getPath().getFileName().toString();
			
			PGApplicationArtefact art = new PGApplicationArtefact(filename, path);
			artefacts.add(art);
			
		});
		
		return artefacts;
	}


}
