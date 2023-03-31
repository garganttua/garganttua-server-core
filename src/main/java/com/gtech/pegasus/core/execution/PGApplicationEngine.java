package com.gtech.pegasus.core.execution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
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
import org.update4j.Property;
import org.update4j.UpdateOptions;
import org.update4j.UpdateOptions.ArchiveUpdateOptions;
import org.update4j.inject.InjectSource;
import org.update4j.inject.Injectable;

import com.gtech.pegasus.core.deployment.artefacts.PGApplicationConfiguration;
import com.gtech.pegasus.core.deployment.artefacts.PGApplicationLib;
import com.gtech.pegasus.core.deployment.artefacts.PGApplicationManifest;
import com.gtech.pegasus.core.deployment.artefacts.PGApplicationPlugin;
import com.gtech.pegasus.core.exceptions.PGApplicationDeploymentManagerException;
import com.gtech.pegasus.core.exceptions.PGApplicationEngineException;
import com.gtech.pegasus.core.exceptions.PGApplicationException;
import com.gtech.pegasus.core.services.PGServiceCommandRight;
import com.gtech.pegasus.core.services.PGServiceException;
import com.gtech.pegasus.core.services.PGServicePriority;
import com.gtech.pegasus.core.services.PGServiceStatus;
import com.gtech.pegasus.core.update.PGApplicationDeploymentManager;
import com.gtech.pegasus.core.update.PGApplicationUpdateHandler;

import lombok.Getter;
import lombok.extern.java.Log;

@Log
public class PGApplicationEngine implements IPGApplicationEngine {
	
	public static final String PEGASUS_SYSTEM_PROPERTY_CONFIGURATION_LOCATIONS = "pegasus.configuration.locations";

	public static final String PEGASUS_ARGUMENT_CONFIGURATIONS = "--pegasus-configurations";

	private String workDirectory;

	private Map<String, FileMetadata> files = new HashMap<String, FileMetadata>();

	private PGApplicationDeploymentManager deploymentManager;

	private Map<String, String> properties = new HashMap<String, String>();

	@Getter
	private PGApplicationManifest mainManifest;

	@Getter
	private PGApplicationManifest fullManifest;

	@Getter
	private List<String> manifests = new ArrayList<String>();

	private List<PGApplicationPlugin> plugins = new ArrayList<PGApplicationPlugin>();

	private PGServiceStatus status = PGServiceStatus.flushed;

	private Configuration config;

	private String[] arguments;

	private Injectable inject;

	private List<IPGApplicationEngineShutdownHandler> shutdownHandler = new ArrayList<IPGApplicationEngineShutdownHandler>();

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

	private PGApplicationEngine(PGApplicationDeploymentManager deploymentManager, String workDirectory)
			throws PGApplicationException, PGApplicationEngineException {
		this.deploymentManager = deploymentManager;
		this.workDirectory = workDirectory;
	}

	public static PGApplicationEngine init(PGApplicationDeploymentManager deploy, String workDirectory)
			throws PGApplicationException, PGApplicationEngineException {
		return new PGApplicationEngine(deploy, workDirectory);
	}

	@Override
	public void addManifest(PGApplicationManifest manifest, boolean isMainManifest) {
		Object[] manifest_ = {manifest, isMainManifest};
		this.manifestsToRead.add(manifest_);
	}

	private void doReadManifest(PGApplicationManifest manifest, boolean isMainManifest) throws PGApplicationEngineException {
		try {
			if (isMainManifest) {
				this.mainManifest = manifest;
			}

			this.manifests.add(manifest.getPathStr());

			Configuration configuration = Configuration.read(Files.newBufferedReader(Paths.get(manifest.getPathStr())));

			Path zipPath = Paths.get(this.workDirectory + File.separator
					+ manifest.getFileName().substring(0, manifest.getFileName().length() - 4) + ".zip");

			ArchiveUpdateOptions options = UpdateOptions.archive(zipPath);

			PGApplicationUpdateHandler updateHandler = new PGApplicationUpdateHandler();
			options.updateHandler(updateHandler);

			configuration.update(options);

			if (updateHandler.isNeedInstall())
				Archive.read(zipPath).install();

			List<FileMetadata> files = new ArrayList<FileMetadata>(configuration.getFiles());
			List<FileMetadata> filesToBeRemoved = new ArrayList<FileMetadata>();

			files.forEach(f -> {
				if (PGApplicationManifest.isManifest(f.getPath().toFile())) {
					try {
						filesToBeRemoved.add(f);
						this.addManifest(new PGApplicationManifest(f.getPath()), false);
					} catch ( PGApplicationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (PGApplicationPlugin.isPlugin(f.getPath().toFile())) {
					try {
						PGApplicationPlugin plugin = new PGApplicationPlugin(f.getPath().toFile());
						this.plugins.add(plugin);
					} catch (PGApplicationException e) {
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

	private void doReadFolder(String path, boolean recursive, boolean isDeployFolder) throws PGApplicationEngineException {
		File file = new File(path);

		if (!file.exists() && !file.isDirectory()) {
			throw new PGApplicationEngineException(path + " is not a directory or does not exist");
		}

		File[] subFiles = file.listFiles();

		for (File subFile : subFiles) {
			if (isDeployFolder && (PGApplicationPlugin.isPlugin(subFile) || PGApplicationConfiguration.isConf(subFile))) {
				Builder builder = Configuration.builder();
				builder.basePath("${user.dir}");
				builder.baseUri("${user.dir}");
				Reference reference = FileMetadata.readFrom(subFile.getAbsolutePath());

				builder.file(reference);

				Configuration configuration = builder.build();

				List<FileMetadata> files = new ArrayList<FileMetadata>(configuration.getFiles());
				this.addFiles(files);
				if( PGApplicationPlugin.isPlugin(subFile) ) {
					try {
						PGApplicationPlugin plugin = new PGApplicationPlugin(subFile);
						this.plugins.add(plugin);
					} catch (PGApplicationException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				continue;
			} else if (isDeployFolder && PGApplicationManifest.isManifest(subFile) ) {
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

	private void doReadFile(File file) {
		Builder builder = Configuration.builder();
		builder.basePath("${user.dir}");
		builder.baseUri("${user.dir}");
		Reference reference = FileMetadata.readFrom(file.getAbsolutePath());

		builder.file(reference);

		Configuration configuration = builder.build();

		List<FileMetadata> files = new ArrayList<FileMetadata>(configuration.getFiles());

		List<FileMetadata> filesToBeRemoved = new ArrayList<FileMetadata>();

		files.forEach(f -> {
			if (PGApplicationManifest.isManifest(f.getPath().toFile())) {
				try {
					filesToBeRemoved.add(f);
					this.addManifest(new PGApplicationManifest(f.getPath()), false);
				} catch (PGApplicationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		files.removeAll(filesToBeRemoved);
		this.addFiles(files);
	}

	public PGApplicationManifest generateManifest(String manifestName, boolean overloadLauncher, String clazz) throws PGApplicationEngineException, PGApplicationException {

		if( this.fullManifest == null ) {
			
			this.foldersToRead.forEach(f -> {
				try {
					this.doReadFolder(((String) f[0]), ((boolean) f[1]), ((boolean) f[2]));
				} catch (PGApplicationEngineException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			
			this.manifestsToRead.forEach(m -> {
				try {
					this.doReadManifest(((PGApplicationManifest) m[0]), ((boolean) m[1]));
				} catch (PGApplicationEngineException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
		
			Builder builder = Configuration.builder();
	
			builder.basePath("${user.dir}");
			builder.baseUri("${user.dir}");
			
	
			this.files.forEach((s, f) -> {
				File file = new File(f.getPath().toString());
				Reference metadata = FileMetadata.readFrom(file.getAbsolutePath());
				if (PGApplicationLib.isLib(file.getAbsolutePath())
						|| PGApplicationConfiguration.isConf(file.getAbsolutePath())) {
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
				throw new PGApplicationEngineException(e);
			}
	
			String filePath = null;
	
			if (manifestName == null) {
				filePath = this.workDirectory + File.separator + UUID.randomUUID().toString() + "."+PGApplicationManifest.PEGASUS_MANIFEST_EXTENSION;
			} else {
				filePath = manifestName;
			}
	
			try {
				BufferedWriter bwriter = new BufferedWriter(new FileWriter(filePath));
				bwriter.write(writer.toString());
				bwriter.close();
			} catch (IOException e) {
				throw new PGApplicationEngineException(e);
			}
	
			this.fullManifest = new PGApplicationManifest(filePath);
		}

		return this.fullManifest;
	}

	@Override
	public List<PGApplicationConfiguration> getConfigurations() throws PGApplicationException {

		List<PGApplicationConfiguration> configurations = new ArrayList<PGApplicationConfiguration>();

		for (FileMetadata meta : this.files.values()) {
			if (PGApplicationConfiguration.isConf(meta.getPath().toString())) {
				configurations.add(new PGApplicationConfiguration(meta.getPath()));
			}
		}

		return configurations;
	}

	public PGApplicationManifest generateManifest() throws PGApplicationEngineException, PGApplicationException {
		return this.generateManifest(null);
	}

	public PGApplicationManifest generateManifest(String manifestName)
			throws PGApplicationEngineException, PGApplicationException {
		return this.generateManifest(manifestName, this.launcherClass!=null, this.launcherClass);
	}

	@Override
	public List<PGApplicationPlugin> getPlugins() throws PGApplicationException {
		List<PGApplicationPlugin> plugins = new ArrayList<PGApplicationPlugin>(this.plugins);

		// TODO add plugins that are not installed, but .peg is present in deploy folder
		return plugins;
	}

	private void doDeployments() throws PGApplicationDeploymentManagerException, PGApplicationException {
		this.deploymentManager.doDeployment();
	}
	
	public void undeploy(PGApplicationPlugin plugin) throws PGApplicationDeploymentManagerException, PGApplicationException {
		this.deploymentManager.undeploy(plugin);
	}

	@Override
	public String getName() {
		return "pegasus-application-engine";
	}

	@Override
	public PGServiceStatus getStatus() {
		return this.status;
	}

	@Override
	public PGServicePriority getPriority() {
		return PGServicePriority.system;
	}

	@Override
	public void start(PGServiceCommandRight right) throws PGServiceException {
		this.status = PGServiceStatus.starting;
		
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
			
			list.add(PEGASUS_ARGUMENT_CONFIGURATIONS);
			list.add(this.getConfigurationLocation(this.getConfigurations()));
			
			System.setProperty(PEGASUS_SYSTEM_PROPERTY_CONFIGURATION_LOCATIONS, this.getConfigurationLocation(this.getConfigurations()));

			this.config = org.update4j.Configuration.read(Files.newBufferedReader(Paths.get(this.fullManifest.getPathStr())));
			this.inject = new Injectable() {
				@InjectSource(target = "args")
				List<String> args = list;
			};
			this.config.launch(inject);
			
		} catch (PGApplicationEngineException| IllegalStateException | PGApplicationException | IOException e) {
			this.surveyThread.interrupt();
			throw new PGServiceException(e);
		} catch (PGApplicationDeploymentManagerException e) {
			this.surveyThread.interrupt();
			throw new PGServiceException(e);
		} 
		this.status = PGServiceStatus.running;
	}
	
	private String getConfigurationLocation(List<PGApplicationConfiguration> configurations) {
		StringBuilder sb = new StringBuilder();
		int i = 1;
		for( PGApplicationConfiguration configuration: configurations ) {
			sb.append(configuration.getPathStr());
			if( i != configurations.size() ) {
				sb.append(",");
				i++;
			}
		}
		return sb.toString();
	}

	@Override
	public void stop(PGServiceCommandRight right) throws PGServiceException {
		this.status = PGServiceStatus.stopping;
		
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
		
		this.status = PGServiceStatus.stopped;
	}

	@Override
	public void init(PGServiceCommandRight right, String[] arguments) throws PGServiceException {
		this.status = PGServiceStatus.initializing;
		this.arguments = arguments;
		this.status = PGServiceStatus.initialized;
	}

	@Override
	public void flush(PGServiceCommandRight right) throws PGServiceException {
		this.status = PGServiceStatus.flushing;
		this.manifests.clear();
		this.purgeManifestsToRead();
		this.plugins.clear();
		this.files.clear();
		this.manifests.clear();
		this.mainManifest = null;
		this.fullManifest = null;
		this.status = PGServiceStatus.flushed;
	}
	
	private void purgeManifestsToRead() {
		PGApplicationManifest manifestToKeep = null;
		for( Object[] manifest: this.manifestsToRead) {
			if( (boolean) manifest[1] ) {
				manifestToKeep = (PGApplicationManifest) manifest[0];
				break;
			}
		}
		this.manifestsToRead.clear();
		if( manifestToKeep != null ) {
			this.addManifest(manifestToKeep, true);
		}
	}

	@Override
	public void kill() throws PGServiceException {
		this.surveyThread.interrupt();
		this.stop(PGServiceCommandRight.system);	
	}

	@Override
	public void restart(PGServiceCommandRight right, String[] arguments) throws PGServiceException {
		Thread t = new Thread(){
			@Override
			public void run() {
				try {
					PGApplicationEngine.this.stop(right);
					PGApplicationEngine.this.flush(right);
					PGApplicationEngine.this.init(right, arguments);
					PGApplicationEngine.this.start(right);
				} catch (PGServiceException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		t.start();
	}

	@Override
	public void registerShutdownHandler(IPGApplicationEngineShutdownHandler shutdownHandler) {
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
