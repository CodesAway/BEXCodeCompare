# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

**NOTE**: Initially, the version number for BEX Code Compare and the Eclipse plugin will be in sync. At some point, they will diverge, which will be indicated in the below change log. When they diverage, the BEX Code Compare version number will be what takes precidency, since this will also be the version for the Maven artifact. The Eclipse plugin changelog will always indicate which version of BEX Code Compare is used.

## [Unreleased]
### Added
* Refactoring for enhanced for loop recognizes changing from iterator for loop to enhanced for loop
### Changed
* Eclipse plugin has better handling of ignaring blank lines when ignoring whitespace in the compare
* Enhanced the compare of the existing refactorings
### Fixed
* Eclipse plugin compare editor scrolling issues (think got them, but if not file a new issue and attach code files, before and after, where you see the issue)
### Removed
* Eclipse plugin no longer matches moved lines (Eclipse didn't handle these changes well)

## [0.1.3] - 06/25/2020
### Added
*	Improved scrolling in compare window (still wonky, but much better than before)
*	Recognize semicolon removal as a non-important change (there's a save action to remove redundant semicolons)

## [0.1.2] - 06/24/2020
### Added
*	Pressing enter in BEX Code Compare view jumps to code in compare window (still wonky, but much better than before)
*	Enhanced the compare with better handling of common refactorings

## [0.1.1] - 06/23/2020
### Added
* Initial version
