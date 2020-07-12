package info.codesaway.becr.diff;

import java.nio.file.Path;

public final class PathChangeInfo {
	private final Path relativePath;
	private final String project;
	private final String directory;
	private final String filenameWithoutExtension;
	private final String extension;
	private final FileType fileType;
	private final PathChangeType pathChangeType;
	private final int differenceCount;
	private final int deltaCount;

	public PathChangeInfo(final Path relativePath, final String project, final String directory,
			final String filenameWithoutExtension,
			final String extension, final FileType fileType,
			final PathChangeType pathChangeType, final int differenceCount, final int deltaCount) {
		this.relativePath = relativePath;
		this.project = project;
		this.directory = directory;
		this.filenameWithoutExtension = filenameWithoutExtension;
		this.extension = extension;
		this.fileType = fileType;
		this.pathChangeType = pathChangeType;
		this.differenceCount = differenceCount;
		this.deltaCount = deltaCount;
	}

	public Path getRelativePath() {
		return this.relativePath;
	}

	public String getProject() {
		return this.project;
	}

	public String getDirectory() {
		return this.directory;
	}

	public String getFilenameWithoutExtension() {
		return this.filenameWithoutExtension;
	}

	public String getExtension() {
		return this.extension;
	}

	public FileType getFileType() {
		return this.fileType;
	}

	public PathChangeType getPathChangeType() {
		return this.pathChangeType;
	}

	public int getDifferenceCount() {
		return this.differenceCount;
	}

	public int getDeltaCount() {
		return this.deltaCount;
	}
}
