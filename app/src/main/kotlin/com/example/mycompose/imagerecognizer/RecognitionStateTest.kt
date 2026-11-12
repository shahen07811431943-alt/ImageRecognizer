package com.example.mycompose.imagerecognizer

import com.example.mycompose.imagerecognizer.data.Prediction
import com.example.mycompose.imagerecognizer.ui.RecognitionState
import org.junit.Assert.*
import org.junit.Test

class RecognitionStateTest {

    @Test
    fun defaultState_isEmpty() {
        val s = RecognitionState()
        assertNull(s.preview)
        assertTrue(s.predictions.isEmpty())
        assertFalse(s.loading)
        assertNull(s.error)
    }

    @Test
    fun prediction_keepsValues() {
        val p = Prediction("Cat", 0.92f)
        assertEquals("Cat", p.label)
        assertEquals(0.92f, p.confidence, 0.0001f)
    }
}
