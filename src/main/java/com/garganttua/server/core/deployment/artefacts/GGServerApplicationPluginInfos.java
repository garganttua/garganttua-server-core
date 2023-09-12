package com.garganttua.server.core.deployment.artefacts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import com.garganttua.server.core.exceptions.GGServerApplicationException;

import lombok.Getter;


/**
 * 
 * @author Jeremy Colombet
 * 
 * 
 * 
Descriptor-Version: 
Created-By: 
Issuer: 
Plugin-Title: 
Plugin-Version: 
Required-Plugins: 
Required-Libs: 
 *
 */
@Getter
public class GGServerApplicationPluginInfos {
	
	private File pluginInfosFile;
	private String fileName;
	private String fileExtension;
	
	private String descriptionVersion;
	private String createdBy;
	private String issuer; 
	private String pluginTitle;
	private String pluginVersion;
	private String requiredPlugins;
	private String requiredLibs;
	

	public GGServerApplicationPluginInfos(File pluginInfosFile) throws GGServerApplicationException {
		this.pluginInfosFile = pluginInfosFile;
		if (pluginInfosFile == null || !pluginInfosFile.exists()) {
			throw new GGServerApplicationException("Plugin file is null or does not exist");
		}

		this.fileName = pluginInfosFile.getName();
		this.fileExtension = this.fileName.split("\\.")[this.fileName.split("\\.").length - 1];

		if (!this.fileExtension.equals("ggd")) {
			throw new GGServerApplicationException("PluginInfos file " + this.pluginInfosFile.getPath().toString()
					+ " is not a valid file. Should be with extension .ggd");
		}
		
		
		InputStream is;
		try {
			is = new FileInputStream(pluginInfosFile);
			Manifest manifest = new Manifest(is) ;
			
			manifest.getMainAttributes().forEach( (k, v) -> {
	
				switch( k.toString() ) {
				case "Descriptor-Version":
					descriptionVersion = (String) v;
					break;
				case "Created-By":
					createdBy = (String) v;
					break;
				case "Issuer":
					issuer = (String) v;
					break;
				case "Plugin-Title":
					pluginTitle = (String) v;
					break;
				case "Plugin-Version":
					pluginVersion = (String) v;
					break;
				case "Required-Plugins":
					requiredPlugins = (String) v;
					break;
				case "Required-Libs":
					requiredLibs = (String) v;
					break;
				}
				
			});

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static boolean isPluginInfos(String name) {
		if( name.endsWith(".ggd") ) {
			return true;
		}
		return false;
	}
	
	public static boolean isPluginInfos(File file) {
		if( file.getPath().toString().endsWith(".ggd") ) {
			return true;
		}
		return false;
	}
}
