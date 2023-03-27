package com.gtech.pegasus.core.update;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPopupMenu.Separator;

import com.gtech.pegasus.core.deployment.artefacts.PGApplicationManifest;
import com.gtech.pegasus.core.deployment.artefacts.PGApplicationPlugin;
import com.gtech.pegasus.core.deployment.artefacts.PGApplicationScript;
import com.gtech.pegasus.core.exceptions.PGApplicationDeploymentManagerException;
import com.gtech.pegasus.core.exceptions.PGApplicationException;
import com.gtech.pegasus.core.execution.PGApplicationStage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PGApplicationDeploymentManager {

	private String manifestsFolder;
	private String deployFolder;

	private PGApplicationDeploymentManager(String manifestsFolder, String deployFolder) {
		this.manifestsFolder = manifestsFolder;
		this.deployFolder = deployFolder;
	}

	public void deploy(PGApplicationPlugin plugin, boolean force) throws PGApplicationDeploymentManagerException, PGApplicationException {
		
		File manifestsFolder = new File(this.manifestsFolder);
		File deployFolder = new File(this.deployFolder);
		
		if( plugin == null ) {
			throw new PGApplicationDeploymentManagerException("Plugin file is null");
		}
		if( this.deployFolder == null ) {
			throw new PGApplicationDeploymentManagerException("Deploy folder is null");
		}
		if( this.manifestsFolder == null ) {
			throw new PGApplicationDeploymentManagerException("Manifest folder is null");
		}
		
		if( !deployFolder.exists() ) {
			throw new PGApplicationDeploymentManagerException("Deploy folder does not exist");
		}
		if( !manifestsFolder.exists() ) {
			throw new PGApplicationDeploymentManagerException("Manifest folder does not exist");
		}
		
		log.info(" -> Plugin name "+plugin.getFileName());
		
		if( !force ) {
			if( PGApplicationPlugin.isDeployed(plugin, deployFolder, deployFolder) ) {
				log.warn(" -> Plugin already deployed");
				PGApplicationManifest manifest = PGApplicationPlugin.generateManifest(deployFolder, manifestsFolder, plugin);
				log.info("            -> "+manifest.getFileName());
				return ;
			}
		} 
		log.info("        -> Checking dependencies");
		PGApplicationPlugin.checkDependencies(plugin);
	
		log.info("        -> Extracting files");
		PGApplicationPlugin.extract(deployFolder, plugin);

		List<PGApplicationScript> scripts = plugin.getScripts();

		log.info("        -> Executing scripts");
		scripts.forEach(s -> {
			PGApplicationScript.exec(PGApplicationStage.DEPLOYMENT, s);			
		});
		
		log.info("        -> Generating manifest");
		PGApplicationManifest manifest = PGApplicationPlugin.generateManifest(deployFolder, manifestsFolder, plugin);
		
		log.info("            -> "+manifest.getFileName());
		return ;
		
	}

	public static PGApplicationDeploymentManager init(String manifestsFolder, String deployFolder) {
		return new PGApplicationDeploymentManager(manifestsFolder, deployFolder);
	}

	public void doDeployment() throws PGApplicationDeploymentManagerException, PGApplicationException {
		log.info("== Pegasus Server Plugins Deployment ==");
		
		File deployFolder = new File(this.deployFolder);
		
		List<String> pluginNames = new ArrayList<String>();
		
		if( !deployFolder.exists() && !deployFolder.isDirectory() ) {
			throw new PGApplicationDeploymentManagerException("");
		} else {
			File[] subFiles = deployFolder.listFiles();
			
			for( File subFile: subFiles) {
				if( PGApplicationPlugin.isPlugin(subFile) ) {
					PGApplicationPlugin plugin = new PGApplicationPlugin(subFile);
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
//						throw new PGApplicationDeploymentManagerException("Cannot delete folder "+subFile.getAbsolutePath());
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

	public void undeploy(PGApplicationPlugin plugin) throws PGApplicationDeploymentManagerException, PGApplicationException {
		log.info("== Pegasus Server Plugin Undeployment ==");
		File manifestsFolder = new File(this.manifestsFolder);
		File deployFolder = new File(this.deployFolder);
		
		if( this.deployFolder == null ) {
			throw new PGApplicationDeploymentManagerException("Deploy folder is null");
		}
		
		if( this.manifestsFolder == null ) {
			throw new PGApplicationDeploymentManagerException("Manifest folder is null");
		}
		log.info(" -> Plugin name "+plugin.getFileName());
		
		if( PGApplicationPlugin.isDeployed(plugin, deployFolder, manifestsFolder) ) {
			log.info("        -> Deleting manifest");
			PGApplicationManifest manifest = plugin.getManifest(manifestsFolder);
			
			File manifestFile = manifest.getFile();
			if( !manifestFile.delete() ) {
				throw new PGApplicationDeploymentManagerException("Cannot delete file "+manifest.getFileName());
			}
			
			log.info("        -> Deleting plugin archive");
			File pluginFile = plugin.getPluginFile();
			if( !pluginFile.delete() ) {
				throw new PGApplicationDeploymentManagerException("Cannot delete file "+plugin.getFileName());
			}
			
		} else {
			log.warn(" -> Plugin not deployed");
		}
		log.info("=======================================");
	}
}
