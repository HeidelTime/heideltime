#######################
# UIMA HEIDELTIME KIT #
#######################

Author:  Jannik Strötgen
Date:    October 4, 2016
Version: 2.2
eMail:   jannik.stroetgen@mpi-inf.mpg.de

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
Cross-domain Temporal Tagging". Here, we detail the features and architecture of 
HeidelTime:
  Jannik Strötgen and Michael Gertz.
  Multilingual and Cross-domain Temporal Tagging.
  In: Language Resources and Evaluation, 47(2), pages 269-298, 2013, Springer.
  http://link.springer.com/article/10.1007%2Fs10579-012-9179-y

Please cite one of these papers if you use HeidelTime. Or, alternatively:

If you use HeidelTime for processing colloquial text (such as SMS or tweets) or scientific 
publications (e.g., biomedical studies), you may want to cite the following paper instead:
  Jannik Strötgen and Michael Gertz:
  Temporal Tagging on Different Domains: Challenges, Strategies, and Gold Standards.
  In: LREC 2012: Proceedings of the 8th International Conference on Language Resources 
  and Evaluation. Pages 3746-3753, Istanbul, Turkey, May 21-27, 2012. ELRA.
  http://www.lrec-conf.org/proceedings/lrec2012/pdf/425_Paper.pdf
  
Starting with version 2.0, HeidelTime contains also automatically created resources. If you
use them, please cite the following paper:
  Jannik Strötgen and Michael Gertz:
  A Baseline Temporal Tagger for All Languages.
  In EMNLP: Proceedings of the 2015 Conference on Empirical Methods in Natural Language 
  Processing. Pages 541-547, Lisbon, Portugal, September 17-21, 2015. ACL.
  
Starting with version 2.2, HeidelTime can be used for temponym tagging. 
If you use HeidelTime as a temponym tagger, please cite the following paper:
  Erdal Kuzey, Jannik Strötgen, Vinay Setty, and Gerhard Weikum:
  Temponym Tagging: Temporal Scopes for Textual Phrases.
  In TempWeb: Proceedings of the 6th Temporal Web Analytics Workshop. Pages 841-842, Montreal,
  Canada, April 12, 2016. ACM.

###################
# 2. Introduction #
###################
HeidelTime is a multilingual temporal tagger that extracts temporal expressions from documents
and normalizes them according to the TIMEX3 annotation standard, which is part of the mark-up
language TimeML. HeidelTime uses different normalization strategies depending on the domain of 
the documents that are to be processed (news, narratives, scientific or colloquial texts). It 
is a rule-based system and due to its architectural feature that the source code and the 
resources (patterns, normalization information, and rules) are strictly separated, one can 
simply develop resources for additional languages using HeidelTime's well-defined rule syntax.

HeidelTime was the best system for the extraction and normalization of English temporal 
expressions from documents in the TempEval-2 challenge in 2010. Furthermore, it is evaluated on 
several additional corpora, as described in our papers, e.g., in "Multilingual Cross-domain 
Temporal Tagging" (http://www.springerlink.com/content/64767752451075k8/). In TempEval-3, 
HeidelTime achieved the best results for the combination of extraction and normalization for 
English and Spanish. In the EVENTI competition of EVALITA 2014, HeidelTime (version 1.8) achieved 
the best results for Italian temporal tagging.

HeidelTime with resources for several languages is one component of our UIMA HeidelTime kit.
- German
- English (and Englischcoll (for colloquial texts), Englishsci (for scientific texts))
- Dutch (kindly provided by Matje van de Camp, Tilburg University, 
    http://www.tilburguniversity.edu/webwijs/show/?uid=m.m.v.d.camp)
- Arabic
- Vietnamese
- Spanish
- Italian
- French (kindly provided by Véronique Moriceau, LIMSI - CNRS, http://vero.moriceau.free.fr/)
- Chinese
- Russian (a preliminary version was kindly shared by Elena Klyachko)
- Croatian (kindly provided by Luka Skukan)
- Estonian
- Portuguese (kindly provided by Zunsik Lim)

Starting with version 2.0, HeidelTime contains automatically created resources for 200+ 
languages. It can thus be used as a baseline temporal tagger for all these languages or
as a starting point for developing more sophisticated temporal tagging capabilities for 
languages which have not been addressed so far.

Additionally, whilst expanding the set of domains that HeidelTime can recognize temporal 
expressions in, English resources for colloquial as well as scientific style documents were 
developed. Colloquial documents are for example SMS or Twitter messages where language is
abbreviated to fit a length constraint and non-standard language is common. Scientific documents
include for example biomedical studies, in which temporal information is often given relative 
to a document-specific "local" time frame.

In addition to HeidelTime with resources for the respective languages, the UIMA HeidelTime kit
contains collection readers, cas consumers, and analysis engines to process temporal annotated 
corpora and reproduce HeidelTime's evaluation results on these corpora. The HeidelTime kit 
contains:

    * ACE Tern Reader: This Collection Reader reads corpora of the ACE Tern style and annotates 
      the document creation time. Using the corpora preparation script (for details, see below), 
      several corpora can be processed, e.g.: ACE Tern 2004 training, WikiWars, and WikiWars_DE.
      
    * TempEval-2 Reader: This Collection Reader reads the TempEval-2 input data of the training 
      and the evaluation sets and annotates the document creation time as well as token and 
      sentence information.
      
    * TempEval-3 Reader: This Collection Reader reads the TempEval-3 input TimeML data of the
      training and the evaluation sets and annotates the document creation time as well as some
      meta information.
      
    * Eventi 2014 Reader: This Collection Reader reads the Eventi 2014 input data of the
      training and the evaluation sets and annotates the tokenization information.
      
    * AllLanguagesTokenizer: This Analysis Engine produces Token and Sentence annotations. It is a 
      simple yet generic tool and should be used for languages which are not supported by any other
      preprocessing tool, i.e., for most of the languages for which HeidelTime resources have been
      automatically created.
      
    * TreeTaggerWrapper: This Analysis Engine produces Token, Sentence and Part-of-Speech annotations
      required by HeidelTime by using the multilingual TreeTagger tool.
      
    * HunPosTaggerWrapper: This Analysis Engine produces Token, Sentence and Part-of-Speech annotations
      required by HeidelTime by using the HunPosTagger tool.
      
    * StanfordPOSTaggerWrapper: This Analysis Engine produces Token, Sentence and Part-of-Speech
      annotations required by HeidelTime by using the multilingual Stanford POS Tagger.
      
    * JVnTextProWrapper: This Analysis Engine produces Token, Sentence and Part-of-Speech annotations
      required by HeidelTime by using the JVnTextPro tool for documents in Vietnamese.
    
    * Annotation Translator: This Analysis Engine translates Sentence, Token, and Part-of-Speech 
      annotations of one type system into HeidelTime's type system.
      
    * HeidelTime: Possible parameter values are:
        - languages: english, englishcoll, englishsci, german, spanish, italian, vietnamese, arabic,
                   dutch, chinese, french, russian, croatian, estonian, portuguese
                   as well as any "auto-LANGUAGE" contained in the resource folder
        - types: news, narratives, colloquial (for use with englishcoll), scientific (englishsci)
        - locale: the locale to use for date calculation. Leave it empty to use en_GB.
        - Debugging: to output verbose debugging information to stderr.
        - Date / Time / Duration / Set / Temponym: if respective temporal expressions shall be 
          extracted

    * IntervalTagger: This Analysis Engine in conjunction with HeidelTime recognizes
      temporal intervals in documents.
      
    * ACE Tern Writer: This CAS Consumer creates output data as needed to run the official ACE 
      Tern evaluation scripts.
      
    * TempEval-2 Writer: This CAS Consumer creates two files needed to evaluate the tasks of 
      extracting and evaluating temporal expressions of the TempEval-2 challenge using the 
      official evaluation scripts.
      
    * TempEval-3 Writer: This CAS Consumer writes annotated TimeML files that were read in by
      the TempEval-3 Reader and processed by HeidelTime in the format required by the TempEval-3
      evaluation scripts.
      
    * Eventi 2014 Writer: This CAS Consumer writes annotated Eventi 2014 files that were read in by
      the Eventi 2014 Reader and processed by HeidelTime in the format required by the Eventi 2014
      evaluation script.
      
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
        - wget http://archive.apache.org/dist/uima/uimaj-2.6.0/uimaj-2.6.0-bin.tar.gz
    * Extract UIMA:
        - tar xvfz uimaj-2.6.0-bin.tar.gz
    * Set environment variable (you can set variables globally, e.g., in your $HOME/.bashrc)
        - set UIMA_HOME to the path of your "apache-uima" folder
            * export UIMA_HOME="$(pwd)/apache-uima"
        - make sure that JAVA_HOME is set correctly
        - add the "$UIMA_HOME/bin" to your PATH
            * export PATH=$PATH:$UIMA_HOME/bin
    * Adjust the UIMA's example paths:
        - $UIMA_HOME/bin/adjustExamplePaths.sh
    * For further information about UIMA, see http://uima.apache.org/

2. Download and install the UIMA HeidelTime kit
    * download the latest heideltime-kit from
      https://github.com/HeidelTime/heideltime/releases
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
   
   To process English, German, Dutch, Spanish, Italian, French, Chinese or Russian documents, 
   the TreeTaggerWrapper can be used for pre-processing: 
    * Download the TreeTagger and its tagging scripts, installation scripts, as well as 
      English, German, and Dutch (and all required) parameter files into one directory from:
      http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/
      - mkdir treetagger 
      - cd treetagger
      - wget http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/tree-tagger-linux-3.2.1.tar.gz
      - or alternatively: wget http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/tree-tagger-linux-3.2-old.tar.gz
      - wget http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/tagger-scripts.tar.gz
      - wget http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/install-tagger.sh
      - wget http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/german-par-linux-3.2-utf8.bin.gz
      - wget http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/english-par-linux-3.2-utf8.bin.gz
      - wget http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/dutch-par-linux-3.2-utf8.bin.gz
      - wget http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/italian-par-linux-3.2-utf8.bin.gz
      - wget http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/spanish-par-linux-3.2-utf8.bin.gz
      - wget http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/french-par-linux-3.2-utf8.bin.gz
      - wget http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/portuguese-par-linux-3.2-utf8.bin.gz
      - wget http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/data/estonian-par-linux-3.2-utf8.bin.gz
      Attention: If you do not use Linux, please download all TreeTagger files directly from
                 http://www.cis.uni-muenchen.de/~schmid/tools/TreeTagger/
    * (OPTIONAL) For Chinese documents, please get the Tokenizer and TreeTagger parameter file
      from Serge Sharoff's page http://corpus.leeds.ac.uk/tools/zh/:
      - wget http://corpus.leeds.ac.uk/tools/zh/tt-lcmc.tgz
      - wget https://drive.google.com/uc?id=0B1ZoOwaeRsbva2F3NThLd3ptRWM -O zh-tokenise.tgz
      Extract the Tokenizer into a new directory and TreeTagger parameter files like this:
      - mkdir chinese-tokenizer
      - tar -xzvf tt-lcmc.tgz
      - tar -xzvf zh-tokenise.tgz -C chinese-tokenizer
    * (OPTIONAL) For Russian documents, please grab a copy of the Russian parameter file from
      Serge Sharoff's page at http://corpus.leeds.ac.uk/mocky/ and extract it into TreeTagger's
      lib/-folder:
      - cd /path/to/treetagger/
      - mkdir lib && cd lib
      - wget http://corpus.leeds.ac.uk/mocky/russian.par.gz
      - gunzip russian.par.gz
    * Install the TreeTagger
        - sh install-tagger.sh
    * Set environment variables (you can set variables permanently, e.g., in your $HOME/.bashrc)
      and then source the environment.
        - export TREETAGGER_HOME='path to TreeTagger'
        - source $HEIDELTIME_HOME/metadata/setenv
   For further information on the TreeTagger, take a look at its documentation and our wiki
   page for it: https://github.com/HeidelTime/heideltime/wiki/TreeTaggerWrapper
   
   
   To process Vietnamese documents, we have developed the JVnTextProWrapper Analysis 
   Engine. It makes use of the JVnTextPro tool. To use it, follow these steps:
    * Download JVnTextPro and unpack it:
      - wget http://sourceforge.net/projects/jvntextpro/files/latest/download -O JVnTextPro.zip
      - unzip JVnTextPro.zip
    * Set the relevant environment variable, then source the environment to construct
      the CLASSPATH.
      - export JVNTEXTPRO_HOME='<path to JVnTextPro>/bin'
      - source $HEIDELTIME_HOME/metadata/setenv
    Further information about JVnTextPro can be found on our Wiki page for the Engine:
        https://github.com/HeidelTime/heideltime/wiki/JVnTextProWrapper
    
    
    To process Arabic documents, we have developed the Stanford POS Tagger Wrapper
    Analysis Engine. It utilizes the Stanford POS Tagger. Follow these instructions 
    to set it up:
     * Download the Stanford POS Tagger *Full Package* from this URL and unzip it:
       http://nlp.stanford.edu/software/tagger.shtml
       - wget http://nlp.stanford.edu/software/stanford-postagger-full-2014-01-04.zip
       - unzip stanford-postagger-full-2014-01-04.zip
     * Set the relevant environment variable, then source the environment to construct
       the CLASSPATH.
       - export STANFORDTAGGER='path to stanford-postagger-<version>.jar'
       - source $HEIDELTIME_HOME/metadata/setenv
     For more information on the Stanford POS Tagger Wrapper, see our Wiki page:
         https://github.com/HeidelTime/heideltime/wiki/StanfordPOSTaggerWrapper
         
    
    To process Croatian documents, Luka Skukan has developed a Wrapper for the 
    HunPosTagger. You will need to get a copy of the HunPos tagger as well
    as a Croatian model file for it: 
    From https://code.google.com/p/hunpos/downloads/list, download hunpos-1.0:
    - wget https://hunpos.googlecode.com/files/hunpos-1.0-linux.tgz
    From http://nlp.ffzg.hr/resources/models/tagging/, download the model file:
    - wget http://nlp.ffzg.hr/data/tagging/model.hunpos.mte5.defnpout.tar.gz
    Extract both of the files into the same directory
    - tar -xzvf hunpos-1.0-linux.tgz
    - tar -xzvf model.hunpos.mte5.defnpout.tar.gz -C hunpos-1.0-linux/
    You will need to enter the full path of the hunpos-1.0-linux directory in the
    HunPosTaggerWrapper.
    
    To process any of the language with automatically created resources, you can use 
    the AllLanguagesTokenizer, which is part of the heideltime kit. It is a simple 
    (whitespace-based) yet generic tool and creaetes sentence and token annotation.
     
     
    For sample UIMA workflows for many of the supported languages, please take a look
    at our evaluation results reproduction Wiki page:
        https://github.com/HeidelTime/heideltime/wiki/Reproducing-Evaluation-Results
    and select a workflow description for a corpus of the language of your choice.


#########################
# 4. Testing HeidelTime #
#########################
1. source the environment and copy the resources into the CLASSPATH
    * source $HEIDELTIME_HOME/metadata/setenv
    * cd $HEIDELTIME_HOME/resources && sh printResourceInformation.sh

2. run cpeGui.sh and create a workflow
    * cpeGui.sh
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
            set "Temponym" to "false"
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
style, colloquial style and scientific style documents. In addition, temponyms such as "John 
F. Kennedy's death" can be identified and normalized if "temponym" is set to true.  This file 
here is a narrative document. This version of HeidelTime was released in 2016. To be more 
precise, and using a relative expression, it was released on Ocotber 4. HeidelTime was the best 
performing system of task A of the TempEval-2 challenge in 2010 and the system was presented at the 
TempEval Workshop at the ACL conference in Uppsala, Sweden on July 15, 2010 or July 16. 
In the meantime, it is October 2016 and HeidelTime is made publicly available and identifies 
these temporal expressions: January 22, 2001 or twice a week.

##########################################
# 7. Additional HeidelTime documentation #
##########################################
HeidelTime's GitHub Project contains a lot of valuable information on how to use
HeidelTime or its components, as well as additional resources, an always up-to-date
code repository and issue tracker in case you spot a bug.
Visit the project at
    
    https://github.com/HeidelTime/heideltime


#######################################################################
# 8. Reproducing HeidelTime's evaluation results on different corpora #
#######################################################################
To reproduce HeidelTime's evaluation results reported in in our papers, e.g., in
"Multilingual Cross-domain Temporal Tagging", follow the instructions on:
https://github.com/HeidelTime/heideltime/wiki/Reproducing-Evaluation-Results

##############
# 9. License #
##############
Copyright (c) 2012-2016, Database Research Group, Institute of Computer Science, Heidelberg University. 
All rights reserved. This program and the accompanying materials 
are made available under the terms of the GNU General Public License.

author: Jannik Strötgen
email:  jannik.stroetgen@mpi-inf.mpg.de

HeidelTime is a multilingual, cross-domain temporal tagger.
For details, see http://dbs.ifi.uni-heidelberg.de/heideltime
