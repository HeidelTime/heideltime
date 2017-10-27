#! /usr/env jython


# Python imports. for now, only Python std library is possible
import json
import os

# importing Java packages
# see http://wiki.python.org/jython/UserGuide#accessing-java-from-jython
from org.apache.uima.jcas.tcas import Annotation
from de.unihd.dbs.uima.types.heideltime import Sentence
from de.unihd.dbs.uima.types.heideltime import Token


# How do we recuperate the jsons ? environement variable
sentence_file_path = os.environ['SENTENCE_ANNOTATION_FILE_PATH']
pos_file_path = os.environ['POS_ANNOTATION_FILE_PATH']

# sentence_file_path = "/misc/home/reco/reboutli/Dev/Python/PythonPOSTaggerWrapper/Ressources/sentences.json"
# pos_file_path = "/misc/home/reco/reboutli/Dev/Python/PythonPOSTaggerWrapper/Ressources/pos.json"
sentences_json = []
pos_json = []

# if not os.path.isfile(sentence_file_path):
#     raise FileNotFoundError("the sentence annotation file path was not found")

with open(sentence_file_path, 'r') as sentences_file :
    sentences_json = json.load(sentences_file)

with open(pos_file_path, 'r') as pos_file :
    pos_json = json.load(pos_file)

# Create annotations and add it to jcas. This is then accessible from UIMA
for s in sentences_json:
    sentence = Sentence(jcas, s['offsets'][0]['begin'], s['offsets'][0]['end'])
    sentence.addToIndexes()

for t in pos_json:
    token = Token(jcas, t['offsets'][0]['begin'], t['offsets'][0]['end'])
    token.setPos(t['category'])
    token.addToIndexes()

