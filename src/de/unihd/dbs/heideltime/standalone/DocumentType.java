package de.unihd.dbs.heideltime.standalone;

/**
 * Legacy constants to transition to the enum at {@link de.unihd.dbs.uima.annotator.heideltime.DocumentType}.
 *
 * Because we cannot subclass enums, this will not be binary compatible,
 * but at least we get compile time compatibility.
 *
 * @author Erich Schubert
 */
@Deprecated
public final class DocumentType {
	/** Use {@link de.unihd.dbs.uima.annotator.heideltime.DocumentType.NARRATIVE} instead. */
	@Deprecated
	public static final de.unihd.dbs.uima.annotator.heideltime.DocumentType NARRATIVES = de.unihd.dbs.uima.annotator.heideltime.DocumentType.NARRATIVE;

	/** Use {@link de.unihd.dbs.uima.annotator.heideltime.DocumentType.NEWS} instead. */
	@Deprecated
	public static final de.unihd.dbs.uima.annotator.heideltime.DocumentType NEWS = de.unihd.dbs.uima.annotator.heideltime.DocumentType.NEWS;

	/** Use {@link de.unihd.dbs.uima.annotator.heideltime.DocumentType.COLLOQUIAL} instead. */
	@Deprecated
	public static final de.unihd.dbs.uima.annotator.heideltime.DocumentType COLLOQUIAL = de.unihd.dbs.uima.annotator.heideltime.DocumentType.COLLOQUIAL;

	/** Use {@link de.unihd.dbs.uima.annotator.heideltime.DocumentType.SCIENTIFIC} instead. */
	@Deprecated
	public static final de.unihd.dbs.uima.annotator.heideltime.DocumentType SCIENTIFIC = de.unihd.dbs.uima.annotator.heideltime.DocumentType.SCIENTIFIC;
}
