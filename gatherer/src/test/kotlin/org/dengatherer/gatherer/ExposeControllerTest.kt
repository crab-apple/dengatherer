package org.dengatherer.gatherer


import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatcher
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.kotlin.argThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.reflect.KProperty1


@WebMvcTest(ExposeController::class)
internal class ExposeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var exposeService: ExposeService

    @Test
    fun `returns exposes`() {

        fun expose(id: Int) = Expose(
            id,
            "the-image",
            "the-url",
            "expose-num-${id}",
            2.toBigDecimal(),
            700.toBigDecimal(),
            50.toBigDecimal(),
            "the address",
            "the crawler"
        )

        `when`(exposeService.getAllExposes())
            .thenReturn(listOf(expose(1), expose(2)))

        mockMvc.perform(get("/exposes"))
            .andExpect(
                content().string(
                    allOf(
                        containsString("expose-num-1"),
                        containsString("expose-num-2")
                    )
                )
            )
    }

    @Test
    fun `posts expose`() {

        val exposeJson = """
        {
          "id": 1234567,
          "image": "https://example.com/apartment.jpg",
          "url": "https://example.com/apartment.html",
          "title": "A cozy apartment",
          "rooms": "2,5",
          "price": "819,50 €",
          "size": "86,03 m²",
          "address": "13351 Berlin (Wedding)",
          "crawler": "CrawlImmowelt"
        }
   """

        mockMvc.perform(
            post("/exposes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exposeJson)
        )


        verify(exposeService).notifyExpose(
            argThat(
                PropertyMatcher(Expose::title, "A cozy apartment")
            )
        )
    }

    @Test
    fun `attempting to post a malformed expose results in a 400 error`() {

        val exposeJson = "{}"

        mockMvc.perform(
            post("/exposes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(exposeJson)

        ).andExpect(status().isBadRequest)
    }
}

class PropertyMatcher<T, F>(private val property: KProperty1<T, F>, private val expectedValue: F) : ArgumentMatcher<T> {

    override fun matches(argument: T?): Boolean {
        return argument?.let(property) == expectedValue
    }

    override fun toString(): String {
        return "${property.name} should be [$expectedValue]"
    }
}