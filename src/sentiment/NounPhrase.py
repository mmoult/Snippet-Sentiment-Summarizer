import os
import re
import glob
import pandas as pd
# import nltk
# from operator import itemgetter
# from nltk.corpus import wordnet
# from nltk.stem.wordnet import WordNetLemmatizer
# import html2text
import numpy as np
import re
import spacy
import sklearn
import nltk
from spacy.lang.en import STOP_WORDS
# from sklearn.feature_extraction.stop_words import ENGLISH_STOP_WORDS
from nltk.corpus import stopwords
nltk.download('stopwords')

pattern = "\[(.*?)\]"
with open('../../test-reviews.txt', 'r') as file:
    reviews = file.read().replace('\n', ' ')#.replace('|', '. ')
    reviews = re.sub(pattern,'',reviews, flags=re.DOTALL)
# reviews = ' '.join(list(filter(lambda a: a != '', reviews.split(' '))))
sentence_list = reviews.split('|')

from nltk.stem import PorterStemmer
porter = PorterStemmer()

nlp = spacy.load("en_core_web_sm")

stops = list(set(stopwords.words('english') + list(set(STOP_WORDS))))
#Stopping checker 
def isStopWord(word):
    if word in stops:
        return True
    else:
        return False
    
facets =[]
feature = []
for sentence in sentence_list:
    doc= nlp(sentence)
    for chunk in doc.noun_chunks:
        new_chunk = ' '.join([porter.stem(i.lower()) for i in chunk.text.split() if i.lower() not in stops])
        print(chunk, ' --> ',new_chunk, '  ', chunk.root.dep_)
        feature.append(chunk.text)
        facets.append(new_chunk)

facet_df = pd.DataFrame(list(zip(feature, facets)), columns=['original_phrase', 'facets'])
facet_df.dropna(subset = ['facets'], inplace=True)
indexNames = facet_df[facet_df['facets'] == ''].index
# Delete these row indexes from dataFrame
facet_df.drop(indexNames , inplace=True)

freq= facet_df.groupby('facets').count().reset_index()
freq.columns = ['facets', 'occurence']
freq.sort_values(by=['occurence'], inplace=True, ascending=False)

df = pd.merge(freq, facet_df, on='facets', how='left')
final = df.groupby(['facets','occurence'])['original_phrase'].apply(lambda x: x.values.tolist())
final = final.reset_index().sort_values(by=['occurence'], ascending=False)
final['original_phrase'] = final['original_phrase'].apply(set)

final.to_csv('../../car_facet3.csv')