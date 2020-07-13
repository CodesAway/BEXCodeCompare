# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

**NOTE**: Initially, the version number for BEX Code Compare and the Eclipse plugin will be in sync. At some point, they will diverge, which will be indicated in the below change log. When they diverage, the BEX Code Compare version number will be what takes precedence, since this will also be the version for the Maven artifact. The Eclipse plugin changelog will always indicate which version of BEX Code Compare is used.

## [Unreleased]

## [0.3.0] - 2020-07-12
### Added
* BEX Library adds some useful Pair classes to handle left / right pairs which are the same type 
  * This helped simplify lots of the compare code, since there was code that did stuff on the left side and the same stuff on the right side; now, by using these BEX Pair classes, it simplified the code the remove the redundancy
  * Some cool Java 8 stuff is made available
  * **package**: info.codesaway.bex
  * BEXListPair
  * BEXMapPair
  * BEXPair (renamed from LeftRightPair)
  * BEXPairCore (interface with lots of useful **default** methods)
  * BEXSide (renamed from DiffSide)
  * IntBEXPair (renamed from IntLeftRightPair)
  * IntPair (interface with a **default** method)
  * MutableIntBEXPair (renamed from MutableIntLeftRightPair)
  
* DiffUnit gets some useful default methods added to the interface

* Eclipse plugin has option to ignore comments when doing a compare
* Eclipse plugin has option to show both sides of substitution in BEX view

* First release of BECR, pronounced  Beccer; BECR is Be Enhanced Code Refactoring
  * This will contain utilities to help refactor code
  * Included is an example CompareDirectories which compares two directories and creates an Excel report of the differences
    * For non-Java files, does a normal compare
    * For Java files, shows changes per method / field
  
### Changed
* Made classes final
* Renamed DiffType.getTag() to DiffType.getSymbol()

### Fixed
* Some odd behavior related to 3-way compares with repository

## [0.2.0] - 2020-07-04
### Added
* Refactoring for enhanced for loop recognizes changing from iterator for loop to enhanced for loop
### Changed
* Eclipse plugin has better handling of ignaring blank lines when ignoring whitespace in the compare
* Enhanced the compare of the existing refactorings
### Fixed
* Eclipse plugin compare editor scrolling issues (think got them, but if not file a new issue and attach code files, before and after, where you see the issue)
### Removed
* Eclipse plugin no longer matches moved lines (Eclipse didn't handle these changes well)

## [0.1.3] - 2020-06-25
### Added
*	Improved scrolling in compare window (still wonky, but much better than before)
*	Recognize semicolon removal as a non-important change (there's a save action to remove redundant semicolons)

## [0.1.2] - 2020-06-24
### Added
*	Pressing enter in BEX Code Compare view jumps to code in compare window (still wonky, but much better than before)
*	Enhanced the compare with better handling of common refactorings

## [0.1.1] - 2020-06-23
### Added
* Initial version
