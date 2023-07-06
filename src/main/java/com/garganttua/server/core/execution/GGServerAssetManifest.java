package com.garganttua.server.core.execution;

import java.util.List;

import com.garganttua.server.core.deployment.artefacts.GGServerApplicationManifest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GGServerAssetManifest {
	
	private String homeDirectory;
	
	private List<String> loadedManifests;
	
	private GGServerApplicationManifest coreManifest;
	
	private GGServerApplicationManifest loadedManifest;

}
