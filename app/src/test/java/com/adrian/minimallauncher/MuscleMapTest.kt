package com.adrian.minimallauncher

import com.adrian.minimallauncher.feature.exercise.*
import org.junit.Assert.*
import org.junit.Test

class MuscleMapTest {
    @Test fun everyCatalogRegionHasNamedGeometry() {
        val drawn = (BodyShapes.front + BodyShapes.back).mapNotNull { it.muscle }.toSet()
        ExerciseCatalog.forEach { exercise ->
            val muscles = musclesOf(exercise.id, exercise.group)
            assertTrue(exercise.id, muscles.isNotEmpty())
            assertTrue(exercise.id, drawn.containsAll(muscles.keys))
            assertTrue(exercise.id, MuscleNames.keys.containsAll(muscles.keys))
        }
    }

    @Test fun shoulderMovementsDoNotPaintAllThreeHeads() {
        val lateral = musclesOf("lateral_raise", "hombros")
        assertEquals(PRIMARY, lateral["deltoide_lateral"])
        assertNull(lateral["deltoide_anterior"])
        assertNull(lateral["deltoide_posterior"])
        assertEquals(HELPS, musclesOf("db_row", "espalda")["deltoide_posterior"])
    }

    @Test fun abdominalRegionsStayActiveTogetherAndAggregateByMaximum() {
        val reverse = musclesOf("reverse_crunch", "core")
        assertEquals(PRIMARY, reverse["abdomen_inferior"])
        assertTrue(reverse.getValue("abdomen_superior") > 0)
        val routine = heatOf(listOf(reverse, musclesOf("plank", "core")))
        assertEquals(PRIMARY, routine["abdomen_superior"])
        assertEquals(PRIMARY, routine["abdomen_inferior"])
    }

    @Test fun unknownSavedExerciseStillUsesItsLegacyGroup() {
        assertEquals(PRIMARY, musclesOf("custom", "core")["abdomen_superior"])
        assertTrue(musclesOf("custom", "unknown").isEmpty())
    }
}
