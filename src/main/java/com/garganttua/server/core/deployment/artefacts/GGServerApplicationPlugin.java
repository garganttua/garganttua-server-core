package com.garganttua.server.core.deployment.artefacts;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.update4j.Configuration;
import org.update4j.Configuration.Builder;

import com.garganttua.server.core.exceptions.GGServerApplicationException;

import org.update4j.FileMetadata;

import lombok.Getter;

@Getter
public class GGServerApplicationPlugin {
	
	public static final String PEGASUS_PLUGIN_EXTENSION = "peg";

	private File pluginFile;
	private String fileName;
	private String fileExtension;
	
	public GGServerApplicationPlugin(File pluginFile) throws GGServerApplicationException {
		this.pluginFile = pluginFile;
		if (pluginFile == null || !pluginFile.exists()) {
			throw new GGServerApplicationException("Plugin file is null or does not exist");
		}

		this.fileName = pluginFile.getName();
		this.fileExtension = this.fileName.split("\\.")[this.fileName.split("\\.").length - 1];

		if (!this.fileExtension.equals("peg")) {
			throw new GGServerApplicationException("Plugin file " + this.pluginFile.getPath().toString()
					+ " is not a valid file. Should be with extension "+GGServerApplicationPlugin.PEGASUS_PLUGIN_EXTENSION);
		}
	}

	public GGServerApplicationPluginInfos getInfos(File deployFolder) throws GGServerApplicationException {
		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(this.pluginFile));
			ZipEntry zipEntry = zis.getNextEntry();

			while (zipEntry != null) {

				if( GGServerApplicationPluginInfos.isPluginInfos(zipEntry.getName()) ) {
					zis.closeEntry();
					zis.close();
					return new GGServerApplicationPluginInfos(new File(deployFolder.getPath().toString() + File.separator + this.fileName.substring(0, this.fileName.length() - 4) + File.separator + zipEntry.getName()));
				}
				
				zipEntry = zis.getNextEntry();
			}
			zis.closeEntry();
			zis.close();
		} catch (IOException e) {
			throw new GGServerApplicationException("Unable to extract plugin", e);
		} finally {
			
		}

		return null;
	}

	public static void extract(File deployFolder, GGServerApplicationPlugin plugin) throws GGServerApplicationException {
		byte[] buffer = new byte[1024];
		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(plugin.pluginFile));
			ZipEntry zipEntry = zis.getNextEntry();

			String unzipFolder = deployFolder.getPath().toString() + File.separator
					+ plugin.fileName.substring(0, plugin.fileName.length() - 4);

			while (zipEntry != null) {

				File newFile = newFile(new File(unzipFolder), zipEntry);
				if (zipEntry.isDirectory()) {
					if (!newFile.isDirectory() && !newFile.mkdirs()) {
						zis.closeEntry();
						zis.close();
						throw new IOException("Failed to create directory " + newFile);
					}
				} else {
					// fix for Windows-created archives
					File parent = newFile.getParentFile();
					if (!parent.isDirectory() && !parent.mkdirs()) {
						zis.closeEntry();
						zis.close();
						throw new IOException("Failed to create directory " + parent);
					}

					// write file content
					FileOutputStream fos = new FileOutputStream(newFile);
					int len;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
					fos.close();
				}
				zipEntry = zis.getNextEntry();
			}
			zis.closeEntry();
			zis.close();
		} catch (IOException e) {
			throw new GGServerApplicationException("Unable to extract plugin", e);
		}
	}

	private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
		File destFile = new File(destinationDir, zipEntry.getName());

		String destDirPath = destinationDir.getCanonicalPath();
		String destFilePath = destFile.getCanonicalPath();

		if (!destFilePath.startsWith(destDirPath + File.separator)) {
			throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
		}

		return destFile;
	}

	public List<GGServerApplicationLib> getLibs(File deployFolder) throws GGServerApplicationException {
		ArrayList<GGServerApplicationLib> libs = new ArrayList<GGServerApplicationLib>();
		byte[] buffer = new byte[1024];
		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(this.pluginFile));
			ZipEntry zipEntry = zis.getNextEntry();

			while (zipEntry != null) {
							
				if( GGServerApplicationLib.isLib(zipEntry.getName()) ) {
					libs.add(new GGServerApplicationLib(new File(deployFolder.getPath().toString() + File.separator + this.fileName.substring(0, this.fileName.length() - 4) + File.separator + zipEntry.getName())));
				}
				zipEntry = zis.getNextEntry();
			}
			zis.closeEntry();
			zis.close();
		} catch (IOException e) {
			throw new GGServerApplicationException("Unable to extract plugin", e);
		}

		return libs;
	}

	public List<GGServerApplicationConfiguration> getConfigurations(File deployFolder) throws GGServerApplicationException {
		ArrayList<GGServerApplicationConfiguration> configurations = new ArrayList<GGServerApplicationConfiguration>();
		byte[] buffer = new byte[1024];
		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(this.pluginFile));
			ZipEntry zipEntry = zis.getNextEntry();

			while (zipEntry != null) {	
				if( !zipEntry.isDirectory() && zipEntry.getName().startsWith("conf") ) {
					configurations.add(new GGServerApplicationConfiguration(new File(deployFolder.getPath().toString() + File.separator + this.fileName.substring(0, this.fileName.length() - 4) + File.separator + zipEntry.getName())));
				}
				zipEntry = zis.getNextEntry();
			}
			zis.closeEntry();
			zis.close();
		} catch (IOException e) {
			throw new GGServerApplicationException("Unable to extract plugin", e);
		} catch (GGServerApplicationException e) {
			throw new GGServerApplicationException("Unable to extract plugin", e);
		} 
		return configurations;
	}

	public List<GGServerApplicationManifest> getManifests() {
		ArrayList<GGServerApplicationManifest> manifests = new ArrayList<GGServerApplicationManifest>();
		return manifests;
	}

	public List<GGServerApplicationScript> getScripts() {
		ArrayList<GGServerApplicationScript> scripts = new ArrayList<GGServerApplicationScript>();
		return scripts;
	}


	public static void checkDependencies(GGServerApplicationPlugin plugin) throws GGServerApplicationException {
		// TODO Auto-generated method stub

	}

	public static GGServerApplicationManifest generateManifest(File deployFolder, File manifestFolder, GGServerApplicationPlugin plugin) throws GGServerApplicationException {

		List<GGServerApplicationLib> libs = plugin.getLibs(deployFolder);
		List<GGServerApplicationConfiguration> configurations = plugin.getConfigurations(deployFolder);
		
		Builder config = Configuration.builder();
		config.basePath(deployFolder.getAbsolutePath()+File.separator+plugin.fileName.substring(0, plugin.fileName.length()-4));
		config.baseUri(deployFolder.getAbsolutePath()+File.separator+plugin.fileName.substring(0, plugin.fileName.length()-4));
		
		libs.forEach(l -> {
			config.file(FileMetadata.readFrom(l.getFile().getAbsolutePath()).classpath(true));
		});

		configurations.forEach(c -> {
			config.file(FileMetadata.readFrom(c.getFile().getAbsolutePath()).classpath(true));
		});
		
		StringWriter writer = new StringWriter();
		try {
			config.build().write(writer);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String manifestContent = writer.toString();
		
		manifestFolder.mkdirs();
		
		String filePath = manifestFolder.getAbsolutePath()+File.separator+plugin.fileName.substring(0, plugin.fileName.length()-4)+"."+GGServerApplicationManifest.PEGASUS_MANIFEST_EXTENSION;

	    try {
	    	BufferedWriter bwriter = new BufferedWriter(new FileWriter(filePath));
			bwriter.write(manifestContent);
			bwriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return new GGServerApplicationManifest(filePath);
	}

	public static boolean isPlugin(File file) {
		if( file.getPath().toString().endsWith(GGServerApplicationPlugin.PEGASUS_PLUGIN_EXTENSION) ) {
			return true;
		}
		return false;
	}

	public static boolean isDeployed(GGServerApplicationPlugin plugin, File deployFolder, File manifestFolder) {
		String fileName = plugin.fileName.substring(0, plugin.fileName.length()-4);
		boolean dirExist = false; 
		boolean manifestExist = false; 
		
		for( File f: deployFolder.listFiles() ) {
			if( f.getName().equals(fileName) && f.isDirectory() ) {
				dirExist = true;
			}
		}
		
		for( File f: manifestFolder.listFiles() ) {
			if( f.getName().equals(fileName+"."+GGServerApplicationManifest.PEGASUS_MANIFEST_EXTENSION) && f.isFile() ) {
				manifestExist = true;
			}
		}
		
		boolean result = dirExist & manifestExist;
		
		return result;
	}

	public String getVersion() {
		return "1.0.0";
	}

	public GGServerPluginStatus getStatus(File deployFolder, File manifestFolder) {
		GGServerPluginStatus status = GGServerPluginStatus.not_deployed;
		String fileName = this.fileName.substring(0, this.fileName.length()-4);
		boolean dirExist = false; 
		boolean manifestExist = false; 
		boolean pegFileExist = false;
		
		for( File f: deployFolder.listFiles() ) {
			if( f.getName().equals(fileName) && f.isDirectory() ) {
				dirExist = true;
			}
		}
		
		for( File f: manifestFolder.listFiles() ) {
			if( f.getName().equals(fileName+"."+GGServerApplicationManifest.PEGASUS_MANIFEST_EXTENSION) && f.isFile() ) {
				manifestExist = true;
			}
		}
		
		if( this.pluginFile.exists() ) {
			pegFileExist = true;
		}
		
		if( dirExist & manifestExist ) {
			status = GGServerPluginStatus.deployed;
		}
		
		if( !dirExist & !manifestExist ) {
			status = GGServerPluginStatus.not_deployed;
		}
		
		if( dirExist & !manifestExist & !pegFileExist ) {
			status = GGServerPluginStatus.undeployed;
		}
		
		return status;
	}

	public GGServerApplicationManifest getManifest(File manifestsFolder) throws GGServerApplicationException {
		String manifestFileName = this.fileName.substring(0, this.fileName.length()-4)+"."+GGServerApplicationManifest.PEGASUS_MANIFEST_EXTENSION;
		
		for( File f: manifestsFolder.listFiles() ) {
			if( f.getName().equals(manifestFileName) && GGServerApplicationManifest.isManifest(f) ) {
				return new GGServerApplicationManifest(f);
			}
		}
		return null;
	}

}
