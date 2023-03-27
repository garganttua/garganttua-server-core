package com.gtech.pegasus.core.execution;

import java.util.List;

import com.gtech.pegasus.core.deployment.artefacts.PGApplicationManifest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PGAssetManifest {
	
	private String homeDirectory;
	
	private List<String> loadedManifests;
	
	private PGApplicationManifest coreManifest;
	
	private PGApplicationManifest loadedManifest;

}
