.PHONY: help setup doctor fmt fmt-check lint detekt test screenshots screenshots-record \
        build assemble bundle check cover deps clean status map search kmp-ios

# Единый словарь для агента: `make <verb>`. Concise-by-default (scripts/tools/*),
# VERBOSE=1 — сырой Gradle.

help: ## сгруппированный список команд
	@awk 'BEGIN {FS = ":.*## "} \
		/^##@ / {printf "\n\033[1m%s\033[0m\n", substr($$0, 5); next} \
		/^[a-zA-Z0-9_-]+:.*## / {printf "  \033[36m%-18s\033[0m %s\n", $$1, $$2}' $(MAKEFILE_LIST)

##@ LOOP
fmt: ## Автоформат (Spotless/ktlint) — единственная автофиксилка формата
	./gradlew spotlessApply --console=plain -q

fmt-check: ## Проверка формата без мутации дерева (шаг гейта)
	@bash scripts/tools/gradle-step.sh "format" spotlessCheck

detekt: ## Статический анализ Kotlin + compose-rules
	@bash scripts/tools/gradle-step.sh "detekt" detekt

lint: ## Android Lint (warningsAsErrors) по всем модулям
	@bash scripts/tools/gradle-step.sh "android lint" lint

test: ## JVM-тесты всех модулей (ViewModel, репозитории, Robolectric, Konsist)
	@bash scripts/tools/gradle-step.sh "unit tests" test

screenshots: ## Сверить скриншоты (Roborazzi verify) — без устройства
	@bash scripts/tools/gradle-step.sh "screenshots" verifyRoborazziDebug

screenshots-record: ## Перезаписать эталоны скриншотов
	./gradlew recordRoborazziDebug --console=plain -q

build: ## Debug APK
	@bash scripts/tools/gradle-step.sh "assembleDebug" :app:assembleDebug

cover: ## Отчёт покрытия (XML+HTML) и вывод процента
	./gradlew koverXmlReport koverHtmlReport koverLog --console=plain -q

check: ## Канонический gate (pre-push). GATE_RELEASE=1 добавит assembleRelease
	@bash scripts/tools/check.sh

##@ RELEASE
assemble: ## Подписанный release APK (нужны STIT_KEYSTORE_* в env)
	./gradlew :app:assembleRelease --console=plain

bundle: ## Подписанный AAB для сторов
	./gradlew :app:bundleRelease --console=plain

##@ KMP
kmp-ios: ## Проверить, что общий код компилируется под iOS (klib, без линковки)
	@bash scripts/tools/gradle-step.sh "kmp ios klib" :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64

##@ INTEL
status: ## Кто есть кто: модули и их плагины
	@./gradlew projects --console=plain -q | sed 's/^/  /'

map: ## Список тасков верхнего уровня
	@./gradlew tasks --console=plain -q | sed 's/^/  /'

deps: ## Дерево зависимостей модуля (M=:feature:todos C=debugRuntimeClasspath)
	@./gradlew $(if $(M),$(M):,)dependencies $(if $(C),--configuration $(C),) --console=plain -q

search: ## Поиск по коду (Q=pattern [P=path])
	@if command -v rg >/dev/null 2>&1; then rg -n --max-count=80 -- "$(Q)" $(if $(P),$(P),.); \
	else grep -rn --exclude-dir=build --exclude-dir=.gradle -- "$(Q)" $(if $(P),$(P),.); fi

##@ SETUP
setup: ## Проверить окружение и прогреть демон
	@bash scripts/tools/doctor.sh
	./gradlew help --console=plain -q

doctor: ## Fail-fast preflight: JDK, Android SDK, лицензии
	@bash scripts/tools/doctor.sh

clean: ## Остановить демон и снести build/
	./gradlew --stop || true
	./gradlew clean --console=plain -q || true
