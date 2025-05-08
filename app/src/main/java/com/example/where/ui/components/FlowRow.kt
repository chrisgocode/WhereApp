package com.example.where.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.foundation.layout.Arrangement

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        val maxWidth = constraints.maxWidth
        val isWidthUnbounded = maxWidth == Constraints.Infinity

        // Measure children with relaxed constraints
        val placeables = measurables.map { measurable ->
            measurable.measure(
                constraints.copy(
                    minWidth = 0,
                    minHeight = 0,
                    maxWidth = if (isWidthUnbounded) Int.MAX_VALUE / 2 else maxWidth // Prevent overflow
                )
            )
        }

        // Arrange children in rows
        val rows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0

        placeables.forEach { placeable ->
            val placeableWidth = placeable.width.coerceAtMost(maxWidth)
            if (!isWidthUnbounded && currentRowWidth + placeableWidth <= maxWidth) {
                currentRow.add(placeable)
                currentRowWidth += placeableWidth
            } else {
                if (currentRow.isNotEmpty()) {
                    rows.add(currentRow)
                    currentRow = mutableListOf()
                    currentRowWidth = 0
                }
                currentRow.add(placeable)
                currentRowWidth = placeableWidth
            }
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        // Calculate layout dimensions
        val totalWidth = if (isWidthUnbounded) {
            rows.maxOfOrNull { row -> row.sumOf { it.width } } ?: 0
        } else {
            maxWidth
        }
        val totalHeight = rows.sumOf { row -> row.maxOfOrNull { it.height } ?: 0 }

        // Perform layout
        layout(
            width = totalWidth.coerceAtMost(if (isWidthUnbounded) Int.MAX_VALUE / 2 else maxWidth),
            height = totalHeight.coerceAtMost(constraints.maxHeight)
        ) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width
                }
                y += row.maxOfOrNull { it.height } ?: 0
            }
        }
    }
}