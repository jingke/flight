package com.flightbooking.app.domain.util

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    @Test
    fun `Success isSuccess returns true`() {
        val result: Result<String> = Result.Success("data")
        assertTrue(result.isSuccess)
        assertFalse(result.isError)
        assertFalse(result.isLoading)
    }

    @Test
    fun `Error isError returns true`() {
        val result: Result<String> = Result.Error("failed")
        assertFalse(result.isSuccess)
        assertTrue(result.isError)
        assertFalse(result.isLoading)
    }

    @Test
    fun `Loading isLoading returns true`() {
        val result: Result<String> = Result.Loading
        assertFalse(result.isSuccess)
        assertFalse(result.isError)
        assertTrue(result.isLoading)
    }

    @Test
    fun `getOrNull returns data for Success`() {
        val result: Result<Int> = Result.Success(42)
        assertEquals(42, result.getOrNull())
    }

    @Test
    fun `getOrNull returns null for Error`() {
        val result: Result<Int> = Result.Error("failed")
        assertNull(result.getOrNull())
    }

    @Test
    fun `getOrNull returns null for Loading`() {
        val result: Result<Int> = Result.Loading
        assertNull(result.getOrNull())
    }

    @Test
    fun `map transforms Success data`() {
        val result: Result<Int> = Result.Success(10)
        val mapped = result.map { it * 2 }
        assertTrue(mapped is Result.Success)
        assertEquals(20, (mapped as Result.Success).data)
    }

    @Test
    fun `map preserves Error`() {
        val result: Result<Int> = Result.Error("failed", code = 404)
        val mapped = result.map { it * 2 }
        assertTrue(mapped is Result.Error)
        assertEquals("failed", (mapped as Result.Error).message)
        assertEquals(404, mapped.code)
    }

    @Test
    fun `map preserves Loading`() {
        val result: Result<Int> = Result.Loading
        val mapped = result.map { it * 2 }
        assertTrue(mapped is Result.Loading)
    }

    @Test
    fun `fromResponse returns Success on normal execution`() = runTest {
        val block = Result.fromResponse { "hello" }
        val result = block()
        assertTrue(result is Result.Success)
        assertEquals("hello", (result as Result.Success).data)
    }

    @Test
    fun `fromResponse returns Error on exception`() = runTest {
        val block = Result.fromResponse<String> { throw RuntimeException("boom") }
        val result = block()
        assertTrue(result is Result.Error)
        assertEquals("boom", (result as Result.Error).message)
    }

    @Test
    fun `Error with null exception message uses fallback`() = runTest {
        val block = Result.fromResponse<String> { throw RuntimeException() }
        val result = block()
        assertTrue(result is Result.Error)
        assertEquals("Unknown error occurred", (result as Result.Error).message)
    }
}
