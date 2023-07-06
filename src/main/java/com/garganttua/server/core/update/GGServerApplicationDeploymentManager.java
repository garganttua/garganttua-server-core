package com.garganttua.server.core.update;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPopupMenu.Separator;

import com.garganttua.server.core.deployment.artefacts.GGServerApplicationManifest;
import com.garganttua.server.core.deployment.artefacts.GGServerApplicationPlugin;
import com.garganttua.server.core.deployment.artefacts.GGServerApplicationScript;
import com.garganttua.server.core.exceptions.GGServerApplicationDeploymentManagerException;
import com.garganttua.server.core.exceptions.GGServerApplicationException;
import com.garganttua.server.core.execution.GGServerApplicationStage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GGServerApplicationDeploymentManager {

	private String manifestsFolder;
	private String deployFolder;

	private GGServerApplicationDeploymentManager(String manifestsFolder, String deployFolder) {
		this.manifestsFolder = manifestsFolder;
		this.deployFolder = deployFolder;
	}

	public void deploy(GGServerApplicationPlugin plugin, boolean force) throws GGServerApplicationDeploymentManagerException, GGServerApplicationException {
		
		File manifestsFolder = new File(this.manifestsFolder);
		File deployFolder = new File(this.deployFolder);
		
		if( plugin == null ) {
			throw new GGServerApplicationDeploymentManagerException("Plugin file is null");
		}
		if( this.deployFolder == null ) {
			throw new GGServerApplicationDeploymentManagerException("Deploy folder is null");
		}
		if( this.manifestsFolder == null ) {
			throw new GGServerApplicationDeploymentManagerException("Manifest folder is null");
		}
		
		if( !deployFolder.exists() ) {
			throw new GGServerApplicationDeploymentManagerException("Deploy folder does not exist");
		}
		if( !manifestsFolder.exists() ) {
			throw new GGServerApplicationDeploymentManagerException("Manifest folder does not exist");
		}
		
		log.info(" -> Plugin name "+plugin.getFileName());
		
		if( !force ) {
			if( GGServerApplicationPlugin.isDeployed(plugin, deployFolder, deployFolder) ) {
				log.warn(" -> Plugin already deployed");
				GGServerApplicationManifest manifest = GGServerApplicationPlugin.generateManifest(deployFolder, manifestsFolder, plugin);
				log.info("            -> "+manifest.getFileName());
				return ;
			}
		} 
		log.info("        -> Checking dependencies");
		GGServerApplicationPlugin.checkDependencies(plugin);
	
		log.info("        -> Extracting files");
		GGServerApplicationPlugin.extract(deployFolder, plugin);

		List<GGServerApplicationScript> scripts = plugin.getScripts();

		log.info("        -> Executing scripts");
		scripts.forEach(s -> {
			GGServerApplicationScript.exec(GGServerApplicationStage.DEPLOYMENT, s);			
		});
		
		log.info("        -> Generating manifest");
		GGServerApplicationManifest manifest = GGServerApplicationPlugin.generateManifest(deployFolder, manifestsFolder, plugin);
		
		log.info("            -> "+manifest.getFileName());
		return ;
		
	}

	public static GGServerApplicationDeploymentManager init(String manifestsFolder, String deployFolder) {
		return new GGServerApplicationDeploymentManager(manifestsFolder, deployFolder);
	}

	public void doDeployment() throws GGServerApplicationDeploymentManagerException, GGServerApplicationException {
		log.info("== Pegasus Server Plugins Deployment ==");
		
		File deployFolder = new File(this.deployFolder);
		
		List<String> pluginNames = new ArrayList<String>();
		
		if( !deployFolder.exists() && !deployFolder.isDirectory() ) {
			throw new GGServerApplicationDeploymentManagerException("");
		} else {
			File[] subFiles = deployFolder.listFiles();
			
			for( File subFile: subFiles) {
				if( GGServerApplicationPlugin.isPlugin(subFile) ) {
					GGServerApplicationPlugin plugin = new GGServerApplicationPlugin(subFile);
					this.deploy(plugin, false);
					pluginNames.add(plugin.getFileName().substring(0, plugin.getFileName().length()-4));
				}
			}
		}
		
		File[] subFiles = deployFolder.listFiles();
		
		for( File subFile: subFiles) {
			if( subFile.isDirectory() ) {
				boolean found = false;
				
				for( String name: pluginNames ) {
					if( name.equals(subFile.getName())) {
						found = true;
						break;
					}
				}
				
				if( !found ) {
					log.info(" -> Deleting folder "+subFile.getAbsolutePath());
//					if( !this.deleteDirectory(subFile) ) {
//						throw new GGServerApplicationDeploymentManagerException("Cannot delete folder "+subFile.getAbsolutePath());
//					}
				}
			}
		}
		
		log.info("=======================================");
	}
	
	boolean deleteDirectory(File directoryToBeDeleted) {
	    File[] allContents = directoryToBeDeleted.listFiles();
	    if (allContents != null) {
	        for (File file : allContents) {
	            this.deleteDirectory(file);
	        }
	    }
	    return directoryToBeDeleted.delete();
	}

	public void undeploy(GGServerApplicationPlugin plugin) throws GGServerApplicationDeploymentManagerException, GGServerApplicationException {
		log.info("== Pegasus Server Plugin Undeployment ==");
		File manifestsFolder = new File(this.manifestsFolder);
		File deployFolder = new File(this.deployFolder);
		
		if( this.deployFolder == null ) {
			throw new GGServerApplicationDeploymentManagerException("Deploy folder is null");
		}
		
		if( this.manifestsFolder == null ) {
			throw new GGServerApplicationDeploymentManagerException("Manifest folder is null");
		}
		log.info(" -> Plugin name "+plugin.getFileName());
		
		if( GGServerApplicationPlugin.isDeployed(plugin, deployFolder, manifestsFolder) ) {
			log.info("        -> Deleting manifest");
			GGServerApplicationManifest manifest = plugin.getManifest(manifestsFolder);
			
			File manifestFile = manifest.getFile();
			if( !manifestFile.delete() ) {
				throw new GGServerApplicationDeploymentManagerException("Cannot delete file "+manifest.getFileName());
			}
			
			log.info("        -> Deleting plugin archive");
			File pluginFile = plugin.getPluginFile();
			if( !pluginFile.delete() ) {
				throw new GGServerApplicationDeploymentManagerException("Cannot delete file "+plugin.getFileName());
			}
			
		} else {
			log.warn(" -> Plugin not deployed");
		}
		log.info("=======================================");
	}
}
