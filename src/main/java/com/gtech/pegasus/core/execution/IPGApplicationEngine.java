package com.gtech.pegasus.core.execution;

import java.util.List;

import com.gtech.pegasus.core.deployment.artefacts.PGApplicationConfiguration;
import com.gtech.pegasus.core.deployment.artefacts.PGApplicationManifest;
import com.gtech.pegasus.core.deployment.artefacts.PGApplicationPlugin;
import com.gtech.pegasus.core.exceptions.PGApplicationDeploymentManagerException;
import com.gtech.pegasus.core.exceptions.PGApplicationEngineException;
import com.gtech.pegasus.core.exceptions.PGApplicationException;
import com.gtech.pegasus.core.services.IPGService;
import com.gtech.pegasus.core.services.PGServiceException;

public interface IPGApplicationEngine extends IPGService {

	public void addManifest( PGApplicationManifest manifest, boolean isMainManifest );
	
	public void readFolder(String path, boolean recursive, boolean isDeployFolder);
	
	public void setArguments(List<String> arguments);

	public PGApplicationManifest generateManifest() throws PGApplicationEngineException, PGApplicationException;

	public List<PGApplicationPlugin> getPlugins() throws PGApplicationException;

	public PGApplicationManifest getFullManifest();
	
	public PGApplicationManifest getMainManifest();
	
	public void registerShutdownHandler(IPGApplicationEngineShutdownHandler shutdownHandler);
	
	public List<PGApplicationConfiguration> getConfigurations() throws PGApplicationException;

	public void kill() throws PGServiceException;

	public void undeploy(PGApplicationPlugin plugin) throws PGApplicationDeploymentManagerException, PGApplicationException;

}
