package com.garganttua.server.core.update;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.update4j.FileMetadata;
import org.update4j.service.DefaultUpdateHandler;

import lombok.Getter;

public class GGServerApplicationUpdateHandler extends DefaultUpdateHandler {
	
	@Getter
	private boolean needInstall = false;
	private List<FileMetadata> downloadedFiles = new ArrayList<FileMetadata>();
	
	@Override
	public void startDownloads() throws Throwable {
		// TODO Auto-generated method stub
		super.startDownloads();
	}
	
	@Override
	public void startDownloadFile(FileMetadata file) throws Throwable {
		// TODO Auto-generated method stub
		super.startDownloadFile(file);
	}
	
	@Override
	public void doneDownloadFile(FileMetadata file, Path tempFile) throws Throwable {
		// TODO Auto-generated method stub
		super.doneDownloadFile(file, tempFile);
	}
	
	@Override
	public void updateDownloadProgress(float frac) throws Throwable {
		// TODO Auto-generated method stub
		super.updateDownloadProgress(frac);
	}
	
	@Override
	public void updateDownloadFileProgress(FileMetadata file, float frac) throws Throwable {
		// TODO Auto-generated method stub
		super.updateDownloadFileProgress(file, frac);
	}
	
	@Override
	public void updateCheckUpdatesProgress(float frac) throws Throwable {
		// TODO Auto-generated method stub
		super.updateCheckUpdatesProgress(frac);
	}
	
	@Override
	public void doneCheckUpdateFile(FileMetadata file, boolean requires) throws Throwable {
		// TODO Auto-generated method stub
		this.needInstall = this.needInstall | requires;
		this.downloadedFiles.add(file);
		super.doneCheckUpdateFile(file, requires);
	}

	public List<FileMetadata> getFiles() {
		return this.downloadedFiles ;
	}

}
