# Firebase

Проект: `svitlo-play-releases-exxuslee` (Spark).
Android package: `org.exxlexxlee.svitlo`.
Firebase App ID: `1:452907115683:android:66550f8b82f97f180b8b16`.

`app/google-services.json` — клиентская конфигурация, необходимая также для CI.
Она не является ключом сервисного аккаунта. Серверные ключи и JSON сервисных
аккаунтов нельзя помещать в Android-приложение или Git.

## Crashlytics

SDK автоматически собирает падения и ANR. В debug сборках автоматическая
отправка выключена, в release включена. Обработанные ошибки загрузки графиков
сами по себе не являются падениями и отдельно не отправляются.
`FirebaseCrashContext` добавляет в отчёт ключ `screen` из общего списка
`AppScreen`. Имена мест, аргументы маршрута и содержимое push туда не попадают.
Настройки сбора остаются в manifest placeholders; адаптер их не переопределяет.

Release workflow включает `-PuploadCrashlyticsMapping=true` при сборке AAB:
Crashlytics Gradle plugin загружает соответствующий R8 mapping в Firebase.
Для локальных сборок и обычного CI загрузка выключена. При ручной публикации
собирайте AAB с тем же параметром. Ошибка загрузки mapping должна быть устранена
до публикации, иначе отчёты об обфусцированном коде будут трудны для чтения.

Проверка после установки release: временно добавить тестовое падение в локальную
сборку, воспроизвести, снова открыть приложение для отправки отчёта и проверить
[Crashlytics](https://console.firebase.google.com/project/svitlo-play-releases-exxuslee/crashlytics).
Тестовый код не должен попадать в публикацию.

## Analytics

SDK собирает стандартные события использования. `TrackNavigation` подписывается
на Navigation в `DisposableEffect` и снимает слушатель при выходе из композиции.
`ScreenTracker` сопоставляет шаблон маршрута с `AppScreen` и передаёт его
адаптерам `FirebaseScreenAnalytics` и `FirebaseCrashContext`, полученным через Koin.
Analytics отправляет
`screen_view` для `home`, `address`, `settings`, `about`; автоматическое
отслеживание Activity выключено для исключения дублирования. Имена мест,
placeId и аргументы маршрутов в события не передаются. Сбор advertising ID
и сигналы персонализации рекламы выключены; разрешение AD_ID удалено.

Для отчётов проект должен быть связан с Google Analytics в Firebase Console →
Project settings → Integrations → Google Analytics. Связь нужно проверить:
она не создаётся добавлением SDK или регистрацией Android-приложения через CLI.

Для проверки на устройстве:

```powershell
adb shell setprop debug.firebase.analytics.app org.exxlexxlee.svitlo
# Откройте приложение и переключайте экраны.
# После проверки:
adb shell setprop debug.firebase.analytics.app .none.
```

[Analytics DebugView](https://console.firebase.google.com/project/svitlo-play-releases-exxuslee/analytics/debugview)
показывает события устройства, пока включён debug mode.

## FCM

FCM SDK автоматически получает и обновляет токен установки. Токены не пишутся
в логи и не отправляются на собственный сервер: серверной регистрации устройств
в приложении пока нет. Для массовых notification-сообщений можно использовать
Firebase Console → Messaging с целевой аудиторией Android-приложения.

Notification payload отображается SDK в фоне; в открытом приложении его
показывает SvitloMessagingService. Также поддерживается data payload с полями
`title` и `body`. Сервис преобразует payload через `PushMessage.fromPayload`,
а `PushNotifications`, полученный через Koin, проверяет разрешение и показывает
уведомление. Пустые поля notification заменяются соответствующими полями data;
сообщения без заголовка и текста игнорируются. Канал задан одним ресурсом
`push_channel_id` для SDK и собственного показа, его ID остаётся `service_messages`.
При нажатии открывается главный экран; произвольные ссылки
и команды из payload не выполняются. Отдельный канал «Повідомлення сервісу»
не меняет настройки локальных напоминаний. На Android 13+ используется
существующий запрос разрешения POST_NOTIFICATIONS.

Проверять доставку нужно с Google Play services и интернетом: в открытом
приложении, в фоне, при запрещённых уведомлениях и при нажатии на сообщение.
После принудительной остановки через настройки Android нужно заново открыть
приложение. FCM не заменяет точные локальные напоминания.

Автоматические сообщения об изменении графика требуют отдельного серверного
процесса, который отслеживает источники и отправляет FCM. Эта интеграция
подключает клиентский приём сообщений, а не мониторинг источников на сервере.

## Публикация и данные

Перед выпуском обновить политику конфиденциальности и декларацию Data safety
в Google Play с учётом Analytics, Crashlytics и Firebase Installations/FCM.
Проверять фактический сбор данных по документации используемых SDK:
[раскрытие данных](https://firebase.google.com/docs/android/play-data-disclosure).

Проверка компиляции не подтверждает доставку событий и сообщений в Firebase.
