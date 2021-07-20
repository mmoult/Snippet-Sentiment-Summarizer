The reviews and metadata files have been split up to be able to be saved on GitHub.
To get them back into a usable state, use
  $ cat reviews*.7z > reviews.7z
and
  $ cat metadata*.7z > metadata.7z
then extract the files from the two generated 7zip archives. This should give the
meta*.txt series and the review*.txt series as referenced by FileResources.java.