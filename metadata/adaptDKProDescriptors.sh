#!/bin/bash

# author: Jannik Stroetgen
# email:  stroetgen@uni-hd.de
# date:   April 23, 2011

# About this script:
####################
# This script adapts the descriptor files of the DKPro components.
# The type system information has to be adapted:
#       * in DKPRO_HOME/desc/annotator/SentenceSplitter.xml change 
#               "<import name="desc.type.Sentence"/>" into
#               "<import location="../type/Sentence.xml"/>
#       * in DKPRO_HOME/desc/annotator/Tokenizer.xml change 
#               "<import name="desc.type.Token"/>" into
#               "<import location="../type/Token.xml"/>
#               and
#               "<import name="desc.type.Sentence"/>" into
#               "<import location="../type/Sentence.xml"/>              
#       * in DKPRO_HOME/desc/annotator/TreeTagger.xml change 
#               "<import name="desc.type.Token"/>" into
#               "<import location="../type/Token.xml"/>
#               and
#               "<import name="desc.type.POS"/>" into
#               "<import location="../type/POS.xml"/>   
#               and
#               "<import name="desc.type.Lemma"/>" into
#               "<import location="../type/Lemma.xml"/>
# The path to the TreeTagger has to be adapted:
#       * in DKPRO_HOME/desc/annotator/TreeTagger.xml change
#				"/<fileUrl>file:resources\/TreeTagger_3.1<\/fileUrl>" into
#				/<fileUrl>file:$TREETAGGER_HOME<\/fileUrl>/" with $TREETAGGER_HOME being the path to TreeTagger
				


###########################
# Type system information #
###########################
# SentenceSplitter.xml
echo "adapting type system information in  $DKPRO_HOME/desc/annotator/SentenceSplitter.xml"
sed -i 's/<import name="desc.type.Sentence"\/>/<import location="..\/type\/Sentence.xml"\/>/g' $DKPRO_HOME/desc/annotator/SentenceSplitter.xml
# Tokenizer.xml
echo "adapting type system information in $DKPRO_HOME/desc/annotator/Tokenizer.xml"
sed -i 's/<import name="desc.type.Sentence"\/>/<import location="..\/type\/Sentence.xml"\/>/g' $DKPRO_HOME/desc/annotator/Tokenizer.xml
sed -i 's/<import name="desc.type.Token"\/>/<import location="..\/type\/Token.xml"\/>/g' $DKPRO_HOME/desc/annotator/Tokenizer.xml
# TreeTagger.xml
echo "adapting type system information in $DKPRO_HOME/desc/annotator/TreeTagger.xml"
sed -i 's/<import name="desc.type.Token"\/>/<import location="..\/type\/Token.xml"\/>/g' $DKPRO_HOME/desc/annotator/TreeTagger.xml
sed -i 's/<import name="desc.type.POS"\/>/<import location="..\/type\/POS.xml"\/>/g' $DKPRO_HOME/desc/annotator/TreeTagger.xml
sed -i 's/<import name="desc.type.Lemma"\/>/<import location="..\/type\/Lemma.xml"\/>/g' $DKPRO_HOME/desc/annotator/TreeTagger.xml

##########################
# TreeTagger information #
##########################
echo "adapting TreeTagger information in $DKPRO_HOME/desc/annotator/TreeTagger.xml"
result=$(echo $TREETAGGER_HOME | sed 's/\//\\\//g');
sed -i "s/<fileUrl>file:resources\/TreeTagger_3.1<\/fileUrl>/<fileUrl>file:$result<\/fileUrl>/g" $DKPRO_HOME/desc/annotator/TreeTagger.xml
