package com.garganttua.server.core.execution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.update4j.Archive;
import org.update4j.Configuration;
import org.update4j.Configuration.Builder;
import org.update4j.DynamicClassLoader;
import org.update4j.FileMetadata;
import org.update4j.FileMetadata.Reference;
import org.update4j.OS;
import org.update4j.Property;
import org.update4j.UpdateOptions;
import org.update4j.UpdateOptions.ArchiveUpdateOptions;
import org.update4j.inject.InjectSource;
import org.update4j.inject.Injectable;

import com.garganttua.server.core.deployment.artefacts.GGServerApplicationConfiguration;
import com.garganttua.server.core.deployment.artefacts.GGServerApplicationLib;
import com.garganttua.server.core.deployment.artefacts.GGServerApplicationManifest;
import com.garganttua.server.core.deployment.artefacts.GGServerApplicationPlugin;
import com.garganttua.server.core.exceptions.GGServerApplicationDeploymentManagerException;
import com.garganttua.server.core.exceptions.GGServerApplicationEngineException;
import com.garganttua.server.core.exceptions.GGServerApplicationException;
import com.garganttua.server.core.services.GGServerServiceCommandRight;
import com.garganttua.server.core.services.GGServerServiceException;
import com.garganttua.server.core.services.GGServerServicePriority;
import com.garganttua.server.core.services.GGServerServiceStatus;
import com.garganttua.server.core.update.GGServerApplicationDeploymentManager;
import com.garganttua.server.core.update.GGServerApplicationUpdateHandler;

import lombok.Getter;
import lombok.extern.java.Log;

@Log
public class GGServerApplicationEngine implements IGGServerApplicationEngine {
	
	public static final String GARGANTTUA_SERVER_SYSTEM_PROPERTY_CONFIGURATION_LOCATIONS = "garganttua.server.configuration.locations";

	public static final String GARGANTTUA_SERVER_ARGUMENT_CONFIGURATIONS = "--garganttua-server-configurations";

	private String workDirectory;

	private Map<String, FileMetadata> files = new HashMap<String, FileMetadata>();

	private GGServerApplicationDeploymentManager deploymentManager;

	private Map<String, String> properties = new HashMap<String, String>();

	@Getter
	private GGServerApplicationManifest mainManifest;

	@Getter
	private GGServerApplicationManifest fullManifest;

	@Getter
	private List<String> manifests = new ArrayList<String>();

	private List<GGServerApplicationPlugin> plugins = new ArrayList<GGServerApplicationPlugin>();

	private GGServerServiceStatus status = GGServerServiceStatus.flushed;

	private Configuration config;

	private String[] arguments;

	private Injectable inject;

	private List<IGGServerApplicationEngineShutdownHandler> shutdownHandler = new ArrayList<IGGServerApplicationEngineShutdownHandler>();

	private List<Object[]> foldersToRead = new ArrayList<Object[]>();

	private List<Object[]> manifestsToRead = new ArrayList<Object[]>();

	private String launcherClass;

	private Thread surveyThread = new Thread() {
		@Override
		public void run() {
			while(true) {
				try {
					sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	};

	private GGServerApplicationEngine(GGServerApplicationDeploymentManager deploymentManager, String workDirectory)
			throws GGServerApplicationException, GGServerApplicationEngineException {
		this.deploymentManager = deploymentManager;
		this.workDirectory = workDirectory;
	}

	public static GGServerApplicationEngine init(GGServerApplicationDeploymentManager deploy, String workDirectory)
			throws GGServerApplicationException, GGServerApplicationEngineException {
		return new GGServerApplicationEngine(deploy, workDirectory);
	}

	@Override
	public void addManifest(GGServerApplicationManifest manifest, boolean isMainManifest) {
		Object[] manifest_ = {manifest, isMainManifest};
		this.manifestsToRead.add(manifest_);
	}

	private void doReadManifest(GGServerApplicationManifest manifest, boolean isMainManifest) throws GGServerApplicationEngineException {
		try {
			if (isMainManifest) {
				this.mainManifest = manifest;
			}

			this.manifests.add(manifest.getPathStr());

			Configuration configuration = Configuration.read(Files.newBufferedReader(Paths.get(manifest.getPathStr())));

			Path zipPath = Paths.get(this.workDirectory + File.separator + manifest.getFileName().substring(0, manifest.getFileName().length() - 4) + ".zip");

			ArchiveUpdateOptions options = UpdateOptions.archive(zipPath);

			GGServerApplicationUpdateHandler updateHandler = new GGServerApplicationUpdateHandler();
			options.updateHandler(updateHandler);

			configuration.update(options);

			if (updateHandler.isNeedInstall())
				Archive.read(zipPath).install();

			List<FileMetadata> files = new ArrayList<FileMetadata>(configuration.getFiles());
			List<FileMetadata> filesToBeRemoved = new ArrayList<FileMetadata>();

			files.forEach(f -> {
				if (GGServerApplicationManifest.isManifest(f.getPath().toFile())) {
					try {
						filesToBeRemoved.add(f);
						this.addManifest(new GGServerApplicationManifest(f.getPath()), false);
					} catch ( GGServerApplicationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (GGServerApplicationPlugin.isPlugin(f.getPath().toFile())) {
					try {
						GGServerApplicationPlugin plugin = new GGServerApplicationPlugin(f.getPath().toFile());
						this.plugins.add(plugin);
					} catch (GGServerApplicationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});

			files.removeAll(filesToBeRemoved);
			this.addProperties(configuration.getProperties());
			this.addFiles(files);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void addProperties(List<Property> properties) {
		for (Property prop : properties) {
			log.info("Adding property " + prop.getKey() + " : " + prop.getValue());
			this.properties.put(prop.getKey(), prop.getValue());
		}
	}

	private void addFiles(List<FileMetadata> files) {
		for (FileMetadata file : files) {
			log.info("Loading file " + file.getPath().toString());
			this.files.put(file.getPath().toString(), file);
		}
	}

	@Override
	public void readFolder(String path, boolean recursive, boolean isDeployFolder) {
		Object[] folder = {path, recursive, isDeployFolder};
		this.foldersToRead.add(folder);
	}

	private void doReadFolder(String path, boolean recursive, boolean isDeployFolder) throws GGServerApplicationEngineException, MalformedURLException, URISyntaxException {
		File file = new File(path);

		if (!file.exists() && !file.isDirectory()) {
			throw new GGServerApplicationEngineException(path + " is not a directory or does not exist");
		}

		File[] subFiles = file.listFiles();

		for (File subFile : subFiles) {
			if (isDeployFolder && (GGServerApplicationPlugin.isPlugin(subFile) || GGServerApplicationConfiguration.isConf(subFile))) {
				Builder builder = Configuration.builder();
				builder.basePath("file://${user.dir}");
				builder.baseUri("file://${user.dir}");
				Reference reference = FileMetadata.readFrom(subFile.getAbsolutePath());

				builder.file(reference);

				Configuration configuration = builder.build();

				List<FileMetadata> files = new ArrayList<FileMetadata>(configuration.getFiles());
				this.addFiles(files);
				if( GGServerApplicationPlugin.isPlugin(subFile) ) {
					try {
						GGServerApplicationPlugin plugin = new GGServerApplicationPlugin(subFile);
						this.plugins.add(plugin);
					} catch (GGServerApplicationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				continue;
			} else if (isDeployFolder && GGServerApplicationManifest.isManifest(subFile) ) {
				this.doReadFile(subFile);
				continue;
			} else if (isDeployFolder) {
				continue;
			} else {
				if (subFile.isDirectory() && recursive) {
					this.readFolder(subFile.getAbsolutePath(), recursive, recursive);
				} else if (!subFile.isDirectory()) {
					this.doReadFile(subFile);
				}
			}
		}
	}

	private void doReadFile(File file) throws URISyntaxException, MalformedURLException {
		Builder builder = Configuration.builder();
		builder.basePath("file://${user.dir}");
		builder.baseUri("file://${user.dir}");

		Reference reference = FileMetadata.readFrom(file.getAbsolutePath()).os(OS.LINUX);

		builder.file(reference);

		Configuration configuration = builder.build();

		List<FileMetadata> files = new ArrayList<FileMetadata>(configuration.getFiles());

		List<FileMetadata> filesToBeRemoved = new ArrayList<FileMetadata>();

		files.forEach(f -> {
			if (GGServerApplicationManifest.isManifest(f.getPath().toFile())) {
				try {
					filesToBeRemoved.add(f);
					this.addManifest(new GGServerApplicationManifest(f.getPath()), false);
				} catch (GGServerApplicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		files.removeAll(filesToBeRemoved);
		this.addFiles(files);
	}

	public GGServerApplicationManifest generateManifest(String manifestName, boolean overloadLauncher, String clazz) throws GGServerApplicationEngineException, GGServerApplicationException {

		if( this.fullManifest == null ) {
			
			this.foldersToRead.forEach(f -> {
				try {
					this.doReadFolder(((String) f[0]), ((boolean) f[1]), ((boolean) f[2]));
				} catch (GGServerApplicationEngineException | MalformedURLException | URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			
			this.manifestsToRead.forEach(m -> {
				try {
					this.doReadManifest(((GGServerApplicationManifest) m[0]), ((boolean) m[1]));
				} catch (GGServerApplicationEngineException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		
			Builder builder = Configuration.builder();
	
			builder.basePath("file://${user.dir}");
			builder.baseUri("file://${user.dir}");
			
			this.files.forEach((s, f) -> {
				File file = new File(f.getPath().toString());
				Reference metadata = FileMetadata.readFrom(file.getAbsolutePath());
				if (GGServerApplicationLib.isLib(file.getAbsolutePath())
						|| GGServerApplicationConfiguration.isConf(file.getAbsolutePath())) {
					metadata.classpath(true);
					metadata.modulepath(true);
				}
				builder.file(metadata);
			});
	
			this.properties.forEach((k, v) -> {
				Property p = new Property(k, v);
				builder.property(p);
			});
	
			if (overloadLauncher) {
				Property prop = null;
				for (Property p : builder.getProperties()) {
					if (p.getKey().equals("default.launcher.main.class")) {
						prop = p;
					}
				}
				if (prop != null) {
					builder.getProperties().remove(prop);
				}
				builder.property("default.launcher.main.class", clazz);
			}
	
			StringWriter writer = new StringWriter();
			try {
				builder.build().write(writer);
			} catch (IOException e) {
				throw new GGServerApplicationEngineException(e);
			}
	
			String filePath = null;
	
			if (manifestName == null) {
				filePath = this.workDirectory + File.separator + UUID.randomUUID().toString() + "."+GGServerApplicationManifest.GARGANTTUA_SERVER_MANIFEST_EXTENSION;
			} else {
				filePath = manifestName;
			}
	
			try {
				BufferedWriter bwriter = new BufferedWriter(new FileWriter(filePath));
				bwriter.write(writer.toString());
				bwriter.close();
			} catch (IOException e) {
				throw new GGServerApplicationEngineException(e);
			}
	
			this.fullManifest = new GGServerApplicationManifest(filePath);
		}

		return this.fullManifest;
	}

	@Override
	public List<GGServerApplicationConfiguration> getConfigurations() throws GGServerApplicationException {

		List<GGServerApplicationConfiguration> configurations = new ArrayList<GGServerApplicationConfiguration>();

		for (FileMetadata meta : this.files.values()) {
			if (GGServerApplicationConfiguration.isConf(meta.getPath().toString())) {
				configurations.add(new GGServerApplicationConfiguration(meta.getPath()));
			}
		}

		return configurations;
	}

	public GGServerApplicationManifest generateManifest() throws GGServerApplicationEngineException, GGServerApplicationException {
		return this.generateManifest(null);
	}

	public GGServerApplicationManifest generateManifest(String manifestName)
			throws GGServerApplicationEngineException, GGServerApplicationException {
		return this.generateManifest(manifestName, this.launcherClass!=null, this.launcherClass);
	}

	@Override
	public List<GGServerApplicationPlugin> getPlugins() throws GGServerApplicationException {
		List<GGServerApplicationPlugin> plugins = new ArrayList<GGServerApplicationPlugin>(this.plugins);

		// TODO add plugins that are not installed, but .peg is present in deploy folder
		return plugins;
	}

	private void doDeployments() throws GGServerApplicationDeploymentManagerException, GGServerApplicationException {
		this.deploymentManager.doDeployment();
	}
	
	public void undeploy(GGServerApplicationPlugin plugin) throws GGServerApplicationDeploymentManagerException, GGServerApplicationException {
		this.deploymentManager.undeploy(plugin);
		this.plugins.remove(plugin);
	}

	@Override
	public String getName() {
		return "gargantta-server-application-engine";
	}

	@Override
	public GGServerServiceStatus getStatus() {
		return this.status;
	}

	@Override
	public GGServerServicePriority getPriority() {
		return GGServerServicePriority.system;
	}

	@Override
	public void start(GGServerServiceCommandRight right) throws GGServerServiceException {
		this.status = GGServerServiceStatus.starting;
		
		List<String> list = new ArrayList<String>();
		for( String arg: this.arguments ) {
			list.add(arg);
		}
		
		if( !this.surveyThread.isAlive() ) {
			this.surveyThread.start();
		}
		
		try {
			this.doDeployments();
			this.generateManifest();
			
			list.add(GARGANTTUA_SERVER_ARGUMENT_CONFIGURATIONS);
			list.add(this.getConfigurationLocation(this.getConfigurations()));
			
			System.setProperty(GARGANTTUA_SERVER_SYSTEM_PROPERTY_CONFIGURATION_LOCATIONS, this.getConfigurationLocation(this.getConfigurations()));

			this.config = org.update4j.Configuration.read(Files.newBufferedReader(Paths.get(this.fullManifest.getPathStr())));
			this.inject = new Injectable() {
				@InjectSource(target = "args")
				List<String> args = list;
			};
			this.config.launch(inject);
			
		} catch (GGServerApplicationEngineException| IllegalStateException | GGServerApplicationException | IOException e) {
			this.surveyThread.interrupt();
			throw new GGServerServiceException(e);
		} catch (GGServerApplicationDeploymentManagerException e) {
			this.surveyThread.interrupt();
			throw new GGServerServiceException(e);
		} 
		this.status = GGServerServiceStatus.running;
	}
	
	private String getConfigurationLocation(List<GGServerApplicationConfiguration> configurations) {
		StringBuilder sb = new StringBuilder();
		int i = 1;
		for( GGServerApplicationConfiguration configuration: configurations ) {
			sb.append(configuration.getPathStr());
			if( i != configurations.size() ) {
				sb.append(",");
				i++;
			}
		}
		return sb.toString();
	}

	@Override
	public void stop(GGServerServiceCommandRight right) throws GGServerServiceException {
		this.status = GGServerServiceStatus.stopping;
		
		this.shutdownHandler.forEach(h -> {
			h.handleShutdown();
		});
		
		DynamicClassLoader cl = ((DynamicClassLoader) ClassLoader.getSystemClassLoader());
		try {
			cl.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.status = GGServerServiceStatus.stopped;
	}

	@Override
	public void init(GGServerServiceCommandRight right, String[] arguments) throws GGServerServiceException {
		this.status = GGServerServiceStatus.initializing;
		this.arguments = arguments;
		this.status = GGServerServiceStatus.initialized;
	}

	@Override
	public void flush(GGServerServiceCommandRight right) throws GGServerServiceException {
		this.status = GGServerServiceStatus.flushing;
		this.manifests.clear();
		this.purgeManifestsToRead();
		this.plugins.clear();
		this.files.clear();
		this.manifests.clear();
		this.mainManifest = null;
		this.fullManifest = null;
		this.status = GGServerServiceStatus.flushed;
	}
	
	private void purgeManifestsToRead() {
		GGServerApplicationManifest manifestToKeep = null;
		for( Object[] manifest: this.manifestsToRead) {
			if( (boolean) manifest[1] ) {
				manifestToKeep = (GGServerApplicationManifest) manifest[0];
				break;
			}
		}
		this.manifestsToRead.clear();
		if( manifestToKeep != null ) {
			this.addManifest(manifestToKeep, true);
		}
	}

	@Override
	public void kill() throws GGServerServiceException {
		this.surveyThread.interrupt();
		this.stop(GGServerServiceCommandRight.system);	
	}

	@Override
	public void restart(GGServerServiceCommandRight right, String[] arguments) throws GGServerServiceException {
		Thread t = new Thread(){
			@Override
			public void run() {
				try {
					GGServerApplicationEngine.this.stop(right);
					GGServerApplicationEngine.this.flush(right);
					GGServerApplicationEngine.this.init(right, arguments);
					GGServerApplicationEngine.this.start(right);
				} catch (GGServerServiceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		t.start();
	}

	@Override
	public void registerShutdownHandler(IGGServerApplicationEngineShutdownHandler shutdownHandler) {
		this.shutdownHandler.add(shutdownHandler);
	}

	@Override
	public void setArguments(List<String> arguments) {
		this.arguments = arguments.toArray(new String[arguments.size()]);
	}

	public void setLauncherClass(String launcherClass) {
		this.launcherClass = launcherClass;
	}

}
