#######################
# UIMA HEIDELTIME KIT #
#######################

Author: Jannik Strötgen
Date:   June 30, 2012
eMail:  stroetgen@uni-hd.de

###################################
# 1. Papers describing HeidelTime #
###################################
HeidelTime was used in the TempEval-2 challenge as described in:
Jannik Strötgen and Michael Gertz.
HeidelTime: High Quality Rule-based Extraction and Normalization of Temporal Expressions.
In: SemEval-2010: Proceedings of the 5th International Workshop on Semantic Evaluation. 
Pages 321-324, Uppsala, Sweden, July 15-16, 2010. ACL.
http://www.aclweb.org/anthology/S/S10

In "Language Resources and Evaluation", we have published a paper on "Multilingual and 
Cross-domain Temporal Tagging". In addition, we detail the features and architecture of 
HeidelTime:
Jannik Strötgen and Michael Gertz.
Multilingual and Cross-domain Temporal Tagging.
In: Language Resources and Evaluation, 2012, Springer.

Please cite one of these papers if you use HeidelTime. 

If you use HeidelTime for processing colloquial text such as SMS or tweets or scientific 
publications (e.g., biomedical studies), you may want to cite the following paper instead:
Jannik Strötgen and Michael Gertz:
Temporal Tagging on Different Domains: Challenges, Strategies, and Gold Standards.
In: LREC 2012: Proceedings of the 8th International Conference on Language Resources 
and Evaluation. Pages 3746--3753, Istanbul, Turkey, May 21-27, 2012. ELRA.

###################
# 2. Introduction #
###################
HeidelTime is a multilingual temporal tagger that extracts temporal expressions from documents
and normalizes them according to the TIMEX3 annotation standard, which is part of the mark-up
language TimeML. HeidelTime uses different normalization strategies depending on the domain of 
the documents that are to be processed (news, narratives, scientific or colloquial texts). It 
is a rule-based system and due  to its architectural feature that the source code and the 
resources (patterns, normalization information, and rules) are strictly separated, one can 
simply develop resources for additional languages using HeidelTime's well-defined rule syntax.

HeidelTime was the best system for the extraction and normalization of English temporal 
expressions from documents in the TempEval-2 challenge in 2010. Furthermore, it is evaluated on 
several additional corpora, as described in our paper "Multilingual Cross-domain Temporal 
Tagging" (http://www.springerlink.com/content/64767752451075k8/).

HeidelTime with resources for English and German is one component of our UIMA HeidelTime kit.
Furthermore, resources for Dutch were developed and kindly provided by Matje van de Camp 
(Tilburg University, http://www.tilburguniversity.edu/webwijs/show/?uid=m.m.v.d.camp).

Additionally, whilst expanding the set of domains that HeidelTime can recognize temporal 
expressions in, English resources for colloquial as well as scientific style documents were 
developed. Colloquial documents are for example SMS or Twitter messages where language is
abbreviated to fit a length constraint and non-standard language is common. Scientific documents
include for example biomedical studies, in which temporal information is often given relative 
to a document-specific "local" time frame.

In addition to HeidelTime with resources for English, German, and Dutch, the UIMA HeidelTime kit
contains collection readers, cas consumers, and analysis engines to process temporal annotated 
corpora and reproduce HeidelTime's evaluation results on these corpora. The HeidelTime kit 
contains:

    * ACE Tern Reader: This Collection Reader reads corpora of the ACE Tern style and annotates 
      the document creation time. Using the corpora preparation script (for details, see below), 
      the following corpora can be processed: ACE Tern 2004 training, TimeBank, WikiWars, and 
      WikiWars_DE.
    * TempEval-2 Reader: This Collection Reader reads the TempEval-2 input data of the training 
      and the evaluation sets and annotates the document creation time as well as token and 
      sentence information.
    * TreeTaggerWrapper: This Analysis Engine produces Token, Sentence and Part-of-Speech annotations
      required by HeidelTime by using the language independent TreeTagger tool.
    * HeidelTime
    * Annotation Translator: This Analysis Engine translates Sentence, Token, and Part-of-Speech 
      annotations of one type system into HeidelTime's type system.
    * ACE Tern Writer: This CAS Consumer creates output data as needed to run the official ACE 
      Tern evaluation scripts.
    * TempEval-2 Writer: This CAS Consumer creates two files needed to evaluate the tasks of 
      extracting and evaluating temporal expressions of the TempEval-2 challenge using the 
      official evaluation scripts.
      
######################
# 3. Getting started #
######################
Most of the descriptions are tested and written for Linux, e.g., how to set environment
variables. If you do not use Linux, make sure that you carry out analogous steps, e.g.,
set the environment variables.

1. UIMA (if you already use UIMA, you can skip this step):
   To be able to use HeidelTime, you have to install UIMA:
	* Download UIMA:
		- either from http://uima.apache.org/downloads.cgi or
		- wget http://ftp-stud.hs-esslingen.de/pub/Mirrors/ftp.apache.org/dist//uima///uimaj-2.3.1-bin.tar.gz
	* Extract UIMA:
		- tar xvfz uimaj-2.3.1-bin.tar.gz
	* Set environment variable (you can set variables globally, e.g., in your $HOME/.bashrc)
		- set UIMA_HOME to the path of your "apache-uima" folder
			* export UIMA_HOME=`pwd`/apache-uima
		- make sure that JAVA_HOME is set correctly
		- add the "$UIMA_HOME/bin" to your PATH
			* export PATH=$PATH:$UIMA_HOME/bin
	* Adjust the UIMA's example paths: 
		- $UIMA_HOME/bin/adjustExamplePaths.sh
	* For further information about UIMA, see http://uima.apache.org/

2. Download and install the UIMA HeidelTime kit
	* download the latest heideltime-kit from
	  https://code.google.com/p/heideltime/downloads/list
	* unzip or untar the heideltime-kit into a path called HEIDELTIME_HOME from hereon out.
	* set the environment variable HEIDELTIME_HOME (you can set these variables globally, 
	  e.g., in your $HOME/.bashrc):
		- export HEIDELTIME_HOME='/path/to/heideltime/'

3. HeidelTime requires sentence, token, and part-of speech annotations. We have developed
   our own wrapper for the popular TreeTagger tool that will support any language for which
   there are parameter files available.
   However, you may use other tools (if so, you either have to adjust the analysis engine
   "Annotation Translator", which is part of the UIMA HeidelTime kit or you may adjust the 
   imports in HeidelTime.java itself. Furthermore, if a differing tag set is used, all rules
   containing part-of-speech information have to be adapted).
    * Download the TreeTagger and its tagging scripts, installation scripts, as well as 
      English, German, and Dutch (or any other) parameter files into one directory from:
      http://www.ims.uni-stuttgart.de/projekte/corplex/TreeTagger/
      - mkdir treetagger 
      - cd treetagger
      - wget ftp://ftp.ims.uni-stuttgart.de/pub/corpora/tree-tagger-linux-3.2.tar.gz
      - wget ftp://ftp.ims.uni-stuttgart.de/pub/corpora/tagger-scripts.tar.gz
      - wget ftp://ftp.ims.uni-stuttgart.de/pub/corpora/install-tagger.sh
      - wget ftp://ftp.ims.uni-stuttgart.de/pub/corpora/german-par-linux-3.2.bin.gz
      - wget ftp://ftp.ims.uni-stuttgart.de/pub/corpora/german-par-linux-3.2-utf8.bin.gz
      - wget ftp://ftp.ims.uni-stuttgart.de/pub/corpora/english-par-linux-3.1.bin.gz
      - wget ftp://ftp.ims.uni-stuttgart.de/pub/corpora/dutch-par-linux-3.1.bin.gz
      Attention: If you do not use Linux, please download all TreeTagger files directly from
                 http://www.ims.uni-stuttgart.de/projekte/corplex/TreeTagger/ 
    * Install the TreeTagger
		- sh install-tagger.sh 
	* Set environment variables (you can set variables globally, e.g., in your $HOME/.bashrc)
    	- export TREETAGGER_HOME='path to TreeTagger'
   For further information on the TreeTagger, take a look at its documentation.		


#########################
# 4. Testing HeidelTime #
#########################
1. source the environment
	* . $HEIDELTIME_HOME/metadata/setenv

2. run cpeGUI.sh and create a workflow
	* cpeGUI.sh
	* create a workflow with the following components:
		Collection reader:
		- UIMA's file system collection reader:
			$UIMA_HOME/examples/descriptors/collection_reader/FileSystemCollectionReader.xml
			set "Input directory" to $HEIDELTIME_HOME/doc/
		Analysis Engines
		- TreeTaggerWrapper located at
			HEIDELTIME_HOME/desc/annotator/TreeTaggerWrapper.xml
			set "Language" to "english"
			set "Annotate_tokens" to "true"
			set "Annotate_partofspeech" to "true"
			set "Annotate_sentences" to "true"
			set "Improvegermansentences" to "false"
		- HeidelTime located at
			HEIDELTIME_HOME/desc/annotator/HeidelTime.xml
			set "Date" to "true"
			set "Time" to "true"
			set "Duration" to "true"
			set "Set" to "true"
			set "Language" to "english"
			set "Type" to "narratives"
		CAS Consumer
		- UIMA's XMI Writer CAS Consumer located at
			$UIMA_HOME/examples/descriptors/cas_consumer/XmiWriterCasConsumer.xml
			set "Output Directory" to OUTPUT
	* (save the workflow) 
	* run the workflow
	
###########################################################
# 5. Analyze the results using the UIMA annotation viewer #
###########################################################
To analyze the annotations produced by HeidelTime you may use UIMA's annotation viewer:
	* annotationViewer.sh
		set "Input Directory" to "OUTPUT"
		set TypeSystem or AE Descriptor File" to "$HEIDELTIME_HOME/desc/type/HeidelTime_TypeSystem.xml"
		
	* focus the analysis on Section 6 of the "readme.txt" file.

####################################################################
# 6. What kind of temporal expressions can be found and normalized #
####################################################################
HeidelTime distinguishes between four types of documents: news style, narrative
style, colloquial style and scientific style documents. This file here is a narrative 
document. This version of HeidelTime was released in 2011. To be more precise, and using
a relative expression, it was released on May 4. HeidelTime was the best performing 
system of task A of the TempEval-2 challenge in 2010 and the system was presented at the 
TempEval Workshop at the ACL conference in Uppsala, Sweden on July 15, 2010 or July 16. 
In the meantime, it is May 2011 and HeidelTime is made publicly available and identifies 
these temporal expressions: January 22, 2001 or twice a week.

#######################################################################
# 7. Reproducing HeidelTime's evaluation results on different corpora #
#######################################################################
To reproduce HeidelTime's evaluation results reported in in our paper "Multilingual 
Cross-domain Temporal Tagging", follow the instructions on:
http://code.google.com/p/heideltime/wiki/ReproduceEvaluationResults

##############
# 8. License #
##############
Copyright (c) 2012, Database Research Group, Institute of Computer Science, University of Heidelberg. 
All rights reserved. This program and the accompanying materials 
are made available under the terms of the GNU General Public License.

author: Jannik Strötgen
email:  stroetgen@uni-hd.de

HeidelTime is a multilingual, cross-domain temporal tagger.
For details, see http://dbs.ifi.uni-heidelberg.de/heideltime and
http://code.google.com/p/heideltime/
