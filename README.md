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
  * Allow disabling lessons for certain sources per deck
  * Improve WK data load performance
  * Review screen: Wrap up
  * QoL: Add synonyms from review screen details foldout
  * Implement source and deck deletion
  * Change screen transitions to not blink (no loading text until 500ms have passed)
  * Allow reordering sources and decks on mobile (use touch api instead of dnd)
* Bugs
  * History: Reloading the page doesn't clear history, so need to handle or prevent going 
    "back" from first page
  * Wanikani sources seems to lose connection easily on mobile during reviews
