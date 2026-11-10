# Автоматический релиз в Google Play

Workflow `.github/workflows/release.yml` запускается после отправки тега `vX.X.X`
в GitHub: проверяет тег, запускает unit-тесты, собирает подписанный release AAB
с R8, сохраняет AAB и `mapping.txt` в артефактах Actions на 30 дней и отправляет
релиз приложения `org.exxlexxlee.svitlo` в **production** со статусом `completed`
(полное распространение).

## Настроенное окружение

Для Svitlo создан отдельный Google Cloud проект `svitlo-play-releases-exxuslee`
(Svitlo Releases), в котором включён Google Play Android Developer API.
Сервисный аккаунт
`svitlo-github-releases@svitlo-play-releases-exxuslee.iam.gserviceaccount.com`
имеет доступ только к приложению `org.exxlexxlee.svitlo`: чтение информации
и выпуск рабочей версии. Его JSON-ключ хранится в repository secret
`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`. Все пять секретов из таблицы ниже настроены.

9 ноября 2026 года проверены доступ к production через API, совпадение сертификата
ключа загрузки с локальным keystore и выключенная управляемая публикация.
Текущий production-релиз на момент проверки — `3 (1.3)`, `versionCode = 3`.

## Однократная настройка

Интеграция Firebase и проверка сбора данных описаны в [firebase.md](firebase.md).
Release workflow загружает R8 mapping в Crashlytics; обычный CI этого не делает.

1. В Google Play Console должно существовать приложение с этим package name,
   настроенной Play App Signing, заполненными обязательными разделами и доступом
   к production. Если приложение ещё не публиковалось, первую сборку загрузите
   через Play Console вручную.
2. Подготовьте **существующий upload key** приложения: JKS/keystore, пароль
   хранилища, alias и пароль ключа. Ключ должен соответствовать сертификату
   загрузки в Play Console → App integrity. Новый случайный ключ для существующего
   приложения не подойдёт.
3. В Google Cloud включите **Google Play Android Developer API**, создайте
   сервисный аккаунт и скачайте его JSON-ключ. В Play Console → Users and
   permissions пригласите email этого аккаунта и выдайте доступ только к Svitlo:
   просмотр информации о приложении и разрешение **Release to production,
   exclude devices, and use Play App Signing**.
4. В [GitHub → Settings → Secrets and variables → Actions](https://github.com/exxuslee/Svitlo/settings/secrets/actions)
   создайте repository secrets:

   | Секрет | Значение |
   | --- | --- |
   | `ANDROID_KEYSTORE_BASE64` | Содержимое upload keystore в Base64, одной строкой |
   | `ANDROID_KEYSTORE_PASSWORD` | Пароль хранилища |
   | `ANDROID_KEY_ALIAS` | Alias ключа загрузки |
   | `ANDROID_KEY_PASSWORD` | Пароль ключа загрузки |
   | `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Полное содержимое JSON-ключа сервисного аккаунта, без Base64 |

   Подготовить Base64 в PowerShell, скопировав в буфер обмена:

   ```powershell
   [Convert]::ToBase64String([IO.File]::ReadAllBytes('D:\secure\svitlo-upload.jks')) | Set-Clipboard
   ```

   Ключи и пароли не добавляйте в Git. Без всех пяти секретов workflow завершится
   с сообщением о недостающем секрете до сборки.
5. Для автоматического появления обновления у пользователей отключите
   **Managed publishing** в Play Console. Workflow отправляет изменения на
   проверку Google; время модерации не контролируется GitHub Actions.

## Версии

`versionName` берётся из тега без `v`. `versionCode` вычисляется по формуле:

```text
MAJOR * 1 000 000 + MINOR * 1 000 + PATCH
v1.3.1 → versionName 1.3.1, versionCode 1003001
```

Компоненты — целые числа без ведущих нулей; MINOR и PATCH от 0 до 999.
Результат должен быть от 1 до 2 100 000 000. Prerelease-теги вроде
`v1.4.0-beta.1` не публикуются: проверка тега завершит workflow с ошибкой.
Каждый следующий релиз должен иметь больший `versionCode`, чем все ранее
загруженные сборки, включая тестовые треки. Перед первым запуском сравните
вычисленный код с максимальным кодом в Play Console; сейчас локальное значение — 3.
Повторная загрузка уже опубликованного кода будет отклонена Google Play.

Локальные сборки без параметров сохраняют версию `1.3` / `3` и не требуют ключа.
CI передаёт `-PreleaseVersionName` и `-PreleaseVersionCode`; подпись подключается
через переменные окружения `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.

## Запуск релиза

Сначала закоммитьте и отправьте изменения, включая workflow. Создавайте тег на
коммите, который должен попасть в Google Play:

```bash
git tag -a v1.3.1 -m "Release 1.3.1"
git push origin v1.3.1
```

Локальное создание тега не запускает Actions: нужен `git push` тега в GitHub.
Публикация выполняется только после успешных тестов и сборки. Состояние и
артефакты доступны в [Actions](https://github.com/exxuslee/Svitlo/actions).
Новый релиз запускайте после завершения предыдущего: публикации сериализованы
группой concurrency, GitHub сохраняет только один ожидающий запуск в группе.

### Повторный запуск после исправления workflow

Чтобы собрать существующий тег с актуальной настройкой CI, откройте
Actions → Release to Google Play → Run workflow, выберите ветку `master`
и укажите тег в поле `release_tag`, например `v1.3.1`. Через CLI:

```bash
gh workflow run release.yml --ref master -f release_tag=v1.3.1
```

Workflow берётся из выбранной ветки, а код приложения — строго из указанного
тега. Переносить или пересоздавать тег не требуется. Используйте этот запуск,
если сборка ещё не загружена в Google Play: уже использованный `versionCode`
повторно загрузить нельзя. Кнопка Re-run jobs использует прежнюю версию
workflow и поэтому не подхватывает исправления из нового коммита.

## Официальная документация

- [Настройка доступа к Google Play Developer API](https://developers.google.com/android-publisher/getting_started)
- [Версии Android-приложения](https://developer.android.com/studio/publish/versioning)
- [Модерация и Managed publishing](https://support.google.com/googleplay/android-developer/answer/9859654)
- [Параметры upload-google-play](https://github.com/r0adkll/upload-google-play)
