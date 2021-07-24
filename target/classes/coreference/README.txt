This relies on an external project "neuralcoref", which itself relies on "spaCy". It is
*EXTREMELY IMPORTANT* to get the correct versions of the two projects and of Python or
else they will not be compatible.

As of writing this (2021-7-21), the most updated configuration is:
-  Python 3.7.9
	https://www.python.org/downloads/release/python-379/

-  neuralcoref 4.0
	https://github.com/huggingface/neuralcoref
	pip install neuralcoref==4.0
	git checkout 754d470d

-  spaCy v2.1.0
	https://github.com/explosion/spaCy
	git checkout f0c1efcb

If you find a more recent configuration, please update this page.

