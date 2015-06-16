## About HeidelTime
**HeidelTime** is a multilingual, cross-domain temporal tagger developed at the [Database Systems Research Group](http://dbs.ifi.uni-heidelberg.de/) at [Heidelberg University](http://www.uni-heidelberg.de/index_e.html). It extracts temporal expressions from documents and normalizes them according to the TIMEX3 annotation standard. HeidelTime is available as [UIMA](http://uima.apache.org/) annotator and as standalone version.

**HeidelTime** currently understands documents in **13 languages**: English, German, Dutch, Vietnamese, Arabic, Spanish, Italian, French, Chinese, Russian, Croatian, Estonian and Portuguese.

**HeidelTime** distinguishes between **news-style** documents and **narrative-style documents** (e.g., Wikipedia articles) in all languages. In addition, English colloquial (e.g., Tweets and SMS) and scientific articles (e.g., clinical trails) are supported.

Want to see what it can do before you delve in? Take a look at our **[online demo](http://heideltime.ifi.uni-heidelberg.de/heideltime/)**.

![HeidelTime demo picture](https://drive.google.com/uc?export=download&id=0BwqFBQjz9NUicWEzaWlzT1J1SzQ)

## Latest downloads

* Our latest as well as past releases are always available on the [Releases page](https://github.com/HeidelTime/heideltime/releases).
* Bleeding edge version is available via our Git repository.
* Our temporal annotated corpora and supplementary evaluation scripts can be found [here](http://dbs.ifi.uni-heidelberg.de/index.php?id=form-downloads).
* If you want to receive notifications on updates of HeidelTime, please fill out [this form](http://dbs.ifi.uni-heidelberg.de/index.php?id=form-downloads).
* You can also follow us on Twitter ![Twitter](https://i.imgur.com/dtKBCF8.png)[@HeidelTime](https://twitter.com/heideltime).

## Publications

If you use HeidelTime, please cite the appropriate paper (in general, this would be the journal paper [5]):

1. Manfredi et al.: HeidelTime at EVENTI: Tuning Italian Resources and Addressing TimeML's Empty Tags. EVALITA'14. [pdf](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/2014_EVALITA_ManfrediEtAl.pdf) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#EVALITA2014)
2. Strötgen et al.: Extending HeidelTime for Temporal Expressions Referring to Historic Dates. LREC'14. [pdf](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/StroetgenEtAl2014_LREC.pdf) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#LREC2014b)
3. Li et al.: Chinese Temporal Tagging with HeidelTime. EACL'14. [pdf](http://www.aclweb.org/anthology/E/E14/E14-4026.pdf) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#EACL2014)
4. Strötgen et al.: Time for More Languages: Temporal Tagging of Arabic, Italian, Spanish, and Vietnamese. TALIP, 2014. [pdf](http://dl.acm.org/citation.cfm?id=2540989&CFID=415441800&CFTOKEN=19912471) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#TALIPjournal2014)
5. Strötgen, Gertz: Multilingual and Cross-domain Temporal Tagging. Language Resources and Evaluation, 2013. [pdf](http://www.springerlink.com/content/64767752451075k8/) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#LREjournal2013)
6. Strötgen et al.: HeidelTime: Tuning English and Developing Spanish Resources for TempEval-3. SemEval'13. [pdf](http://www.aclweb.org/anthology/S13-2003) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#SEMEVAL2013)
7. Strötgen, Gertz: Temporal Tagging on Different Domains: Challenges, Strategies, and Gold Standards. LREC'12. [pdf](http://www.lrec-conf.org/proceedings/lrec2012/pdf/425_Paper.pdf) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#LREC2012)
8. Strötgen, Gertz: HeidelTime: High Qualitiy Rule-based Extraction and Normalization of Temporal Expressions. SemEval'10. [pdf](http://www.newdesign.aclweb.org/anthology/S/S10/S10-1071.pdf) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#SEMEVAL2010)

## Language Resources
We want to thank the following researchers for their efforts to develop HeidelTime resources:

1. [Dutch resources](http://www.univ-orleans.fr/lifo/evenements/CSLP2012/proceedings_CSLP12.pdf): [Matje van de Camp, Tilburg University](http://www.tilburguniversity.edu/webwijs/show/?uid=m.m.v.d.camp)
2. [French resources](http://www.lrec-conf.org/proceedings/lrec2014/pdf/45_Paper.pdf): [Véronique Moriceau, LIMSI - CNRS](http://vero.moriceau.free.fr/)
3. Russian resources: Elena Klyachko
4. [Croatian resources](http://nl.ijs.si/isjt14/proceedings/isjt2014_17.pdf): Luka Skukan, University of Zagreb
5. Portuguese resources: Zunsik Lim

---

## Tell me more!
**HeidelTime** was developed in Java with extensibility in mind -- especially in terms of **[language-specific resources](https://github.com/HeidelTime/heideltime/wiki/Developing-Resources)**, as well as in terms of programmatic functionality.

## Get your hands dirty!
* You'd like to reproduce HeidelTime's evaluation results described in our papers on several corpora?
  Download the heideltime-kit or clone our repository and check out our tutorial on **[reproducing evaluation results](https://github.com/HeidelTime/heideltime/wiki/Reproducing-Evaluation-Results)**. This will also explain how to integrate the HeidelTime annotator into a UIMA pipeline.
* You'd like to participate in the development of HeidelTime; maybe create an addon or improve functionality?
  Clone our repository and see how to set up Eclipse to [develop HeidelTime](https://github.com/HeidelTime/heideltime/wiki/Development-Setup). Then have a look at HeidelTime's [architectural concepts](https://github.com/HeidelTime/heideltime/wiki/Architectural-Overview) and have a go at it!
* You'd like to share some changes you've made, resources for a new language, or you think that HeidelTime could be improved in a specific way?
  Open up a [pull request](https://github.com/HeidelTime/heideltime/compare) or an [issue](https://github.com/HeidelTime/heideltime/issues) and let us know, we're eager to read your thoughts!
