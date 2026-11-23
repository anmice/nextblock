package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
    assertEquals("NextBlock", appName)
  }

  @Test
  fun `initials and category mapping work safely`() {
    assertEquals("JT", com.example.model.initialsFor("Jordan Taylor"))
    assertEquals("NB", com.example.model.initialsFor("   "))
    assertEquals(
      com.example.model.PostCategory.SAFETY,
      com.example.model.PostCategory.fromRaw("SAFETY")
    )
    assertEquals(
      com.example.model.PostCategory.GENERAL,
      com.example.model.PostCategory.fromRaw("UNKNOWN_CAT")
    )
  }
}
