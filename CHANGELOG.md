# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

**NOTE**: The below change log references the version of the BEX library, since this is the version for the Maven artifact. The below change log will indicate when the Eclipse plugin is updated and its changes; this will be listed after the BEX library changes.

## [Unreleased]

## BECR Examples [0.2.1] - 2020-07-21
* Fixed issues with long project names used as Excel sheet names ([#37](https://github.com/CodesAway/BEXCodeCompare/issues/37))
* Fixed issues with large amount of text in Excel report  ([#36](https://github.com/CodesAway/BEXCodeCompare/issues/36)

## [0.4.0] - 2020-07-18

**NOTE**: Starting with BEX Code Compare 0.4.0, the version number for BEX Code Compare and the Eclipse plugin diverged. Since no changes were required in the Eclipse plugin, it remains at version 0.3.0 (using BEX 0.3.0).

### Added
* BEXPair helper methods
  * mirror- returns a new BEXPair with the sides swapped (#29)
  * Various test methods which test one side then the mirror
    * testLeftMirror
    * testRightMirror
    * testLeftRightMirror
    * testRightLeftMirror

* BEXSides.BEX_SIDES - BEXPair<BEXSide> containing LEFT / RIGHT; this is used to help reduce some of the redundant code in the BEX library

* IntPair method toBEXPair

* CompareDirectories settings
  * substitutionGroups - allows specifying the order which substitutions are applied (#25)
  * excludeLCSMinSubstitution (#25)
  * shouldCheckPath (#24)

* CompareDirectoriesOption enum
  * EXCLUDE_DEFAULT_SUBSTITUTIONS
  * Other options can be added over time as they are requested and implemented

* DiffEdit has additional constructors

* BEXPairs utility class with static methods

* BEXUtilities helper methods

* ParsingUtilities.createASTs method

* TestUtilities to help simplify common tests

### Changed
* Renamed class BEXPair to BEXPairValue
* Renamed interface BEXPairCore to BEXPair
* This follow's Java's naming convention where the Interface name is short and used in most places (for parameters and return values), such as List. Whereas, the Class name is more descriptive, such as ArrayList

* SubstitutionType method **accept** now uses BEXPair instead of passing left / right values separately (#28)
  * This allows easily getting the normalized text
  * BEXPair<String> normalizedText = checkPair.map(normalizedTexts::get);
  * Used new mirror methods to reduce the repeated code

* Rename RefactorEnchanedForLoop to EnhancedForLoopRefactoring (#27)

* Changed method **from(final Function<BEXSide, T> function)**  instead of constructor for BEXPairValue (Java compiler was complaining about ambiguity in certain cases)

* DiffHelper
  * Several methods now return void instead of List, since the return value wasn't needed
  * Lots of cleanup and refactoring (#30)

* Refactoring of names to make more consistent

### Fixed
* CompareDirectoriesVisitor detects enum constants (#31)

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
* Some odd behavior related to 3-way compares with repository ([#22](https://github.com/CodesAway/BEXCodeCompare/issues/22))

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
