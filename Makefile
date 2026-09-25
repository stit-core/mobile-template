.PHONY: help fmt fmt-check detekt lint test screenshots screenshots-record cover cover-check \
        check-fast run-android build check check-full kmp-check api-dump api-check gen gen-check mock-check \
        check-mac mac-doctor mac-build mac-test snapshot-record run-ios release-preflight release-android \
        setup doctor clean status map deps search gate-selftest validate-plan links-check kin-diff \
        swift-lint swift-test gen-tokens tokens-check i18n-check deeplinks-check capability-check e2e

# Единый словарь для агента: `make <verb>`. Concise-by-default (scripts/tools/*): одна строка OK:/FAIL:
# на шаг, полный лог — build/gate-logs/<шаг>.log. FORMAT=detailed — длиннее выжимка лога при падении,
# VERBOSE=1 — сырой вывод инструмента в терминал.

help: ## сгруппированный список команд
	@awk 'BEGIN {FS = ":.*## "} \
		/^##@ / {printf "\n\033[1m%s\033[0m\n", substr($$0, 5); next} \
		/^[a-zA-Z0-9_-]+:.*## / {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

GRADLE_STEP = bash scripts/tools/gradle-step.sh
SWIFT_STEP = bash scripts/tools/swift-step.sh
IOS_SOURCES = iosApp/App iosApp/Widgets iosApp/Packages iosApp/Tools iosApp/UITests

##@ LOOP
fmt: ## Автоформат Kotlin (Spotless/ktlint, порядок dependencies) + Swift (swift-format) — единственные автофиксеры
	./gradlew sortDependencies spotlessApply --console=plain -q
	@$(SWIFT_STEP) "format swift apply" swift format --in-place --recursive $(IOS_SOURCES)

fmt-check: ## Проверка формата и порядка dependencies без мутации дерева
	@$(GRADLE_STEP) "format kotlin" spotlessCheck checkSortDependencies
	@$(SWIFT_STEP) "format swift" swift format lint --strict --recursive $(IOS_SOURCES)

detekt: ## Статический анализ Kotlin + compose-rules (без detekt-formatting)
	@$(GRADLE_STEP) "detekt" detekt

lint: ## Android Lint (warningsAsErrors, checkDependencies) по всем модулям
	@$(GRADLE_STEP) "android lint" lintDevDebug

test: ## JVM-тесты: shared (jvmTest + androidHostTest), Android (Robolectric, Konsist), lint-детекторы, Swift Pure
	@$(GRADLE_STEP) "shared jvm tests" :shared:jvmTest
	@$(GRADLE_STEP) "android unit tests" testDevDebugUnitTest :shared:testAndroidHostTest :androidApp:lint:test
	@$(SWIFT_STEP) "swift pure build+tests" swift test --package-path iosApp/Packages/Pure --enable-code-coverage

screenshots: ## Roborazzi verify (без устройства)
	@$(GRADLE_STEP) "screenshots" verifyRoborazziDevDebug

screenshots-record: ## Перезаписать эталоны Roborazzi (осознанно)
	./gradlew recordRoborazziDevDebug --console=plain -q

cover: ## Покрытие в процентах: shared и агрегат Android (только отчёт)
	./gradlew :shared:koverLog :androidApp:app:koverLogGate --console=plain -q

cover-check: ## Полы покрытия: shared 80 / Android 70 (koverVerify) + Swift Pure 70 (coverage-floor.sh)
	@$(GRADLE_STEP) "coverage kotlin" :shared:koverVerify :androidApp:app:koverVerifyGate
	@bash scripts/tools/coverage-floor.sh iosApp/Packages/Pure 70

check-fast: ## Цикл «пока пишу»: Swift-формат, Kotlin-формат, detekt, shared jvmTest
	@FAST=1 bash scripts/tools/check.sh

run-android: ## Установить devDebug на подключённый эмулятор/устройство и запустить (applicationId — scripts/tools/app-id.sh)
	./gradlew :androidApp:app:installDevDebug --console=plain -q
	adb shell monkey -p $$(bash scripts/tools/app-id.sh dev) -c android.intent.category.LAUNCHER 1

##@ GATE
build: ## Debug APK: app-dev-debug.apk и app-demo-debug.apk (demo — MockTransport, без сети)
	@$(GRADLE_STEP) "assemble dev+demo debug" :androidApp:app:assembleDevDebug :androidApp:app:assembleDemoDebug

check: ## Канонический gate, Linux-полный; GATE_RELEASE=1 добавит assembleProdRelease (R8)
	@bash scripts/tools/check.sh

check-full: ## check + check-mac; обязателен перед /release
	@bash scripts/tools/check.sh && bash scripts/tools/mac.sh check

e2e: ## Maestro-флоу e2e/<P> против demo-сборки (эмулятор/устройство; вне гейта; P=android)
	@command -v maestro >/dev/null || { echo "FAIL: maestro не установлен — curl -fsSL https://get.maestro.mobile.dev | bash"; exit 2; }
	./gradlew :androidApp:app:installDemoDebug --console=plain -q
	maestro test e2e/$(if $(P),$(P),android)

##@ KMP
kmp-check: ## shared → klib под iOS-таргеты + проверка каталога klib (SKIPPED ≠ OK) + запрет cinterop
	@bash scripts/tools/kmp-check.sh

api-dump: ## Обновить дампы публичного API shared (JVM + klib) — изменение фасада, решение в плане
	./gradlew :shared:apiDump --console=plain -q

api-check: ## Сверить публичный API shared с дампами
	@$(GRADLE_STEP) "shared api" :shared:apiCheck

##@ CONTRACT
gen: ## Скопировать api/openapi.yaml из ../backend (если есть), сгенерировать модели, обновить api/openapi.lock
	@bash scripts/tools/gen.sh

gen-check: ## Дрейф контракта: lock == спека == модели, регенерация совпадает; спека соседа отличается — WARN
	@bash scripts/tools/gen-check.sh

mock-check: ## Фикстуры mock/: сценарии == каталоги, JSON валиден, FixturesTest (строгий разбор моделями) зелёный
	@bash scripts/tools/mock-check.sh

gen-tokens: ## design/tokens.json → Tokens.kt (Compose) + Tokens.swift (Foundation); коммитятся, tokens-check сверяет
	python3 scripts/tools/gen-tokens.py

tokens-check: ## Паритет токенов: регенерация в temp == Tokens.kt и Tokens.swift в дереве
	@bash scripts/tools/tokens-check.sh

i18n-check: ## Ключи values/ == values-ru/ по модулям; error.<code> shared → core_ui_error_*; iOS xcstrings — advisory
	@bash scripts/tools/i18n-check.sh

deeplinks-check: ## Манифест ↔ config/env (плейсхолдеры хоста/схемы) ↔ тесты DeepLinkParser ↔ docs/stack/deep-links.md
	@bash scripts/tools/deeplinks-check.sh

capability-check: ## Вне экранов: задачи shared ↔ исполнители, тесты и BGTaskSchedulerPermittedIdentifiers; виджеты ↔ эталоны состояний; действия ↔ строки обоих values
	@bash scripts/tools/capability-check.sh

##@ SWIFT (Linux-часть)
swift-lint: ## SwiftLint --strict + archlint (граф Package.swift) — стадии swiftlint/archlint гейта
	@$(SWIFT_STEP) "swiftlint" swiftlint lint --strict --quiet --config iosApp/.swiftlint.yml iosApp
	@$(SWIFT_STEP) "archlint" swift run --package-path iosApp/Tools/archlint archlint iosApp/Packages

swift-test: ## swift test чистых пакетов (Pure) + пол покрытия 70 % (llvm-cov по всем тест-бандлам)
	@$(SWIFT_STEP) "swift pure build+tests" swift test --package-path iosApp/Packages/Pure --enable-code-coverage
	@bash scripts/tools/coverage-floor.sh iosApp/Packages/Pure 70

##@ MAC
check-mac: ## macOS-стадии по SSH (MAC_HOST): link framework, iosSimulatorArm64Test, xcodebuild, планы Unit+Snapshot
	@bash scripts/tools/mac.sh check

mac-doctor: ## Доступность MAC_HOST, версия Xcode == .xcode-version, симулятор из iosApp/.test-simulator
	@bash scripts/tools/mac.sh doctor

mac-build: ## xcodebuild build (generic/platform=iOS Simulator)
	@bash scripts/tools/mac.sh build

mac-test: ## xcodebuild test — планы Unit + Snapshot (+ UI при MAC_UI=1)
	@bash scripts/tools/mac.sh test

snapshot-record: ## (Mac) перезаписать snapshot-эталоны
	@bash scripts/tools/mac.sh snapshot-record

run-ios: ## (Mac) собрать и запустить схему Dev на симуляторе из iosApp/.test-simulator
	@bash scripts/tools/mac.sh run

##@ RELEASE
release-preflight: ## Чистое дерево, main, аннотированный тег TAG=vX.Y.Z, doctor, check (+ check-mac при PLATFORM≠android)
	@bash scripts/release/preflight.sh $(TAG)

release-android: ## Подписанные AAB + APK + mapping.txt → dist/ (STIT_KEYSTORE_* в env, TAG=vX.Y.Z)
	@bash scripts/release/build-android.sh $(TAG)

##@ SETUP
setup: ## doctor + git-хуки + Android-скиллы + прогрев демона
	@bash scripts/tools/doctor.sh
	git config core.hooksPath scripts/git-hooks
	@bash scripts/tools/android-skills.sh
	./gradlew help --console=plain -q

doctor: ## Fail-fast preflight: JDK 21, Android SDK, лицензии, RAM/диск, Swift 6.4, MAC_HOST
	@bash scripts/tools/doctor.sh

clean: ## Остановить демоны, снести build/ и iOS .build/
	./gradlew --stop || true
	./gradlew clean --console=plain -q || true
	rm -rf iosApp/Packages/*/.build iosApp/Tools/archlint/.build

##@ INTEL
status: ## Сводка для /smartdev: ветка, дерево, последний гейт, модули, план
	@bash scripts/tools/status.sh

map: ## Модули проекта
	@./gradlew projects --console=plain -q | sed 's/^/  /'

deps: ## Дерево зависимостей (M=:androidApp:feature:todos C=devDebugRuntimeClasspath)
	@./gradlew $(if $(M),$(M):,)dependencies $(if $(C),--configuration $(C),) --console=plain -q

search: ## Поиск по коду (Q=pattern [P=path]) — rg, fallback grep
	@if command -v rg >/dev/null 2>&1; then rg -n --max-count=80 -- "$(Q)" $(if $(P),$(P),.); \
	else grep -rn --exclude-dir=build --exclude-dir=.gradle --exclude-dir=.git -- "$(Q)" $(if $(P),$(P),.); fi

links-check: ## Ссылки в docs/**, README, CLAUDE.md, .claude/** резолвятся в существующие файлы
	@bash scripts/tools/links-check.sh

kin-diff: ## Advisory-сверка идентичных файлов с go-backend-template (docs/reference/kinship.md)
	@bash scripts/tools/kin-diff.sh

##@ META
gate-selftest: ## Негативные фикстуры .gate-fixtures/NF-*: каждая обязана уронить свою стадию (F=NF-03 — одна)
	@bash scripts/tools/gate-selftest.sh $(F)

validate-plan: ## Формат планов .claude/plans/ (--summary — сводка: bash scripts/tools/validate-plan.sh --summary)
	@bash scripts/tools/validate-plan.sh
