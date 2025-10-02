package com.archstarter.core.common.presenter

import org.junit.Assert.assertEquals
import org.junit.Test

class ParamInitTest {
    private class TestInit : ParamInit<String> {
        var value: String? = null
        override fun initOnce(params: String?) {
            val actual = params ?: return
            if (value == null) {
                value = actual
            }
        }
    }

    @Test
    fun initOnce_onlyStoresFirstValue() {
        val init = TestInit()
        init.initOnce("first")
        init.initOnce("second")
        assertEquals("first", init.value)

        init.initOnce(null)
        assertEquals("first", init.value)
    }
}
