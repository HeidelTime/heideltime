package de.unihd.dbs.uima.annotator.alllanguagestokenizer;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.jcas.JCas;

import de.unihd.dbs.uima.types.heideltime.Sentence;
import de.unihd.dbs.uima.types.heideltime.Token;


public class AllLanguagesTokenizer extends JCasAnnotator_ImplBase {
	private String PChar = "\\[¿¡\\{\\(\\`\"‚„†‡‹‘’“”•–—›'";
	private String FChar = "\\]\\}\\'\\`\"\\),;:\\!\\?\\%‚„…†‡‰‹‘’“”•–—›";
	private String FClitic = "";
	private String PClitic = "";
	
	public AllLanguagesTokenizer() {
		FClitic += "'(s|re|ve|d|m|em|ll)|n't";
		PClitic += "[dD][ae]ll'|[nN]ell'|[Aa]ll'|[lLDd]'|[Ss]ull'|[Qq]uest'|[Uu]n'|[Ss]enz'|[Tt]utt'";
		PClitic += "|[dcjlmnstDCJLNMST]'|[Qq]u'|[Jj]usqu'|[Ll]orsqu'";
		FClitic += "|-t-elles?|-t-ils?|-t-on|-ce|-elles?|-ils?|-je|-la|-les?|-leur|-lui|-mmes?|-m'|-moi|-nous|-on|-toi|-tu|-t'|-vous|-en|-y|-ci|-l";
		FClitic += "|-la|-las|-lo|-los|-nos";
	}

	public void process(JCas jcas) throws AnalysisEngineProcessException {
		tokenize(jcas);
		
		sentenceTokenize(jcas);
	}

	
	public List<Token> tokenize(JCas jcas) {
		StringBuilder outBuf = new StringBuilder();
		
		for(String text : jcas.getDocumentText().split("\n")) {
			// replace newlines and tab characters with blanks
			text = text.replaceAll("[\r\n\t]", " ");
			// replace blanks within SGML tags
			text = text.replaceAll("(<[^<> ]*) ([^<>]*>)", "$1\377$2");
			// replace whitespace with a special character
			text = text.replaceAll("[\\u2000-\\u200A \\u202F\\u205F\\u3000\\u00A0\\u1680\\u180E]", "\376");
			// restore SGML tags
			text = text.replaceAll("\377", " ");
			text = text.replaceAll("\376", "\377");
			// prepare SGML-Tags for tokenization
			text = text.replaceAll("(<[^<>]*>)", "\377$1\377");
			text = text.replaceAll("^\377", "");
			text = text.replaceAll("\377$", "");
			text = text.replaceAll("\377\377\377*", "\377");
			
			String[] texts = text.split("\377");
			
			for(String line : texts) {
				if(line.matches("^<.*>$")) {
					// SGML tag
					outBuf.append(line + "\n");
					continue;
				}
				// add a blank at the beginning and the end of each segment
				line = " " + line + " ";

				// insert missing blanks after punctuation
				line = line.replaceAll("\\.\\.\\.", " ... ");
				line = line.replaceAll("([;\\!\\?])([^ ])", "$1 $2");
				line = line.replaceAll("([.,:])([^ 0-9.])", "$1 $2");

				String[] lines = line.split(" ");

				for(String token : lines) {
					// remove some whitespaces that \s doesn't catch
					if(token.equals(""))
						continue;

					String suffix = "";

					// separate punctuation and parentheses from words
					boolean finished = false;
					Matcher m;
					do {
						finished = true;

						// cut off preceding punctuation
						m = Pattern.compile("^([" + PChar + "])(.)").matcher(token);
						if(m.find()) {
							token = token.replaceAll("^([" + PChar + "])(.)", "$2");
							outBuf.append(m.group(1) + "\n");
							finished = false;
						}

						// cut off trailing punctuation
						m = Pattern.compile("(.)([" + FChar + "])$").matcher(token);
						if(m.find()) {
							token = token.replaceAll("(.)([" + FChar + "])$", "$1");
							suffix = m.group(2) + "\n" + suffix;
							finished = false;
						}

						// cut off trailing periods if punctuation precedes
						m = Pattern.compile("([" + FChar + "])\\.$").matcher(token);
						if(m.find()) {
							token = token.replaceAll("([" + FChar + "])\\.$", "");
							suffix = ".\n" + suffix;

							if(token.equals("")) {
								token = m.group(1);
							} else {
								suffix = m.group(1) + "\n" + suffix;
							}

							finished = false;
						}
					} while(!finished);
					/* TODO:commented out because those are language-specific
						// handle explicitly listed tokens
						if(abbreviations.contains(token)) {
							outBuf.append(token + "\n" + suffix);
							continue;
						}*/

					// abbreviations of the form A. or U.S.A.
					if(token.matches("^([A-Za-z-]\\.)+$")) {
						outBuf.append(token + "\n" + suffix);
						continue;
					}

					// disambiguate periods
					m = Pattern.compile("^(..*)\\.$").matcher(token);
					if(m.matches() && !line.equals("...") 
							/* TODO:commented out because those are language-specific: && !(flags.contains(Flag.GALICIAN) && token.matches("^[0-9]+\\.$"))*/) {
						token = m.group(1);
						suffix = ".\n" + suffix;
						/* TODO:commented out because those are language-specific
							if(abbreviations.contains(token)) {
								outBuf.append(token + "\n" + suffix);
								continue;
							}*/
					}

					// cut off clitics
					while(true) {
						m = Pattern.compile("^(--)(.)").matcher(token);
						if(!m.find())
							break;

						token = token.replaceAll("^(--)(.)", "$2");
						outBuf.append(m.group(1) + "\n");
					}
					if(!PClitic.equals("")) {
						while(true) {
							m = Pattern.compile("^(" + PClitic + ")(.)").matcher(token);
							if(!m.find())
								break;

							token = token.replaceAll("^(" + PClitic + ")(.)", "$2");
							outBuf.append(m.group(1) + "\n");
						}
					}

					while(true) {
						m = Pattern.compile("^(--)(.)").matcher(token);
						if(!m.find())
							break;

						token = token.replaceAll("^(--)(.)", "$1");
						suffix = m.group(2) + "\n" + suffix;
					}
					if(!FClitic.equals("")) {
						while(true) {
							m = Pattern.compile("(.)(" + FClitic + ")$").matcher(token);
							if(!m.find())
								break;

							token = token.replaceAll("(.)(" + FClitic + ")$", "$1");
							suffix = m.group(2) + "\n" + suffix;
						}
					}
					outBuf.append(token + "\n" + suffix);
				}
			}
		}
		
		// find the tokens in the original text and create token annotations
		LinkedList<Token> outList = new LinkedList<Token>();
		String origText = jcas.getDocumentText();
		int origTextOffset = 0;
		
		for(String s : outBuf.toString().split("\n")) {
			int begin = origText.indexOf(s, origTextOffset);
			int end = begin + s.length();
			
			Token t = new Token(jcas);
			t.setBegin(begin);
			t.setPos("");
			t.setEnd(end);
			
			t.addToIndexes();
			
			origTextOffset = t.getEnd();
			
			outList.add(t);
		}
		
		return outList;
	}
	
	public List<Sentence> sentenceTokenize(JCas jcas) {
		List<Sentence> outList = new LinkedList<Sentence>();
		AnnotationIndex<Token> tokens = jcas.getAnnotationIndex(Token.type);
		FSIterator<Token> tokIt = tokens.iterator();
		
		Sentence s = new Sentence(jcas);
		boolean sentenceStarted = false;
		Token tOld = null;
		Token t = null;
		while(tokIt.hasNext()) {
			if (t != null)
				tOld = t;
			t = tokIt.next();
			
			// set sentence beginning
			if(sentenceStarted == false) {
				sentenceStarted = true;
				
				s.setBegin(t.getBegin());
			}
			
			/* detect sentence ends
			 * second character class taken from: http://en.wikipedia.org/wiki/Quotation_mark#Curved_quotes_and_Unicode
			 */
			if(!tokIt.hasNext() ||
					(t.getCoveredText().matches("[.:!\\?]+") && 
							!((tOld != null && tOld.getCoveredText().matches("[\\d]+")) || (jcas.getDocumentText().substring(t.getEnd()).length() > 2 && jcas.getDocumentText().substring(t.getEnd(),t.getEnd()+3).matches(" [A-Z][.-]"))))){
//							((!(tOld.getCoveredText().matches("[\\d]+")))) && (!((jcas.getDocumentText().substring(t.getEnd())).matches("^[\\s]*"))))) {
//					(t.getCoveredText().matches("[.:!\\?]+") && (!(tOld.getCoveredText().matches("[\\d]+"))))) { // das funktioniert ok
				sentenceStarted = false;
				s.setEnd(t.getEnd());

				// check for whether the punctuation mark is followed by a closing quotation mark
				if(tokIt.hasNext()) {
					Token tNext = tokIt.next();
					
					if(tNext.getCoveredText().matches("[»’'\"‛”‟›〞』」﹄＂＇｣﹂]+")) {
						s.setEnd(tNext.getEnd());
					} else {
						tokIt.moveToPrevious();
					}
				}
				
				s.addToIndexes();
				
				outList.add(s);
				
				s = new Sentence(jcas);
			}
		}
		
		return outList;
	}
}
