package com.andrerinas.openheadunit.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SUExecutorSetPropTest {

    @Test
    fun `plain property keys and values are allowed`() {
        assertTrue(SUExecutor.isSafeProp("sys.carlink.type", "0"))
        assertTrue(SUExecutor.isSafeProp("persist.vendor.x_y-z", "a.b:c/d,e=1"))
    }

    @Test
    fun `anything that could run another shell command is refused`() {
        assertFalse(SUExecutor.isSafeProp("sys.carlink.type", "0; reboot"))
        assertFalse(SUExecutor.isSafeProp("sys.carlink.type", "\$(id)"))
        assertFalse(SUExecutor.isSafeProp("sys.carlink.type", "`id`"))
        assertFalse(SUExecutor.isSafeProp("sys.carlink.type", "a b"))
        assertFalse(SUExecutor.isSafeProp("sys.carlink.type", "a'b"))
        assertFalse(SUExecutor.isSafeProp("sys.carlink.type", "a\nb"))
        assertFalse(SUExecutor.isSafeProp("sys key", "1"))
        assertFalse(SUExecutor.isSafeProp("", "1"))
    }
}
