package info.codesaway.becr.parsing;

import java.nio.file.Path;

public final class ProjectPath {
	private final String project;
	private final Path path;

	public ProjectPath(final String project, final Path path) {
		this.project = project;
		this.path = path;
	}

	public String getProject() {
		return this.project;
	}

	public Path getPath() {
		return this.path;
	}

	public String getPathname() {
		return this.path.toString();
	}

	public String getName() {
		Path filename = this.path.getFileName();
		return filename != null ? filename.toString() : "";
	}

	@Override
	public String toString() {
		return "(" + this.getProject() + ") " + this.getPath();
	}
}
