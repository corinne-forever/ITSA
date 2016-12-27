# ITSA (Individual Twitter Sentiment Analysis)
ITSA is a tool for collecting tweets from specific twitter users and applying sentiment analysis to the tweets. This analysis is applied in three stages through a CLI. The results are stored in CSV files for further analysis.

## Dependencies
It is recommended that you place the dependencies in a folder called `lib`, otherwise you will need to change some code for SentiStrength.

* [Twitter4J 4.0.4](http://twitter4j.org/archive/twitter4j-4.0.4.zip)
* [Stanford CoreNLP 3.6.0](http://nlp.stanford.edu/software/stanford-corenlp-full-2015-12-09.zip)
* [Apache Commons CSV 1.4](https://commons.apache.org/proper/commons-csv/download_csv.cgi)
* [SentiStrength](http://sentistrength.wlv.ac.uk/)
	* SentiStrength Dec 2015 Data - place in `lib/sentistrength_data` or modify the code accordingly
	* SentiStrength jar

## Stages
This program goes through 3 stages. Through the CLI you may select which stages to run. The results from each stage are stored in CSV files

These stages are as follows:

1. Data collection
	* Tweets are collected from twitter from the users defined in `itsa.properties`
2. Normalization
	* Tweets are stripped of their links, case, mentions, setc and hashtags
3. Sentiment analysis
	* Positive and negative scores are calculated for each tweet

## Limitations
* The twitter API limits the number of tweets that can be collected from a user to 3200
* No modularity in first phase, must be done manually
* Limited configuration options. Most tweaks will need to be made in the code.
