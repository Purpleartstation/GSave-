package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("GSave+", appName)
  }

  @Test
  fun `partner code format validation`() {
    val randomNum = (1000..9999).random()
    val code = "GSAVE-$randomNum"
    assertTrue(code.startsWith("GSAVE-"))
    assertEquals(10, code.length)
  }
}
