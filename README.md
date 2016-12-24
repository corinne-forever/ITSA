# ITSA (Individual Twitter Sentiment Analysis)


## Dependencies
* Twitter4J 4.0.4
* Stanford CoreNLP 3.6.0

## Usage
This program goes through 3 stages. Through the CLI you may select which stages to run. These stages are as follows:
1. Data collection
	* Tweets are collected from twitter from the users defined in `itsa.properties`
2. Normalization
	* Tweets are stripped of their links, case, mentions, setc and hashtags
3. Sentiment analysis
	* Positive and negative scores are calculated for each tweet
	