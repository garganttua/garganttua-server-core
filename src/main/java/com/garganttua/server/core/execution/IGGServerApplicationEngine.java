package com.garganttua.server.core.execution;

import java.util.List;

import com.garganttua.server.core.deployment.artefacts.GGServerApplicationConfiguration;
import com.garganttua.server.core.deployment.artefacts.GGServerApplicationManifest;
import com.garganttua.server.core.deployment.artefacts.GGServerApplicationPlugin;
import com.garganttua.server.core.exceptions.GGServerApplicationDeploymentManagerException;
import com.garganttua.server.core.exceptions.GGServerApplicationEngineException;
import com.garganttua.server.core.exceptions.GGServerApplicationException;
import com.garganttua.server.core.services.IGGServerService;
import com.garganttua.server.core.services.GGServerServiceException;

public interface IGGServerApplicationEngine extends IGGServerService {

	public void addManifest( GGServerApplicationManifest manifest, boolean isMainManifest );
	
	public void readFolder(String path, boolean recursive, boolean isDeployFolder);
	
	public void setArguments(List<String> arguments);

	public GGServerApplicationManifest generateManifest() throws GGServerApplicationEngineException, GGServerApplicationException;

	public List<GGServerApplicationPlugin> getPlugins() throws GGServerApplicationException;

	public GGServerApplicationManifest getFullManifest();
	
	public GGServerApplicationManifest getMainManifest();
	
	public void registerShutdownHandler(IGGServerApplicationEngineShutdownHandler shutdownHandler);
	
	public List<GGServerApplicationConfiguration> getConfigurations() throws GGServerApplicationException;

	public void kill() throws GGServerServiceException;

	public void undeploy(GGServerApplicationPlugin plugin) throws GGServerApplicationDeploymentManagerException, GGServerApplicationException;

}
