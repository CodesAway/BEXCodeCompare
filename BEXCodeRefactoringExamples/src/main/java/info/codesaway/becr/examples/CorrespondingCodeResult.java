package info.codesaway.becr.examples;

import java.util.List;

import com.google.common.collect.BiMap;

import info.codesaway.becr.parsing.CodeInfoWithLineInfo;

public final class CorrespondingCodeResult {
	// TODO: see if can refactor to not use BiMap (so can put into BECR instead of BECR examples
	private final BiMap<CodeInfoWithLineInfo, CodeInfoWithLineInfo> codeBlocksMap;
	private final List<CodeInfoWithLineInfo> deletedBlocks;
	private final List<CodeInfoWithLineInfo> addedBlocks;

	public CorrespondingCodeResult(final BiMap<CodeInfoWithLineInfo, CodeInfoWithLineInfo> codeBlocksMap,
			final List<CodeInfoWithLineInfo> deletedBlocks, final List<CodeInfoWithLineInfo> addedBlocks) {
		this.codeBlocksMap = codeBlocksMap;
		this.deletedBlocks = deletedBlocks;
		this.addedBlocks = addedBlocks;
	}

	public BiMap<CodeInfoWithLineInfo, CodeInfoWithLineInfo> getCodeBlocksMap() {
		return this.codeBlocksMap;
	}

	public List<CodeInfoWithLineInfo> getDeletedBlocks() {
		return this.deletedBlocks;
	}

	public List<CodeInfoWithLineInfo> getAddedBlocks() {
		return this.addedBlocks;
	}
}
