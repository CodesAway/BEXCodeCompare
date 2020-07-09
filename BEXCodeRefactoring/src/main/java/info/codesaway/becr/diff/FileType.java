package info.codesaway.becr.diff;

import java.io.File;

public enum FileType {
	FILE, DIRECTORY, OTHER;

	public static FileType determineFileType(final File file) {
		if (file.isDirectory()) {
			return DIRECTORY;
		} else if (file.isFile()) {
			return FILE;
		} else {
			return OTHER;
		}
	}
}
