package tech.stit.mobile.android.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.UParenthesizedExpression
import org.jetbrains.uast.UPrefixExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.UReferenceExpression

// Сырые значения дизайна живут только в core:designsystem (rule design-tokens): Color(0x…) и 16.dp в фиче — ошибка.
// Второй рубеж после detekt MagicNumber: ловит и property-декларации, которые detekt осознанно пропускает.
class DesignSystemDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf("Color")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (context.inDesignSystem()) return
        val owner = method.containingClass?.qualifiedName
        if (owner == COLOR_FACTORY && node.valueArguments.any { it.isLiteral() }) {
            context.report(ISSUE, node, context.getLocation(node), "Сырой Color(…) вне core:designsystem — используй StitTheme.colors")
        }
    }

    override fun getApplicableReferenceNames(): List<String> = listOf("dp", "sp")

    override fun visitReference(context: JavaContext, reference: UReferenceExpression, referenced: PsiElement) {
        val owner = (referenced as? PsiMethod)?.containingClass?.qualifiedName
        val receiver = (reference.uastParent as? UQualifiedReferenceExpression)?.receiver
        if (!context.inDesignSystem() && owner in UNIT_FACTORIES && receiver.isLiteral()) {
            context.report(
                ISSUE,
                reference,
                context.getLocation(reference),
                "Сырой размер `.dp`/`.sp` вне core:designsystem — используй StitTheme",
            )
        }
    }

    private fun JavaContext.inDesignSystem(): Boolean = uastFile?.packageName?.contains(DESIGN_SYSTEM_PACKAGE) == true

    private fun UExpression?.isLiteral(): Boolean = when (this) {
        is ULiteralExpression -> true
        is UPrefixExpression -> operand.isLiteral()
        is UParenthesizedExpression -> expression.isLiteral()
        else -> false
    }

    companion object {
        private const val COLOR_FACTORY = "androidx.compose.ui.graphics.ColorKt"
        private val UNIT_FACTORIES = setOf("androidx.compose.ui.unit.DpKt", "androidx.compose.ui.unit.TextUnitKt")
        private const val DESIGN_SYSTEM_PACKAGE = ".core.designsystem"

        val ISSUE: Issue = Issue.create(
            id = "StitRawDesignValue",
            briefDescription = "Сырое значение дизайна вне core:designsystem",
            explanation = "Цвета и размеры берутся из токенов StitTheme (core:designsystem); " +
                "литералы Color(0x…), .dp и .sp в других модулях запрещены.",
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.ERROR,
            implementation = Implementation(DesignSystemDetector::class.java, Scope.JAVA_FILE_SCOPE),
        )
    }
}
