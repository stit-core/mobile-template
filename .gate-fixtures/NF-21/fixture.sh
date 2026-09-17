# NF-21 — фича зависит от другой фичи (AuthFeature → TodosFeature в Package.swift): ловит Tools/archlint; import — SwiftLint no_feature_import
EXPECT="archlint"
TOUCHES="iosApp/Packages/Features/Package.swift iosApp/Packages/Features/Sources/AuthFeature/LoginModel.swift"
apply() {
    printf '\npackage.targets.first { $0.name == "AuthFeature" }?.dependencies.append(.target(name: "TodosFeature"))\n' >>iosApp/Packages/Features/Package.swift
    sed -i '1i import TodosFeature' iosApp/Packages/Features/Sources/AuthFeature/LoginModel.swift
}
