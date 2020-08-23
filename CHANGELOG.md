# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

**NOTE**: The below change log references the version of the BEX library, since this is the version for the Maven artifact. The below change log will indicate when the Eclipse plugin is updated and its changes; this will be listed after the BEX library changes.

## [Unreleased]

## [0.11.0] - 2020-08-23

### Added
* Support for parsing SQL
  * Includes support for ensuring BEGIN / END delimiters are balanced in a match
  * Added support for matching custom delimiters in a language (like BEGIN / END for SQL)
  * Method BEXMatchingUtilities.parseSQLTextStates

* MatchingLanguage interface
  * Allows users to define custom language, if a sutable one isn't implemented
  * For example, this would be used to allow writing a parser to match language specific keywords (no plans / need to implement in BEX Matching, but user could implement for their own needs)
  
 * Part of custom delimiters
   * MatchingLanguage methods
     * findStartDelimiter
     * findEndDelimiter
    
   * BEXMatcher now internally tracks the MatchingLanguage, so that custom delimiters can be matched as part of a search
   
   * Internal BEXMatchingState changed to accept collection of delimiters versus a String of brackets
   
   * Enum MatchingDelimiterResult
   * Class MatchingDelimiterState
   * Interface MatchingLanguageSetting
     * No methods to implement
     * Used just to indicate a setting, so can write custom settings (trying to be flexible in design)
     
   * MatchingLanguageOption (implements MatchingLanguageSetting)

* MatchingStateOption interface (implemented by BEXMatchingStateOption)

* BEXMatchingUtilities
  * Method hasText which takes parameter to indicate if text search should be case-insensitive
  * Method hasCaseInsensitiveText

* hashCode / equals method in BEXListPair and BEXMapPair (per SpotBugs warning)

### Changed

* BEXMatchingLanguage renamed **extract** method to **parse**
* BEXMatchingUtilities renamed various **extract** methods to **parse**

* BEXMatchingStateOption renamed MISMATCHED_BRACKETS to MISMATCHED_DELIMITERS

* Changed to use MatchingStateOption interface instead of BEXMatchingStateOption enum
  * BEXMatcher
  * BEXMatchingUtilities
  * BEXString

* BEXString keeps track of MatchingLanguage used when parser (helps BEXMatcher handle custom delimiters)

* Minor tweaks to how BEXPattern caches patterns (ran into corner case with tests that checked for cached patterns)

## [0.10.1] - 2020-08-16
### Fixed
* Matcher will retry if encounter mismatched brackets

## [0.10.0] - 2020-08-15
### Added
* BEXPattern syntax
  * ```:[group\n]``` to match rest of line, including line terminator
  * ```:[group\n$]``` to match rest of line, including line terminator (will also match if last line in text with no line terminator)

* BEXPair interface extends Comparable interface (so elements are comparable, left then right, if the type is comparable
* BEXString substring method overridden to take IntPair (including IntRange)
* BEXPairs.bexPair helper method to create a BEXPair (can use static import)
* IntRange.length method

### Changed
* Renamed methods which reference start/end now refer to as range
  * BEXMatcher
  * BEXMatchResult
  * ASTNodeUtilities

### Fixed
* In BEXMatcher / BEXMatchResult, throw IllegalStateException if try to get match info if a successful match hasn't occurred yet
* IntBEXRange will throw exception if range is invalid (start and end match and exclusive on both ends, such as ```(1, 1)```)

## [0.9.1] - 2020-08-10
### Fixed
* Short-term bug fix for infinite loop in some cases related to mismatched brackets
* A long-term fix is still being investigated (#75)

## [0.9.0] - 2020-08-09
### Added
* Added support for Comby style of regex ```:[groupName~regex]```
* Added some unit tests from Comby (found and fixed several bugs as a result)
* BEXMatcher / BEXMatchResult method entrySet()
* ImmutableIntRangeMap.asMapOfRanges
* Made AbstractImmutableSet public (used by ImmutableIntRangeMap and BEXMatcher)
* BEXUtilities.entry method
  * Creates an immutable entry (used lots in tests)
  * Similar to Java 9 Map.entry method

### Changed
* In regex, added multiline flag by default, so ```^``` and ```$``` match for each separate line
  * This should be more common
  * Can always disable using the (?-m) remove multiline flag option in regex

## [0.8.0] - 2020-08-06
### Added
* ImmutableIntRangeMap
* IntRange / IntBEXRange methods

* BEXPattern
  * Basic caching of BEXPattern (done behind the scenes)
  * Method getThreadLocalMatcher
  * matcher method which takes no arguments (can then call reset to set text)

* BEXMatcher methods
  * toMatchResult

### Changed
* Refactored matching code to use ImmutableIntRangeMap

### Removed
* Code no longer needed after change to use ImmutableIntRangeMap
  * Class BEXMatchingTextState
  * BEXUtilities methods
    * getEntryInRanges
    * hasEntryInRanges

* In BECR, ASTNodeUtilities, removed methods which aren't used (may be added back if a need arises)
    * getCommentRanges
    * findNode

## [0.7.0] - 2020-08-05
### Added
* Initial support for matching JSP
  * Added BEXMatchingLanguage enum
  * Passes function to extract the necessary information (used in BEXString)
  * BEXMatchingUtilities has the methods 
 
* BEXPatternFlag to REQUIRE_SPACE
  * When this flag is passed, spaces in the pattern are always required
  * By default, spaces are usually optional (except in certain circumstancances - to allow matching without regard to exact formatting in the code)

* Improvements to IntBEXRange (can created closed range)
* Method IntRange.canonical to "normalize" the int range

### Changed
* Simplified BEXMatcher code and removed repeated code
* BEXMatchingTextState now implements IntRange

### Fixed
* 0.6.0 didn't have all the code checked in (such as supporting star group to indicate the entire match)
* Whitespace before and after group in match is optional if not next to word character
  * For example, the pattern ```method(:[1] ,  :[2])``` now allows the space before and after the comma to be optional
  * If want to always require space, can use the new flag REQUIRE_SPACE
  * If want just this specific space to be required, can put two spaces in the pattern

## [0.6.0] - 2020-08-03
### Added
* BEXPattern
  * Syntax
    * :[@] to escape @ symbol (rarely needed, mainly useful for BEXPattern.literal
    * :[group:d] to to have group consisting of only digits (similar to :[group:w] which is group consisting of only word characters)
  * In group / get method, can specify group name as "\*" to get the entire match (similar to group 0 for regex) 
  * Methods
    * literal (use to specify literal text in pattern)
    * compile (method that just takes String pattern, makes easier to use in IDE)
    
* BEXMatcher
  * Replacement functionality (similar to what's provided in regex's Matcher class
  * Can specify :[group] for a specific group's value or :[\*] for the entire match's value  
  * Methods
    * replaceAll (including method which takes Function<BEXMatchResult, String>
    * replaceFirst
    * getReplacement
    * appendReplacement / appendTail
    * quoteReplacement (use to specify literal text in replacement)
    * reset

* IntBEXPair / IntBEXRange equals and hashCode method

### Changed
* BEX will contain all code that doesn't require the Eclipse JDT dependency (such as the new structured matching functionality)

* Moved info.codesaway.becr.matching package to info.codesaway.bex.matching
  * Moved from BECR to BEX project
  * Renamed classes to mention BEX instead of BECR
  * Renamed classes to clarify that they were used for matching
  
* Moved IntRange from BECR to BEX project
* Moved functions in BECRUtilities to BEXUtilities

## [0.5.0] - 2020-08-02
### Added
* Structured source matching (adopted some syntax from https://comby.dev)
  * This will be used in a future release to help with refactoring
  * Some syntax comes from Comby, other is new syntax (such as RegEx support)
  * There is no guarantee that Comby and BECR will behave the same with the same syntax

#### Classes and Interfaces
* BECRRange
* IntRange (interface)

* BECRString
* BECRTextState
* BECRStateOption (enum)

* BECRPattern
* BECRPatternFlag (enum)
* BECRMatcher
* BECRMatchResult (interface)

* BECRUtilities
* BECRMatchingUtilities
* ASTNodeUtilities

* BECRGroupMatchSetting (package private)
* BECRState (package private)

* ParsingUtilities.getParser

#### Methods
* IntPair.toIntBEXPair

* MutableIntBEXPair
  * set
  * setLeft
  * setRight

* BEXUtilities
  * isBetween
  * getSubstring
  * getSubSequence

### Changed
* Made some classes final
* Moved method ParsingUtilities.not to BEXUtilities
* Renamed package info.codesaway.becr.examples to info.codesaway.becr.comparedirectories

## BECR Examples [0.2.1] - 2020-07-21
* Fixed issues with long project names used as Excel sheet names ([#37](https://github.com/CodesAway/BEXCodeCompare/issues/37))
* Fixed issues with large amount of text in Excel report  ([#36](https://github.com/CodesAway/BEXCodeCompare/issues/36))

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
