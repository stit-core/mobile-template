package tech.stit.mobile.android.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

// Реестр проектных Lint-детекторов; подключён в app через lintChecks и Lint-Registry-v2 в манифесте jar.
class StitIssueRegistry : IssueRegistry() {
    override val issues: List<Issue> = listOf(DesignSystemDetector.ISSUE)

    override val api: Int = CURRENT_API

    override val vendor: Vendor = Vendor(
        vendorName = "stit",
        identifier = "tech.stit.mobile.lint",
        feedbackUrl = "https://git.stit.tech/stit-core/mobile-template/issues",
    )
}
