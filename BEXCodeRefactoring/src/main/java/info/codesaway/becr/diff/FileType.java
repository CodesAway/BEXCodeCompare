package info.codesaway.becr.diff;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public enum FileType {
	FILE, DIRECTORY, OTHER;

	public static FileType determineFileType(final File file) {
		if (file.isFile()) {
			return FILE;
		} else if (file.isDirectory()) {
			return DIRECTORY;
		} else {
			return OTHER;
		}
	}

	public static FileType determineFileType(final Path path) {
		if (Files.isRegularFile(path)) {
			return FILE;
		} else if (Files.isDirectory(path)) {
			return DIRECTORY;
		} else {
			return OTHER;
		}
	}
}
