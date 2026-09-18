#!/usr/bin/env bash
# Поверхности вне экранов (08 §4.2): фоновая задача, виджет и системное действие исполняются системой без экрана и без
# процесса приложения, поэтому их согласованность держит статическая сверка, а не прогон UI. Источник истины — код
# shared: у каждой задачи есть исполнитель в реестре, тест и идентификатор платформы; у каждого виджета — эталон и тест
# на каждое состояние WidgetState; у каждого действия — тест и строки ответа в обоих values; объявленный в манифесте
# компонент существует как класс. Пустой набор — норма (форк после /de-template), а не провал.
#   capability-check.sh → OK:/FAIL: capability-check (+ WARN по iOS-каталогам строк и пустым наборам строк)
set -uo pipefail
cd "$(dirname "$0")/../.."
LABEL="capability-check"
LOGDIR="${STIT_LOGDIR:-build/gate-logs}"
mkdir -p "$LOGDIR"
LOG="$LOGDIR/capability_check.log"
: >"$LOG"
start=$(date +%s)
fails=0
note() { echo "$*" >>"$LOG"; fails=$((fails + 1)); }
warn() { printf 'WARN: %-24s %s\n' "$LABEL (ios)" "$*"; }

SHARED_MAIN=shared/src/commonMain
SHARED_TEST=shared/src/commonTest
PLIST=iosApp/App/Info.plist

# Имя носителя → ключ карты: CompleteTodo → complete-todo, SignedOut → signed_out.
kebab() { printf '%s' "$1" | sed 's/\(.\)\([A-Z]\)/\1-\2/g' | tr '[:upper:]' '[:lower:]'; }
snake() { printf '%s' "$1" | tr '-' '_'; }

# Ключ объявляется рядом с носителем: const val ID или const val SLUG в его companion.
slug_of() { grep -oE 'const val (ID|SLUG) = "[^"]+"' "$1" | head -1 | sed 's/.*= "//; s/"$//'; }

# Строки одного носителя во всех модулях: ключи values/ обязаны совпадать с values-ru/ (i18n-check ловит то же по модулю,
# здесь — адресно по префиксу носителя, чтобы отказ назвал задачу, а не файл).
strings_parity() {
    local prefix="$1" owner="$2" en ru
    en=$(find androidApp -path '*/src/main/res/values/strings.xml' -not -path '*/build/*' -exec \
        grep -ohE "<string name=\"$prefix[^\"]*\"" {} + 2>/dev/null | sed 's/.*name="//; s/"$//' | LC_ALL=C sort -u)
    ru=$(find androidApp -path '*/src/main/res/values-ru/strings.xml' -not -path '*/build/*' -exec \
        grep -ohE "<string name=\"$prefix[^\"]*\"" {} + 2>/dev/null | sed 's/.*name="//; s/"$//' | LC_ALL=C sort -u)
    if [ -z "$en" ] && [ -z "$ru" ]; then
        printf 'WARN: %-24s %s: строк %s* нет ни в одном values/\n' "$LABEL" "$owner" "$prefix"
        return
    fi
    for key in $(comm -23 <(echo "$en") <(echo "$ru")); do
        note "$owner: строка $key есть в values/, но не в values-ru/"
    done
    for key in $(comm -13 <(echo "$en") <(echo "$ru")); do
        note "$owner: строка $key есть в values-ru/, но не в values/"
    done
}

# ——— Фоновые задачи: реестр shared ↔ исполнитель ↔ тест ↔ BGTaskSchedulerPermittedIdentifiers ———

container=$(grep -rl 'fun backgroundRunner(' "$SHARED_MAIN" 2>/dev/null | head -1)
task_slugs=""
bg_slugs="" # задачи, которые на iOS ведёт BGTaskScheduler, — только они попадают в Info.plist

while IFS= read -r file; do
    [ -n "$file" ] || continue
    grep -q ': BackgroundRunner' "$file" || continue
    class=$(basename "$file" .kt)
    slug=$(slug_of "$file")
    if [ -z "$slug" ]; then
        note "задача $class: ключ не объявлен (ожидается const val ID или SLUG в companion)"
        continue
    fi
    task_slugs="$task_slugs $slug"

    if [ -z "$container" ]; then
        note "задача $slug: в $SHARED_MAIN нет реестра — функции backgroundRunner(id) не существует"
    elif ! grep -q "$class" "$container"; then
        note "задача $slug ($class) не поднимается backgroundRunner(id) в $container"
    fi

    [ -n "$(grep -rl -e "$class" -e "$slug" "$SHARED_TEST" 2>/dev/null)" ] ||
        note "задача $slug без теста в $SHARED_TEST (ожидается <Group>TasksTest)"

    # Механизм iOS выбирается по timing и trigger (BgTaskScheduler.mechanism): exact ведётся уведомлением,
    # continuous и remote-event планировщику не отдаются — идентификатор Info.plist им не нужен.
    timing=$(grep -oE 'BackgroundTiming\.[A-Za-z]+' "$file" | head -1 | cut -d. -f2)
    trigger=$(grep -oE 'BackgroundTrigger\.[A-Za-z]+' "$file" | head -1 | cut -d. -f2)
    case "$timing:$trigger" in
        Exact:* | Continuous:* | *:RemoteEvent) ;;
        *) bg_slugs="$bg_slugs $slug" ;;
    esac
done < <(find "$SHARED_MAIN" -path '*/feature/*' -name '*Task.kt' 2>/dev/null | LC_ALL=C sort)

if [ -f "$PLIST" ]; then
    plist_ids=$(
        awk '/<key>BGTaskSchedulerPermittedIdentifiers<\/key>/ {f=1; next} f && /<\/array>/ {f=0} f' "$PLIST" |
            grep -oE '<string>[^<]+</string>' | sed 's|<string>||; s|</string>||; s/^\$([A-Za-z_][A-Za-z0-9_]*)\.//'
    )
    for slug in $bg_slugs; do
        echo "$plist_ids" | grep -qx "$slug" ||
            note "задача $slug не объявлена в BGTaskSchedulerPermittedIdentifiers ($PLIST) — BGTaskScheduler её не поднимет"
    done
    for id in $plist_ids; do
        echo "$bg_slugs" | tr ' ' '\n' | grep -qx "$id" ||
            note "идентификатор $id в $PLIST не соответствует ни одной задаче BGTaskScheduler в $SHARED_MAIN"
    done
elif [ -n "$bg_slugs" ]; then
    printf 'SKIP: %-24s %s отсутствует — идентификаторы задач не сверены\n' "$LABEL (ios)" "$PLIST"
fi

# Реестр стека, если он заведён: строка таблицы Tasks обязана иметь носитель в shared.
if [ -f docs/stack/background.md ]; then
    while IFS= read -r slug; do
        [ -n "$slug" ] || continue
        echo "$task_slugs" | tr ' ' '\n' | grep -qx "$slug" ||
            note "задача $slug из docs/stack/background.md без носителя *Task в $SHARED_MAIN"
    done < <(sed -n '/^## Tasks/,/^## /p' docs/stack/background.md |
        grep -oE '^\| *_?`[^`]+`' | sed 's/^| *_\?`//; s/`$//' | LC_ALL=C sort -u)
fi

# ——— Виджеты: каждое состояние WidgetState — эталон Roborazzi и тест снимка ———

state_file=$(find "$SHARED_MAIN" -name 'WidgetState.kt' 2>/dev/null | head -1)
states=""
if [ -n "$state_file" ]; then
    states=$(awk '/^enum class WidgetState/ {f=1; next} f && /^}/ {f=0} f' "$state_file" |
        grep -oE '^ +[A-Z][A-Za-z0-9]*' | tr -d ' ')
fi

while IFS= read -r file; do
    [ -n "$file" ] || continue
    class=$(basename "$file" .kt)
    slug=$(slug_of "$file")
    if [ -z "$slug" ]; then
        note "виджет $class: ключ не объявлен (ожидается const val SLUG в companion)"
        continue
    fi
    if [ -z "$states" ]; then
        note "виджет $slug: словарь состояний не найден (ожидается enum WidgetState в $SHARED_MAIN)"
        continue
    fi
    # Тест снимка адресует носитель фабрикой контейнера (todayTodosWidgetProvider) — регистр имени там иной.
    tests=$(grep -rliE "${class%Provider}|$slug" "$SHARED_TEST" 2>/dev/null)
    for state in $states; do
        png="widget_$(snake "$slug")_$(snake "$(kebab "$state")").png"
        [ -n "$(find androidApp -path '*/src/test/screenshots/*' -not -path '*/build/*' -name "$png" 2>/dev/null)" ] ||
            note "виджет $slug: состояние $state без эталона $png (make screenshots-record — осознанно)"
        [ -n "$tests" ] && grep -ql "WidgetState.$state" $tests 2>/dev/null ||
            note "виджет $slug: состояние $state без теста снимка в $SHARED_TEST"
    done
    strings_parity "widget_$(snake "$slug")_" "виджет $slug"
done < <(find "$SHARED_MAIN" -path '*/feature/*' -name '*WidgetProvider.kt' 2>/dev/null | LC_ALL=C sort)

# ——— Системные действия: тест ветвления и ответ пользователю на обоих языках ———

actions=""
while IFS= read -r file; do
    [ -n "$file" ] || continue
    grep -q 'suspend fun run(' "$file" || continue
    class=$(basename "$file" .kt)
    slug=$(kebab "${class%Action}")
    actions="$actions $slug"

    [ -n "$(grep -rliE "$class" "$SHARED_TEST" 2>/dev/null)" ] ||
        note "действие $slug без теста в $SHARED_TEST (ожидается <Group>ActionsTest)"

    strings_parity "core_platform_action_$(snake "$slug")_" "действие $slug"
done < <(find "$SHARED_MAIN" -path '*/feature/*' -name '*Action.kt' 2>/dev/null | LC_ALL=C sort)

# Паритет с iOS — advisory: каталоги строк живут в пакетах и на Linux не собираются.
if [ -n "$actions" ]; then
    xcstrings=$(find iosApp -name 'Localizable.xcstrings' -not -path '*/.build/*' 2>/dev/null | LC_ALL=C sort)
    if [ -z "$xcstrings" ]; then
        printf 'SKIP: %-24s iOS Localizable.xcstrings отсутствует\n' "$LABEL (ios)"
    else
        missing=$(for slug in $actions; do grep -lq "\"action\.$slug\." $xcstrings || echo "action.$slug.*"; done | tr '\n' ' ')
        [ -z "$missing" ] || warn "в iosApp/**/Localizable.xcstrings нет ответов действий: $missing"
    fi
fi

# ——— Манифест: приёмник, сервис и провайдер обязаны существовать как класс ———

while IFS= read -r pair; do
    [ -n "$pair" ] || continue
    manifest=${pair%%|*}
    name=${pair##*|}
    module=${manifest%/src/main/AndroidManifest.xml}
    class=${name##*.}
    [ -n "$(find "$module/src/main" -name "$class.kt" 2>/dev/null)" ] && continue
    # Сервис реестра AppFunctions дописывает компилятор: в исходниках его имя живёт строкой в entry point.
    grep -rq "\"$class\"" "$module/src/main" 2>/dev/null ||
        note "$manifest объявляет $name, но класса $class.kt в $module/src/main нет"
done < <(find androidApp -name AndroidManifest.xml -not -path '*/build/*' 2>/dev/null | LC_ALL=C sort |
    while IFS= read -r m; do
        grep -oE 'android:name="\.[A-Za-z0-9_.]+"' "$m" | sed "s|.*=\"|$m\||; s|\"$||"
    done)

dur=$(($(date +%s) - start))
if [ "$fails" -gt 0 ]; then
    printf 'FAIL: %-24s %3ds  (лог: %s)\n' "$LABEL" "$dur" "$LOG"
    head -5 "$LOG"
    exit 1
fi
printf 'OK:   %-24s %3ds\n' "$LABEL" "$dur"
