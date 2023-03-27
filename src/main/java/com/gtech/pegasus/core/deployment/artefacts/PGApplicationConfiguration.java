package com.gtech.pegasus.core.deployment.artefacts;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;

import com.gtech.pegasus.core.exceptions.PGApplicationException;

import lombok.Getter;

public class PGApplicationConfiguration {

	private String path;
	@Getter
	private String fileName;
	private String fileExtension;
	@Getter
	private File file;
	
	public static String[] configurationFileExtensions;

	public PGApplicationConfiguration(File file) throws PGApplicationException {
		this.file = file;
		if (file == null || !file.exists()) {
			throw new PGApplicationException("Configuration file is null or does not exist");
		}
		
		this.fileName = file.getName();
		this.fileExtension = this.fileName.split("\\.")[this.fileName.split("\\.").length-1];
		
		if( !isConf(this.fileName) ) {
			throw new PGApplicationException("Configuration file "+this.file.getPath().toString()+" is not a valid file. Should be with extension .properties");
		}
		
		this.path = file.getAbsolutePath();
	}
	
	public PGApplicationConfiguration(String string) throws PGApplicationException {
		this(new File(string));
	}

	public PGApplicationConfiguration(Path path) throws PGApplicationException {
		this(path.toFile());
	}

	public URI getPath() {
		return URI.create("file:///"+path);
	}

	public String getPathStr() {
		return this.path;
	}
	
	public static boolean isConf(String name) {
		
		for(String extension: configurationFileExtensions ) {
			if( name.endsWith(extension)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isConf(File file) {
		return isConf(file.getName());
	}

	
}
