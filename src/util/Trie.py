import pandas as pd

df = pd.read_csv('car_facet.csv')
print(len(df))
del df['Unnamed: 0']
df.head(10)

class TrieNode():
    def __init__(self, parent): 
        # Initialising one node for trie
        self.children = {}
        self.parent = parent # for backtracking during traversal
        self.leaf = False
        self.occ = 0 # initialize the number of occurrences
    
    def addOccurrence(self, multiplicity=1):
        self.occ += multiplicity
    

class Trie():
    def __init__(self):
        # Initialising the trie structure.
        self.root = TrieNode()
        self.root.leaf = True
        self.word_list = []
        self.leaves = set(self.root) # for searching it later

    def formTrie(self, keys):
        # Forms a trie structure with the given set of strings
        # if it does not exists already else it merges the key
        # into it by extending the structure as required
        for key in keys:
            self.insert(key) # inserting one key to the trie.

    def insert(self, key, multiplicity=1):
        # Inserts a key into trie if it does not exist already.
        # And if the key is a prefix of the trie node, just
        # marks it as leaf node.
        node = self.root
        last = False # whether we add new nodes during processing

        for a in list(key):
            if not node.children.get(a):
                last = True # if we create new nodes, then result is leaf
                # if the parent thought it was a leaf, update it
                if node.leaf:
                    node.leaf = False
                    self.leaves.remove(node)
                node.children[a] = TrieNode()

            node = node.children[a]

        if last:
            node.leaf = True
            self.leaves.add(node)
        node.addOccurrence(multiplicity)

    def search(self, key):
        # Searches the given key in trie for a full match
        # and returns what was found, if any.
        node = self.root
        found = True

        for a in list(key):
            if not node.children.get(a):
                found = False
                break
                
            node = node.children[a]

        if not found or node.multiplicity==0:
            return None
        return node

    def suggestionsRec(self, node, word):
 
        # Method to recursively traverse the trie
        # and return a whole word.
        if node.occ > 0:
            self.word_list.append(word)

        for a,n in node.children.items():
            self.suggestionsRec(n, word + a)

    def printAutoSuggestions(self, key):
 
        # Returns all the words in the trie whose common
        # prefix is the given key thus listing out all
        # the suggestions for autocomplete.
        node = self.root
        not_found = False
        temp_word = ''

        for a in list(key):
            if not node.children.get(a):
                not_found = True
                break

            temp_word += a
            node = node.children[a]

        if not_found:
            return 0
        elif node.occ > 0 and not node.children:
            return -1, []

        self.suggestionsRec(node, temp_word)

        return 1,self.word_list


# Driver Code
keys = df['facets'].tolist() # keys to form the trie structure.
# creating trie object
t = Trie()

# creating the trie structure with the
# given set of strings.
t.formTrie(keys)

def search_query(key):
    status = ['Not found', 'found']
    comp,list_query = t.printAutoSuggestions(key)
    if comp == -1:
        print("No other strings found with this prefix\n")
        return -1
    elif comp == 0:
        print("No string found with this prefix\n")
        return -1
    else:
        return df.loc[df['facets'].isin(list_query)]
        
#             print(df.loc[df['facets']==q]['occurence'].values[0],' ', q)

result = search_query('car')
result.head(20)