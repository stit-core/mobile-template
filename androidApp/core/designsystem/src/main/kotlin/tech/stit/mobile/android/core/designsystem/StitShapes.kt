package tech.stit.mobile.android.core.designsystem

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import tech.stit.mobile.android.core.designsystem.generated.Tokens

@Immutable
data class StitShapes(
    val control: CornerBasedShape,
    val card: CornerBasedShape,
)

internal val StitShapesDefault = StitShapes(
    control = RoundedCornerShape(Tokens.Dimensions.radiusControl),
    card = RoundedCornerShape(Tokens.Dimensions.radiusCard),
)

internal fun StitShapes.toMaterialShapes(): Shapes = Shapes(small = control, medium = control, large = card, extraLarge = card)
