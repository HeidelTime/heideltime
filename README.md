**HeidelTime can now also be used for English temponym tagging. For details, see our [TempWeb'16 paper](http://www2016.net/proceedings/companion/p841.pdf).**

**HeidelTime contains automatically created resources for 200+ languages in addition to manually created ones for 13 languages. For further details, take a look at our [EMNLP 2015 paper](https://aclweb.org/anthology/D/D15/D15-1063.pdf).**

## About HeidelTime
**HeidelTime** is a multilingual, domain-sensitive temporal tagger developed at the [Database Systems Research Group](http://dbs.ifi.uni-heidelberg.de/) at [Heidelberg University](http://www.uni-heidelberg.de/index_e.html). It extracts temporal expressions from documents and normalizes them according to the TIMEX3 annotation standard. HeidelTime is available as [UIMA](http://uima.apache.org/) annotator and as standalone version.

**HeidelTime** currently contains hand-crafted resources for **13 languages**: English, German, Dutch, Vietnamese, Arabic, Spanish, Italian, French, Chinese, Russian, Croatian, Estonian and Portuguese. In addition, starting with version 2.0, HeidelTime contains **automatically created resources for more than 200 languages**. Although these resources are of lower quality than the manually created ones, temporal tagging of many of these languages has never been addressed before. Thus, HeidelTime can be used as a baseline for temporal tagging of all these languages or as a starting point for developing temporal tagging capabilities for them. 

**HeidelTime** distinguishes between **news-style** documents and **narrative-style documents** (e.g., Wikipedia articles) in all languages. In addition, English colloquial (e.g., Tweets and SMS) and scientific articles (e.g., clinical trails) are supported.

Want to see what it can do before you delve in? Take a look at our **[online demo](http://heideltime.ifi.uni-heidelberg.de/heideltime/)**.

![HeidelTime demo picture](https://drive.google.com/uc?export=download&id=0BwqFBQjz9NUicWEzaWlzT1J1SzQ)

## Latest downloads

* Our latest as well as past releases are always available on the [Releases page](https://github.com/HeidelTime/heideltime/releases).
* Bleeding edge version is available via our Git repository.
* Our temporal annotated corpora and supplementary evaluation scripts can be found [here](http://dbs.ifi.uni-heidelberg.de/index.php?id=form-downloads).
* If you want to receive notifications on updates of HeidelTime, please fill out [this form](http://dbs.ifi.uni-heidelberg.de/index.php?id=form-downloads).
* You can also follow us on Twitter ![Twitter](https://i.imgur.com/dtKBCF8.png)[@HeidelTime](https://twitter.com/heideltime).

## Maven

A minimal set of dependencies is satisfied by these entries for your pom.xml:

```xml
		<dependency>
			<groupId>org.apache.uima</groupId>
			<artifactId>uimaj-core</artifactId>
			<version>2.8.1</version>
		</dependency>
		<dependency>
			<groupId>com.github.heideltime</groupId>
			<artifactId>heideltime</artifactId>
			<version>2.2</version>
		</dependency>
```

For some additional features, you will need to provide additional dependencies. See our [Maven wiki page](https://github.com/HeidelTime/heideltime/wiki/Maven-Support).

## Publications

If you use HeidelTime, please cite the appropriate paper (in general, this would be the journal paper [4]; if you use HeidelTime with automatically created resources, please cite paper [10]; if you use HeidelTime for temponym tagging, please cite paper [11]):

1. Strötgen, Gertz: HeidelTime: High Qualitiy Rule-based Extraction and Normalization of Temporal Expressions. SemEval'10. [pdf](http://www.newdesign.aclweb.org/anthology/S/S10/S10-1071.pdf) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#SEMEVAL2010)
2. Strötgen, Gertz: Temporal Tagging on Different Domains: Challenges, Strategies, and Gold Standards. LREC'12. [pdf](http://www.lrec-conf.org/proceedings/lrec2012/pdf/425_Paper.pdf) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#LREC2012)
3. Strötgen et al.: HeidelTime: Tuning English and Developing Spanish Resources for TempEval-3. SemEval'13. [pdf](http://www.aclweb.org/anthology/S13-2003) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#SEMEVAL2013)
4. Strötgen, Gertz: Multilingual and Cross-domain Temporal Tagging. Language Resources and Evaluation, 2013. [pdf](http://www.springerlink.com/content/64767752451075k8/) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#LREjournal2013)
5. Strötgen et al.: Time for More Languages: Temporal Tagging of Arabic, Italian, Spanish, and Vietnamese. TALIP, 2014. [pdf](http://dl.acm.org/citation.cfm?id=2540989&CFID=415441800&CFTOKEN=19912471) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#TALIPjournal2014)
6. Li et al.: Chinese Temporal Tagging with HeidelTime. EACL'14. [pdf](http://www.aclweb.org/anthology/E/E14/E14-4026.pdf) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#EACL2014)
7. Strötgen et al.: Extending HeidelTime for Temporal Expressions Referring to Historic Dates. LREC'14. [pdf](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/StroetgenEtAl2014_LREC.pdf) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#LREC2014b)
8. Manfredi et al.: HeidelTime at EVENTI: Tuning Italian Resources and Addressing TimeML's Empty Tags. EVALITA'14. [pdf](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/2014_EVALITA_ManfrediEtAl.pdf) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#EVALITA2014)
9. Strötgen: Domain-sensitive Temporal Tagging for Event-centric Information Retrieval. PhD Thesis. [pdf](http://archiv.ub.uni-heidelberg.de/volltextserver/18357/1/thesis.pdf) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#THESIS2015)
10. Strötgen, Gertz: A Baseline Temporal Tagger for All Languages. EMNLP'15. [pdf](https://aclweb.org/anthology/D/D15/D15-1063.pdf) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#EMNLP2015)
11. Kuzey, Strötgen, Setty, Weikum: Temponym Tagging: Temporal Scopes for Textual Phrases. TempWeb'16. [pdf](http://dl.acm.org/citation.cfm?id=2889289) [bibtex](http://dbs.ifi.uni-heidelberg.de/fileadmin/Team/jannik/publications/stroetgen_bib.html#TEMPWEB2016)

## Language Resources
We want to thank the following researchers for their efforts to develop HeidelTime resources:

1. [Dutch resources](http://www.univ-orleans.fr/lifo/evenements/CSLP2012/proceedings_CSLP12.pdf): [Matje van de Camp, Tilburg University](http://www.tilburguniversity.edu/webwijs/show/?uid=m.m.v.d.camp)
2. [French resources](http://www.lrec-conf.org/proceedings/lrec2014/pdf/45_Paper.pdf): [Véronique Moriceau, LIMSI - CNRS](http://vero.moriceau.free.fr/)
3. Russian resources: Elena Klyachko
4. [Croatian resources](http://nl.ijs.si/isjt14/proceedings/isjt2014_17.pdf): Luka Skukan, University of Zagreb
5. Portuguese resources: Zunsik Lim

Please feel free to use our automatically created resources as starting point, if you plan to manually address a language. 

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
