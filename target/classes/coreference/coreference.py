import spacy
# Load your usual SpaCy model (one of SpaCy English models)
nlp = spacy.load('en_core_web_sm')

# Add neural coref to SpaCy's pipe
import neuralcoref
# The blacklist is to prevent evaluation of first person and second person pronouns (I, me, you, your)
neuralcoref.add_to_pipe(nlp, blacklist=True)
# You're done. You can now use NeuralCoref as you usually manipulate a SpaCy document annotations.

# Get the sentence to analyze from the command line args
import sys
# print('Number of arguments:', len(sys.argv), 'arguments.')
# print('Argument List:', str(sys.argv))

sentence = sys.argv[1]
doc = nlp(sentence)

# doc._.has_coref
# doc._.coref_clusters
# print(doc._.coref_clusters)

# Lightweight duplicate classes since the spaCy span will not let me modify the range
class SpanDup:
    def __init__(self, start, end, val):
        self.start = start
        self.end = end
        self.string = val.strip()


class ClusterDup:
    def __init__(self, main):
        self.main = main
        self.spans = []
    def add(self, span):
        self.spans.append(span)


def updateClusters(clusters, delta, at):
    for cluster in clusters:
        for span in cluster.spans:
            if span.start > at:
                span.start += delta
            if span.end > at:
                span.end += delta


# Convert the returned clusters into clusters that we can use
clusters = []
for cluster in doc._.coref_clusters:
    main = SpanDup(cluster.main.start_char, cluster.main.end_char, cluster.main.string)
    myCluster = ClusterDup(main)
    for mention in cluster.mentions:
        if mention is cluster.main:
            continue
        myCluster.add(SpanDup(mention.start_char, mention.end_char, mention.string))
    clusters.append(myCluster)


def isPossessive(word):
    return word == "her" or word == "his" or word == "their" or word == "its" or \
            word == "theirs" or word == "our" or word == "ours"


# Now we can go through and resolve
ret = sentence   
for cluster in clusters:
    # We can resolve multiple references in the same sentence as long as they aren't for the same source
    if len(cluster.spans) > 0 and cluster.main.end <= cluster.spans[0].start:
        lastEnd = cluster.main.end
    else:
        lastEnd = -1
    
    for span in cluster.spans:
        # We only want to make one replacement per sentence since more is redundant
        # and can be confusing.
        if lastEnd != -1:
            # Therefore, we try to find punctuation between end of the last and beginning of this
            punctFound = False
            for i in range(lastEnd, span.start):
                if ret[i] == '.' or ret[i] == '|':
                    punctFound = True
                    break
            if not punctFound:
                lastEnd = span.end
                continue # if no punctuation was found, we don't want to resolve this instance
        
        # We want to match the case of the word/phrase being replaced
        if not span.string.islower():
            replacement = cluster.main.string.capitalize()
        else:
            replacement = cluster.main.string.lower()
        # we may want to put a possessive marker on it if the pronoun was possessive
        if isPossessive(span.string):
            if cluster.main.string.lower() == "it":
                replacement += "s"
            else:
                replacement += "'s"
        ret = ret[0:span.start] + replacement + ret[span.end:]
        #print(span.string, span.start, span.end, isPossessive(span.string))
        updateClusters(clusters, len(replacement) - len(span.string), span.start)
        lastEnd = span.start + len(replacement)
 
print(ret)

