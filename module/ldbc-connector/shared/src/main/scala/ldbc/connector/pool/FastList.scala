/**
 * Copyright (c) 2023-2025 by Takahiko Tominaga
 * This software is licensed under the MIT License (MIT).
 * For more information see LICENSE or https://opensource.org/licenses/MIT
 */

package ldbc.connector.pool

import scala.reflect.ClassTag

/**
 * A specialized list implementation optimized for minimal allocations and maximum performance.
 * 
 * Inspired by HikariCP's FastList, this implementation provides:
 * - No range checking for get operations
 * - Identity-based removal using eq instead of ==
 * - Optimized add operation using exception-based control flow
 * - Direct array access without abstraction overhead
 * 
 * This is specifically designed for tracking Statement objects in pooled connections,
 * where we expect:
 * - Relatively small lists (< 32 elements in 99% of cases)
 * - Frequent add/remove operations
 * - Identity-based equality checks
 * - No concurrent access (used within a single connection)
 * 
 * @tparam T the element type (must have a ClassTag for array creation)
 */
final class FastList[T: ClassTag](initialCapacity: Int = 32):
  private var elementData: Array[T] = new Array[T](initialCapacity)
  private var size:        Int      = 0

  /**
   * Get an element at the specified index without bounds checking.
   * 
   * WARNING: This will throw ArrayIndexOutOfBoundsException if index is invalid.
   * The caller must ensure the index is valid.
   * 
   * @param index the index of the element
   * @return the element at the specified index
   */
  def get(index: Int): T = elementData(index)

  /**
   * Add an element to the end of the list.
   * 
   * Uses exception-based control flow for performance:
   * - In the common case (array has space), this is a simple array write
   * - Only when the array is full do we pay the cost of exception handling
   * 
   * @param element the element to add
   * @return true (always succeeds)
   */
  def add(element: T): Boolean =
    try {
      elementData(size) = element
      size += 1
      true
    } catch {
      case _: ArrayIndexOutOfBoundsException =>
        // Overflow-conscious code: double the capacity
        val oldCapacity    = elementData.length
        val newCapacity    = oldCapacity << 1
        val newElementData = new Array[T](newCapacity)
        System.arraycopy(elementData, 0, newElementData, 0, oldCapacity)
        newElementData(size) = element
        size += 1
        elementData = newElementData
        true
    }

  /**
   * Remove an element using identity comparison (eq).
   * 
   * Searches from the end of the list for better performance when removing
   * recently added elements (common pattern for Statement tracking).
   * 
   * @param element the element to remove
   * @return true if the element was found and removed, false otherwise
   */
  def remove(element: T): Boolean =
    var index = size - 1
    while index >= 0 do {
      if elementData(index).asInstanceOf[AnyRef] eq element.asInstanceOf[AnyRef] then {
        val numMoved = size - index - 1
        if numMoved > 0 then {
          System.arraycopy(elementData, index + 1, elementData, index, numMoved)
        }
        size -= 1
        elementData(size) = null.asInstanceOf[T] // Help GC
        return true
      }
      index -= 1
    }
    false

  /**
   * Remove and return the last element in the list.
   * 
   * @return the last element
   * @throws NoSuchElementException if the list is empty
   */
  def removeLast(): T =
    if size == 0 then throw new NoSuchElementException("FastList is empty")
    size -= 1
    val element = elementData(size)
    elementData(size) = null.asInstanceOf[T] // Help GC
    element

  /**
   * Remove the element at the specified index.
   * 
   * @param index the index of the element to remove
   * @return the removed element
   */
  def removeAt(index: Int): T =
    val element  = elementData(index)
    val numMoved = size - index - 1
    if numMoved > 0 then {
      System.arraycopy(elementData, index + 1, elementData, index, numMoved)
    }
    size -= 1
    elementData(size) = null.asInstanceOf[T] // Help GC
    element

  /**
   * Clear all elements from the list.
   */
  def clear(): Unit =
    // Help GC by nulling references
    var i = 0
    while i < size do {
      elementData(i) = null.asInstanceOf[T]
      i += 1
    }
    size = 0

  /**
   * Get the current size of the list.
   * 
   * @return the number of elements in the list
   */
  def getSize: Int = size

  /**
   * Check if the list is empty.
   * 
   * @return true if the list contains no elements
   */
  def isEmpty: Boolean = size == 0

  /**
   * Check if the list is not empty.
   * 
   * @return true if the list contains at least one element
   */
  def nonEmpty: Boolean = size > 0

  /**
   * Apply a function to each element in the list.
   * 
   * @param f the function to apply
   */
  def foreach(f: T => Unit): Unit =
    var i = 0
    while i < size do {
      f(elementData(i))
      i += 1
    }

  /**
   * Create an iterator over the elements.
   * Note: This allocates an Iterator object, use foreach when possible.
   * 
   * @return an iterator over the elements
   */
  def iterator: Iterator[T] = new Iterator[T] {
    private var index = 0

    def hasNext: Boolean = index < FastList.this.size

    def next(): T = {
      if !hasNext then throw new NoSuchElementException
      val element = elementData(index)
      index += 1
      element
    }
  }

  /**
   * Convert to a standard List.
   * Note: This allocates a new List, use only when necessary.
   * 
   * @return a List containing all elements
   */
  def toList: List[T] = {
    var result = List.empty[T]
    var i      = size - 1
    while i >= 0 do {
      result = elementData(i) :: result
      i -= 1
    }
    result
  }

  /**
   * Get a string representation of the list.
   * 
   * @return a string representation
   */
  override def toString: String = {
    if size == 0 then return "FastList()"

    val sb = new StringBuilder("FastList(")
    var i  = 0
    while i < size - 1 do {
      sb.append(elementData(i)).append(", ")
      i += 1
    }
    sb.append(elementData(i)).append(")")
    sb.toString
  }
