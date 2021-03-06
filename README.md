# Flashcards

* Concepts:
  * Card: A front (question) & back (answer) pair, along with additional metadata.
  * Card group: One or more cards that will be reviewed together. All cards in a group must be
    answered correctly to pass. A card groups has an SRS stage.
  * Card source: A set of card groups available for lessons or review, drawn from a single
    source. There are multiple implementations:
    * Custom source: Card groups stored in the app
    * Wanikani source: Cards (and their SRS stage) sourced from a Wanikani account. As new
      items become unlocked, they become available in this set.
  * Deck: One or more card sources that will be mixed together for reviews.
  * Account: Many sources and many decks

## TODO

* Features
  * Deck overview screen: Review Forecast
  * Review summary improvements
    * Split into two columns, correct vs missed
    * show new SRS stage
  * Add created_dt column to deck and card source tables, sort by created_dt when querying
  * Block-list and close-list support in reviews
    * Block-list: Hidden list of inputs that are rejected, even if close to an answer
    * close-list: Hidden list of inputs that are close but not correct, e.g., reading for a meaning card
  * Improve WK data load performance
  * QoL: Make custom card editor easier to create a large amount of cards
    * Maybe JSON/CSV import?
  * Review screen: Wrap up
  * View SRS stages on custom card editor
  * QoL: Add synonyms from review screen details foldout
  * Implement source and deck deletion
  * Change screen transitions to not blink (no loading text until 500ms have passed)
* Bugs
  * History: Reloading the page doesn't clear history, so need to handle or prevent going 
    "back" from first page