package info.codesaway.bexcodecompare.diff;

// XXX: instead of using enum, type using interface
// Create enum that implements interface
// See if can make generic so can pass lambda function (create SAM interface - Single Abstract Method)
public enum SubstitutionType {
	SIMPLE, REFACTORING, ANY;
}
