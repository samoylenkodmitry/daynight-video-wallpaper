package com.archstarter.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiquidGlassTiltProcessorTest {

    private fun createProcessor(config: LiquidGlassTiltProcessor.Config = LiquidGlassTiltProcessor.Config(
        filterAlpha = 1f,
        angleEpsilon = 0f,
        pitchEpsilon = 0f,
        maxShaderRotation = 1f,
        maxRoll = 1f,
        maxPitch = 1f,
        rollMultiplier = -1f,
        pitchMultiplier = -1f,
    )): LiquidGlassTiltProcessor = LiquidGlassTiltProcessor(config)

    @Test
    fun `positive roll tilts glass in the same direction as device`() {
        val processor = createProcessor()

        val result = processor.update(rawRoll = 1f, rawPitch = 0f)

        requireNotNull(result)
        assertEquals(-1f, result.angle, 0.0001f)
        assertEquals(0f, result.pitch, 0.0001f)
    }

    @Test
    fun `positive pitch decreases highlight intensity`() {
        val processor = createProcessor()

        val result = processor.update(rawRoll = 0f, rawPitch = 1f)

        requireNotNull(result)
        assertEquals(0f, result.angle, 0.0001f)
        assertEquals(-1f, result.pitch, 0.0001f)
    }

    @Test
    fun `values are clamped to configured bounds`() {
        val processor = createProcessor()

        val result = processor.update(rawRoll = 5f, rawPitch = -5f)

        requireNotNull(result)
        assertEquals(-1f, result.angle, 0.0001f)
        assertEquals(1f, result.pitch, 0.0001f)
    }

    @Test
    fun `low pass filter smooths the signal`() {
        val processor = LiquidGlassTiltProcessor(
            LiquidGlassTiltProcessor.Config(
                filterAlpha = 0.5f,
                angleEpsilon = 0f,
                pitchEpsilon = 0f,
                maxShaderRotation = 1f,
                maxRoll = 1f,
                maxPitch = 1f,
                rollMultiplier = -1f,
                pitchMultiplier = -1f,
            ),
        )

        val result = processor.update(rawRoll = 1f, rawPitch = 0f)

        requireNotNull(result)
        assertEquals(-0.5f, result.angle, 0.0001f)
        assertEquals(0f, result.pitch, 0.0001f)
    }

    @Test
    fun `epsilon thresholds prevent insignificant updates`() {
        val processor = LiquidGlassTiltProcessor(
            LiquidGlassTiltProcessor.Config(
                filterAlpha = 1f,
                angleEpsilon = 0.2f,
                pitchEpsilon = 0.2f,
                maxShaderRotation = 1f,
                maxRoll = 1f,
                maxPitch = 1f,
                rollMultiplier = -1f,
                pitchMultiplier = -1f,
            ),
        )

        val smallChange = processor.update(rawRoll = 0.1f, rawPitch = 0.1f)
        assertNull(smallChange)

        val largeChange = processor.update(rawRoll = 1f, rawPitch = 1f)
        requireNotNull(largeChange)
        assertEquals(-1f, largeChange.angle, 0.0001f)
        assertEquals(-1f, largeChange.pitch, 0.0001f)
    }

    @Test
    fun `reset uses provided tilt as the new baseline`() {
        val processor = createProcessor()

        processor.update(rawRoll = 1f, rawPitch = 1f)
        processor.reset(LiquidGlassTilt(angle = 0.25f, pitch = -0.5f))

        val result = processor.update(rawRoll = 1f, rawPitch = 1f)

        requireNotNull(result)
        assertEquals(-1f, result.angle, 0.0001f)
        assertEquals(-1f, result.pitch, 0.0001f)
    }

    @Test
    fun `non finite sensor readings are ignored`() {
        val processor = createProcessor()

        val result = processor.update(rawRoll = Float.NaN, rawPitch = Float.POSITIVE_INFINITY)

        assertNull(result)

        val next = processor.update(rawRoll = 1f, rawPitch = 0f)
        requireNotNull(next)
        assertEquals(-1f, next.angle, 0.0001f)
        assertEquals(0f, next.pitch, 0.0001f)
    }
}
