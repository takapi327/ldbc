/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import munit.FunSuite

class FastListTest extends FunSuite:

  test("FastList should handle basic add and get operations") {
    val list = new FastList[String]()

    list.add("first")
    list.add("second")
    list.add("third")

    assertEquals(list.getSize, 3)
    assertEquals(list.get(0), "first")
    assertEquals(list.get(1), "second")
    assertEquals(list.get(2), "third")
  }

  test("FastList should grow dynamically when capacity is exceeded") {
    val list = new FastList[Int](initialCapacity = 2)

    // Add more elements than initial capacity
    (1 to 10).foreach(list.add)

    val results = (0 until 10).map(list.get).toList
    assertEquals(results, (1 to 10).toList)
    assertEquals(list.getSize, 10)
  }

  test("FastList should remove elements by identity") {
    val list = new FastList[TestItem]()

    val item1     = TestItem("item1")
    val item2     = TestItem("item2")
    val item3     = TestItem("item3")
    val item2Copy = TestItem("item2") // Same content but different instance

    list.add(item1)
    list.add(item2)
    list.add(item3)

    // Should remove exact instance
    assert(list.remove(item2))
    assertEquals(list.getSize, 2)

    // Should not remove copy with same content
    assert(!list.remove(item2Copy))
    assertEquals(list.getSize, 2)

    assertEquals(list.get(0), item1)
    assertEquals(list.get(1), item3)
  }

  test("FastList should remove last element efficiently") {
    val list = new FastList[String]()

    list.add("first")
    list.add("second")
    list.add("third")

    assertEquals(list.removeLast(), "third")
    assertEquals(list.getSize, 2)

    assertEquals(list.removeLast(), "second")
    assertEquals(list.getSize, 1)

    assertEquals(list.removeLast(), "first")
    assertEquals(list.getSize, 0)
    assert(list.isEmpty)
  }

  test("FastList should throw exception when removing from empty list") {
    val list = new FastList[String]()

    intercept[NoSuchElementException] {
      list.removeLast()
    }
  }

  test("FastList should handle removeAt correctly") {
    val list = new FastList[String]()

    list.add("first")
    list.add("second")
    list.add("third")
    list.add("fourth")

    assertEquals(list.removeAt(1), "second")
    assertEquals(list.getSize, 3)
    assertEquals(list.get(0), "first")
    assertEquals(list.get(1), "third")
    assertEquals(list.get(2), "fourth")
  }

  test("FastList should clear all elements") {
    val list = new FastList[String]()

    list.add("first")
    list.add("second")
    list.add("third")

    list.clear()

    assertEquals(list.getSize, 0)
    assert(list.isEmpty)
  }

  test("FastList should support foreach iteration") {
    val list = new FastList[Int]()

    (1 to 5).foreach(list.add)

    var sum = 0
    list.foreach(sum += _)

    assertEquals(sum, 15)
  }

  test("FastList should provide iterator") {
    val list = new FastList[String]()

    list.add("a")
    list.add("b")
    list.add("c")

    val collected = list.iterator.toList
    assertEquals(collected, List("a", "b", "c"))
  }

  test("FastList should convert to List") {
    val list = new FastList[Int]()

    (1 to 5).foreach(list.add)

    assertEquals(list.toList, List(1, 2, 3, 4, 5))
  }

  test("FastList should handle empty state correctly") {
    val list = new FastList[String]()

    assert(list.isEmpty)
    assert(!list.nonEmpty)
    assertEquals(list.getSize, 0)
    assertEquals(list.toList, List.empty)
  }

  test("FastList should handle large number of elements") {
    val list  = new FastList[Int](initialCapacity = 8)
    val count = 1000

    (1 to count).foreach(list.add)

    assertEquals(list.getSize, count)

    // Verify all elements are accessible
    val allValid = (0 until count).forall(i => list.get(i) == i + 1)
    assert(allValid)
  }

  test("FastList should maintain order after multiple removes") {
    val list = new FastList[String]()

    val elements = List("a", "b", "c", "d", "e", "f")
    elements.foreach(list.add)

    list.remove("b")
    list.remove("d")
    list.remove("f")

    assertEquals(list.toList, List("a", "c", "e"))
  }

  test("FastList toString should format correctly") {
    val emptyList = new FastList[String]()
    assertEquals(emptyList.toString, "FastList()")

    val list = new FastList[Int]()
    list.add(1)
    list.add(2)
    list.add(3)

    assertEquals(list.toString, "FastList(1, 2, 3)")
  }

  // Helper case class for identity testing
  case class TestItem(value: String)
