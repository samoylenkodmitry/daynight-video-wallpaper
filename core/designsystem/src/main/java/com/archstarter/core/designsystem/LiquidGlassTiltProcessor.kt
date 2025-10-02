package com.archstarter.core.designsystem

import kotlin.math.PI
import kotlin.math.abs

internal object LiquidGlassTiltConfig {
    const val FILTER_ALPHA = 0.12f
    const val ANGLE_EPSILON = 0.0005f
    const val PITCH_EPSILON = 0.0005f
    const val MAX_SHADER_ROTATION = 0.45f
    val HALF_PI = (PI / 2.0).toFloat()
    val MAX_ROLL = HALF_PI * 0.9f
    val MAX_PITCH = HALF_PI * 0.9f
}

internal class LiquidGlassTiltProcessor(
    private val config: Config = Config(),
) {

    data class Config(
        val filterAlpha: Float = LiquidGlassTiltConfig.FILTER_ALPHA,
        val angleEpsilon: Float = LiquidGlassTiltConfig.ANGLE_EPSILON,
        val pitchEpsilon: Float = LiquidGlassTiltConfig.PITCH_EPSILON,
        val maxShaderRotation: Float = LiquidGlassTiltConfig.MAX_SHADER_ROTATION,
        val maxRoll: Float = LiquidGlassTiltConfig.MAX_ROLL,
        val maxPitch: Float = LiquidGlassTiltConfig.MAX_PITCH,
        val rollMultiplier: Float = -1f,
        val pitchMultiplier: Float = 1f,
    )

    private var filteredAngle = LiquidGlassTilt.Zero.angle
    private var filteredPitch = LiquidGlassTilt.Zero.pitch
    private var lastDispatchedAngle = LiquidGlassTilt.Zero.angle
    private var lastDispatchedPitch = LiquidGlassTilt.Zero.pitch

    fun reset(initialTilt: LiquidGlassTilt = LiquidGlassTilt.Zero) {
        filteredAngle = initialTilt.angle
        filteredPitch = initialTilt.pitch
        lastDispatchedAngle = initialTilt.angle
        lastDispatchedPitch = initialTilt.pitch
    }

    fun update(rawRoll: Float, rawPitch: Float): LiquidGlassTilt? {
        val sanitizedRoll = rawRoll.coerceFinite()
        val sanitizedPitch = rawPitch.coerceFinite()
        val targetAngle = mapRoll(sanitizedRoll)
        val targetPitch = mapPitch(sanitizedPitch)
        filteredAngle += (targetAngle - filteredAngle) * config.filterAlpha
        filteredPitch += (targetPitch - filteredPitch) * config.filterAlpha
        if (shouldDispatch()) {
            val newTilt = LiquidGlassTilt(filteredAngle, filteredPitch)
            lastDispatchedAngle = newTilt.angle
            lastDispatchedPitch = newTilt.pitch
            return newTilt
        }
        return null
    }

    private fun shouldDispatch(): Boolean {
        return abs(filteredAngle - lastDispatchedAngle) > config.angleEpsilon ||
            abs(filteredPitch - lastDispatchedPitch) > config.pitchEpsilon
    }

    private fun mapRoll(rawRoll: Float): Float {
        if (config.maxRoll <= 0f) return 0f
        val clamped = rawRoll.coerceIn(-config.maxRoll, config.maxRoll)
        val normalized = clamped / config.maxRoll
        val scaled = normalized * config.rollMultiplier
        return scaled * config.maxShaderRotation
    }

    private fun mapPitch(rawPitch: Float): Float {
        if (config.maxPitch <= 0f) return 0f
        val clamped = rawPitch.coerceIn(-config.maxPitch, config.maxPitch)
        val normalized = clamped / config.maxPitch
        val scaled = normalized * config.pitchMultiplier
        return scaled * config.maxShaderRotation
    }
}

private fun Float.coerceFinite(): Float =
    if (isNaN() || isInfinite()) 0f else this
