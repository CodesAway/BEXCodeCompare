package info.codesaway.becr.parsing;

import java.io.File;

public class ProjectFile {
	private final String project;
	private final File file;

	public String getPath() {
		return this.file.getPath();
	}

	public String getName() {
		return this.file.getName();
	}

	public ProjectFile(final String project, final File file) {
		this.project = project;
		this.file = file;
	}

	public String getProject() {
		return this.project;
	}

	public File getFile() {
		return this.file;
	}

	@Override
	public String toString() {
		return "(" + this.getProject() + ") " + this.getFile();
	}
}
