# Отслеживание экранов Firebase Analytics

Передаются только постоянные имена экранов, без placeId, названий мест и аргументов навигации.
Отслеживание централизовано в `TrackNavigation` через `OnDestinationChangedListener`.
`ScreenTracker` использует список `AppScreen` для Analytics и контекста Crashlytics.
Вызовы SDK изолированы в адаптерах модуля Koin `firebaseModule`.

| TaskID | Completed | Title | Description | File Path |
| --- | --- | --- | --- | --- |
| 1 | [x] | Home | Графики | `app/src/main/kotlin/org/exxlexxlee/svitlo/features/home/HomeScreen.kt` |
| 2 | [x] | Address | Добавление и редактирование места | `app/src/main/kotlin/org/exxlexxlee/svitlo/features/address/AddressScreen.kt` |
| 3 | [x] | Settings | Настройки | `app/src/main/kotlin/org/exxlexxlee/svitlo/features/settings/SettingsScreen.kt` |
| 4 | [x] | About | О приложении | `app/src/main/kotlin/org/exxlexxlee/svitlo/features/settings/AboutScreen.kt` |
