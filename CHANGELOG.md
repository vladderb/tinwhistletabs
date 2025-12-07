# Changelog

## [Unreleased] - 2025-12-07

### Added
- **ABC Parser**: Full ABC notation parser for importing songs from ABC format
  - Supports all standard ABC headers (T:, M:, L:, K:, Q:, R:)
  - Automatic transposition to D whistle range (54-78)
  - Avoids half-holed notes when possible by trying octave shifts (-12, 0, +12)
  - Key signature support for major and minor keys
  - Tempo parsing with beat duration support

- **Custom Songs**: Ability to add custom songs via ABC notation
  - Dialog for adding songs with title, author, type, and ABC notation
  - Auto-fill title from ABC T: header
  - Validation and parsing before saving
  - Custom songs stored separately from built-in songs

- **ABC Editor**: Edit ABC notation for custom songs
  - View and edit ABC notation in a dialog
  - Copy ABC to clipboard
  - Save changes with automatic re-parsing
  - Only available for custom songs

- **Trash System**: Soft delete for built-in songs with 30-day retention
  - Move built-in songs to trash instead of permanent deletion
  - Restore songs from trash
  - Automatic permanent deletion after 30 days
  - Shows days remaining until permanent deletion
  - Separate trash activity with restore/delete buttons

- **Sheet Music View**: Display ABC notation as sheet music with tablature
  - Integrated ABCJS 6.5.1 for rendering sheet music
  - Shows both standard notation and tin whistle tablature
  - Click notes to jump to that position
  - Auto-scroll during playback
  - Highlight current note during playback
  - Toggle between text tabs and sheet music view

- **Favorites System**: Mark songs as favorites and filter by favorites
  - Star button on each song to toggle favorite status
  - Favorites button in action bar to show only favorite songs
  - Favorites stored using SharedPreferences
  - Visual feedback with filled/outlined star icons

- **Sorting Options**: Multiple ways to sort the song list
  - Sort by name (A-Z or Z-A)
  - Sort by favorites first
  - Sort by song type (Reel, Jig, etc.)
  - Sort menu accessible from action bar

- **Delete Functionality**: Delete songs from main screen and song view
  - Delete button on each song in the list
  - Delete button (trash icon) in song view action bar
  - All songs (built-in and custom) move to trash with 30-day retention
  - Consistent delete dialog across the app

- **Metronome**: Built-in metronome synchronized with music playback
  - Toggle metronome on/off with dedicated button
  - Automatically syncs with current tempo (BPM)
  - Visual feedback when metronome is enabled
  - Starts/stops with music playback

### Fixed
- **Half-holed notes bug**: Fixed automatic transposition algorithm
  - Added octave down (-12) check before trying other shifts
  - Now correctly avoids half-holed notes for songs in E minor and similar keys
  - Transposition order: 0 → +12 → -12 → other shifts (-24 to +24)

- **Add Song Dialog UX improvements**:
  - Back button now closes keyboard first, then dialog
  - ScrollView with maxHeight allows access to Save button when keyboard is open
  - Title auto-fills from ABC T: header
  - Added hints and tips in the dialog

- **Tempo control**: Fixed tempo not affecting playback speed
  - Tempo modifier now correctly calculated based on BPM
  - Music plays faster/slower according to tempo setting
  - Formula: tempoModifier = currentTempo / defaultTempo (100 BPM)

- **Key transposition on playback**: Fixed selected key not applied to audio
  - Current key setting now applied when opening a song
  - Audio playback uses correctly transposed notes
  - Tablature display matches audio transposition

### Changed
- **TabActivity**: Moved escape() method from deleted SheetActivity
  - escape() method now part of TabActivity for JavaScript string escaping
  - Removed SheetActivity declaration from AndroidManifest.xml

- **MainActivity**: Added trash menu item and custom song management
  - New "Add Song" button in action bar
  - New "Trash" button to view deleted songs
  - Integration with CustomSongsManager and TrashManager

- **MusicDB**: Extended to support custom songs
  - open() method now handles both built-in and custom song files
  - Custom songs use "custom_" prefix, built-in use "builtin_" prefix
  - ABC parsing integrated for all song types

- **MusicSheet**: Added ABC structure parsing for line breaks
  - notesToTabsWithLineBreaks() preserves ABC line structure
  - parseABCStructure() matches tabs to ABC notation lines
  - Improved tab spacing based on note duration



### Technical Details
- **Dependencies**: Updated ABCJS from 5.11.0 to 6.5.1
- **New Icons**: 
  - ic_trash.xml - trash/bin icon
  - ic_delete.xml - delete icon
  - ic_stop.xml - stop playback icon
  - ic_star.xml - filled star icon for favorites
  - ic_star_border.xml - outlined star icon
  - ic_sort.xml - sort icon
  - ic_metronome.xml - metronome icon
- **New Layouts**:
  - activity_trash.xml - trash activity layout
  - trash_item_layout.xml - trash item card layout
  - dialog_abc.xml - ABC editor dialog layout
  - dialog_add_custom_song.xml - add song dialog layout
- **Architecture**: 
  - ABCParser handles all ABC notation parsing
  - CustomSongsManager handles custom song storage (SharedPreferences)
  - TrashManager handles soft delete functionality (SharedPreferences)
  - FavoritesManager handles favorite songs (SharedPreferences)
  - Metronome class generates click sounds synchronized with tempo
  - MusicDB.open() now supports both built-in and custom songs
- **Storage**:
  - Custom songs stored in SharedPreferences as JSON
  - Trash items stored in SharedPreferences with deletion timestamp
  - Built-in songs remain in db.json (res/raw)

---

# История изменений

## [Unreleased] - 2025-12-07

### Добавлено
- **ABC Parser**: Полный парсер ABC нотации для импорта мелодий из формата ABC
  - Поддержка всех стандартных заголовков ABC (T:, M:, L:, K:, Q:, R:)
  - Автоматическая транспозиция в диапазон свистка D (54-78)
  - Избегает полузакрытых отверстий при возможности, пробуя сдвиги октав (-12, 0, +12)
  - Поддержка тональностей для мажорных и минорных ключей
  - Парсинг темпа с поддержкой длительности долей

- **Пользовательские мелодии**: Возможность добавлять свои мелодии через ABC нотацию
  - Диалог добавления мелодий с названием, автором, типом и ABC нотацией
  - Автозаполнение названия из заголовка T: в ABC
  - Валидация и парсинг перед сохранением
  - Пользовательские мелодии хранятся отдельно от встроенных

- **Редактор ABC**: Редактирование ABC нотации для пользовательских мелодий
  - Просмотр и редактирование ABC нотации в диалоге
  - Копирование ABC в буфер обмена
  - Сохранение изменений с автоматическим повторным парсингом
  - Доступно только для пользовательских мелодий

- **Система корзины**: Мягкое удаление встроенных мелодий с хранением 30 дней
  - Перемещение встроенных мелодий в корзину вместо постоянного удаления
  - Восстановление мелодий из корзины
  - Автоматическое постоянное удаление через 30 дней
  - Показывает оставшиеся дни до постоянного удаления
  - Отдельная активность корзины с кнопками восстановления/удаления

- **Просмотр нот**: Отображение ABC нотации в виде нотного стана с табулатурой
  - Интегрирован ABCJS 6.5.1 для рендеринга нот
  - Показывает стандартную нотацию и табулатуру для тин-вистла
  - Клик по нотам для перехода к позиции
  - Автопрокрутка во время воспроизведения
  - Подсветка текущей ноты во время воспроизведения
  - Переключение между текстовыми табами и нотным станом

- **Система избранного**: Отметка мелодий как избранных и фильтрация по избранным
  - Кнопка звездочки на каждой мелодии для переключения статуса избранного
  - Кнопка избранного в панели действий для показа только избранных мелодий
  - Избранное сохраняется через SharedPreferences
  - Визуальная обратная связь с заполненными/контурными иконками звезд

- **Опции сортировки**: Несколько способов сортировки списка мелодий
  - Сортировка по названию (А-Я или Я-А)
  - Сортировка с избранными в начале
  - Сортировка по типу мелодии (Reel, Jig и т.д.)
  - Меню сортировки доступно из панели действий

- **Функция удаления**: Удаление мелодий с главного экрана и из окна мелодии
  - Кнопка удаления на каждой мелодии в списке
  - Кнопка удаления (иконка корзины) в панели действий окна мелодии
  - Все мелодии (встроенные и пользовательские) перемещаются в корзину с хранением 30 дней
  - Единообразный диалог удаления во всем приложении

- **Метроном**: Встроенный метроном, синхронизированный с воспроизведением музыки
  - Переключение метронома вкл/выкл специальной кнопкой
  - Автоматическая синхронизация с текущим темпом (BPM)
  - Визуальная обратная связь при включенном метрономе
  - Запускается/останавливается вместе с воспроизведением музыки

### Исправлено
- **Баг полузакрытых отверстий**: Исправлен алгоритм автоматической транспозиции
  - Добавлена проверка октавы вниз (-12) перед попыткой других сдвигов
  - Теперь корректно избегает полузакрытых отверстий для мелодий в E minor и подобных тональностях
  - Порядок транспозиции: 0 → +12 → -12 → другие сдвиги (-24 до +24)

- **Улучшения UX диалога добавления мелодии**:
  - Кнопка Назад теперь сначала закрывает клавиатуру, затем диалог
  - ScrollView с maxHeight позволяет получить доступ к кнопке Save при открытой клавиатуре
  - Название автоматически заполняется из заголовка T: в ABC
  - Добавлены подсказки и советы в диалоге

- **Управление темпом**: Исправлена проблема с темпом, не влияющим на скорость воспроизведения
  - Модификатор темпа теперь правильно рассчитывается на основе BPM
  - Музыка воспроизводится быстрее/медленнее в соответствии с настройкой темпа
  - Формула: tempoModifier = текущийТемп / дефолтныйТемп (100 BPM)

- **Транспозиция тональности при воспроизведении**: Исправлена проблема с неприменением выбранной тональности к аудио
  - Текущая настройка тональности теперь применяется при открытии мелодии
  - Воспроизведение аудио использует правильно транспонированные ноты
  - Отображение табулатуры соответствует транспозиции аудио

### Изменено
- **TabActivity**: Перенесен метод escape() из удаленного SheetActivity
  - Метод escape() теперь часть TabActivity для экранирования строк JavaScript
  - Удалена декларация SheetActivity из AndroidManifest.xml

- **MainActivity**: Добавлено управление корзиной и пользовательскими мелодиями
  - Новая кнопка "Add Song" в панели действий
  - Новая кнопка "Trash" для просмотра удаленных мелодий
  - Интеграция с CustomSongsManager и TrashManager

- **MusicDB**: Расширена поддержка пользовательских мелодий
  - Метод open() теперь обрабатывает встроенные и пользовательские файлы мелодий
  - Пользовательские мелодии используют префикс "custom_", встроенные "builtin_"
  - Интегрирован парсинг ABC для всех типов мелодий

- **MusicSheet**: Добавлен парсинг структуры ABC для переносов строк
  - notesToTabsWithLineBreaks() сохраняет структуру строк ABC
  - parseABCStructure() сопоставляет табы со строками ABC нотации
  - Улучшены пробелы между табами на основе длительности нот



### Технические детали
- **Зависимости**: Обновлен ABCJS с 5.11.0 до 6.5.1
- **Новые иконки**: 
  - ic_trash.xml - иконка корзины
  - ic_delete.xml - иконка удаления
  - ic_stop.xml - иконка остановки воспроизведения
  - ic_star.xml - заполненная иконка звезды для избранного
  - ic_star_border.xml - контурная иконка звезды
  - ic_sort.xml - иконка сортировки
  - ic_metronome.xml - иконка метронома
- **Новые макеты**:
  - activity_trash.xml - макет активности корзины
  - trash_item_layout.xml - макет карточки элемента корзины
  - dialog_abc.xml - макет диалога редактора ABC
  - dialog_add_custom_song.xml - макет диалога добавления мелодии
- **Архитектура**:
  - ABCParser обрабатывает весь парсинг ABC нотации
  - CustomSongsManager управляет хранением пользовательских мелодий (SharedPreferences)
  - TrashManager управляет функциональностью мягкого удаления (SharedPreferences)
  - FavoritesManager управляет избранными мелодиями (SharedPreferences)
  - Класс Metronome генерирует звуки клика, синхронизированные с темпом
  - MusicDB.open() теперь поддерживает встроенные и пользовательские мелодии
- **Хранение данных**:
  - Пользовательские мелодии хранятся в SharedPreferences как JSON
  - Элементы корзины хранятся в SharedPreferences с меткой времени удаления
  - Встроенные мелодии остаются в db.json (res/raw)

---

## Format
This changelog follows [Keep a Changelog](https://keepachangelog.com/) format.
