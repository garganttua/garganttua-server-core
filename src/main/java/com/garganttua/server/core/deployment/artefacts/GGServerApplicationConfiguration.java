package com.garganttua.server.core.deployment.artefacts;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;

import com.garganttua.server.core.exceptions.GGServerApplicationException;

import lombok.Getter;

public class GGServerApplicationConfiguration {

	private String path;
	@Getter
	private String fileName;
	private String fileExtension;
	@Getter
	private File file;
	
	public static String[] configurationFileExtensions;

	public GGServerApplicationConfiguration(File file) throws GGServerApplicationException {
		this.file = file;
		if (file == null || !file.exists()) {
			throw new GGServerApplicationException("Configuration file is null or does not exist");
		}
		
		this.fileName = file.getName();
		this.fileExtension = this.fileName.split("\\.")[this.fileName.split("\\.").length-1];
		
		if( !isConf(this.fileName) ) {
			throw new GGServerApplicationException("Configuration file "+this.file.getPath().toString()+" is not a valid file. Should be with extension .properties");
		}
		
		this.path = file.getAbsolutePath();
	}
	
	public GGServerApplicationConfiguration(String string) throws GGServerApplicationException {
		this(new File(string));
	}

	public GGServerApplicationConfiguration(Path path) throws GGServerApplicationException {
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
