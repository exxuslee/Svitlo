# План проекта: Android-приложение с графиком отключений света

Nov 8, 2026 · @Exx

## Цель и главный вывод

Строим Android-приложение: пользователь выбирает область, город, улицу и дом и видит график отключений света на сегодня и завтра в виде четырёх круглых циферблатов.

Главное, что выяснилось: ha-svitlo-yeah — это интеграция для Home Assistant, а не готовый REST API. Она опрашивает сторонние источники каждые 15 минут и работает только с регионом (оператором) и группой, например 3.1. Выбора по улице и дому в ней нет, поэтому приложению нужен собственный шаг «адрес → группа».

Что это меняет в плане:

- Данные берём из тех же первоисточников, что и интеграция (раздел «Источники данных»), а не из самого ha-svitlo-yeah.
- Цепочку «область → город → улица → дом» можно сделать только там, где источник умеет искать группу по адресу. Для остальных регионов в приложении будет выбор группы вручную.
- Репозиторий ha-svitlo-yeah лицензирован как GPL-3.0: используем его как документацию, код не копируем, пока не решён вопрос лицензии приложения.

## Источники данных

Все источники отдают расписание по группе (очереди), а не по адресу. Каталог источников приложение хранит у себя: списка регионов в самих источниках нет, интеграция ha-svitlo-yeah тоже зашивает его в код.

| Источник | Регионы | Адрес данных |
| --- | --- | --- |
| [Yasno API](https://yasno.ua) | Киев (ДТЭК), Днепр (DnEM и CEK) | `https://app.yasno.ua/api/blackout-service/public/shutdowns/regions/{regionId}/dsos/{dsoId}/planned-outages`; Киев — 25/902, Днепр — 3/301 и 3/303 |
| [Baskerville42/outage-data-ua](https://github.com/Baskerville42/outage-data-ua/blob/main/data/kyiv-region.json) | Киевская область, Днепр и область, Одесса и область (ДТЭК) | JSON-файлы в `data/`, например `kyiv-region.json` |
| [yaroslav2901/OE\_OUTAGE\_DATA](https://github.com/yaroslav2901/OE_OUTAGE_DATA/blob/main/data/Lvivoblenerho.json) | Хмельницкая, Ивано-Франковская, Закарпатская, Львовская, Тернопольская, Черниговская, Запорожская, Житомирская, Полтавская, Ровенская области | JSON-файл на область в `data/` |
| [olnet93/gpv-voe-vinnytsia](https://github.com/olnet93/gpv-voe-vinnytsia/blob/main/data/Vinnytsiaoblenerho.json) | Винницкая область | JSON-файл, есть зеркало vn-progr |
| [E-Svitlo](https://sm.e-svitlo.com.ua/) | Сумская область | нужен логин и пароль личного кабинета, в MVP не берём |

### Формат JSON-файлов (проверено на kyiv-region.json)

- `fact.data[<unix-время начала суток>]["GPV3.1"]["1"…"24"]` — статус каждого часа суток; в файле обычно два ключа суток, сегодня и завтра.
- `fact.today` — ключ сегодняшних суток, `fact.update` и `lastUpdated` — время обновления.
- `preset.sch_names` — названия групп, например «Черга 3.1».
- Статусы часа: `yes` — свет есть, `no` — нет, `first` / `second` — нет первые / вторые 30 минут, `maybe`, `mfirst`, `msecond` — отключение возможно.

### Формат Yasno

По описанию сторонней документации, напрямую не проверено: `{группа: {today: {slots: […]}, tomorrow: {…}}}`, где слот — `{start, end, type}`, время в минутах от полуночи (0–1440), `type` — `Definite` или `Possible`. Слот означает отключение, отсутствие слота — свет есть. Поле `status` бывает `Planned` или `EmergencyShutdowns`. Формат нужно сверить на живом ответе на этапе 3.

### Как получить группу по адресу

Публичного API для этого нет. Сайты ДТЭК отдают данные через WordPress AJAX (`/ua/ajax`, нужен CSRF-токен): город → улицы → дома с группами. Это реверс-инжиниринг, на нём построен Python-клиент [dtek-blackout-client](https://pypi.org/project/dtek-blackout-client/). Сайт Yasno ищет по адресу, но публичный адрес запроса я не подтвердил. Поэтому адресный выбор — отдельный спайк (этап 2).

### Свежесть данных

По загруженной странице `kyiv-region.json` обновлялся 8 октября 2026, то есть месяц назад. README интеграции прямо предупреждает, что ДТЭК-источники перестают публиковаться, когда отключений нет. Приложение обязано показывать время обновления и состояние «данные устарели».

Sources: [ha-svitlo-yeah](https://github.com/ALERTua/ha-svitlo-yeah), [Baskerville42/outage-data-ua](https://github.com/Baskerville42/outage-data-ua/blob/main/data/kyiv-region.json), [описание Yasno API (DeepWiki)](https://deepwiki.com/chaichuk/UA-power-outages-monitor/2.3-yasno-api-integration), [dtek-blackout-client](https://pypi.org/project/dtek-blackout-client/).

## Архитектура

Три Gradle-модуля, зависимости идут в одну сторону: `app` и `data` зависят от `domain`, а `domain` не зависит ни от кого.

&#91;embedded content: модули app, data, domain · 3 зависимости\]

`data` реализует интерфейсы, объявленные в `domain`, а `app` подключает обе стороны только при сборке графа Koin.

| Модуль | Что содержит | Чего знать не должен |
| --- | --- | --- |
| `domain` | сущности, интерфейсы репозиториев, юзкейсы | Android, Ktor, Room, Koin, Compose |
| `data` | Ktor-клиент, Room, мапперы, реализации репозиториев | Compose, ViewModel, модуль `app` |
| `app` | Compose-экраны, ViewModel, навигация, Koin-модуль приложения | детали сети и базы: видит только интерфейсы и юзкейсы домена |

Экраны и ViewModel работают только с юзкейсами. Пакеты: в `domain` — `model`, `repository`, `usecase`; в `data` — как в разделе «Модуль data»; в `app` — `ui/address`, `ui/schedule`, `ui/dial`, `di`. В `settings.gradle.kts`: `include(":app", ":domain", ":data")`.

## Каталог версий (libs.versions.toml)

Все библиотеки и плагины описываются в одном файле `gradle/libs.versions.toml`, модули ссылаются на них через `libs.*`. Номера версий ниже — ориентир по памяти, не проверенный по свежим релизам: перед стартом обновите их (Android Studio подсвечивает устаревшие версии в toml).

```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
ksp = "2.1.0-1.0.29"
coroutines = "1.10.1"
serialization = "1.8.0"
datetime = "0.6.1"
composeBom = "2025.01.00"
activityCompose = "1.10.0"
navigationCompose = "2.8.5"
lifecycle = "2.8.7"
koin = "4.0.1"
ktor = "3.0.3"
room = "2.7.0"

[libraries]
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "datetime" }

compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-material3 = { module = "androidx.compose.material3:material3" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigationCompose" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }

koin-core = { module = "io.insert-koin:koin-core", version.ref = "koin" }
koin-android = { module = "io.insert-koin:koin-android", version.ref = "koin" }
koin-compose = { module = "io.insert-koin:koin-androidx-compose", version.ref = "koin" }

ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-content-negotiation = { module = "io.ktor:ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-json = { module = "io.ktor:ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }

room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
room = { id = "androidx.room", version = "2.7.0" }
```

Кто что подключает:

| Модуль | Тип | Зависимости из каталога |
| --- | --- | --- |
| `domain` | чистый Kotlin/JVM, без Android | coroutines-core, datetime |
| `data` | Android library | domain, ktor (core, okhttp, content-negotiation, serialization-json), room (runtime, ktx, ksp compiler), serialization, koin-core |
| `app` | Android application | domain, data, compose (bom, material3, ui), activity-compose, navigation-compose, lifecycle, koin (android, compose) |

Koin в `domain` не подключаем: юзкейсы остаются независимыми от DI и регистрируются в модуле `app`.

## Модуль domain

Домен — чистый Kotlin: сущности, интерфейсы репозиториев и юзкейсы. Он ничего не знает про Ktor, Room и Android, поэтому его целиком проверяют обычные unit-тесты с fake-репозиториями.

### Сущности

| Сущность | Поля | Зачем |
| --- | --- | --- |
| `Region` | id, name, providerId | область или оператор из каталога |
| `City`, `Street`, `House` | id, name; у `House` ещё number и groupId | цепочка выбора адреса |
| `Address` | region, city, street, house | выбранный адрес |
| `OutageGroup` | id (например «3.1»), name | группа из источника |
| `PowerState` | `ON`, `OFF`, `MAYBE`, `UNKNOWN` | состояние света |
| `DaySchedule` | date, halfHours: 48 значений `PowerState` | сутки в получасовом разрешении, из него строятся часы |
| `OutageSchedule` | group, today, tomorrow (может быть null), sourceUpdatedAt, fetchedAt | расписание группы |
| `Dials` | todayAm, todayPm, tomorrowAm, tomorrowPm | готовые данные для четырёх циферблатов |

Храним получасы, потому что источники дают статусы вроде «нет света первые 30 минут». Час при необходимости сворачивается из двух половин.

### Интерфейсы репозиториев

```kotlin
interface LocationRepository {
    suspend fun regions(): List<Region>
    suspend fun cities(regionId: String): List<City>
    suspend fun streets(cityId: String): List<Street>
    suspend fun houses(streetId: String): List<House>
    suspend fun groups(regionId: String): List<OutageGroup> // запасной выбор без адреса
}

interface ScheduleRepository {
    fun observe(selection: Selection): Flow<OutageSchedule?>   // кэш — источник правды
    suspend fun refresh(selection: Selection): Result<Unit>
}

interface SelectionRepository {
    fun observe(): Flow<Selection?>
    suspend fun save(selection: Selection)
}
```

`Selection` — либо адрес с группой, либо регион с вручную выбранной группой.

### Юзкейсы

- `GetRegions`, `GetCities`, `GetStreets`, `GetHouses`, `GetGroups` — шаги выбора.
- `SaveSelection`, `ObserveSelection` — сохранение выбора.
- `ObserveSchedule` — расписание выбранной группы из кэша.
- `RefreshSchedule` — принудительное обновление из сети.
- `ObserveDials` — собирает `Dials` из `OutageSchedule`: делит каждые сутки на «до обеда» (часы 0–11) и «после обеда» (12–23). Это чистая функция, её удобно покрыть тестами.

## Модуль data

Data реализует интерфейсы домена: ходит в сеть через Ktor, хранит кэш в Room и превращает ответы источников в сущности домена. Источник правды для экрана — база: сеть только обновляет её.

### Пакеты

```text
data/
  remote/    YasnoApi, GithubJsonApi, DtekAddressApi (если спайк удачен)
  dto/       YasnoDto, GpvJsonDto
  mapper/    YasnoMapper, GpvMapper → DaySchedule (48 получасов)
  catalog/   regions.json в assets: регион → тип источника → адрес данных
  local/     AppDatabase, SelectionDao, ScheduleDao, entities
  repository/ LocationRepositoryImpl, ScheduleRepositoryImpl, SelectionRepositoryImpl
  di/        dataModule (Koin)
```

### Ktor

- Один `HttpClient` с движком OkHttp, `ContentNegotiation` + kotlinx.serialization (`ignoreUnknownKeys = true`), `HttpTimeout` и `HttpRequestRetry`.
- GitHub raw часто отдаёт JSON как `text/plain`, поэтому парсим через `bodyAsText()` и `Json.decodeFromString`, а не полагаемся на content type.
- Парсинг терпимый: неизвестный статус превращается в `UNKNOWN`, а не в ошибку, потому что источники — чужие и могут поменять формат.

### Мапперы

- JSON-файлы: ключ суток → часы 1…24 → два получаса: `yes` → ON/ON, `no` → OFF/OFF, `first` → OFF/ON, `second` → ON/OFF, `maybe*` → MAYBE.
- Yasno: слоты `start`/`end` в минутах накладываются на 48 получасов; `Definite` → OFF, `Possible` → MAYBE, остальное ON.
- Сутки считаем в зоне `Europe/Kyiv`, а не в системной. Ключи суток в JSON — unix-время начала дня (в проверенном файле это полночь по киевскому времени), и в день перехода на зимнее время в сутках 25 часов, поэтому «+24 часа» не использовать.

### Room

| Таблица | Что хранит |
| --- | --- |
| `selection` | выбранный адрес или регион с группой (одна запись) |
| `schedule_cache` | группа, дата суток, 48 состояний (строкой или JSON), `sourceUpdatedAt`, `fetchedAt` |

При отсутствии сети показываем последнее сохранённое расписание с пометкой времени обновления.

### Обновление

`ScheduleRepositoryImpl.refresh` запрашивает источник, мапит ответ и записывает в Room; `observe` отдаёт `Flow` из базы. Обновляем при открытии экрана и по pull-to-refresh. Периодическое обновление раз в 15 минут через WorkManager (как в интеграции) — необязательный этап после MVP.

### Koin

`dataModule` создаёт `HttpClient`, базу, DAO, API-классы и регистрирует реализации репозиториев как `single<LocationRepository> { … }` и аналогично для остальных.

## Модуль app

App — единственная Activity на Compose с двумя экранами и Koin для внедрения зависимостей. Здесь живут ViewModel, UI и регистрация юзкейсов.

### Экраны

1. **Выбор адреса.** Четыре зависимых списка с поиском: область → город → улица → дом. Каждый следующий загружается после выбора предыдущего. Для регионов без адресного поиска вместо улицы и дома показывается выбор группы. Выбор сохраняется, при следующем запуске приложение сразу открывает график.
2. **График.** Шапка с адресом и группой, время последнего обновления, плашка «данные устарели», pull-to-refresh и четыре циферблата. Если расписание на завтра ещё не опубликовано, вместо двух правых циферблатов показывается заглушка.

Навигация — Navigation Compose с двумя маршрутами. `AddressViewModel` и `ScheduleViewModel` получают юзкейсы через `koinViewModel()`, состояние отдают как `StateFlow<UiState>`.

### Четыре циферблата

На экране две строки: «Сегодня» (до обеда, после обеда) и «Завтра» (до обеда, после обеда). Каждый циферблат — круг с 12 секторами, по одному на час, как на обычных часах: 12 вверху, отсчёт по часовой стрелке. Левый показывает часы 0–12, правый — 12–24, под каждым циферблатом подпись «0–12» или «12–24».

- Час со светом закрашивается светлым цветом, без света — тёмным. Цвета берём из темы, чтобы работали светлая и тёмная схемы.
- Сектор рисуется в Compose `Canvas` через `drawArc`, весь циферблат — один компонент `OutageDial(hours: List<HourState>)`.
- Каждый час состоит из двух половин по 15°, потому что источники умеют «нет света первые или вторые 30 минут». Если обе половины одинаковы, сектор выглядит как цельный час.
- На сегодняшнем циферблате текущий час обведён, прошедшие часы слегка приглушены.
- Метки 12, 3, 6, 9 по краям; для доступности у компонента есть `contentDescription` вида «с 18 до 22 света нет».

### Тесты UI

Превью и скриншот-тесты циферблата на трёх случаях: весь день со светом, весь день без света, смешанный день с отключениями на полчаса.

## Этапы реализации

Спайк по адресам стоит рано, потому что его исход решает, каким будет первый экран. Этапы 2 и 3 можно вести параллельно.

1. **Каркас.** Три модуля, `libs.versions.toml`, плагины, пустые Koin-модули, сборка в CI. Готово, когда проект собирается, `app` видит `domain` и `data`, а `domain` не тянет Android-зависимости.
2. **Спайк «адрес → группа».** Проверить на живых запросах ДТЭК-сайтов (город → улицы → дома → группа) и поиск по адресу у Yasno. Результат — решение: делаем адресный поиск (напрямую или через собственный прокси) или остаёмся на выборе региона и группы. Готово, когда решение записано в этом плане.
3. **Domain.** Сущности, интерфейсы, юзкейсы и unit-тесты с fake-репозиториями. Готово, когда `ObserveDials` проходит тесты на сутках из ДТЭК и Yasno.
4. **Data: источники.** Ktor-клиент, DTO, мапперы, каталог регионов, тесты на сохранённых JSON-фикстурах (в том числе Yasno, чтобы сверить формат). Готово, когда расписание Киева (Yasno) и Киевской области (JSON) превращается в 48 получасов на сегодня и завтра.
5. **Data: кэш.** Room, реализации репозиториев, обновление, работа без сети. Готово, когда график открывается без сети из кэша и показывает время обновления.
6. **UI: выбор.** Экран выбора адреса (или региона и группы) с сохранением. Готово, когда после перезапуска приложение открывает сохранённый выбор.
7. **UI: циферблаты.** `OutageDial`, экран графика, скриншот-тесты. Готово, когда четыре циферблата совпадают с данными источника на трёх контрольных днях.
8. **Полировка.** Плашка «данные устарели», заглушка «завтра ещё нет», тёмная тема, доступность, иконка. Готово, когда пройдены ручные сценарии: нет сети, устаревший файл, смена зимнего времени.
9. **Релиз.** R8, подпись, внутреннее тестирование в Google Play.

После MVP: периодическое обновление через WorkManager, уведомление за 30 минут до отключения, виджет на рабочий стол.

## Риски и открытые вопросы

Ниже — что требует решения и что я заложил по умолчанию. Скажите, если нужно иначе, план поправлю.

| Вопрос или риск | Что заложено по умолчанию |
| --- | --- |
| Нет публичного API «адрес → группа»; у ДТЭК это AJAX с CSRF и возможной защитой от ботов | Спайк на этапе 2: публичный стабильный API не подтверждён. MVP — выбор региона и группы вручную; адресный поиск не входит в первую версию |
| Охват: только Киев и Днепр через Yasno и около 15 областей через GitHub-JSON | Каталог ограничен этими регионами, остальные не показываем |
| Источники — чужие репозитории без гарантий: могут исчезнуть или сменить формат | Терпимый парсинг, каталог источников в assets, обновляется релизами |
| Данные устаревают (при отсутствии отключений ДТЭК-файлы перестают обновляться) | Показываем время обновления и плашку «данные устарели» |
| Статус «возможно» (`maybe`) и получасовые статусы при двух цветах циферблата | Получасы делят сектор пополам; «возможно» рисуем светлым с тёмным контуром. Если нужны строго два цвета, это один флаг в `OutageDial` |
| Лицензии: ha-svitlo-yeah под GPL-3.0, условия данных в других репозиториях не проверены | Код интеграции не копируем; перед публикацией проверить условия использования данных |
| Расписание на завтра публикуется не сразу | Заглушка вместо двух циферблатов «Завтра» |
| Язык интерфейса | Украинский как основной, русский вторым — уточните, нужен ли русский |
