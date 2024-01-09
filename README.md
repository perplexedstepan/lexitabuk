# LexiTabuk
LexiTabuk is an Android app which can be used by first-year students studying at the University of Tabuk in order to help them practice their English pronunciation. The interface is meant to supply them with 3 different things:
1.   Course selection
2.   Unit selection
3.   Word/Phrase selection

## Course Selection

Upon opening the app, users are prompted to choose which course they wish to practice with. These are based on the University of Tabuk's course numbers
*   ELS 1101
*   ELS 1103
*   ELS 1104

## Unit Selection

After choosing their class, students are able to choose the specific list of vocabulary they wish to practice with.

## Word/Phrase Selection

After choosing the unit or list of vocabulary, students can select the word or phrase in order to hear their phone's text-to-speech pronounce them. They can then record their own voice using the in-app feature by pressing the record button. After this, students can listen to their own voice pronouncing the word/phrase and tap on the word or phrase again in order to hear the proper pronunciation again.

## Request of Permissions

Upon installing, the app will ask for permission to access this microphone. This can be either accepted or rejected. If accepted, the user will be able to use app in the properly meant way. However, if the user refuses to give permission, the app will still function, but as a reading device since only the text-to-speech feature will be available to use.

## Analytics Collected

This app is set up to collect analytics using the basic events (e.g. `first_open`, `session_start`, `app_remove`, etc.) as well as some custom analytics including:
*   `select_class`: which will allow developers to identify the number of users enrolled in each course using the app
*   `select_topic`: which will allow developers to identify which word lists students are using for practice
*   `select_phrase`: which will allow developers to identify which word or phrase students are selecting the most (ideally - which words or phrase students are having the most difficulty with which can be identified for additional in-class practice in the future)

