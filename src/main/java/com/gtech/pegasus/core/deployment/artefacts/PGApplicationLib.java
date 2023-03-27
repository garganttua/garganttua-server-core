package com.gtech.pegasus.core.deployment.artefacts;

import java.io.File;

import com.gtech.pegasus.core.exceptions.PGApplicationException;

import lombok.Getter;

@Getter
public class PGApplicationLib {

	private File file;
	private String fileName;
	private String fileExtension;

	public PGApplicationLib(String string) throws PGApplicationException {
		this(new File(string));
	}

	public PGApplicationLib(File file) throws PGApplicationException {
		this.file = file;
		if (file == null || !file.exists()) {
			throw new PGApplicationException("Plugin file is null or does not exist");
		}

		this.fileName = file.getName();
		this.fileExtension = this.fileName.split("\\.")[this.fileName.split("\\.").length - 1];

		if (!this.fileExtension.equals("gge") && !this.fileExtension.equals("jar")) {
			throw new PGApplicationException("Plugin file " + this.file.getPath().toString()
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
