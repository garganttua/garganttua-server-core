package com.garganttua.server.core.deployment.artefacts;

import java.io.File;

import com.garganttua.server.core.exceptions.GGServerApplicationException;

import lombok.Getter;

@Getter
public class GGServerApplicationLib {

	private File file;
	private String fileName;
	private String fileExtension;

	public GGServerApplicationLib(String string) throws GGServerApplicationException {
		this(new File(string));
	}

	public GGServerApplicationLib(File file) throws GGServerApplicationException {
		this.file = file;
		if (file == null || !file.exists()) {
			throw new GGServerApplicationException("Plugin file is null or does not exist");
		}

		this.fileName = file.getName();
		this.fileExtension = this.fileName.split("\\.")[this.fileName.split("\\.").length - 1];

		if (!this.fileExtension.equals("gge") && !this.fileExtension.equals("jar")) {
			throw new GGServerApplicationException("Plugin file " + this.file.getPath().toString()
					+ " is not a valid file. Should be with extension .gge");
		}
	}

	public static boolean isLib(String name) {
		if( name.endsWith(".gge") || name.endsWith(".jar")) {
			return true;
		}
		return false;
	}

}
