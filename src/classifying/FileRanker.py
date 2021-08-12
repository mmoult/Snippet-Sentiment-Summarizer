import spacy
import sys

nlp = spacy.load("en_core_web_lg")  # make sure to use larger package!

# arg1: the query
# arg2: a list of the files to load
# return: the file that best matched the query

query = nlp(sys.argv[1])
bestScore = 0
best = None

paths = sys.argv[2].split(';')

for path in paths:
    file = open(path, 'r')
    text = ''
    
    while True:
        # Get next line from file
        line = file.readline()
        
        # if line is empty
        # end of file is reached
        if not line:
            break
        
        # line without any data
        startToken = '"words": '
        index = line.find(startToken)
        if index == -1:
            continue
        
        text += line[(index + len(startToken)):]
        
    file.close()
    
    doc = nlp(text)
    similarity = query.similarity(doc)
    # higher similarity is better
    if similarity > bestScore:
        bestScore = similarity
        best = path

print(path)
